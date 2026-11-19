# NutriScan — Checklist Final, Nhóm 04

## Mã nguồn và phiên bản

- Nhánh chuẩn bị: `final-readiness`, tích hợp main `e1272da` và M2 `73e1571`.
- Ứng dụng: package `vn.edu.vlu.milestone2`, version 2.0.2, code 4.
- Không đưa `.idea`, `.git`, build/cache, `local.properties`, khóa ký hoặc file test riêng vào source ZIP.
- Khóa ký dùng file riêng đang cấu hình tại máy. Bản Final mới có chữ ký khác APK 2.0.1 trong bộ ZIP ngày 18/11; giữ nguyên APK cũ, không coi bản mới là cập nhật cài đè tương thích chữ ký.

## Bốn nhóm file bắt buộc

1. `Repository_Link.txt`: URL repository công khai và revision của source được đóng gói. Chỉ ghi nhánh đã được xuất bản khi có push thành công.
2. `NutriScan_SourceCode.zip`: Android Studio project đã làm sạch.
3. `Nhom04_NutriScan_Release.apk`: Release đã ký, cùng source revision với ZIP.
4. `NutriScan_Final_Project_Report.pdf` và `Nhom04_NutriScan.pptx`: MVVM, Room schema, REST endpoints, Firebase, WBS, kết quả kiểm thử và giới hạn thực tế.

`AI_USAGE.md` đi cùng source/hồ sơ theo yêu cầu khai báo. Tỷ lệ 46% là ước lượng do nhóm tự khai qua trao đổi nội bộ, chưa phải số đo đã kiểm chứng; phạm vi AI trực tiếp thực hiện được giữ đúng.

## Diễn tập 15 phút

| Thời gian | Nội dung |
| --- | --- |
| 0–3 phút | Vấn đề, kiến trúc MVVM, Room/API/Firebase và WBS; chỉ trình bày các slide chính. |
| 3–4:30 | Register/Login, mở lại app vẫn giữ phiên, Logout và đăng nhập lại. |
| 4:30–6 | Gọi API thật: Loading, Success và lỗi mạng có kiểm soát; thao tác chưa cache để minh họa lỗi. |
| 6–8 | Lưu dữ liệu Room, bật Airplane Mode, đóng/mở app và đọc lại dữ liệu đã lưu. |
| 8–10 | Ảnh từ URL/Glide cache, recipe/cooking; giới thiệu queue ảnh nếu còn thời gian. |
| 10–15 | Mỗi thành viên giải thích feature slice và trả lời câu hỏi độc lập. |

## Những giới hạn phải trình bày đúng

- Gemini online cần key hợp lệ và mạng; phân tích barcode có fallback local. Không trình bày dữ liệu mẫu như kết quả API thật.
- Favorites/history/recipes hiện là dữ liệu local dùng chung trên thiết bị, chưa phân vùng UID; hồ sơ sức khỏe phân vùng theo UID.
- Có destructive-migration fallback; cần explicit migration khi nâng schema mà phải giữ dữ liệu.
- Unit tests, kiểm tra vòng đời và phép đo hữu hạn không chứng minh toàn ứng dụng luôn zero leak hoặc ổn định 60 fps.
- Kết quả cuối ghi trong `submission/final/VERIFICATION.json`; không dùng kết quả test của snapshot cũ để mô tả snapshot mới.

## Dọn thư mục

Chỉ gom bản nháp/audit/log và công cụ kiểm tra dùng một lần vào `.local-archive/`. Giữ code, cấu hình Gradle, tài liệu học phần, tests, các script ký/đóng gói có ích và các bộ nộp cũ. Không xóa khóa ký hoặc lịch sử khai báo AI.
