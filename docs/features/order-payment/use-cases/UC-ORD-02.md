# UC-ORD-02 — Sửa/xóa/hợp nhất giỏ

> Actor: Guest, User, Vendor. Owner: feature/order-payment. P0 bắt buộc. Đặc tả ban đầu được giữ để đối chiếu; kết quả triển khai và kiểm chứng thực tế ở phần hồ sơ bên dưới.

## Đặc tả chức năng

Mục tiêu và điều kiện nghiệm thu: Giỏ database cho người đăng nhập, session cho Guest; merge chống lặp.

Nguồn: [Danh mục và BR](../../../../USE_CASES.md), [từ điển dữ liệu](../../../../DATABASE_DESIGN.md), [quy định chung](../../../QUY_DINH_VA_CAU_HINH_CHUNG.md). Nền và contract: [COMMON_BASELINE](../../../COMMON_BASELINE.md).

- Kích hoạt: actor chọn chức năng sửa/xóa/hợp nhất giỏ trên giao diện hoặc gọi route tương ứng.
- Tiền điều kiện: nền main đã đồng bộ; bảng cần dùng có migration đã review; dữ liệu tham chiếu tồn tại. Public chỉ đọc dữ liệu được duyệt; thao tác bảo vệ cần tài khoản active/verified, phiên hợp lệ và đúng phạm vi.
- Đầu vào: itemId, quantity, version, mergeKey. ID và số tiền từ client không thay thế kiểm quyền/tính toán phía server.
- Dữ liệu: `carts, cart_items, cart_item_toppings`. Ngoài ba bảng identity V1, đây là các bảng cần triển khai, chưa được xem là đã có.
- Contract dự kiến: `CartService`; route dự kiến: `/cart`. Chốt DTO/signature và migration trên main trước khi code; không tự tạo Service trùng chức năng module khác.
- Hậu điều kiện thành công: đạt điều kiện nghiệm thu nêu trên; trạng thái và dữ liệu nhất quán, trả DTO/view đã escape. GET không thay đổi dữ liệu nghiệp vụ; ghi nhiều bảng phải có transaction và kiểm quyền trước ghi.
- Hậu điều kiện thất bại: không ghi dở/mất dữ liệu; lỗi validation/quyền/version rõ ràng; provider thất bại có pending/retry hoặc lỗi thật, không giả thành công.

### Luồng chính

1. Kiểm owner cart.
2. sửa/xóa dưới version.
3. khi login merge guest cart bằng key.
4. giữ dữ liệu đúng shop.

### Sơ đồ hoạt động dự kiến

```mermaid
flowchart TD
    Start([Bắt đầu]) --> Permission{Đúng điều kiện và quyền?}
    Permission -->|Không| Denied[Từ chối; giữ dữ liệu]
    Permission -->|Có| S1
    S1["Kiểm owner cart"]
    S1 --> S2
    S2["sửa/xóa dưới version"]
    S2 --> S3
    S3["khi login merge guest cart bằng key"]
    S3 --> S4
    S4["giữ dữ liệu đúng shop"]
    S4 --> Result([Trả kết quả])
    Denied --> Result
```

Sơ đồ mô tả luồng thiết kế; các nhánh lỗi/cạnh tranh cụ thể ở mục tiếp theo. Cập nhật theo code thực và xuất ảnh khi lập báo cáo.

### Luồng thay thế, lỗi và cạnh tranh

ID cart khác; stale version conflict; merge retry không nhân đôi; không âm thầm xóa giỏ khác shop. Request thay đổi phải có CSRF; dùng version/unique/locking theo schema khi ghi. Event ra ngoài transaction chỉ phát sau commit; không đưa credential, OTP rõ, token hoặc password vào log/DTO.

### Kế hoạch nghiệm thu ban đầu (không phải kết quả chạy)

| Case | Mong đợi | Kết quả hiện tại |
| --- | --- | --- |
| Luồng chính với itemId, quantity, version, mergeKey hợp lệ | Giỏ database cho người đăng nhập, session cho Guest; merge chống lặp | Chưa chạy — UC chưa triển khai |
| Input hoặc tham chiếu không hợp lệ | Lỗi rõ ràng, không ghi dở | Chưa chạy |
| Actor/owner/scope không hợp lệ | 401/403 hoặc 404 theo contract, không lộ dữ liệu | Chưa chạy |
| Tình huống đặc thù: ID cart khác; stale version conflict; merge retry không nhân đôi; không âm thầm xóa giỏ khác shop | Tuân thủ luồng lỗi và tính nhất quán nêu trên | Chưa chạy |

### Triển khai theo ba tầng

1. Chốt DTO và các bảng `carts, cart_items, cart_item_toppings`; nhóm trưởng đặt version migration duy nhất trên main, giữ nguyên V1 đã áp dụng.
2. Entity/Repository ánh xạ FK/unique/version và query scoped; map DTO trong Service khi `open-in-view=false`.
3. `CartService` xử lý luồng chính, validation/quyền/transaction và các lỗi đặc thù ở trên.
4. Controller cho `/cart` nhận DTO, principal và CSRF; JSP dùng Bootstrap/layout chung, hiển thị loading/empty/error.
5. Viết và chạy các case nghiệm thu; bổ sung code path/symbol/commit và ảnh chạy thật vào các mục hồ sơ bên dưới.

Các ô minh chứng bên dưới được điền khi có kết quả thực; không dùng đặc tả hoặc sơ đồ dự kiến làm bằng chứng chức năng hoàn thành.

## 1. Hồ sơ triển khai ngày 14/10/2026

- Nhánh: `feature/order-payment`; baseline trước triển khai: `bb700d1`; commit code: `29aac50`. Người/reviewer trong nhóm chưa được cung cấp, không tự gán tên.
- Trạng thái: **Đã có code và kiểm chứng với dữ liệu test; chờ review/ghép các module phụ thuộc**.
- Prompt: [ORD-AI-001 đến ORD-AI-008](../../../AI_PROMPT_HISTORY.md#ord-ai-001); [phản hồi và sửa lỗi](../AI_LOG.md).
- Quy tắc thực hiện: Giỏ DB và session; kiểm owner/version; merge key chống lặp; khác shop phải xác nhận xóa, không mất giỏ khi merge lỗi.

## 2. Code và hướng dẫn theo ba tầng

1. [V2 order-payment](../../../../src/main/resources/db/migration/mysql/V2__order_payment.sql) tạo các bảng thuộc nhánh; [schema đề xuất](../SCHEMA_PROPOSAL.md) giải thích phần phụ thuộc không tự migrate. Entity và `OrderDataRepository` map bảng/unique/version; `CommerceReferenceRepository` đọc contract catalog/identity.
2. Service: `CartService.change / merge / clear; GuestCartService.change`. [CartService](../../../../src/main/java/com/utetra/order/service/CartService.java); [GuestCartService](../../../../src/main/java/com/utetra/order/service/GuestCartService.java) Service kiểm identity/scope và ràng buộc trước ghi; các mutation DB sử dụng transaction READ_COMMITTED, row locks và unique/version. Nhánh lỗi rollback dữ liệu, thông báo được lưu cùng transaction; gửi socket sau commit.
3. Controller: `CartController`; route thực: `/cart/items/{id}, /cart/merge, /cart/clear`. Lấy actor từ principal, không nhận userId/role/số tiền quyết định từ client. DTO validation và CSRF cho mutation.
4. View: `src/main/webapp/WEB-INF/views/order/cart.jsp`; `orders.js` gọi endpoint JSON/multipart, hiển thị lỗi bằng textContent và dùng layout/Bootstrap chung. Dữ liệu JSP escape bằng c:out. Giao diện/endpoint dựa cùng Service để giữ quy tắc khi gọi ngoài trình duyệt.
5. Kiểm thử có thể chạy lại bằng [SETUP](../SETUP.md). Tên case thực: `guestMergeIsAtomicAndIdempotent`. [VERIFICATION](../VERIFICATION.md) phân biệt Maven/H2, MySQL, Playwright và provider thật chưa chạy.

## 3. Sơ đồ và minh chứng

Activity Mermaid ở phần đặc tả là nguồn thiết kế ban đầu; kiểm tra các nhánh lỗi/quyền theo Service và test ở mục 2. [Sơ đồ module](../assets/order-architecture.svg) và [nguồn Graphviz](../assets/order-architecture.dot) chỉ mô tả ranh giới ba tầng/transaction thực, không thay activity riêng của từng UC trong báo cáo cuối kỳ.

Ảnh riêng cho UC này chưa chụp; xem ảnh luồng liên quan trong [VERIFICATION](../VERIFICATION.md). Không coi ảnh/sơ đồ thiết kế là screenshot chạy.

## 4. Setup, giới hạn và bàn giao

Đọc [SETUP](../SETUP.md) về dữ liệu catalog/address/identity/session/loyalty/audit cần ghép. VNPAY hoãn; Cloudinary chưa cấu hình/test provider thật. Shipper cần `features.shipper.enabled=true`. Test fixtures chỉ dành cho H2 in-memory hoặc DB MySQL localhost `utetra_order_*` có opt-in, không chạy reset/seed trên utetra_db.

Review schema/version migration và contract cross-module trên integration trước khi ghép main; lần này chỉ thay feature/order-payment. DOCX, ảnh source có callout, sơ đồ activity xuất riêng và reviewer cá nhân cần nhóm hoàn thiện khi nộp báo cáo; không ghi đã có nếu chưa làm.
