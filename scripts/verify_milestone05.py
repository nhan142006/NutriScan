"""ADB evidence collection for the real APK; credentials come only from environment.

The performance dataset is inserted by Milestone05PerformanceTest, never bundled in
the shipping app. Keep results under submission/evidence, outside Git.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = Path(os.environ.get("ANDROID_HOME", r"C:\Users\Admin\AppData\Local\Android\Sdk")) / "platform-tools/adb.exe"
PACKAGE = "vn.edu.vlu.milestone2"
SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5554")


def adb(*args: str, binary: bool = False, timeout: int = 40):
    result = subprocess.run([str(ADB), "-s", SERIAL, *map(str, args)], capture_output=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError(f"ADB {args[:3]}: {result.stderr.decode('utf-8', 'replace')}")
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")


def ui():
    text = adb("exec-out", "uiautomator", "dump", "/dev/tty")
    match = re.search(r"<\?xml.*?</hierarchy>", text, re.S)
    if not match:
        raise RuntimeError("No UI hierarchy returned")
    return ET.fromstring(match.group())


def find(resource_id: str, tree=None):
    for node in (tree if tree is not None else ui()).iter("node"):
        if node.get("resource-id") == f"{PACKAGE}:id/{resource_id}":
            bounds = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
            if len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1]:
                return node
    return None


def wait_id(resource_id: str, seconds: int = 20):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        node = find(resource_id)
        if node is not None:
            return node
        time.sleep(0.4)
    raise AssertionError(f"UI did not show {resource_id}")


def tap_node(node):
    if node is None:
        raise AssertionError("Cannot tap a missing node")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
    adb("shell", "input", "tap", (x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(0.35)


def tap(resource_id):
    tap_node(find(resource_id))


def fill(resource_id, text):
    tap(resource_id)
    time.sleep(0.4)
    adb("shell", "input", "keycombination", "113", "29")
    adb("shell", "input", "keyevent", "67")
    adb("shell", "input", "text", text.replace(" ", "%s"))
    time.sleep(0.2)
    adb("shell", "input", "keyevent", "4")


def screenshot(folder, name):
    (folder / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def start():
    adb("shell", "am", "start", "-n", f"{PACKAGE}/.MainActivity")
    time.sleep(1.5)


def login(folder):
    start()
    if find("edit_email") is not None:
        screenshot(folder, "login")
        fill("edit_email", os.environ["NUTRISCAN_TEST_EMAIL"])
        fill("edit_password", os.environ["NUTRISCAN_TEST_PASSWORD"])
        tap("button_login")
    wait_id("homeFragment", 30)
    screenshot(folder, "home")
    print("PASS: real Firebase sign-in and current main Home UI", flush=True)


def favorites():
    tap("scanHubFragment")
    tap("button_favorites")
    wait_id("recycler_favorites")


def snapshot(folder, name):
    (folder / f"{name}-meminfo.txt").write_text(adb("shell", "dumpsys", "meminfo", PACKAGE), encoding="utf-8")
    (folder / f"{name}-gfxinfo.txt").write_text(adb("shell", "dumpsys", "gfxinfo", PACKAGE), encoding="utf-8")
    screenshot(folder, name)


def dumpheap(folder, name):
    remote = f"/data/local/tmp/m05-{name}.hprof"
    result = adb("shell", "am", "dumpheap", PACKAGE, remote, timeout=60)
    (folder / f"{name}-heap-command.txt").write_text(result, encoding="utf-8")
    private_folder = ROOT / "app/build/milestone05/private-profiling" / folder.name
    private_folder.mkdir(parents=True, exist_ok=True)
    heap = private_folder / f"{name}.hprof"
    adb("pull", remote, str(heap), timeout=60)
    subprocess.run([sys.executable, str(ROOT / "scripts/summarize_heap.py"), str(heap),
                    "--output", str(folder / f"{name}-heap-summary.json")], check=True)


def profile(folder, heaps, trace=False, memory_only=False):
    start()
    favorites()
    wait_id("text_name")
    snapshot(folder, "before-navigation")
    if heaps:
        dumpheap(folder, "before-navigation")
    for index in range(10):
        tap("text_name")
        wait_id("button_analyze")
        if index == 0:
            screenshot(folder, "product-detail")
        adb("shell", "input", "keyevent", "4")
        wait_id("recycler_favorites")
        print(f"PASS: cached product detail round trip {index+1}/10", flush=True)
    snapshot(folder, "after-navigation")
    if heaps:
        dumpheap(folder, "after-navigation")
    if memory_only:
        return
    trace_pid = None
    trace_path = f"/data/misc/perfetto-traces/m05-{folder.name}.perfetto-trace"
    if trace:
        trace_output = adb("shell", "perfetto", "--background-wait", "-o", trace_path,
                           "-t", "90s", "-b", "32mb", "-a", PACKAGE,
                           "sched", "gfx", "view", "dalvik", "freq", "idle", "am", "wm", "binder_driver")
        if not trace_output.strip().isdigit():
            raise RuntimeError(f"Unexpected Perfetto background PID: {trace_output}")
        trace_pid = trace_output.strip()
    # Warm up, then isolate the RecyclerView scrolling workload.
    adb("shell", "input", "swipe", "550", "1750", "550", "650", "300")
    adb("shell", "input", "swipe", "550", "650", "550", "1750", "300")
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    for _ in range(12):
        adb("shell", "input", "swipe", "550", "1750", "550", "650", "350")
    for _ in range(12):
        adb("shell", "input", "swipe", "550", "650", "550", "1750", "350")
    snapshot(folder, "product-scroll")
    adb("shell", "input", "keyevent", "4")
    measure_recipes(folder)
    if trace_pid:
        adb("shell", "kill", "-TERM", trace_pid)
        time.sleep(1)
        adb("pull", trace_path, str(folder / "scrolling.perfetto-trace"), timeout=60)
    print("PASS: product/recipe scrolling metrics captured (test fixtures, network excluded)", flush=True)


def position_recipe_row():
    recipe_row = find("recycler_my")
    for _ in range(8):
        if recipe_row is not None:
            bounds = list(map(int, re.findall(r"\d+", recipe_row.get("bounds"))))
            if bounds[3] - bounds[1] >= 250 and bounds[1] >= 300 and bounds[3] <= 2100:
                break
            if bounds[1] < 300:
                adb("shell", "input", "swipe", "550", "800", "550", "1350", "350")
            else:
                adb("shell", "input", "swipe", "550", "1750", "550", "1200", "350")
        else:
            adb("shell", "input", "swipe", "550", "1750", "550", "1200", "350")
        recipe_row = find("recycler_my")
    if recipe_row is None:
        raise AssertionError("Test recipe row was not visible")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", recipe_row.get("bounds")))
    row_y = (y1+y2)//2
    if not (300 <= row_y <= 2100):
        raise AssertionError(f"Recipe row is outside app content: {recipe_row.get('bounds')}")
    return x1, row_y, x2


def measure_recipes(folder):
    tap("recipesFragment")
    wait_id("fab_add_recipe")
    x1, row_y, x2 = position_recipe_row()
    # Android's edge-back gesture can consume horizontal swipes near the display
    # edge. Start well inside the RecyclerView so we measure scrolling, not Back.
    inset = min(220, (x2-x1)//3)
    left, right = x1+inset, x2-inset
    # Warm the actual horizontal My Recipes RecyclerView before resetting counters.
    adb("shell", "input", "swipe", right, row_y, left, row_y, "350")
    adb("shell", "input", "swipe", left, row_y, right, row_y, "350")
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    for _ in range(6):
        adb("shell", "input", "swipe", right, row_y, left, row_y, "350")
        adb("shell", "input", "swipe", left, row_y, right, row_y, "350")
    if find("fab_add_recipe") is None:
        raise AssertionError("Recipe scrolling left the app; discard this measurement")
    snapshot(folder, "recipe-scroll")
    frame_text = (folder / "recipe-scroll-gfxinfo.txt").read_text(encoding="utf-8")
    frames = re.search(r"Total frames rendered:\s*(\d+)", frame_text)
    if frames is None or int(frames.group(1)) < 50:
        raise AssertionError("Too few recipe frames for a scrolling measurement; discard it")
    print("PASS: verified recipe RecyclerView scrolling measurement", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=["login", "snapshot", "profile", "memory", "recipes", "session", "inspect"])
    parser.add_argument("--label", default="release")
    parser.add_argument("--heaps", action="store_true")
    parser.add_argument("--trace", action="store_true")
    args = parser.parse_args()
    folder = ROOT / "submission/evidence" / args.label
    folder.mkdir(parents=True, exist_ok=True)
    if args.phase == "login":
        login(folder)
    elif args.phase == "profile":
        profile(folder, args.heaps, args.trace)
    elif args.phase == "memory":
        profile(folder, True, memory_only=True)
    elif args.phase == "recipes":
        start()
        measure_recipes(folder)
    elif args.phase == "session":
        adb("shell", "am", "force-stop", PACKAGE)
        start()
        wait_id("homeFragment")
        screenshot(folder, "session-restored")
        print("PASS: process restart restores Firebase session", flush=True)
    elif args.phase == "inspect":
        print(json.dumps([{"id": n.get("resource-id"), "text": n.get("text"), "bounds": n.get("bounds")}
                          for n in ui().iter("node") if n.get("text") or n.get("clickable") == "true"], ensure_ascii=False))
    else:
        snapshot(folder, "snapshot")


if __name__ == "__main__":
    main()
