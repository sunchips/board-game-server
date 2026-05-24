#!/usr/bin/env python3
"""GitHub webhook receiver — fires deploy.sh on push to main.

Listens on 127.0.0.1; nginx terminates TLS and proxies
`/webhook/github` here. Validates the X-Hub-Signature-256 HMAC
against a shared secret, then spawns deploy.sh in the background
and returns 202 so GitHub doesn't time out.

The timer-based poll stays installed as a safety net for missed
webhooks (GitHub outage, transient failure, restart races).

Env:
  WEBHOOK_SECRET_FILE   path to file containing the shared secret
  DEPLOY_SCRIPT         absolute path to deploy.sh
  WEBHOOK_PORT          port to listen on (default 9001)
"""
import hashlib
import hmac
import os
import subprocess
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

with open(os.environ["WEBHOOK_SECRET_FILE"]) as f:
    SECRET = f.read().strip().encode()
DEPLOY_SCRIPT = os.environ["DEPLOY_SCRIPT"]
PORT = int(os.environ.get("WEBHOOK_PORT", "9001"))


class Handler(BaseHTTPRequestHandler):
    server_version = "BoardGameWebhook/1"

    def do_GET(self):
        if self.path == "/health":
            return self._send(200, "ok")
        return self._send(404, "not found")

    def do_POST(self):
        if self.path != "/hooks/deploy":
            return self._send(404, "not found")
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        sig = self.headers.get("X-Hub-Signature-256", "")
        expected = "sha256=" + hmac.new(SECRET, body, hashlib.sha256).hexdigest()
        if not hmac.compare_digest(sig, expected):
            return self._send(401, "bad signature")
        event = self.headers.get("X-GitHub-Event", "")
        # GitHub fires a `ping` on webhook creation — answer it so the UI
        # shows a green check.
        if event == "ping":
            return self._send(200, "pong")
        if event != "push":
            return self._send(204, "")
        # Fire-and-forget. stdout/stderr inherit from this process so the
        # deploy logs land in the same journald stream as the receiver.
        subprocess.Popen([DEPLOY_SCRIPT], stdout=sys.stdout, stderr=sys.stderr)
        return self._send(202, "queued")

    def _send(self, code, msg):
        body = msg.encode() if msg else b""
        self.send_response(code)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)

    def log_message(self, fmt, *args):
        sys.stderr.write(f"{self.address_string()} {fmt % args}\n")
        sys.stderr.flush()


if __name__ == "__main__":
    HTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
