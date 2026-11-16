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

> Kết quả AI chỉ mang tính tham khảo, không thay thế tư vấn y tế.
