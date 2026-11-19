# NutriScan – AI-Powered Food Label Analyzer & Healthy Cooking Assistant

Ứng dụng Android (Kotlin, XML + ConstraintLayout, MVVM) của **Nhóm 04 – Lập trình ứng dụng di động (71ITSE30603)**.

- Quét mã vạch (CameraX + ML Kit) → Open Food Facts → đánh giá theo hồ sơ sức khỏe (Gemini, structured JSON).
- Không có trong Open Food Facts → chụp nhãn thành phần, ảnh nén < 1 MB lưu trong `filesDir`, phân tích tự động khi có mạng (WorkManager).
- Lịch sử quét, sản phẩm yêu thích, hồ sơ sức khỏe – lưu offline bằng Room, đồng bộ Firestore.
- Gợi ý món ăn dựa trên **HealthProfile + các ScanResult gần đây**, chế độ nấu từng bước, CRUD "Món của tôi".

## Chạy project

1. Mở bằng Android Studio, đợi Gradle sync.
2. Thêm `GEMINI_API_KEY=...` vào `local.properties` (không commit file này).
3. Firebase: bật Email/Password Authentication và Cloud Firestore cho project `nutriscan-6381d`.
4. Run `app`.

Chi tiết kiến trúc, sơ đồ màn hình, ERD và bảng đối chiếu từng yêu cầu Milestone / feedback giảng viên: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Kiểm thử

```bash
./gradlew testDebugUnitTest          # NutritionAnalyzer, RecipeCompatibilityChecker, GeminiDataSource
./gradlew connectedDebugAndroidTest  # Room DAO (cần máy ảo/thiết bị)
```

## Milestone 02 — MVVM & ViewBinding

Đối chiếu yêu cầu, các màn hình và kết quả kiểm tra: [docs/MILESTONE_02_REPORT.md](docs/MILESTONE_02_REPORT.md).
File URL repository để nộp: [Repository_Link.txt](Repository_Link.txt).
Mã nguồn dùng ViewBinding cho Activity/Fragment/Adapter; dữ liệu đi qua Repository và ViewModel,
được quan sát theo vòng đời view. Cấu hình IDE, cache và thư mục build được loại khỏi Git.

> Kết quả AI chỉ mang tính tham khảo, không thay thế tư vấn y tế.

## Chuẩn bị Final — 2.0.2

Nhánh `final-readiness` ghép main `e1272da` với sửa M2: tải hồ sơ đúng trạng thái,
giữ ViewBinding theo vòng đời, tìm món tiếng Việt và chuyển ngôn ngữ Việt/Anh.
Checklist hồ sơ và demo: [docs/FINAL_SUBMISSION.md](docs/FINAL_SUBMISSION.md).
Các file được chuẩn bị riêng trong `submission/final/`; bộ nộp cũ được giữ nguyên.

## Milestone 05 — Release 2.0.1 (bản trước)

Phần production/security được cập nhật từ `main` tại `c11cab7`, giữ nguyên các module mới của phiên bản 2.0.

1. Sao chép `local.properties.example` thành `local.properties`, thêm Gemini key của nhóm nếu cần AI online.
2. Tạo khóa ký một lần: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/New-ReleaseKeystore.ps1`.
3. Sao lưu riêng thư mục `.signing/`; không gửi khóa hoặc mật khẩu vào Git/bài nộp.
4. Build và đóng gói: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Package-Milestone05.ps1`.

Release bật R8 + resource shrinking, có signing riêng và được shell profiling mà không bật debuggable.
Chỉ Debug có LeakCanary. Dữ liệu token do app quản lý được mã hóa, ràng buộc UID, hết hạn tối đa 5 phút và xóa khi đăng xuất.
Firebase SDK vẫn quản lý phiên của SDK; app không lưu mật khẩu.

Hướng dẫn đo/kiểm chứng và giới hạn: [`docs/MILESTONE_05_REPORT.md`](docs/MILESTONE_05_REPORT.md).
Các file nộp sinh ra trong `submission/`; thư mục này và `.signing/` được loại khỏi Git.
