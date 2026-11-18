"""Run the full device suite, supplying test credentials without logging them."""
import os
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[1]
email = os.environ.get("NUTRISCAN_TEST_EMAIL")
password = os.environ.get("NUTRISCAN_TEST_PASSWORD")
command = [str(root / "gradlew.bat"), ":app:connectedDebugAndroidTest", "--max-workers=1", "--console=plain"]
if email and password:
    command += [f"-Pandroid.testInstrumentationRunnerArguments.testEmail={email}",
                f"-Pandroid.testInstrumentationRunnerArguments.testPassword={password}"]
log = root / "app/build/milestone05/device-tests.log"
log.parent.mkdir(parents=True, exist_ok=True)
process = subprocess.Popen(command, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                           encoding="utf-8", errors="replace")
with log.open("w", encoding="utf-8") as output:
    for line in process.stdout:
        if password:
            line = line.replace(password, "<redacted>")
        output.write(line)
        print(line, end="", flush=True)
raise SystemExit(process.wait())
