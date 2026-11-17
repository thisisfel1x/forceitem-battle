"""
Shared helpers for smoke_test.py and e2e_test.py: downloads Paper, prepares a server directory (accepts the
Minecraft EULA - https://aka.ms/MinecraftEULA), runs the server and checks its log.
"""

import hashlib
import json
import shutil
import socket
import subprocess
import threading
import time
import urllib.request
from pathlib import Path

FILL_API = "https://fill.papermc.io/v3/projects/paper"
USER_AGENT = "paper-plugin-smoke-test/1.0"
PLUGIN_NAME = "forceitem-battle"

# Log lines that fail a test (checked case sensitive)
FAILURE_MARKERS = [
    "/ERROR]",
    "Exception",
    "Error occurred while enabling",
    "Could not load plugin",
    "No scoreboard packet adapter available",
]
# Known harmless lines that contain a failure marker (none so far)
IGNORED = []


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def file_hash(path, algorithm="sha256"):
    digest = hashlib.new(algorithm)
    with open(path, "rb") as file:
        for chunk in iter(lambda: file.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download_paper(minecraft_version, cache_dir):
    build = fetch_json(f"{FILL_API}/versions/{minecraft_version}/builds/latest")
    download = build["downloads"]["server:default"]
    expected = download["checksums"]["sha256"]
    target = cache_dir / download["name"]

    if target.is_file() and file_hash(target) == expected:
        print(f"Using cached Paper {minecraft_version} build {build['id']}")
        return target

    print(f"Downloading Paper {minecraft_version} build {build['id']} ({build.get('channel', '?')})")
    cache_dir.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(download["url"], headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=300) as response, open(target, "wb") as file:
        shutil.copyfileobj(response, file)
    if file_hash(target) != expected:
        raise SystemExit(f"Checksum mismatch for {target}")
    return target


def free_port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def prepare_server(server_dir, plugin_jar, port):
    if server_dir.exists():
        shutil.rmtree(server_dir)
    (server_dir / "plugins").mkdir(parents=True)
    shutil.copy2(plugin_jar, server_dir / "plugins" / plugin_jar.name)

    (server_dir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (server_dir / "server.properties").write_text(
        "\n".join([
            f"server-port={port}",
            "server-ip=127.0.0.1",
            "online-mode=false",
            # Newer Paper versions enable the whitelist by default - the end-to-end test client has to join
            "white-list=false",
            "enforce-whitelist=false",
            "level-type=minecraft\\:flat",
            "generate-structures=false",
            "spawn-protection=0",
            "view-distance=4",
            "simulation-distance=4",
            "",
        ]),
        encoding="utf-8",
    )


class PaperServer:
    """A running Paper server; every log line is printed and kept in self.lines."""

    def __init__(self, server_dir, paper_jar, java):
        command = [java, "-Xms1G", "-Xmx2G", "-jar", str(Path(paper_jar).resolve()), "--nogui"]
        print("$ " + " ".join(command), flush=True)
        self.process = subprocess.Popen(
            command, cwd=server_dir, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace", bufsize=1,
        )
        self.lines = []
        self.started = False
        self._lock = threading.Lock()
        self._reader = threading.Thread(target=self._read_output, daemon=True)
        self._reader.start()

    def _read_output(self):
        for line in self.process.stdout:
            line = line.rstrip("\n")
            with self._lock:
                self.lines.append(line)
            print(f"[server] {line}", flush=True)
            if "Done (" in line and "For help" in line:
                self.started = True

    def wait_for(self, predicate, timeout):
        """Waits until predicate(line) matches a log line (also earlier ones) or the server exits."""
        deadline = time.monotonic() + timeout
        checked = 0
        while time.monotonic() < deadline:
            with self._lock:
                new_lines = self.lines[checked:]
                checked = len(self.lines)
            if any(predicate(line) for line in new_lines):
                return True
            if self.process.poll() is not None:
                return False
            time.sleep(0.25)
        return False

    def wait_for_count(self, predicate, count, timeout):
        """Waits until predicate(line) matched count log lines in total."""
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            with self._lock:
                matches = sum(1 for line in self.lines if predicate(line))
            if matches >= count:
                return True
            if self.process.poll() is not None:
                return False
            time.sleep(0.25)
        return False

    def wait_until_started(self, timeout):
        return self.wait_for(lambda line: "Done (" in line and "For help" in line, timeout)

    def command(self, command):
        try:
            self.process.stdin.write(command + "\n")
            self.process.stdin.flush()
        except (BrokenPipeError, OSError):
            pass

    def stop(self):
        if self.process.poll() is None:
            self.command("stop")
            try:
                self.process.wait(timeout=120)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait()
        self._reader.join(timeout=10)
        return self.process.returncode

    def write_log(self, path):
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        Path(path).write_text("\n".join(self.lines) + "\n", encoding="utf-8")

    def problems(self):
        problems = []
        if not self.started:
            problems.append("Server did not finish starting")
        if not any(f"[{PLUGIN_NAME}] Enabling {PLUGIN_NAME}" in line for line in self.lines):
            problems.append(f"Plugin {PLUGIN_NAME} was not enabled")
        for line in self.lines:
            if any(marker in line for marker in FAILURE_MARKERS) and not any(ignored in line for ignored in IGNORED):
                problems.append(f"Log: {line.strip()}")
        if self.started and not any(f"[{PLUGIN_NAME}] Disabling {PLUGIN_NAME}" in line for line in self.lines):
            problems.append("Server did not shut down cleanly")
        return problems
