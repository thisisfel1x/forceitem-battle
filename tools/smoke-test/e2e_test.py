#!/usr/bin/env python3
"""
End-to-end test with a real game client: Paper server with the plugin + the resource pack served over local
HTTP + the Fabric client test of tools/icon-exporter (ServerEndToEndGameTest), which joins the server.

    python3 tools/smoke-test/e2e_test.py --minecraft 26.3 \\
        --plugin build/libs/forceitem-battle-1.1.0.jar --pack build/libs/forceitem-battle-pack-1.1.0.zip

A game needs two players, so two clients join (Player0 and Player1, two Gradle runs). Both accept the resource
pack, the script starts a game from the server console once both are in, and the primary client checks that the
boss bar shows the item icon sprite from the pack (not the "missing texture"). Screenshots of lobby and boss bar:
tools/icon-exporter/build/e2e-test. Locally two game windows open - they close themselves when done. The client has to match the Minecraft version of
tools/icon-exporter (the newest supported version). Accepts the Minecraft EULA (https://aka.ms/MinecraftEULA).
In CI the script runs inside xvfb-run (the client needs a display).
"""

import argparse
import http.server
import os
import re
import subprocess
import sys
import threading
import zipfile
from pathlib import Path

import paper

ROOT = Path(__file__).resolve().parents[2]
EXPORTER_DIR = ROOT / "tools" / "icon-exporter"


def serve_pack(pack_file, port):
    """Serves the pack as http://127.0.0.1:<port>/pack.zip in a background thread."""

    class Handler(http.server.SimpleHTTPRequestHandler):
        def translate_path(self, path):
            return str(pack_file) if path.split("?")[0] == "/pack.zip" else "/nonexistent"

        def log_message(self, format, *args):
            print(f"[pack-http] {format % args}", flush=True)

    server = http.server.ThreadingHTTPServer(("127.0.0.1", port), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def write_plugin_config(server_dir, plugin_jar, pack_url, pack_sha1):
    """The default config of the jar, with the resource pack pointing to the local HTTP server."""
    with zipfile.ZipFile(plugin_jar) as jar:
        config = jar.read("config.yml").decode("utf-8")
    config = re.sub(r'^(\s*)url: ".*"$', rf'\1url: "{pack_url}"', config, count=1, flags=re.MULTILINE)
    config = re.sub(r'^(\s*)sha1: ".*"$', rf'\1sha1: "{pack_sha1}"', config, count=1, flags=re.MULTILINE)
    target = server_dir / "plugins" / paper.PLUGIN_NAME / "config.yml"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(config, encoding="utf-8")


# Logged by the server for every joining player (the plugin replaces the "joined the game" message)
JOIN_LINE = "logged in with entity id"


class Client:
    """A Fabric client test run (ServerEndToEndGameTest) - its output is printed with a prefix and kept."""

    def __init__(self, role, address):
        self.role = role
        self.lines = []
        self.exit_code = None
        self.process = subprocess.Popen(
            ["sh", str(ROOT / "gradlew"), "-p", str(EXPORTER_DIR), "runClientGameTest", "--no-daemon",
             "-PclientTest=e2e", f"-Pe2eRole={role}", f"-Pe2eServer={address}"],
            cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
        )
        self._reader = threading.Thread(target=self._read_output, daemon=True)
        self._reader.start()

    def _read_output(self):
        for line in self.process.stdout:
            line = line.rstrip("\n")
            self.lines.append(line)
            print(f"[{self.role}] {line}", flush=True)

    def wait(self, timeout):
        try:
            self.exit_code = self.process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            self.exit_code = "timeout"
        self._reader.join(timeout=10)

    def kill(self):
        if self.process.poll() is None:
            self.process.kill()
            self.process.wait()

    def problems(self):
        # A crashed client can still end with a successful Gradle run - the explicit pass message counts
        passed = any(f"End-to-end test passed ({self.role} client)" in line for line in self.lines)
        if self.exit_code == 0 and passed:
            return []
        failure = next((line.strip() for line in self.lines if "AssertionError" in line or "Exception:" in line), None)
        return [f"{self.role} client failed (exit code {self.exit_code})" + (f": {failure}" if failure else "")]


def exporter_minecraft_version():
    text = (EXPORTER_DIR / "gradle.properties").read_text(encoding="utf-8")
    return re.search(r"^minecraft_version=(.*)$", text, re.MULTILINE).group(1).strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--minecraft", help="Server version (default: the client version of tools/icon-exporter)")
    parser.add_argument("--plugin", required=True, type=Path, help="Plugin jar")
    parser.add_argument("--pack", required=True, type=Path, help="Resource pack zip")
    parser.add_argument("--workdir", type=Path, default=Path("build/e2e-test"), help="Working directory")
    parser.add_argument("--java", default=os.environ.get("SMOKE_TEST_JAVA", "java"), help="Java for the server")
    parser.add_argument("--timeout", type=int, default=900, help="Seconds for the whole client test")
    args = parser.parse_args()

    for file in (args.plugin, args.pack):
        if not file.is_file():
            raise SystemExit(f"Not found: {file}")

    minecraft = args.minecraft or exporter_minecraft_version()
    if minecraft != exporter_minecraft_version():
        raise SystemExit(f"The client (tools/icon-exporter) is Minecraft {exporter_minecraft_version()}, "
                         f"the server would be {minecraft}")

    pack_port = paper.free_port()
    pack_server = serve_pack(args.pack.resolve(), pack_port)

    paper_jar = paper.download_paper(minecraft, args.workdir / "cache")
    server_dir = args.workdir / minecraft
    server_port = paper.free_port()
    paper.prepare_server(server_dir, args.plugin, server_port)
    write_plugin_config(server_dir, args.plugin, f"http://127.0.0.1:{pack_port}/pack.zip",
                        paper.file_hash(args.pack, "sha1"))

    server = paper.PaperServer(server_dir, paper_jar, args.java)
    clients = []
    try:
        if not server.wait_until_started(600):
            raise SystemExit("Server did not start")

        address = f"127.0.0.1:{server_port}"
        clients.append(Client("primary", address))
        # A game needs two players. The second client starts once the first one is in (the first Gradle run has
        # prepared everything by then, so the two runs do not get in each other's way)
        if server.wait_for(lambda line: JOIN_LINE in line, 600):
            clients.append(Client("secondary", address))

            # Start a game as soon as both clients joined: 1 minute, no jokers
            def start_game():
                if server.wait_for_count(lambda line: JOIN_LINE in line, 2, args.timeout):
                    server.command("start 1 0")

            threading.Thread(target=start_game, daemon=True).start()

        for client in clients:
            client.wait(args.timeout)
    finally:
        for client in clients:
            client.kill()
        server.stop()
        server.write_log(args.workdir / f"{minecraft}.log")
        pack_server.shutdown()

    problems = []
    if len(clients) < 2:
        problems.append("The primary client did not join the server")
    for client in clients:
        problems += client.problems()
    problems += server.problems()

    print()
    if problems:
        print(f"End-to-end test FAILED for Minecraft {minecraft}:")
        for problem in problems:
            print(f"  - {problem}")
        sys.exit(1)
    print(f"End-to-end test passed for Minecraft {minecraft} - screenshots in tools/icon-exporter/build/e2e-test")


if __name__ == "__main__":
    main()
