# Bản source tối giản — 29/10/2026

Thanh yêu cầu một folder chỉ chứa source và các thành phần cần build/chạy, không Git/tài liệu đồ án/log/test/artifact. Đã tạo E:/Nam3/WEB/UTETra_Source cạnh repo, gồm src/main, pom/wrapper, JDK21, cloudflared, năm helper runtime và setup/RUN/STOP, cấu hình mẫu và hướng dẫn chạy.

Không nhân bản credentials thật: automatic approval review từ chối chép cấu hình SMTP và khóa QR hiện tại. Bản thay thế sanitize application properties sang env/config mẫu, khóa JWT/OTP mới sinh ở máy chạy. Không đọc hoặc in giá trị SMTP thật.

Maven package ngay trong folder xuất đã BUILD SUCCESS bằng JDK21, không dùng src/test (không nằm trong bản giao). Đã so SHA256 source/resources ngoài hai file cấu hình được sanitize; target/log kiểm tra đã xóa theo đường dẫn xác minh. Không khẳng định server chạy đầy đủ khi chưa có cấu hình DB/SMTP/Cloudinary. Chi tiết kiểm ở .local/source-export-verification.json. Không push folder hoặc runtime/secrets lên Git.

Thanh hỏi QR có push Git được không: phần code QR/Cloudflare đã nằm trên main; khóa/tunnel state và credentials không thuộc source public.
