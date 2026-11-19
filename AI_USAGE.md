# AI_USAGE.md — NutriScan, Group 04

## Thông tin sử dụng AI

| Nội dung | Ghi nhận |
| --- | --- |
| Công cụ | ChatGPT (Codex); Claude được ghi nhận trong khai báo lịch sử Milestone 04 |
| Ngày cập nhật khai báo | 20/11/2026 |
| Phạm vi ghi nhận | Hỏi–đáp, định hướng, rà soát; hỗ trợ trực tiếp một số phần code, kiểm thử, tài liệu và đóng gói nêu bên dưới |
| Nền mã nguồn M05 | Nhánh main tại commit `c11cab7`, đã có các module của ứng dụng |
| Bản Final được kiểm tra ngày 20/11 | Source và APK trong ZIP cùng ở `e541ded`; main công khai được đối chiếu ở `e1272da` |

Nhóm sử dụng AI để giải thích vấn đề, đề xuất hướng xử lý và hỗ trợ thực hiện các thay đổi theo yêu cầu. Người dùng cung cấp đề bài, mã nguồn và các tệp cần đối chiếu, xác định phạm vi công việc và quyết định việc xuất bản. Trong phiên này, Codex cũng trực tiếp hỗ trợ chỉnh sửa code, chạy kiểm tra và chuẩn bị các tệp phát hành.

Các nội dung bên dưới tóm tắt câu hỏi và hướng xử lý từ trao đổi thực tế.

## Mức tự ước lượng của nhóm và giới hạn kiểm chứng

Theo thông tin người dùng cung cấp, nhóm đưa ra mức tự ước lượng sử dụng AI **không quá 46%** thông qua trao đổi nội bộ giữa các thành viên. Đây là **ước lượng do nhóm tự khai**, chưa có bảng thống kê khối lượng hoặc phương pháp tính để kiểm chứng độc lập.

Mức tự khai này thấp hơn giới hạn **50%** được nêu trong ảnh yêu cầu Final. Tuy nhiên, trao đổi nội bộ chưa đủ để chứng minh tỷ lệ thực tế hoặc bảo đảm không vượt giới hạn, cho toàn dự án hay cho từng milestone. Tài liệu ghi nhận ý kiến tự đánh giá của nhóm, không chứng nhận một tỷ lệ đã đo. Nhóm cần làm rõ phạm vi tính và căn cứ nếu dùng mức này trong bản nộp.

Phạm vi hỗ trợ thực tế gồm hỏi–đáp và định hướng, đồng thời có trực tiếp sửa code, tạo/chạy kiểm thử và chuẩn bị tài liệu/phát hành như các mục bên dưới. Chưa định lượng mức sửa code so với tổng khối lượng công việc của nhóm. Phần mã nguồn có sẵn được đối chiếu với khai báo lịch sử khi đánh giá tỷ lệ toàn dự án.

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

Kết quả M05 được ghi nhận ngày 18/11/2026, với Codex hỗ trợ chạy kiểm chứng trong workspace; không tự áp dụng những kết quả này cho main mới hoặc cho bản Final chưa được kiểm thử lại:

- 25 unit tests và 6 device tests đạt, không có failure/error/skipped.
- Lint không có lỗi; còn 31 cảnh báo Debug và 32 cảnh báo Release.
- APK/AAB đã ký được kiểm tra chữ ký và đối chiếu metadata với mã nguồn.
- Demo trên máy ảo dài khoảng 142 giây, sử dụng tài khoản test và dữ liệu thử có nhãn rõ.
- Chưa xác nhận cuộn ổn định 60 fps; mẫu cuộn công thức có tỷ lệ jank cao hơn baseline.
- Chưa cấu hình Gemini API key thật nên mới kiểm chứng fallback offline.
- Tại lần đối chiếu ngày 20/11, Release/tag `v1.0.0-release` đã tồn tại công khai và APK trong ZIP khớp asset Release. WBS/phần việc thủ công của từng thành viên vẫn cần được nhóm xác nhận; việc có Release không chứng minh tỷ lệ sử dụng AI.

Kết quả trên mô tả phạm vi đã đo và kiểm tra; nhóm cần tiếp tục đánh giá trên thiết bị và điều kiện sử dụng thực tế.

## 5. Các trao đổi ở milestone trước

Theo bản khai báo đã có trong repository, Milestone 04 sử dụng Claude để hỗ trợ phần code và tài liệu của sáu commit trong PR #3. Các trao đổi gồm cung cấp đề bài và repository, yêu cầu hỗ trợ phần mạng/cloud, hỏi về lệnh Git và chuẩn bị hồ sơ nộp.

Ở phần Firebase Authentication, người dùng cung cấp yêu cầu Login/Register, duy trì phiên và xử lý lỗi; AI hỗ trợ hướng triển khai Repository–ViewModel–ViewBinding và các thay đổi liên quan. Người dùng cung cấp ảnh Firebase Console để đối chiếu cấu hình.

Chi tiết khai báo trước lần biên tập này, gồm prompt và danh sách file, được giữ trong [AI_USAGE_HISTORY.md](docs/AI_USAGE_HISTORY.md). Các kết quả cũ về việc chưa build được thuộc phiên làm việc trước; kết quả M05 được ghi riêng ở mục 4.

## 6. Rà soát trước khi nộp

Nhóm cần kiểm tra lại phạm vi hỗ trợ AI, bổ sung thông tin thành viên và công việc thủ công có bằng chứng, đồng thời tự đánh giá mức sử dụng AI theo quy định môn học. Trước khi xác nhận mức thực tế, cần có:

- Phạm vi được tính: toàn dự án hay từng bài/milestone.
- Tiêu chí, đơn vị và cách tính được môn học chấp nhận; bao gồm code, kiểm thử, tài liệu và các phần khác thuộc phạm vi tính.
- Phân công của từng thành viên, phần tự thực hiện và phần AI hỗ trợ, đối chiếu được với công việc thực tế.
- Xác nhận của nhóm về kết quả tính; không suy tỷ lệ từ tên tác giả commit hoặc tự đặt trọng số để đạt một ngưỡng mong muốn.

Trạng thái hiện tại: **nhóm tự ước lượng không quá 46% qua trao đổi nội bộ; chưa có thống kê định lượng để kiểm chứng**. Việc ghi nhận ước lượng này không thay thế đối chiếu phạm vi AI thực tế ở các mục trên.

Khóa ký, mật khẩu và API key thật được giữ riêng ngoài Git và bộ nộp.

## 7. Bổ sung Milestone 02 — 19/11/2026

| Câu hỏi / yêu cầu của người dùng | Hướng xử lý và phạm vi AI hỗ trợ |
| --- | --- |
| Hoàn thiện M2 cho tôi. | Đối chiếu MVVM, ViewBinding, vòng đời Fragment và quy tắc loại file IDE/build theo đề bài đã cung cấp. Tạo nhánh cục bộ `feature/milestone02-mvvm` từ `main` tại `e541ded6dc810d93c63b2101cefab72fcf5908d4`. |
| Hướng xử lý phần còn thiếu | Giữ kiến trúc MVVM đã có; phân biệt trạng thái đang tải/chưa có hồ sơ; chuyển dữ liệu mặc định và email tài khoản qua StateFlow của ViewModel; bổ sung hồi quy cho luồng này. Loại `.idea` khỏi chỉ mục Git, giữ file IDE trên máy; bổ sung file URL, báo cáo M2 và ZIP mã nguồn sạch. |

Trong phần M2 này, Codex trực tiếp hỗ trợ chỉnh sửa các file trên, chạy build/unit tests/lint và đóng gói mã nguồn theo yêu cầu. Không quy toàn bộ mã nguồn hiện có cho phiên hỗ trợ M2. Người dùng cung cấp đề bài, repository và quyết định phạm vi công việc. Chưa có xác nhận về kiểm thử thủ công hoặc phân công WBS mới của từng thành viên; nhóm cần tự kiểm tra và giải thích được phần mã nguồn trước khi nộp. Không tự đặt tỷ lệ sử dụng AI hay bổ sung lịch sử đóng góp không có bằng chứng.

## 8. Đối chiếu Final — 20/11/2026

| Câu hỏi / yêu cầu của người dùng | Hướng xử lý và phần AI hỗ trợ |
| --- | --- |
| Kiểm tra phần Final theo ảnh yêu cầu. | Đọc bộ ZIP, source, metadata/chữ ký APK, báo cáo PDF, slide PPTX và GitHub; đối chiếu bốn nhóm file bắt buộc, revision và các luồng cần demo. |
| Xác định còn thiếu gì và sửa ở đâu. | Lập báo cáo chỉ rõ vị trí cần cập nhật kết quả kiểm thử, trách nhiệm thành viên, kịch bản demo và các rủi ro dữ liệu theo tài khoản. |

Phiên kiểm tra Final này không sửa mã nguồn ứng dụng, không thay bộ nộp gốc và không chạy lại toàn bộ UI/device tests. Tài liệu kiểm tra do Codex hỗ trợ soạn. Việc biên tập khai báo hiện tại cũng có AI hỗ trợ. Khai báo lịch sử chi tiết được giữ nguyên tại [docs/AI_USAGE_HISTORY.md](docs/AI_USAGE_HISTORY.md).

## 9. Chuẩn bị bản Final sau rà soát — 20/11/2026

Người dùng yêu cầu dọn nhẹ file phụ, giữ bài làm chính và bảo đảm các chức năng, báo cáo cùng hồ sơ theo đề Final. Codex hỗ trợ tích hợp main `e1272da` với M2 trên nhánh `final-readiness`, xử lý xung đột màn hình hồ sơ để giữ cả trạng thái tải và nhãn Việt/Anh, cập nhật phiên bản 2.0.2 và chuẩn bị kiểm thử/tài liệu/đóng gói. Phạm vi này có trực tiếp sửa code và tài liệu. Nó là công việc tiếp theo, tách khỏi phiên kiểm tra chỉ đọc ở mục 8.

Người dùng yêu cầu sử dụng khóa ký ổn định và khai báo đúng trong báo cáo. Bản Final mới dùng keystore riêng đang có trên máy; không tạo khóa mới hoặc đưa khóa/mật khẩu vào bài nộp. APK cũ có chữ ký khác được giữ nguyên. File phụ được lưu trữ có thể khôi phục; không xóa lịch sử đóng góp hoặc khai báo AI.
