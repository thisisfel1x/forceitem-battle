#!/usr/bin/env python3
"""
Smoke test: starts a real Paper server of the given Minecraft version with the plugin, waits until the
server is up, stops it and checks the log. Accepts the Minecraft EULA (https://aka.ms/MinecraftEULA)

    python3 tools/smoke-test/smoke_test.py --minecraft 26.3 --plugin build/libs/forceitem-battle-1.1.0.jar

Fails if the plugin is not enabled, if an error or exception is logged, if the scoreboard library has no
packet adapter for this server version (it accesses server internals and is the most version dependent part) or if
the biome size data pack of the plugin (default config: world.biome-size SMALL) is not enabled.
Needs Python 3.9+ and Java (21 for 1.21.x, 25 for 26.x) on the PATH or via --java
"""

import argparse
import os
import sys
import time
from pathlib import Path

import paper

DATAPACK_ID = "biome_size"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--minecraft", required=True, help="Minecraft version, e.g. 26.3")
    parser.add_argument("--plugin", required=True, type=Path, help="Plugin jar")
    parser.add_argument("--workdir", type=Path, default=Path("build/smoke-test"), help="Working directory")
    parser.add_argument("--java", default=os.environ.get("SMOKE_TEST_JAVA", "java"), help="Java executable")
    parser.add_argument("--timeout", type=int, default=600, help="Seconds until the server has to be started")
    args = parser.parse_args()

    if not args.plugin.is_file():
        raise SystemExit(f"Plugin jar not found: {args.plugin}")

    paper_jar = paper.download_paper(args.minecraft, args.workdir / "cache")
    server_dir = args.workdir / args.minecraft
    paper.prepare_server(server_dir, args.plugin, paper.free_port())

    server = paper.PaperServer(server_dir, paper_jar, args.java)
    datapack_enabled = False
    if server.wait_until_started(args.timeout):
        # Registered by the plugin bootstrap before the world is loaded - the pack format range has to fit
        server.command("datapack list enabled")
        datapack_enabled = server.wait_for(lambda line: "data pack" in line and DATAPACK_ID in line, 30)
        # Let scheduled startup tasks of the plugin run before stopping
        time.sleep(5)
    exit_code = server.stop()
    server.write_log(args.workdir / f"{args.minecraft}.log")

    problems = server.problems()
    if server.started and not datapack_enabled:
        problems.append(f"The biome size data pack ({DATAPACK_ID}) is not enabled (see 'datapack list enabled')")
    print()
    if problems:
        print(f"Smoke test FAILED for Minecraft {args.minecraft} (exit code {exit_code}):")
        for problem in problems:
            print(f"  - {problem}")
        sys.exit(1)

    pool = next((line.split("]: ", 1)[-1] for line in server.lines if "items in the pool" in line), "?")
    print(f"Smoke test passed for Minecraft {args.minecraft}: {pool}")


if __name__ == "__main__":
    main()
