"""Signs the release APK built by GitHub (the Release APK workflow) with the key of keystore.properties.

    python docs/sign_apk.py Pascaline-1.9.1-unsigned.apk

Writes Pascaline-1.9.1.apk next to it, ready to attach to the GitHub release. The passwords go from
keystore.properties to apksigner through environment variables: they are never printed or written.
"""

import os
import struct
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    unsigned = Path(sys.argv[1]).resolve()
    signed = unsigned.with_name(unsigned.name.replace("-unsigned", ""))
    props = {}
    for line in (ROOT / "keystore.properties").read_text(encoding="utf-8").splitlines():
        key, sep, value = line.partition("=")
        if sep and not line.lstrip().startswith("#"):
            props[key.strip()] = value.strip()

    sdk = Path(os.environ.get("ANDROID_HOME") or Path(os.environ["LOCALAPPDATA"]) / "Android" / "Sdk")
    build_tools = max((sdk / "build-tools").iterdir(), key=lambda p: [int(n) for n in p.name.split(".") if n.isdigit()])
    apksigner = build_tools / ("apksigner.bat" if os.name == "nt" else "apksigner")

    env = dict(os.environ, PASCALINE_STORE_PASS=props["storePassword"], PASCALINE_KEY_PASS=props["keyPassword"])
    subprocess.run(
        [
            str(apksigner), "sign",
            "--ks", str(ROOT / props["storeFile"]),
            "--ks-key-alias", props["keyAlias"],
            "--ks-pass", "env:PASCALINE_STORE_PASS",
            "--key-pass", "env:PASCALINE_KEY_PASS",
            # Keeps every entry where Gradle put it: F-Droid copies this signature onto its own
            # build of the same commit, which only works if the bytes before the signature match
            "--alignment-preserved",
            "--out", str(signed),
            str(unsigned),
        ],
        env=env,
        check=True,
    )
    result = subprocess.run([str(apksigner), "verify", "--print-certs", str(signed)], capture_output=True, text=True, check=True)

    # Like F-Droid's check: every entry of the unsigned APK must still be there, byte for byte
    before = unsigned.read_bytes()
    eocd = before.rfind(b"PK\x05\x06")
    entries_end = struct.unpack("<I", before[eocd + 16:eocd + 20])[0]
    if signed.read_bytes()[:entries_end] != before[:entries_end]:
        signed.unlink()
        sys.exit("The signed APK moved some entries: F-Droid could not copy its signature. Nothing written.")
    print(signed)
    print("\n".join(line for line in result.stdout.splitlines() if "SHA-256" in line))


if __name__ == "__main__":
    main()
