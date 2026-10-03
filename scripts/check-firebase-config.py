#!/usr/bin/env python3
"""
google-services.json checker — Google Sign-In ready hai ya nahi, wo batata hai.

Chalane ka tarika (repo root se):
    python3 scripts/check-firebase-config.py

Ye 4 cheezein check karta hai:
  1. File maujood hai + valid JSON hai
  2. Package name build.gradle.kts ke applicationId se match karta hai
  3. Firebase me registered SHA-1 fingerprints (certificate_hash) — CI key aur local debug key
  4. oauth_client (client_type 3 = web) — Google Sign-In ke liye zaroori (isi se default_web_client_id banta hai)
"""

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GS_PATH = ROOT / "app" / "google-services.json"
GRADLE_PATH = ROOT / "app" / "build.gradle.kts"

# Known fingerprints (repo ki stable CI key + aam local debug key)
CI_KEY_SHA1 = "3ff9a2d020100dead4d1501dd62eb45deb60906f"
CI_KEY_SHA256 = "909d8ab17d0ccfda8bc145651d51f2655af4f5ff4c45dcbcf9a827cd3d279741"
KNOWN = {
    CI_KEY_SHA1: "CI/download APK ki stable key (showroom-debug.keystore)",
    "f1efa837745366852627fce5070b74c6dad8beb1": "aapke PC ki local debug key",
}

OK, WARN, FAIL = "✅", "⚠️ ", "❌"


def norm(fp: str) -> str:
    return fp.replace(":", "").replace(" ", "").strip().lower()


def main() -> int:
    problems = 0
    print("=" * 68)
    print("Firebase config check — app/google-services.json")
    print("=" * 68)

    # 1) file
    if not GS_PATH.exists():
        print(f"{FAIL} File nahi mili: {GS_PATH}")
        return 1
    try:
        data = json.loads(GS_PATH.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"{FAIL} JSON padha nahi ja saka: {e}")
        return 1
    print(f"{OK} File hai aur valid JSON hai ({GS_PATH.stat().st_size} bytes)")

    project_id = data.get("project_info", {}).get("project_id", "(?)")
    print(f"    project_id : {project_id}")

    # 2) package name
    app_id = None
    if GRADLE_PATH.exists():
        m = re.search(r'applicationId\s*=\s*"([^"]+)"', GRADLE_PATH.read_text(encoding="utf-8"))
        app_id = m.group(1) if m else None
    packages = [
        c.get("client_info", {}).get("android_client_info", {}).get("package_name")
        for c in data.get("client", [])
    ]
    if app_id:
        if app_id in packages:
            print(f"{OK} Package name match: {app_id}")
        else:
            print(f"{FAIL} Package name MISMATCH — build.gradle.kts: {app_id} | json me: {packages}")
            problems += 1
    else:
        print(f"{WARN} applicationId parse nahi hua (build.gradle.kts check karein)")

    # 3) SAH-1 fingerprints + 4) oauth client
    sha1s: list[str] = []
    has_web_oauth = False
    for c in data.get("client", []):
        ci = c.get("client_info", {})
        pkg = ci.get("android_client_info", {}).get("package_name", "?")
        certs = ci.get("certificate_hash", [])
        if isinstance(certs, str):
            certs = [certs]
        for h in certs:
            sha1s.append(norm(h))
        for oc in c.get("oauth_client", []) or []:
            if oc.get("client_type") == 3:
                has_web_oauth = True

    print("\nRegistered SHA-1 (certificate_hash):")
    if not sha1s:
        print(f"{FAIL} Koi SHA-1 nahi hai! Firebase Console → Project settings → aapka app → Add fingerprint")
        problems += 1
    for h in sha1s:
        pretty = ":".join(h[i : i + 2] for i in range(0, len(h), 2)).upper()
        note = KNOWN.get(h)
        if note:
            print(f"  {OK} {pretty}  <- {note}")
        else:
            print(f"  {WARN} {pretty}  (unknown/fresh key)")
    missing = [k for k in KNOWN if k not in sha1s]
    for k in missing:
        pretty = ":".join(k[i : i + 2] for i in range(0, len(k), 2)).upper()
        print(f"  {WARN} MISSING: {pretty}  ({KNOWN[k]})")

    print("\nGoogle Sign-In ke liye oauth_client (client_type 3):")
    if has_web_oauth:
        print(f"  {OK} Web OAuth client maujood hai — Google Sign-In chalega")
    else:
        print(f"  {FAIL} Web OAuth client NAHI hai — Google sign-in button kaam nahi karega")
        print("      Fix: Firebase Console → Authentication → Google provider ENABLE karein,")
        print("           phir Project settings → General → naya google-services.json download karein.")
        problems += 1

    print("\n" + "=" * 68)
    if problems == 0:
        print("Natija: sab theek — Google Sign-In ready hai ✅")
    else:
        print(f"Natija: {problems} problem mili — upar ke fix karein ⚠️")
        print("(Email/Password login in sab ke bina bhi chalta hai.)")
    print(f"CI/download APK key SHA-256: {CI_KEY_SHA256}")
    print("=" * 68)
    return 0


if __name__ == "__main__":
    sys.exit(main())
