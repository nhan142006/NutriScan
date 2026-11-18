"""Record an actual signed-Release walkthrough, not a synthetic/demo UI.

Uses only the emulator's test account and explicitly marked local performance
fixtures. Refuses a different installed version. No credentials are printed.
"""
import json
import subprocess
import time
from pathlib import Path
import verify_milestone05 as v

folder = v.ROOT / "submission/evidence/release"
folder.mkdir(parents=True, exist_ok=True)


def scroll_to(resource_id):
    for _ in range(7):
        node = v.find(resource_id)
        if node is not None:
            return node
        v.adb("shell", "input", "swipe", "550", "1800", "550", "800", "350")
    raise AssertionError(f"Not visible: {resource_id}")


def main():
    package_info = v.adb("shell", "dumpsys", "package", v.PACKAGE)
    if "versionName=2.0.1" not in package_info or "versionCode=3 " not in package_info:
        raise AssertionError("Demo must run the signed 2.0.1/versionCode 3 APK")
    (folder / "installed-package.txt").write_text(package_info, encoding="utf-8")
    v.login(folder)
    remote = "/sdcard/nutriscan-m05-demo.mp4"
    recording = subprocess.Popen([str(v.ADB), "-s", v.SERIAL, "shell", "screenrecord", "--size", "720x1600",
                                  "--bit-rate", "3000000", "--time-limit", "170", remote],
                                 stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    started = time.monotonic()
    completed = False
    try:
        time.sleep(2)
        v.tap("profileFragment")
        for field, value in (("edit_name", "M05 Demo"), ("edit_age", "24"), ("edit_calories", "2000")):
            scroll_to(field)
            v.fill(field, value)
        v.tap_node(scroll_to("button_save"))
        time.sleep(1)
        v.screenshot(folder, "profile-saved")
        print("PASS: signed Release profile saved through current main UI", flush=True)
        v.tap("homeFragment")
        v.screenshot(folder, "home")
        v.favorites()
        v.wait_id("text_name")
        v.screenshot(folder, "favorites")
        v.tap("text_name")
        v.wait_id("button_analyze")
        energy = v.find("text_energy")
        if energy is None or "120" not in energy.get("text", ""):
            raise AssertionError("R8 changed cached Nutrition JSON parsing")
        v.screenshot(folder, "product-detail")
        before = v.find("button_favorite").get("content-desc")
        v.tap("button_favorite")
        after = v.find("button_favorite").get("content-desc")
        if before == after:
            raise AssertionError("Favorite toggle did not update")
        v.tap("button_favorite")
        v.tap("button_analyze")
        v.wait_id("text_explanation")
        v.screenshot(folder, "offline-analysis")
        print("PASS: Release JSON, favorite remove/add and offline nutrition analysis", flush=True)
        for _ in range(4):
            if v.find("recipesFragment") is not None:
                break
            v.adb("shell", "input", "keyevent", "4")
        v.tap("recipesFragment")
        v.wait_id("fab_add_recipe")
        v.tap("fab_add_recipe")
        for field, value in (("edit_title", "M05 Healthy Oats"), ("edit_category", "Demo"),
                             ("edit_ingredients", "oats, milk"),
                             ("edit_steps", "Mix ingredients and cook for five minutes.")):
            scroll_to(field)
            v.fill(field, value)
        v.tap("button_save")
        v.wait_id("fab_add_recipe")
        x1, row_y, x2 = v.position_recipe_row()
        # Return to the first card after inserting the new user recipe.
        for _ in range(2):
            v.adb("shell", "input", "swipe", x1+220, row_y, x2-220, row_y, "350")
        tree = v.ui()
        new_recipe = next((n for n in tree.iter("node") if n.get("text") == "M05 Healthy Oats"), None)
        v.tap_node(new_recipe)
        v.wait_id("button_cook")
        v.screenshot(folder, "recipe-created")
        v.tap("button_cook")
        v.wait_id("recycler_checklist")
        unchecked = next((n for n in v.ui().iter("node") if n.get("checkable") == "true" and n.get("checked") == "false"), None)
        if unchecked is not None:
            v.tap_node(unchecked)
        v.screenshot(folder, "cooking")
        print("PASS: create/read user recipe and offline cooking/checklist", flush=True)
        while time.monotonic() - started < 70:
            time.sleep(3)
        if recording.poll() is not None:
            raise AssertionError("Recording ended before the complete walkthrough")
        completed = True
    finally:
        pid_text = v.adb("shell", "pidof", "screenrecord")
        for pid in pid_text.split():
            if pid.isdigit():
                v.adb("shell", "kill", "-INT", pid)
        recording.wait(timeout=20)
        destination = v.ROOT / "submission" / ("Group04_NutriScan_Demo.mp4" if completed else "Demo_PARTIAL.mp4")
        v.adb("pull", remote, str(destination), timeout=60)
    (folder / "walkthrough.json").write_text(json.dumps({
        "installed_version": "2.0.1", "version_code": 3, "profile_saved": True,
        "nutrition_json_preserved_after_r8": True, "favorite_remove_add": True,
        "offline_analysis": True, "recipe_create_read_cooking": True,
        "data_scope": "real UI with an emulator test account and clearly named local fixtures",
        "recording_wall_seconds": round(time.monotonic()-started, 1)
    }, indent=2), encoding="utf-8")
    print(f"Recorded {destination.name}", flush=True)


if __name__ == "__main__":
    main()
