# Starts the built web app in headless Chromium and fails if it doesn't start. Android checks and
# tests can't see errors that only happen in the browser, like a library update that no longer
# matches the graphics engine (skiko).
# Usage: python smoke_test.py sharedApp/build/dist/wasmJs/productionExecutable
import functools
import http.server
import sys
import threading

from playwright.sync_api import sync_playwright

# How long the app gets to start: optimized WebAssembly on a slow runner takes a few seconds
WAIT_MS = 15_000
# Messages that are known to be harmless
IGNORED = ["Accessing `memory` via `wasmExports` is deprecated"]


class Handler(http.server.SimpleHTTPRequestHandler):
    # Browsers only run WebAssembly and modules sent with the right type, whatever the system knows
    extensions_map = {
        **http.server.SimpleHTTPRequestHandler.extensions_map,
        ".wasm": "application/wasm",
        ".mjs": "text/javascript",
        ".js": "text/javascript",
    }

    def log_message(self, *args):
        pass


def main(directory):
    server = http.server.ThreadingHTTPServer(
        ("127.0.0.1", 0), functools.partial(Handler, directory=directory)
    )
    threading.Thread(target=server.serve_forever, daemon=True).start()
    url = f"http://127.0.0.1:{server.server_port}/"

    problems = []
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.on("pageerror", lambda e: problems.append(f"Page error: {e}"))
        # Shown for diagnosis, the error box below catches the ones that matter
        page.on(
            "console",
            lambda m: m.type == "error"
            and not any(i in m.text for i in IGNORED)
            and print(f"Console error: {m.text}"),
        )
        page.goto(url)
        page.wait_for_timeout(WAIT_MS)

        # index.html shows what went wrong in this box
        box = page.locator(".start-error")
        if box.count():
            problems.append(f"Error box: {box.inner_text()}")
        # Compose draws on a canvas in a shadow root, locators look inside it
        if not page.locator("canvas").count():
            problems.append(f"The app didn't draw anything in {WAIT_MS // 1000} s")
        browser.close()
    server.shutdown()

    problems = [p for p in problems if not any(i in p for i in IGNORED)]
    for problem in problems:
        print(problem)
    if problems:
        sys.exit(1)
    print("The web app started")


if __name__ == "__main__":
    main(sys.argv[1])
