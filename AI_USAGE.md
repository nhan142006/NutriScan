# AI_USAGE.md — NutriScan, Group 04

## Thông tin sử dụng AI

| Nội dung | Ghi nhận |
| --- | --- |
| Công cụ trong phiên Milestone 05 | ChatGPT (Codex) |
| Ngày làm việc | 18/11/2026 |
| Phạm vi | Rà soát và bổ sung hiệu năng, bảo mật, ký Release và tài liệu nộp bài |
| Nền mã nguồn | Nhánh main tại commit `c11cab7`, đã có các module của ứng dụng |

Nhóm sử dụng AI để giải thích vấn đề, đề xuất hướng xử lý và hỗ trợ thực hiện các thay đổi theo yêu cầu. Người dùng cung cấp đề bài, mã nguồn và các tệp cần đối chiếu, xác định phạm vi công việc và quyết định việc xuất bản. Trong phiên này, Codex cũng trực tiếp hỗ trợ chỉnh sửa code, chạy kiểm tra và chuẩn bị các tệp phát hành.

Các nội dung bên dưới tóm tắt câu hỏi và hướng xử lý từ trao đổi thực tế.

## 1. Trao đổi về Milestone 05

| Câu hỏi / yêu cầu của người dùng | Hướng xử lý AI đưa ra |
| --- | --- |
| Các tệp APK, PDF, video và AI_USAGE cũ có đúng với bản main mới pull không? | Đối chiếu lịch sử Git, phiên bản và metadata của APK với mã nguồn; xác định bộ cũ cần được cập nhật theo main mới. |
| Làm Milestone 05 cho khớp với bản main hiện tại. | Kế thừa kiến trúc và chức năng đang có; chia công việc thành kiểm tra hiệu năng, bảo vệ dữ liệu và đóng gói Release. |
| Khóa Release là khóa gì, nằm ở đâu? | Giải thích vai trò của file keystore khi ký APK/AAB, cách cấu hình ký và yêu cầu giữ lại khóa cho các bản cập nhật. |
| Kiểm tra lại toàn bộ: có pull main rồi mới làm Milestone 05 không? | Kiểm tra reflog, quan hệ tổ tiên giữa main và nhánh M05, SHA trên GitHub, rồi đối chiếu APK/AAB, ZIP mã nguồn và báo cáo. |
| Dùng tài khoản Ngan3112 và đổi danh tính commit tương ứng. | Giới hạn việc đổi author/committer ở các commit M05 chưa push; giữ lịch sử main, tạo lại metadata và bộ nộp theo commit mới. |
| Viết lại AI_USAGE theo cách hỏi–đáp và làm rõ vai trò của người dùng. | Tóm tắt quá trình trao đổi, tách quyết định của người dùng khỏi phần AI hỗ trợ và ghi rõ phạm vi đã được kiểm chứng. |

## 2. Vai trò của người dùng và nhóm

Các nội dung người dùng đã thực hiện hoặc quyết định trong trao đổi:

- Cung cấp repository, yêu cầu của từng milestone, ảnh đề bài và các tệp cần kiểm tra.
- Cung cấp ảnh Firebase Console về cấu hình Authentication và tài khoản đã đăng ký.
- Yêu cầu dùng main mới làm nền cho M05, kiểm tra lại kết quả và điều chỉnh cách trình bày tài liệu.
- Chọn tài khoản GitHub Ngan3112, yêu cầu đổi danh tính commit và cho phép push nhánh M05.

Nhóm cần tự rà soát mã nguồn, hiểu và giải thích được các thay đổi, xác nhận WBS và phần đóng góp thủ công của từng thành viên trước khi nộp. Những việc này chỉ được ghi là đã hoàn thành khi nhóm thực sự xác nhận.

## 3. Phần AI hỗ trợ thực hiện trong M05

| Nhóm công việc | Phạm vi hỗ trợ |
| --- | --- |
| Rà soát mã nguồn | Đọc code và cấu hình hiện có, đối chiếu với yêu cầu M05, xác định các phần cần bổ sung trên main. |
| Hiệu năng và vòng đời UI | Hỗ trợ sửa cách giữ/thả adapter, ảnh và model trong RecyclerView; chuyển một số xử lý JSON sang luồng I/O; tích hợp LeakCanary ở Debug. |
| Bảo mật | Hỗ trợ cấu hình BuildConfig, chính sách backup/cleartext và cache phiên mã hóa bằng EncryptedSharedPreferences; xử lý UID, thời hạn cache và đăng xuất. |
| Release | Hỗ trợ cấu hình R8/ProGuard, shrink resources, ký APK/AAB và script quản lý khóa riêng ngoài Git. |
| Kiểm thử | Hỗ trợ tạo, chạy kiểm thử; thu số đo trên máy ảo; đối chiếu dữ liệu JSON sau R8, phiên đăng nhập và luồng sử dụng Release. |
| Tài liệu và đóng gói | Hỗ trợ soạn báo cáo, quay demo trên máy ảo, tạo ZIP và checksum, rồi cập nhật theo kết quả kiểm tra. |

Các thay đổi này bổ sung vào ứng dụng đã có trên main. Phạm vi cụ thể có thể đối chiếu qua diff của nhánh `milestone05-production-security`, các file kiểm thử và bằng chứng đi kèm.

## 4. Kết quả và giới hạn kiểm chứng

Codex đã hỗ trợ chạy kiểm chứng trong workspace; bằng chứng được lưu cùng bộ nộp:

- 25 unit tests và 6 device tests đạt, không có failure/error/skipped.
- Lint không có lỗi; còn 31 cảnh báo Debug và 32 cảnh báo Release.
- APK/AAB đã ký được kiểm tra chữ ký và đối chiếu metadata với mã nguồn.
- Demo trên máy ảo dài khoảng 142 giây, sử dụng tài khoản test và dữ liệu thử có nhãn rõ.
- Chưa xác nhận cuộn ổn định 60 fps; mẫu cuộn công thức có tỷ lệ jank cao hơn baseline.
- Chưa cấu hình Gemini API key thật nên mới kiểm chứng fallback offline.
- WBS của từng thành viên, GitHub Release/tag và link video công khai cần được nhóm hoàn tất.

Kết quả trên mô tả phạm vi đã đo và kiểm tra; nhóm cần tiếp tục đánh giá trên thiết bị và điều kiện sử dụng thực tế.

## 5. Các trao đổi ở milestone trước

Theo bản khai báo đã có trong repository, Milestone 04 sử dụng Claude để hỗ trợ phần code và tài liệu của sáu commit trong PR #3. Các trao đổi gồm cung cấp đề bài và repository, yêu cầu hỗ trợ phần mạng/cloud, hỏi về lệnh Git và chuẩn bị hồ sơ nộp.

Ở phần Firebase Authentication, người dùng cung cấp yêu cầu Login/Register, duy trì phiên và xử lý lỗi; AI hỗ trợ hướng triển khai Repository–ViewModel–ViewBinding và các thay đổi liên quan. Người dùng cung cấp ảnh Firebase Console để đối chiếu cấu hình.

Chi tiết khai báo trước lần biên tập này, gồm prompt và danh sách file, được giữ trong [AI_USAGE_HISTORY.md](docs/AI_USAGE_HISTORY.md). Các kết quả cũ về việc chưa build được thuộc phiên làm việc trước; kết quả M05 được ghi riêng ở mục 4.

## 6. Rà soát trước khi nộp

Nhóm cần kiểm tra lại phạm vi hỗ trợ AI, bổ sung thông tin thành viên và công việc thủ công có bằng chứng, đồng thời tự đánh giá mức sử dụng AI theo quy định môn học. Tài liệu này chưa đưa ra tỷ lệ phần trăm AI khi nhóm chưa có đánh giá xác nhận.

Khóa ký, mật khẩu và API key thật được giữ riêng ngoài Git và bộ nộp.
