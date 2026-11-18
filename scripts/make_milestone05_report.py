"""Generate a report from actual build/device evidence; fail on missing/failed tests.

Install reportlab/Pillow into a private tool environment, not the Android app.
Run after Package-Milestone05.ps1 and emulator verification.
"""
from __future__ import annotations

import hashlib
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "app/build/milestone05/python-libs"))
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Image

OUT = ROOT / "submission"
EVIDENCE = OUT / "evidence"


def test_stats(files):
    result = dict(tests=0, failures=0, errors=0, skipped=0)
    for path in files:
        node = ET.parse(path).getroot()
        for key in result:
            result[key] += int(node.get(key, 0))
    if not result["tests"] or result["failures"] or result["errors"] or result["skipped"]:
        raise RuntimeError(f"Missing or failing verification: {result}")
    return result


def frame_stats(label, workload):
    text = (EVIDENCE / label / f"{workload}-scroll-gfxinfo.txt").read_text(encoding="utf-8")
    def value(pattern):
        match = re.search(pattern, text)
        return match.group(1) if match else "not recorded"
    return {"frames": value(r"Total frames rendered:\s*(\d+)"),
            "janky": value(r"Janky frames:\s*(\d+\s*\([^\n]+\))"),
            "p50": value(r"50th percentile:\s*(\d+ms)"),
            "p95": value(r"95th percentile:\s*(\d+ms)")}


def main():
    apk = OUT / "Group04_NutriScan_Milestone05_Release.apk"
    bundle = OUT / "Group04_NutriScan_Milestone05_Release.aab"
    sdk = Path(os.environ.get("ANDROID_HOME", r"C:\Users\Admin\AppData\Local\Android\Sdk"))
    tool_dirs = [p for p in (sdk/"build-tools").iterdir() if re.fullmatch(r"\d+\.\d+\.\d+", p.name)]
    tools = max(tool_dirs, key=lambda p: tuple(map(int, p.name.split('.'))))
    badging = subprocess.check_output([str(tools/"aapt.exe"), "dump", "badging", str(apk)], text=True, encoding="utf-8")
    if "versionCode='3'" not in badging or "versionName='2.0.1'" not in badging or "application-debuggable" in badging:
        raise RuntimeError("APK is not the intended non-debuggable 2.0.1 Release")
    unit = test_stats((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    device = test_stats((ROOT / "app/build/outputs/androidTest-results/connected").rglob("TEST-*.xml"))
    lint = {}
    for variant in ("debug", "release"):
        lint_root = ET.parse(ROOT / f"app/build/reports/lint-results-{variant}.xml").getroot()
        issues = lint_root.findall("issue")
        errors = sum(i.get("severity") in ("Error", "Fatal") for i in issues)
        if errors:
            raise RuntimeError(f"Lint {variant}: {errors} errors")
        lint[variant] = {"errors": errors, "warnings": sum(i.get("severity") == "Warning" for i in issues)}
    signature = (OUT / "APK_SIGNATURE.txt").read_text(encoding="utf-8")
    if "Verifies" not in signature or "Android Debug" in signature:
        raise RuntimeError("Missing dedicated Release signature")
    cert = re.search(r"certificate SHA-256 digest: (\w+)", signature).group(1)
    with zipfile.ZipFile(apk) as archive:
        provenance = archive.read("META-INF/version-control-info.textproto").decode()
        revision = re.search(r'revision: "([a-f0-9]+)"', provenance).group(1)
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    author = subprocess.check_output(["git", "log", "-1", "--format=%an <%ae>"], cwd=ROOT, text=True).strip()
    committer = subprocess.check_output(["git", "log", "-1", "--format=%cn <%ce>"], cwd=ROOT, text=True).strip()
    with zipfile.ZipFile(OUT / "Group04_NutriScan_Milestone05_Source.zip") as archive:
        source_revision = archive.comment.decode().strip()
    if revision != head or source_revision != head:
        raise RuntimeError("APK, source archive and current Git revision must match")
    walkthrough = json.loads((EVIDENCE/"release/walkthrough.json").read_text())
    session = json.loads((EVIDENCE/"release/session-storage.json").read_text())
    video = json.loads((EVIDENCE/"release/video-metadata.json").read_text())
    if not 60 <= video["duration_seconds"] <= 180:
        raise RuntimeError("Submission demo must last 1–3 minutes")
    if session.get("fields_after_release_logout") != 0 or not session.get("plaintext_jwt_absent"):
        raise RuntimeError("Missing signed-Release session storage/logout verification")
    apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest()
    info = {"package": "vn.edu.vlu.milestone2", "versionName": "2.0.1", "versionCode": 3,
            "source_revision": revision, "source_author": author, "source_committer": committer,
            "baseline_revision": "c11cab7703b3990ca2bf7844aad275fa45718b19",
            "apk_sha256": apk_hash, "certificate_sha256": cert, "unit_tests": unit,
            "device_tests": device, "lint": lint, "release_walkthrough": walkthrough,
            "release_session_storage": session, "video": video,
            "frames": {label: {w: frame_stats(label, w) for w in ("product", "recipe")}
                       for label in ("baseline-main-host", "release")}}
    (OUT / "VERIFICATION.json").write_text(json.dumps(info, indent=2), encoding="utf-8")

    pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
    pdfmetrics.registerFont(TTFont("ArialBold", r"C:\Windows\Fonts\arialbd.ttf"))
    styles = getSampleStyleSheet()
    for name in ("Normal", "BodyText", "Heading1", "Heading2", "Title"):
        styles[name].fontName = "Arial"
    styles["Normal"].fontSize, styles["Normal"].leading = 9, 13
    styles["Heading1"].fontName, styles["Heading1"].textColor = "ArialBold", colors.HexColor("#14745C")
    styles.add(ParagraphStyle("SmallM05", parent=styles["Normal"], fontSize=7.5, leading=10))
    styles.add(ParagraphStyle("CodeM05", parent=styles["Normal"], fontName="Courier", fontSize=7, leading=10))
    story = []
    def p(text, style="Normal"):
        return Paragraph(text, styles[style])
    def add(text, style="Normal"):
        story.extend([p(text, style), Spacer(1, 7)])
    def table(rows, widths=None):
        cells = [[p(str(cell), "SmallM05") for cell in row] for row in rows]
        item = Table(cells, colWidths=widths, hAlign="LEFT", repeatRows=1)
        item.setStyle(TableStyle([
            ("BACKGROUND", (0,0), (-1,0), colors.HexColor("#DFF0E8")),
            ("VALIGN", (0,0), (-1,-1), "TOP"),
            ("GRID", (0,0), (-1,-1), .3, colors.HexColor("#CBD9D3")),
            ("LEFTPADDING", (0,0), (-1,-1), 7), ("RIGHTPADDING", (0,0), (-1,-1), 7),
            ("TOPPADDING", (0,0), (-1,-1), 6), ("BOTTOMPADDING", (0,0), (-1,-1), 6)
        ]))
        story.extend([item, Spacer(1, 10)])
    def images(items):
        cells = []
        for path, label in items:
            if not path.exists():
                raise RuntimeError(f"Missing Release evidence: {path}")
            image = Image(str(path), width=132, height=293)
            cells.append([p(label, "SmallM05"), image])
        row = Table([cells], colWidths=[(A4[0]-84)/len(cells)]*len(cells))
        row.setStyle(TableStyle([("VALIGN", (0,0), (-1,-1), "TOP")]))
        story.append(row)
        story.append(Spacer(1, 10))
    def page(title):
        if story:
            story.append(PageBreak())
        add("NUTRISCAN · GROUP 04 · MILESTONE 05", "SmallM05")
        add(title, "Heading1")

    page("Production & Security Report")
    add("Release 2.0.1 · versionCode 3 · 18 November 2026 (GMT+7)", "Heading2")
    add("Cập nhật trên mã main mới: giữ các module CameraX, Room v2, Gemini, lịch sử và công thức nấu ăn của phiên bản 2.0. Bộ phát hành này thay thế bộ Milestone 05 cũ dựa trên baseline 2b06278.")
    table([["Nội dung", "Bằng chứng thực tế"],
           ["Baseline", "c11cab7 — PR #6, full UI milestones"],
           ["Source revision trong APK", revision],
           ["Git author / committer", author if author == committer else f"{author}; {committer}"],
           ["Build", "APK/AAB Release có R8 + shrink resources + signing riêng"],
           ["JVM / thiết bị", f"{unit['tests']} unit tests; {device['tests']} device tests; failures/errors = 0; skipped = {device['skipped']}"],
           ["Lint Debug / Release", f"0 errors; warnings {lint['debug']['warnings']} / {lint['release']['warnings']}"],
           ["Kích thước", f"APK {apk.stat().st_size/1048576:.2f} MiB; AAB {bundle.stat().st_size/1048576:.2f} MiB"],
           ["Chữ ký", "Dedicated RSA 3072-bit key, CN=NutriScan Group 04; APK: apksigner; AAB: jarsigner; cùng certificate"]], [150, 360])
    add("APK SHA-256", "Heading2")
    add(apk_hash[:32]+"<br/>"+apk_hash[32:], "CodeM05")
    add("Certificate SHA-256", "Heading2")
    add(cert[:32]+"<br/>"+cert[32:], "CodeM05")
    add("Nguồn/phiên bản được kiểm tra bằng metadata trong APK, không suy đoán từ tên file. Source archive và VERIFICATION.json đi kèm giúp đối chiếu chính xác.")

    page("Security, R8 & Release Signing")
    table([["Hạng mục", "Triển khai / kiểm chứng"],
           ["Encrypted session", "EncryptedSharedPreferences; Keystore AES256_GCM, preference keys AES256_SIV, values AES256_GCM. Device tests đọc lại từ instance mới và xác nhận UID/token không xuất hiện plaintext trong XML."],
           ["Token policy", "Ràng buộc current UID; hết hạn theo Firebase hoặc tối đa 5 phút; clock rollback/blank token bị từ chối. Cache dùng cho Firestore REST restore. Sign-out xóa cache trên I/O và đăng xuất SDK."],
           ["Local security", "Không lưu password. Không log HTTP URL/token. Cleartext traffic bị chặn; backup/device transfer loại preferences, databases và private files."],
           ["API configuration", "GEMINI_API_KEY, GEMINI_MODEL và TheMealDB endpoint đọc từ local.properties, inject qua BuildConfig. File key/password được gitignore."],
           ["R8 / ProGuard", "isMinifyEnabled=true; isShrinkResources=true. Keep rules giới hạn vào Retrofit proxies, Gson DTOs và tên field của Room JSON."],
           ["Signing", "Khóa mới trong .signing/; không dùng debug key, không có unsigned fallback. APK/AAB ký tự động khi build." ]], [120,390])
    add("BuildConfig không làm API key không thể trích xuất từ APK. Không đưa real key, keystore hay password vào Git. Firebase SDK tự quản lý phiên của SDK; cache mã hóa của app không thay thế cơ chế đó.")
    add("EncryptedSharedPreferences hiện deprecated trong AndroidX, được cô lập sau SessionTokenStore vì đề bài yêu cầu rõ API này. Chỉ Debug tích hợp LeakCanary; Release không mang công cụ dò leak và không bật debuggable.")
    add("Kiểm tra chính APK Release: 4 trường của app được mã hóa; không có khóa plaintext UID/token hoặc JWT plaintext. Sau bấm Đăng xuất: 0 trường phiên của app còn lại, UI quay về Login. File session-storage.json chỉ chứa số lượng và boolean, không chứa token.", "SmallM05")
    mapping = ROOT / "app/build/outputs/mapping/release/mapping.txt"
    kept = []
    with mapping.open(encoding="utf-8") as source:
        for line in source:
            if re.match(r"vn\.edu\.vlu\.milestone2\.data\.(remote\..*(Api|Dto)|security\.EncryptedSessionStore|model\.Nutrition) ->", line):
                kept.append(line.strip())
                if len(kept) == 8:
                    break
    add("R8 mapping — trích từ build mới", "Heading2")
    for line in kept:
        add(line.replace("vn.edu.vlu.milestone2.", "").replace("->", "→"), "SmallM05")

    page("Rendering Performance — Same Device / Same Fixtures")
    add("Pixel 6 emulator · Android API 37 · 1080×2400 · 60 Hz · GPU host NVIDIA RTX 4060. Workload: 80 sản phẩm và 40 công thức test-only, ghi nhãn M05 fixture. 24 lượt vuốt danh sách sản phẩm; 12 lượt vuốt đúng RecyclerView công thức sau warm-up. Mạng không nằm trong workload đo.")
    rows = [["Workload", "Build", "Frames", "Janky", "p50 / p95"]]
    for work in ("product", "recipe"):
        for label, name in (("baseline-main-host", "Main 2.0 Debug"), ("release", "M05 2.0.1 Release")):
            f = info["frames"][label][work]
            rows.append([work, name, f["frames"], f["janky"], f"{f['p50']} / {f['p95']}"])
    table(rows, [65,120,50,140,135])
    add("Thay đổi: listener của ViewHolder chỉ gắn một lần; recycle xóa model/image; Fragment detach adapter; Glide cache giữ nguyên; JSON mapping chuyển sang I/O. Gfxinfo và Perfetto trace được giữ trong evidence/. Trace ghi workload sản phẩm và chuyển màn hình; số đo công thức lấy riêng từ gfxinfo, không suy diễn từ trace đó.")
    add("Kết quả hỗn hợp: danh sách sản phẩm giảm tỷ lệ jank; danh sách công thức trong mẫu Release này có tỷ lệ jank cao hơn baseline. Vì vậy chưa xác nhận đạt mục tiêu cuộn 60 fps ổn định. Giữ nguyên số đo và công khai giới hạn thay vì chọn mẫu đẹp hơn.")
    add("Đây là so sánh bản nền Debug với bản shipping Release trên cùng fixture/GPU, không phải benchmark A/B riêng từng tối ưu. Không suy diễn thành cam kết 60 fps trên mọi thiết bị; các frame chậm còn lại phải được đọc theo số đo trên.")
    images([(EVIDENCE/"release/product-scroll.png", "Release: 80 cached product fixtures"),
            (EVIDENCE/"release/recipe-scroll.png", "Release: horizontal recipe RecyclerView")])

    page("Memory & Lifecycle Verification")
    add("Thu heap dump Debug trước và sau 10 lượt Product Details. Tóm tắt chỉ xuất số instance của các lớp UI; heap thô giữ riêng ngoài bộ nộp vì process memory có thể chứa token Firebase.")
    rows = [["Lớp", "Trước", "Sau 10 lượt"]]
    summaries = []
    for name in ("before-navigation", "after-navigation"):
        summaries.append(json.loads((EVIDENCE/"debug"/f"{name}-heap-summary.json").read_text()))
    for suffix in ("MainActivity", "ui.product.ProductDetailFragment", "ui.product.FavoritesFragment"):
        key = "vn.edu.vlu.milestone2." + suffix
        rows.append([suffix, summaries[0]["application_instance_counts"].get(key,0), summaries[1]["application_instance_counts"].get(key,0)])
    table(rows, [330,90,90])
    add("Instrumented test productViewsAreCollectableAfterTenRoundTrips dùng WeakReference với các product views đã hủy, đợi idle/GC rồi kiểm tra không còn view bị giữ. Báo cáo device tests ở trang 1 bao gồm kết quả test này; xem XML gốc để kiểm tra skipped.")
    add("Instance counts/WeakReference trong workload hữu hạn không chứng minh toàn ứng dụng không bao giờ leak. LeakCanary chỉ nằm trong Debug và các file evidence ghi rõ phạm vi. Memory allocations, cache warming và retained leaks là các khái niệm khác nhau.")
    images([(EVIDENCE/"debug/product-detail.png", "Debug: real cached Product Details"),
            (EVIDENCE/"release/session-restored.png", "Signed Release: session restored after force-stop")])

    page("Signed Release Functional Walkthrough")
    add(f"Video mới dài {video['duration_seconds']:.1f} giây, quay chính APK Release 2.0.1 đã ký; APK_SIGNATURE.txt, VERIFICATION.json và metadata thiết bị đi kèm. Đã kiểm tra lưu profile, yêu thích, đọc JSON sau R8, phân tích offline, tạo/xem công thức và checklist nấu ăn. Fixture không giả làm kết quả API live.")
    images([(EVIDENCE/"release/home.png", "A · Firebase login, current main Home"),
            (EVIDENCE/"release/product-detail.png", "B · Cached product/nutrition after R8"),
            (EVIDENCE/"release/cooking.png", "C · Offline cooking flow")])
    add("Giới hạn môi trường", "Heading2")
    add("Máy chưa cấu hình GEMINI_API_KEY: barcode analysis dùng fallback offline; chưa xác nhận Gemini online hoặc xử lý ảnh online. Báo cáo không gán kết quả thành công cho dịch vụ ngoài chưa được kiểm chứng. Thêm key thật vào local.properties rồi rebuild nếu cần demo AI online.")
    add("Khóa Release mới khác khóa test trong APK 1.0 cũ. Không thể cập nhật trực tiếp qua chữ ký cũ; không gỡ app trên máy cá nhân nếu chưa sao lưu dữ liệu. Khóa/password mới phải được nhóm giữ riêng để ký các bản tiếp theo.")

    page("WBS, Provenance & Submission")
    table([["Thành viên / phạm vi", "Bằng chứng Git hoặc công việc thực tế"],
           ["Nguyễn Gia Thành — baseline", "d4c4b50, b376e76, 2b117fc, 97c5296, 05d3ae8: build/data/UI/test/schema nền main 2.0."],
           ["Nguyễn Thành Trí — baseline", "21b0b79, ae982f2, 2b06278: Room/Auth và merge trước M05; giữ nguyên tác giả của lịch sử main."],
           ["Nguyễn Trần Hoàng Nhân — baseline", "c67977f, efa6e08: details/network và giải quyết merge trước khi tích hợp UI mới."],
           ["Phạm Thị Khánh Ngân", "Phần công việc thủ công ngoài Git cần nhóm xác nhận; không suy diễn khối lượng cá nhân từ tên tài khoản Git."]], [175,335])
    add(f"Danh tính Git của commit M05: {author}. Người dùng đã chọn tài khoản Ngan3112 và yêu cầu đổi author/committer cho các commit M05 chưa push. Các tác giả baseline được giữ nguyên. Xem AI_USAGE.md để phân biệt danh tính Git với phần triển khai có Codex hỗ trợ.")
    add("Các phần M05 được Codex hỗ trợ gồm code security/performance/signing, test, emulator measurements, đóng gói và báo cáo. Không khai một tỷ lệ AI sử dụng chưa được nhóm đánh giá.")
    add("Bộ nộp", "Heading2")
    add("PDF này · signed APK/AAB · AI_USAGE.md · demo MP4 · source ZIP · SHA256SUMS.txt · public signing certificate · evidence đã loại dữ liệu nhạy cảm. Không đưa .signing/, local.properties, raw heap hoặc database người dùng vào bộ nộp.")
    add("GitHub Release/tag và đường dẫn video public là bước xuất bản cuối. Nếu chưa có URL thật trong SUBMISSION.md thì không coi việc xuất bản đã hoàn thành. Link /releases/new chỉ là trang tạo Release, không phải kết quả đã phát hành.")
    add("Tham khảo chính thức", "Heading2")
    for url in ("https://developer.android.com/topic/performance/app-optimization/enable-app-optimization",
                "https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences",
                "https://developer.android.com/studio/publish/app-signing"):
        add(url, "SmallM05")

    def footer(canvas, doc):
        canvas.setFont("Arial", 8)
        canvas.setFillColor(colors.HexColor("#50615A"))
        canvas.drawString(42, 24, "NutriScan · Group 04 · Release 2.0.1 · 18/11/2026")
        canvas.drawRightString(A4[0]-42, 24, str(doc.page))
    SimpleDocTemplate(str(OUT/"Group04_Milestone05_Production_Security_Report.pdf"), pagesize=A4,
                      leftMargin=42, rightMargin=42, topMargin=36, bottomMargin=40,
                      title="NutriScan Group 04 — Milestone 05, Release 2.0.1",
                      author="Group 04 — verified evidence with Codex assistance").build(story, onFirstPage=footer, onLaterPages=footer)
    files = [p for p in OUT.iterdir() if p.is_file() and p.name != "SHA256SUMS.txt"]
    (OUT/"SHA256SUMS.txt").write_text("\n".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}" for p in sorted(files))+"\n", encoding="utf-8")
    print(json.dumps(info, indent=2))


if __name__ == "__main__":
    main()
