#!/usr/bin/env python3
"""
Detects new Minecraft versions with a stable Paper build and prepares the repository for them.
Used by .github/workflows/new-minecraft-version.yml, works locally as well:

    python3 tools/minecraft-update/minecraft_update.py detect
        Prints the new versions (newer than the last entry of supportedVersions, with a STABLE Paper build).

    python3 tools/minecraft-update/minecraft_update.py apply 26.4 26.4.1
        - gradle.properties: appends the versions to supportedVersions, raises resourcePackMaxFormat,
          bumps the patch version if the current version was already released (git tag v<version> exists)
        - tools/icon-exporter/gradle.properties: Minecraft, Fabric API, loader and Loom version for the newest
          version - only if Fabric already supports it
        Writes a Markdown summary (--summary) for the pull request.

In GitHub Actions the results are additionally written to $GITHUB_OUTPUT.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ElementTree
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PLUGIN_PROPERTIES = ROOT / "gradle.properties"
EXPORTER_PROPERTIES = ROOT / "tools" / "icon-exporter" / "gradle.properties"

PAPER_API = "https://fill.papermc.io/v3/projects/paper"
MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_META = "https://meta.fabricmc.net/v2"
FABRIC_MAVEN = "https://maven.fabricmc.net"
USER_AGENT = "paper-plugin-version-check/1.0"

# Highest Java version the build (toolchain) and gradle/minecraft-versions.gradle know about
SUPPORTED_JAVA = 25

RELEASE_VERSION = re.compile(r"^\d+(\.\d+)+$")


# ----------------------------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------------------------

def http_get(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def http_json(url):
    return json.loads(http_get(url))


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def read_property(path, key):
    match = re.search(rf"^{re.escape(key)}=(.*)$", path.read_text(encoding="utf-8"), re.MULTILINE)
    if not match:
        raise SystemExit(f"{path}: property {key} not found")
    return match.group(1).strip()


def write_property(path, key, value):
    text = path.read_text(encoding="utf-8")
    new_text, count = re.subn(rf"^{re.escape(key)}=.*$", f"{key}={value}", text, flags=re.MULTILINE)
    if count != 1:
        raise SystemExit(f"{path}: property {key} not found")
    path.write_text(new_text, encoding="utf-8")


def set_output(name, value):
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as file:
            file.write(f"{name}={value}\n")


def supported_versions():
    return [v.strip() for v in read_property(PLUGIN_PROPERTIES, "supportedVersions").split(",") if v.strip()]


# ----------------------------------------------------------------------------------------------
# detect
# ----------------------------------------------------------------------------------------------

def has_stable_paper_build(version):
    builds = http_json(f"{PAPER_API}/versions/{version}/builds")
    return any(build.get("channel") == "STABLE" for build in builds)


def detect():
    latest_supported = supported_versions()[-1]
    project = http_json(PAPER_API)

    candidates = sorted(
        {version for family in project["versions"].values() for version in family
         if RELEASE_VERSION.match(version) and version_key(version) > version_key(latest_supported)},
        key=version_key,
    )
    new_versions = [version for version in candidates if has_stable_paper_build(version)]

    for version in candidates:
        if version not in new_versions:
            print(f"Paper {version} has no stable build yet", file=sys.stderr)

    print(" ".join(new_versions))
    set_output("versions", " ".join(new_versions))
    set_output("latest", new_versions[-1] if new_versions else "")


# ----------------------------------------------------------------------------------------------
# apply
# ----------------------------------------------------------------------------------------------

def mojang_version_info(version):
    """pack_version and java_version from the version.json inside the client jar."""
    manifest = http_json(MOJANG_MANIFEST)
    entry = next((v for v in manifest["versions"] if v["id"] == version), None)
    if entry is None:
        raise SystemExit(f"Minecraft {version} is not in the Mojang version manifest")

    client_url = http_json(entry["url"])["downloads"]["client"]["url"]
    with tempfile.TemporaryDirectory() as directory:
        jar = Path(directory) / "client.jar"
        jar.write_bytes(http_get(client_url))
        with zipfile.ZipFile(jar) as archive:
            info = json.loads(archive.read("version.json"))
    return {
        "resource_format": int(info["pack_version"]["resource_major"]),
        "java": int(info["java_version"]),
    }


def latest_maven_version(group_path, artifact, predicate):
    metadata = ElementTree.fromstring(http_get(f"{FABRIC_MAVEN}/{group_path}/{artifact}/maven-metadata.xml"))
    versions = [element.text for element in metadata.iter("version") if predicate(element.text)]
    return versions[-1] if versions else None


def fabric_versions(minecraft_version):
    """Fabric API / loader / Loom for the exporter, or None if Fabric does not support the version yet."""
    game_versions = http_json(f"{FABRIC_META}/versions/game")
    if not any(v["version"] == minecraft_version for v in game_versions):
        return None

    fabric_api = latest_maven_version("net/fabricmc/fabric-api", "fabric-api",
                                      lambda v: v.endswith(f"+{minecraft_version}"))
    if fabric_api is None:
        return None

    loader = next(v["version"] for v in http_json(f"{FABRIC_META}/versions/loader") if v["stable"])
    # Newest Loom release (no alpha/beta) - new Minecraft versions often need a newer Loom
    loom = latest_maven_version("net/fabricmc", "fabric-loom", lambda v: RELEASE_VERSION.match(v) is not None)
    return {"fabric_api": fabric_api, "loader": loader, "loom": loom}


def tag_exists(tag):
    result = subprocess.run(["git", "tag", "--list", tag], cwd=ROOT, capture_output=True, text=True, check=True)
    return result.stdout.strip() != ""


def apply(new_versions, summary_file):
    supported = supported_versions()
    new_versions = sorted({v for v in new_versions if v not in supported}, key=version_key)
    if not new_versions:
        raise SystemExit("No new versions given")
    if version_key(new_versions[0]) <= version_key(supported[-1]):
        raise SystemExit(f"New versions must be newer than {supported[-1]}")

    latest = new_versions[-1]
    summary = []
    warnings = []

    # Resource pack format and Java version of every new version
    infos = {version: mojang_version_info(version) for version in new_versions}
    max_format = max([int(read_property(PLUGIN_PROPERTIES, "resourcePackMaxFormat"))]
                     + [info["resource_format"] for info in infos.values()])

    for version, info in infos.items():
        summary.append(f"| {version} | {info['resource_format']} | {info['java']} |")
        if info["java"] > SUPPORTED_JAVA:
            warnings.append(f"Minecraft {version} needs Java {info['java']} - the build only knows Java "
                            f"{SUPPORTED_JAVA}. Update the toolchain, the workflows and gradle/minecraft-versions.gradle.")

    write_property(PLUGIN_PROPERTIES, "supportedVersions", ",".join(supported + new_versions))
    write_property(PLUGIN_PROPERTIES, "resourcePackMaxFormat", str(max_format))

    # A new release is needed (new item icons, new pack hash) - unless the current version is still unreleased
    version = read_property(PLUGIN_PROPERTIES, "version")
    if tag_exists(f"v{version}"):
        major, minor, patch = (re.match(r"^(\d+)\.(\d+)\.(\d+)", version).groups())
        new_version = f"{major}.{minor}.{int(patch) + 1}"
        write_property(PLUGIN_PROPERTIES, "version", new_version)
    else:
        new_version = version

    # Icon exporter
    fabric = fabric_versions(latest)
    if fabric:
        write_property(EXPORTER_PROPERTIES, "minecraft_version", latest)
        write_property(EXPORTER_PROPERTIES, "fabric_api_version", fabric["fabric_api"])
        write_property(EXPORTER_PROPERTIES, "loader_version", fabric["loader"])
        if fabric["loom"] and version_key(fabric["loom"]) > version_key(read_property(EXPORTER_PROPERTIES, "loom_version")):
            write_property(EXPORTER_PROPERTIES, "loom_version", fabric["loom"])
    else:
        warnings.append(f"Fabric does not support Minecraft {latest} yet - the item icons were not regenerated. "
                        f"Run the workflow again later or update tools/icon-exporter manually.")

    text = [
        f"Adds support for Minecraft {', '.join(new_versions)}.",
        "",
        "| Minecraft | Resource pack format | Java |",
        "|---|---|---|",
        *summary,
        "",
        f"- `supportedVersions`: {', '.join(supported + new_versions)}",
        f"- `resourcePackMaxFormat`: {max_format}",
        f"- Plugin version: {new_version}",
    ]
    if fabric:
        text.append(f"- Icon exporter: Minecraft {latest}, Fabric API {fabric['fabric_api']}, "
                    f"loader {fabric['loader']}, Loom {read_property(EXPORTER_PROPERTIES, 'loom_version')}")
    if warnings:
        text += ["", "**Warnings**", "", *[f"- {warning}" for warning in warnings]]

    Path(summary_file).write_text("\n".join(text) + "\n", encoding="utf-8")
    print("\n".join(text))

    set_output("latest", latest)
    set_output("version", new_version)
    set_output("fabric", "true" if fabric else "false")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("detect", help="print new Minecraft versions with a stable Paper build")
    apply_parser = commands.add_parser("apply", help="add Minecraft versions to the build")
    apply_parser.add_argument("versions", nargs="+")
    apply_parser.add_argument("--summary", default="build/minecraft-update.md", help="Markdown summary file")
    args = parser.parse_args()

    if args.command == "detect":
        detect()
    else:
        Path(args.summary).parent.mkdir(parents=True, exist_ok=True)
        apply(args.versions, args.summary)


if __name__ == "__main__":
    main()
