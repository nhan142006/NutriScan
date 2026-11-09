# AI_USAGE.md — Milestone 04: Network Layer, RESTful API & Cloud Services Integration

Khai báo theo **Quyết định 983/QĐ-ĐHVL-ĐT** (Pe.AI – Permitted AI).

## 1. Công cụ AI đã dùng

| Hạng mục | Thông tin |
|---|---|
| Công cụ | Claude (Anthropic), model Claude Opus 5.5, dùng qua ứng dụng claude.ai |
| Ngày sử dụng | 09/11/2026 |
| Người dùng | `[Họ tên – MSSV]` |
| Hình thức | Tác tử AI đọc repo, tự viết code, tạo commit trên nhánh `feature/network-cloud-integration` và viết báo cáo |

## 2. Prompt đã dùng (nguyên văn)

1. *(Đính kèm file đề bài Milestone 04)* — "https://github.com/nhan142006/NutriScan.git, đây là github project nhóm tôi, đây là nhiệm vụ của tôi, hãy làm giúp tôi"
2. "push lên git chưa"
3. "để tôi tự push, cho tôi lệnh"
4. *(Các lần dán kết quả Terminal để AI hướng dẫn tiếp lệnh git)*
5. "tôi muốn xóa claude khỏi commit"
6. *(Đính kèm đề bài bản cập nhật)* — "commit rồi bây giờ nộp gì"

## 3. Phạm vi AI đã thực hiện

AI đã sinh **toàn bộ code và tài liệu** của 6 commit trong PR #3:

- Cấu hình Gradle: Retrofit 3, OkHttp, Glide 5, Firebase BoM, google-services.
- Lớp mạng: `NetworkModule`, `ConnectivityInterceptor`, `safeApiCall`, `NetworkError`, `UiState`.
- Retrofit interface và data class cho Open Food Facts và TheMealDB.
- Repository, ViewModel, Fragment, Adapter, layout của 4 phần: Auth, Tra cứu, Phân tích, Gợi ý món.
- Unit test (MockWebServer, NutritionAnalyzer, AuthViewModel).
- Báo cáo `docs/MILESTONE_04_REPORT.md`.
- Hướng dẫn lệnh git: push, tạo PR, sửa commit message.

Ghi chú về lịch sử commit: các commit do AI tạo ban đầu có dòng `Co-Authored-By: Claude`. Nhóm đã xóa dòng này khỏi nhánh `main` bằng `git filter-branch`. Trang PR #3 trên GitHub vẫn lưu bản commit gốc có dòng này.

## 4. Danh sách file do AI sinh / sửa

| File | Loại | Mức độ AI |
|---|---|---|
| `app/build.gradle.kts` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/AndroidManifest.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/MainActivity.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/NutriScanApp.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/ConnectivityInterceptor.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/NetworkError.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/NetworkModule.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/NetworkResult.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/NoConnectivityException.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/network/SafeApiCall.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/ui/ImageLoader.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/ui/NetworkErrorText.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/ui/NetworkStateRenderer.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/core/ui/UiState.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/model/Product.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/model/Recipe.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/remote/mealdb/MealDbApi.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/remote/mealdb/MealDbDtos.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/remote/openfoodfacts/OpenFoodFactsApi.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/remote/openfoodfacts/OpenFoodFactsDtos.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/repository/AuthRepository.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/repository/ProductRepository.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/data/repository/RecipeRepository.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/di/ServiceLocator.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/domain/NutritionAnalyzer.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/analysis/AnalysisFragment.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/analysis/AnalysisViewModel.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/auth/AuthViewModel.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/auth/LoginFragment.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/auth/RegisterFragment.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/auth/SessionViewModel.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/recipe/RecipeAdapter.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/recipe/RecipeFragment.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/recipe/RecipeViewModel.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/scanner/ProductAdapter.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/scanner/ScannerFragment.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/java/vn/edu/vlu/milestone2/ui/scanner/ScannerViewModel.kt` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/drawable/ic_cloud_off.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/drawable/ic_image_error.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/drawable/ic_image_placeholder.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/drawable/ic_logout.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/activity_main.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/fragment_analysis_result.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/fragment_barcode_scanner.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/fragment_login.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/fragment_recipe_recommendation.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/fragment_register.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/item_product.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/item_recipe.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/layout/view_network_state.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/menu/bottom_nav_menu.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/menu/menu_main.xml` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/navigation/nav_graph.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/values/colors.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/main/res/values/strings.xml` | Sửa | AI sinh toàn bộ phần thay đổi |
| `app/src/test/java/vn/edu/vlu/milestone2/data/repository/ProductRepositoryTest.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/test/java/vn/edu/vlu/milestone2/data/repository/RecipeRepositoryTest.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/test/java/vn/edu/vlu/milestone2/domain/NutritionAnalyzerTest.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `app/src/test/java/vn/edu/vlu/milestone2/ui/auth/AuthViewModelTest.kt` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `build.gradle.kts` | Sửa | AI sinh toàn bộ phần thay đổi |
| `docs/MILESTONE_04_REPORT.md` | Tạo mới | AI sinh toàn bộ phần thay đổi |
| `gradle/libs.versions.toml` | Sửa | AI sinh toàn bộ phần thay đổi |

## 5. Phần do thành viên nhóm thực hiện

| Công việc | Người thực hiện |
|---|---|
| Push nhánh, tạo và merge Pull Request #3 | `[Tên]` |
| Tạo project Firebase, bật Email/Password, thêm `google-services.json` | `[Tên]` |
| Build, chạy thử trên thiết bị/emulator, sửa lỗi (nếu có) | `[Tên]` |
| Kiểm tra lại code, chỉnh sửa thêm (ghi rõ file nếu có) | `[Tên]` |
| Chụp màn hình Firebase Console, quay video demo | `[Tên]` |
| Điền WBS, hoàn thiện báo cáo | `[Tên]` |

## 6. Kiểm chứng

- Code do AI sinh **chưa được build trong môi trường của AI**, vì không truy cập được Android SDK. Nhóm đã build và chạy kiểm tra lại: `[Có / Chưa — ghi kết quả]`.
- Unit test: `./gradlew :app:testDebugUnitTest` → `[kết quả]`.

## 7. Tỷ lệ sử dụng AI (tự đánh giá)

`[Nhóm tự điền — ước tính % trên tổng khối lượng milestone, có so sánh với giới hạn 30–50% của đề]`


## 8. Bổ sung — Thành viên 4, Detail / Delete / Network Errors

ChatGPT (Codex) được dùng để đọc repo, viết phần Chi tiết, thao tác lưu/xóa qua Firestore REST, xử lý lỗi và Retry, bổ sung ca kiểm thử Repository và mục 9 của worksheet. Phạm vi thay đổi được ghi trong diff của PR bổ sung. Việc cấu hình Firestore, chạy thử trên thiết bị, kiểm tra code và quay video thuộc phần xác nhận của nhóm. Phiên làm việc này chưa chạy được unit test vì Gradle Wrapper không tải được bản phân phối Gradle.
