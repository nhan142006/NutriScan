# NutriScan — Milestone 02: MVVM & ViewBinding

- Nhóm: 04.
- Ngày đối chiếu: 19/11/2026.
- Repository công khai: <https://github.com/nhan142006/NutriScan>.
- Nền mã nguồn: `origin/main` tại `e541ded6dc810d93c63b2101cefab72fcf5908d4`.
- Nhánh hoàn thiện cục bộ: `feature/milestone02-mvvm`.

## 1. Đối chiếu yêu cầu M2

| Yêu cầu | Hiện thực và bằng chứng trong mã nguồn |
| --- | --- |
| Tổ chức ứng dụng theo Model–View–ViewModel | `data/model`, `data/local`, `data/remote`, `data/repository` lưu dữ liệu và truy cập nguồn; `domain` xử lý nghiệp vụ; `ui/<feature>` chứa Fragment, ViewModel và adapter theo tính năng. `di/ServiceLocator.kt` cung cấp dependency. |
| ViewBinding cho Activity, Fragment và adapter | `app/build.gradle.kts` bật `buildFeatures.viewBinding = true`. `MainActivity` dùng `ActivityMainBinding`; 15 Fragment inflate binding tương ứng; 5 adapter inflate binding của item. Không có lời gọi `findViewById` trong mã nguồn ứng dụng. |
| Dữ liệu và trạng thái thuộc ViewModel | 15 ViewModel cung cấp `StateFlow`/`LiveData`; Fragment chuyển thao tác của người dùng đến ViewModel và render dữ liệu quan sát được. Ví dụ `HomeViewModel.uiState`, `RecipesViewModel`, `ProfileViewModel.profile/form/accountEmail`. Danh sách gợi ý và giá trị mặc định hồ sơ thuộc `ProfileFormState`. |
| ViewModel không giữ View/Activity/Fragment/UI Context | Không có dependency UI này trong các ViewModel. Repository và các model dữ liệu là dependency của ViewModel. Annotation tài nguyên và bộ kiểm tra email không giữ tham chiếu đến view. |
| Quan sát dữ liệu theo vòng đời view | `core/ui/FlowExt.kt`: `viewLifecycleOwner.lifecycleScope` + `viewLifecycleOwner.repeatOnLifecycle(STARTED)`. Các Fragment gọi `collectWithLifecycle`; `FavoritesFragment` dùng `observe(viewLifecycleOwner)`. `MainActivity` dùng `repeatOnLifecycle(STARTED)` của Activity. |
| Giải phóng binding khi hủy view | Cả 15 Fragment đặt `_binding = null` trong `onDestroyView()`. Các màn hình danh sách còn ngắt adapter khỏi RecyclerView trước khi giải phóng view. |
| Không đưa cấu hình máy và output build lên Git | `.gitignore` loại `/.idea/`, `.gradle`, `/.kotlin/`, `/build/`, `/app/build/`, `local.properties` và các cache/private config khác. File `.idea` đã được bỏ khỏi chỉ mục Git; bản trên máy vẫn được giữ. |
| File chứa URL repository | `Repository_Link.txt` ở thư mục gốc dự án và trong bộ nộp M2. |

## 2. Các màn hình và ViewModel

| View | ViewModel |
| --- | --- |
| `MainActivity` | `SessionViewModel` |
| `LoginFragment`, `RegisterFragment` | `AuthViewModel` |
| `HomeFragment` | `HomeViewModel` |
| `ScanHubFragment` | `ScanHubViewModel` |
| `BarcodeScannerFragment` | `BarcodeScannerViewModel` |
| `LabelCameraFragment` | `LabelCameraViewModel` |
| `ProductDetailFragment` | `ProductDetailViewModel` |
| `FavoritesFragment` | `FavoritesViewModel` |
| `AnalysisResultFragment` | `AnalysisResultViewModel` |
| `ScanHistoryFragment` | `ScanHistoryViewModel` |
| `HealthProfileFragment` | `ProfileViewModel` |
| `RecipesFragment` | `RecipesViewModel` |
| `RecipeDetailFragment` | `RecipeDetailViewModel` |
| `AddEditRecipeFragment` | `AddEditRecipeViewModel` |
| `CookingModeFragment` | `CookingViewModel` |

Các adapter dùng ViewBinding: `ProductAdapter`, `ScanHistoryAdapter`, `RecipeCardAdapter`, `FlaggedIngredientAdapter`, `IngredientCheckAdapter`.

## 3. Phần bổ sung trên main

Kiến trúc MVVM và phần lớn ViewBinding đã tồn tại ở baseline. Lần hoàn thiện M2 này bổ sung:

1. `ProfileUiState` phân biệt lần đọc Room chưa trả kết quả (`isLoading = true`) và kết quả không có hồ sơ (`isLoading = false`, `profile = null`). Trước đây, giá trị `null` khởi tạo có thể khiến Fragment đánh dấu đã điền dữ liệu quá sớm và bỏ qua hồ sơ trả về sau đó.
2. ViewModel nạp lựa chọn bệnh/dị ứng; Fragment không gọi hàm nạp dữ liệu vào ViewModel từ observer. Danh sách mục tiêu ăn uống, giá trị mặc định và email tài khoản được đọc từ state của ViewModel.
3. Fragment lưu cờ đã điền form vào `savedInstanceState`, tránh coi mọi lần tạo lại view là đã nạp dữ liệu. Các ô nhập và nút lưu chờ lần đọc hồ sơ đầu tiên hoàn tất.
4. Bổ sung 5 unit tests hồi quy cho ViewModel hồ sơ, giữ các test hiện có.
5. Dọn chỉ mục `.idea`, bổ sung ignore cho cache Kotlin/build, file URL và hồ sơ M2. Không thay đổi lịch sử đóng góp đã có trên main.

## 4. Kiểm chứng

Các kiểm tra thực hiện trên máy phát triển, dùng Android SDK 37 và Gradle Wrapper của dự án:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1 --console=plain
```

- APK Debug: build thành công.
- Unit tests: **30 tests đạt**, không lỗi/failure/skipped; trong đó có **5 tests** của `ProfileViewModelTest`.
- Android lint: **0 lỗi, 33 cảnh báo**. Sau khi sửa đường dẫn SDK trong `local.properties`, đã chạy lại `:app:lintDebug --rerun-tasks` và đạt. File cấu hình máy này không nằm trong mã nguồn nộp. Chi tiết được ghi trong `Verification.json` và `lint-results-debug.xml` kèm bộ nộp; cảnh báo chủ yếu về phiên bản dependency và các gợi ý UI/tài nguyên, chưa xử lý toàn bộ trong phạm vi M2.
- Rà soát mã nguồn: 15 Fragment dùng ViewBinding, lifecycle-aware observer và giải phóng binding; 5 adapter dùng ViewBinding; không có `findViewById`; không có tham chiếu UI bị cấm trong 15 ViewModel.

Các tình huống hồi quy được kiểm tra: đọc hồ sơ bị trễ, database thực sự rỗng, cập nhật database khi người dùng đang sửa lựa chọn, chặn dữ liệu nhập không hợp lệ trước khi ghi và email cập nhật theo trạng thái xác thực.

Đây là kiểm tra mã nguồn, build, unit tests và lint cho M2. Phiên này không xác nhận lại toàn bộ luồng giao diện trên thiết bị hoặc chứng minh ứng dụng không có mọi loại memory leak.

## 5. Bộ nộp và cách mở

Các file được xuất riêng trong `submission/milestone02/`:

- `Group04_Milestone2_MVVM.zip`: mã nguồn từ commit M2, có project root để mở bằng Android Studio.
- `Repository_Link.txt`: URL công khai bắt buộc.
- `MILESTONE_02_REPORT.md`: báo cáo này.
- `SOURCE_REVISION.txt`, `Verification.json`, `SHA256SUMS.txt`: commit và kết quả kiểm tra bộ mã nguồn.
- `Group04_Milestone2_Submission.zip`: gói gom các file nộp trên, bao gồm báo cáo lint và kết quả unit tests.

ZIP loại `.idea`, `.git`, `.gradle`, thư mục build, `local.properties`, khóa ký và bộ nộp phát sinh. Sau khi giải nén, mở thư mục chứa `settings.gradle.kts` bằng Android Studio, chọn SDK/JDK phù hợp và để IDE tạo `local.properties` của máy. Gemini key chỉ cần khi kiểm tra AI online; cấu hình API key thật không được ghi vào Git.

Nhánh M2 chỉ được coi là đã xuất bản khi có push thành công. File URL và báo cáo không khẳng định nhánh cục bộ đã được đưa lên GitHub. Phạm vi AI hỗ trợ được khai báo tại `AI_USAGE.md`, mục 7; nhóm cần tự rà soát nội dung và giải thích được mã nguồn khi nộp.
