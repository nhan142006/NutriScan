# Kết quả kiểm tra Thành viên 1 — Firebase Authentication

Ngày kiểm tra: 09/11/2026 (UTC+7).

Phạm vi: các yêu cầu Firebase Authentication của Thành viên 1. Kiểm tra cấu hình, đọc mã, build, lint, unit test hiện có và thao tác trực tiếp trên emulator Pixel_6, Android API 37, với Firebase thật.

## Đối chiếu yêu cầu

| Yêu cầu | Kết quả | Bằng chứng |
| --- | --- | --- |
| Kết nối Android với Firebase | Đạt | `app/google-services.json`: project `nutriscan-6381d`, package `vn.edu.vlu.milestone2`; tác vụ `processDebugGoogleServices` và APK build thành công; đăng nhập Firebase thật thành công. |
| Màn hình Login và Register | Đạt | `LoginFragment`, `RegisterFragment` và hai XML layout; đã mở và chụp giao diện trên emulator. |
| Email & Password | Đạt | Repository dùng `signInWithEmailAndPassword` và `createUserWithEmailAndPassword`. Đăng ký tài khoản mới qua UI đã thành công trong phiên kiểm thử trước của cuộc hội thoại; lần kiểm tra này xác nhận lại đăng nhập và xử lý email đã đăng ký. |
| Duy trì đăng nhập | Đạt | Firebase `currentUser`; đã force-stop rồi mở lại app, tự vào Hồ sơ và hiển thị đúng email. |
| Sai mật khẩu, email không hợp lệ | Đạt | Đã nhập dữ liệu trên UI và xác nhận thông báo lỗi; không chuyển vào Hồ sơ khi xác thực thất bại. |
| AuthRepository, AuthViewModel, ViewBinding UI | Đạt | Có các lớp tương ứng; `viewBinding = true`; Fragment dùng binding và xóa tham chiếu trong `onDestroyView`. |
| Đăng xuất | Đạt | Firebase `signOut`; về Login, ẩn điều hướng; Back không trở về Hồ sơ; mở lại app vẫn ở Login. |
| Ảnh Firebase Console | Đạt theo ảnh người dùng đã gửi | Một ảnh Email/Password ở trạng thái Enabled và một ảnh Users có hai tài khoản, Created, Signed In và User UID. |

## Các ca UI chạy lại trong lần kiểm tra này

14 ca đều đạt:

1. Mở Login khi chưa đăng nhập; ẩn bottom navigation.
2. Từ chối email và mật khẩu bỏ trống.
3. Từ chối email sai định dạng.
4. Firebase từ chối mật khẩu sai; app hiển thị lỗi dễ hiểu.
5. Mở Register có xác nhận mật khẩu; ẩn bottom navigation.
6. Từ chối mật khẩu dưới 6 ký tự.
7. Từ chối xác nhận mật khẩu không khớp.
8. Firebase từ chối đăng ký bằng email đã tồn tại; app hiển thị đúng lỗi.
9. Quay từ Register về Login.
10. Đăng nhập Firebase thật; Hồ sơ hiển thị đúng email và bottom navigation.
11. Force-stop/mở lại app vẫn đăng nhập.
12. Đăng xuất về Login và ẩn bottom navigation.
13. Nhấn Back sau đăng xuất không mở lại Hồ sơ đã xác thực.
14. Force-stop/mở lại sau đăng xuất vẫn yêu cầu đăng nhập.

Tài khoản thử nghiệm được dùng lại: `nutriscan.ui.20261109.1940@example.com`. Lần kiểm tra này không tạo thêm tài khoản Firebase. Kiểm tra crash buffer của emulator không thấy crash của package ứng dụng.

## Build và chất lượng mã

Lệnh đã chạy thành công:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Lint: 0 lỗi, 25 cảnh báo về phiên bản dependency, chuỗi hardcode, tài nguyên chưa dùng, overdraw và version catalog. Các cảnh báo không làm thất bại yêu cầu Authentication đã kiểm thử.
- Unit test: 1 test mẫu `ExampleUnitTest.addition_isCorrect` đạt. Chưa có bộ unit test riêng cho AuthRepository/AuthViewModel; bằng chứng chức năng Auth ở trên đến từ kiểm thử UI với Firebase thật.
- `git diff --check`: không có lỗi khoảng trắng; có thông báo chuyển LF/CRLF của Git.

## Ảnh và giới hạn bằng chứng

Ảnh UI đã lưu tại `app/build/auth-audit/`: `01-login.png`, `02-invalid-email.png`, `03-wrong-password.png`, `04-register.png`, `05-existing-email.png`, `06-signed-in.png`, `07-session-restored.png`, `08-logged-out.png`.

Các ảnh này là artifact kiểm thử trong thư mục build, có thể bị xóa khi clean. Hai ảnh Firebase Console đã được xem trong chat, chưa được lưu thành file trong repository. Cần giữ bản ảnh Console của người dùng để đính kèm bài nộp.

Kết quả: không phát hiện lỗi cản trở các yêu cầu Thành viên 1 trong các ca đã kiểm thử. Đã hoàn thành chức năng và có minh chứng Console trong cuộc hội thoại; cần lưu hai ảnh Console cùng bài nộp.
