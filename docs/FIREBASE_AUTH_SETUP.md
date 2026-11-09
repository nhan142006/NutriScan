# Cấu hình Firebase Authentication cho NutriScan

Mã nguồn đã tích hợp Firebase Authentication. Các bước dưới đây cần thực hiện bằng tài khoản Firebase của nhóm vì không thể tự động hóa thông tin đăng nhập và quyền sở hữu project.

## 1. Kết nối ứng dụng Android

1. Mở [Firebase Console](https://console.firebase.google.com/) và tạo hoặc chọn project của nhóm.
2. Chọn **Project settings** > **General** > **Your apps** > **Add app** > **Android**.
3. Nhập Android package name chính xác: `vn.edu.vlu.milestone2`.
4. App nickname có thể đặt là `NutriScan`. SHA certificate chưa bắt buộc với Email/Password.
5. Tải file `google-services.json` và đặt đúng tại:

   ```text
   app/google-services.json
   ```

6. Sync Gradle và build lại ứng dụng. Build script sẽ tự áp dụng Google Services plugin khi file trên tồn tại.

## 2. Bật Email/Password

1. Trong Firebase Console, mở **Build** > **Authentication**.
2. Chọn **Get started** nếu project chưa dùng Authentication.
3. Mở tab **Sign-in method**.
4. Chọn **Email/Password**, bật tùy chọn đầu tiên **Email/Password**, rồi **Save**.
5. Không cần bật **Email link (passwordless sign-in)**.

## 3. Kiểm thử bắt buộc

1. Mở app: màn hình Đăng nhập phải xuất hiện và thanh điều hướng dưới được ẩn.
2. Thử email sai định dạng: app phải báo `Email không hợp lệ`.
3. Đăng ký tài khoản mới với mật khẩu từ 6 ký tự.
4. Sau khi đăng ký, app tự chuyển vào màn hình Hồ sơ.
5. Đóng hẳn rồi mở lại app: app phải tự chuyển vào màn hình Hồ sơ mà không yêu cầu đăng nhập lại.
6. Chọn **Đăng xuất**: app quay về màn hình Đăng nhập.
7. Thử mật khẩu sai: app phải báo `Email hoặc mật khẩu không đúng`.
8. Đăng nhập lại bằng tài khoản đúng.

## 4. Ảnh cần chụp để nộp

Chụp ít nhất ba ảnh sau:

1. **Project settings > General > Your apps**: thấy Android app và package `vn.edu.vlu.milestone2`.
2. **Authentication > Sign-in method**: thấy provider Email/Password ở trạng thái **Enabled**.
3. **Authentication > Users**: thấy email tài khoản vừa đăng ký và thời gian tạo/đăng nhập.

Nếu báo cáo được chia sẻ công khai, nên che bớt email cá nhân nhưng vẫn để giảng viên nhận biết có tài khoản đã được tạo.
