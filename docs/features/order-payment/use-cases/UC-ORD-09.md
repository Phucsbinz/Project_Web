# UC-ORD-09 — Yêu cầu trả hàng–hoàn tiền

> Actor: User, Vendor. Owner: feature/order-payment. P0 bắt buộc. Đặc tả ban đầu được giữ để đối chiếu; kết quả triển khai và kiểm chứng thực tế ở phần hồ sơ bên dưới.

## Đặc tả chức năng

Mục tiêu và điều kiện nghiệm thu: Đơn đã giao, cửa sổ cấu hình, lý do/chứng cứ, theo dõi kết quả thật.

Nguồn: [Danh mục và BR](../../../../USE_CASES.md), [từ điển dữ liệu](../../../../DATABASE_DESIGN.md), [quy định chung](../../../QUY_DINH_VA_CAU_HINH_CHUNG.md). Nền và contract: [COMMON_BASELINE](../../../COMMON_BASELINE.md).

- Kích hoạt: actor chọn chức năng yêu cầu trả hàng–hoàn tiền trên giao diện hoặc gọi route tương ứng.
- Tiền điều kiện: nền main đã đồng bộ; bảng cần dùng có migration đã review; dữ liệu tham chiếu tồn tại. Public chỉ đọc dữ liệu được duyệt; thao tác bảo vệ cần tài khoản active/verified, phiên hợp lệ và đúng phạm vi.
- Đầu vào: orderId, reason, evidence. ID và số tiền từ client không thay thế kiểm quyền/tính toán phía server.
- Dữ liệu: `return_requests, refund_transactions, orders, payment_transactions`. Ngoài ba bảng identity V1, đây là các bảng cần triển khai, chưa được xem là đã có.
- Contract dự kiến: `RefundService`; route dự kiến: `/orders/{id}/returns`. Chốt DTO/signature và migration trên main trước khi code; không tự tạo Service trùng chức năng module khác.
- Hậu điều kiện thành công: đạt điều kiện nghiệm thu nêu trên; trạng thái và dữ liệu nhất quán, trả DTO/view đã escape. GET không thay đổi dữ liệu nghiệp vụ; ghi nhiều bảng phải có transaction và kiểm quyền trước ghi.
- Hậu điều kiện thất bại: không ghi dở/mất dữ liệu; lỗi validation/quyền/version rõ ràng; provider thất bại có pending/retry hoặc lỗi thật, không giả thành công.

### Luồng chính

1. Kiểm owner DELIVERED và cửa sổ return.
2. tạo unique request.
3. Vendor review.
4. return/waive.
5. refund pending.
6. tiền hoàn thật.
7. ledger/state.

### Sơ đồ hoạt động dự kiến

```mermaid
flowchart TD
    Start([Bắt đầu]) --> Permission{Đúng điều kiện và quyền?}
    Permission -->|Không| Denied[Từ chối; giữ dữ liệu]
    Permission -->|Có| S1
    S1["Kiểm owner DELIVERED và cửa sổ return"]
    S1 --> S2
    S2["tạo unique request"]
    S2 --> S3
    S3["Vendor review"]
    S3 --> S4
    S4["return/waive"]
    S4 --> S5
    S5["refund pending"]
    S5 --> S6
    S6["tiền hoàn thật"]
    S6 --> S7
    S7["ledger/state"]
    S7 --> Result([Trả kết quả])
    Denied --> Result
```

Sơ đồ mô tả luồng thiết kế; các nhánh lỗi/cạnh tranh cụ thể ở mục tiếp theo. Cập nhật theo code thực và xuất ảnh khi lập báo cáo.

### Luồng thay thế, lỗi và cạnh tranh

Quá hạn/đơn chưa giao/request trùng; sai owner; từ chối có lý do; refund sum không vượt paid; 3 giờ kể từ delivered_at, đúng mốc 3 giờ bị từ chối (ORD-AI-004). Request thay đổi phải có CSRF; dùng version/unique/locking theo schema khi ghi. Event ra ngoài transaction chỉ phát sau commit; không đưa credential, OTP rõ, token hoặc password vào log/DTO.

### Kế hoạch nghiệm thu ban đầu (không phải kết quả chạy)

| Case | Mong đợi | Kết quả hiện tại |
| --- | --- | --- |
| Luồng chính với orderId, reason, evidence hợp lệ | Đơn đã giao, cửa sổ cấu hình, lý do/chứng cứ, theo dõi kết quả thật | Chưa chạy — UC chưa triển khai |
| Input hoặc tham chiếu không hợp lệ | Lỗi rõ ràng, không ghi dở | Chưa chạy |
| Actor/owner/scope không hợp lệ | 401/403 hoặc 404 theo contract, không lộ dữ liệu | Chưa chạy |
| Tình huống đặc thù: Quá hạn/đơn chưa giao/request trùng; sai owner; từ chối có lý do; refund sum không vượt paid; 3 giờ kể từ delivered_at, đúng mốc 3 giờ bị từ chối (ORD-AI-004) | Tuân thủ luồng lỗi và tính nhất quán nêu trên | Chưa chạy |

### Triển khai theo ba tầng

1. Chốt DTO và các bảng `return_requests, refund_transactions, orders, payment_transactions`; nhóm trưởng đặt version migration duy nhất trên main, giữ nguyên V1 đã áp dụng.
2. Entity/Repository ánh xạ FK/unique/version và query scoped; map DTO trong Service khi `open-in-view=false`.
3. `RefundService` xử lý luồng chính, validation/quyền/transaction và các lỗi đặc thù ở trên.
4. Controller cho `/orders/{id}/returns` nhận DTO, principal và CSRF; JSP dùng Bootstrap/layout chung, hiển thị loading/empty/error.
5. Viết và chạy các case nghiệm thu; bổ sung code path/symbol/commit và ảnh chạy thật vào các mục hồ sơ bên dưới.

Các ô minh chứng bên dưới được điền khi có kết quả thực; không dùng đặc tả hoặc sơ đồ dự kiến làm bằng chứng chức năng hoàn thành.

## 1. Hồ sơ triển khai ngày 14/10/2026

- Nhánh: `feature/order-payment`; baseline trước triển khai: `bb700d1`; commit code: `29aac50`. Người/reviewer trong nhóm chưa được cung cấp, không tự gán tên.
- Trạng thái: **Đã có code và kiểm chứng với dữ liệu test; chờ review/ghép các module phụ thuộc**.
- Prompt: [ORD-AI-001 đến ORD-AI-008](../../../AI_PROMPT_HISTORY.md#ord-ai-001); [phản hồi và sửa lỗi](../AI_LOG.md).
- Quy tắc thực hiện: DELIVERED và PAID, owner đúng, now < delivered_at + 3 giờ; một request/đơn; lý do bắt buộc, ảnh tùy chọn; chỉ hoàn cả đơn sau duyệt/nhận trả hoặc waive và có chứng cứ đã hoàn COD.

## 2. Code và hướng dẫn theo ba tầng

1. [V2 order-payment](../../../../src/main/resources/db/migration/mysql/V2__order_payment.sql) tạo các bảng thuộc nhánh; [schema đề xuất](../SCHEMA_PROPOSAL.md) giải thích phần phụ thuộc không tự migrate. Entity và `OrderDataRepository` map bảng/unique/version; `CommerceReferenceRepository` đọc contract catalog/identity.
2. Service: `OrderWorkflowService.requestReturn / refundCash; ReturnEvidenceService.submit`. [OrderWorkflowService](../../../../src/main/java/com/utetra/order/service/OrderWorkflowService.java); [ReturnEvidenceService](../../../../src/main/java/com/utetra/order/service/ReturnEvidenceService.java) Service kiểm identity/scope và ràng buộc trước ghi; các mutation DB sử dụng transaction READ_COMMITTED, row locks và unique/version. Nhánh lỗi rollback dữ liệu, thông báo được lưu cùng transaction; gửi socket sau commit.
3. Controller: `OrderController`; route thực: `/orders/{id}/returns, /orders/{id}/returns/evidence`. Lấy actor từ principal, không nhận userId/role/số tiền quyết định từ client. DTO validation và CSRF cho mutation.
4. View: `src/main/webapp/WEB-INF/views/order/detail.jsp`; `orders.js` gọi endpoint JSON/multipart, hiển thị lỗi bằng textContent và dùng layout/Bootstrap chung. Dữ liệu JSP escape bằng c:out. Giao diện/endpoint dựa cùng Service để giữ quy tắc khi gọi ngoài trình duyệt.
5. Kiểm thử có thể chạy lại bằng [SETUP](../SETUP.md). Tên case thực: `returnWindowIsExactlyThreeHoursAndRefundReversesPointsOnce; returnsAtThreeHoursAreRejected; rejectedReturnCannotBeSubmittedAgainAndCancelledAssignmentLosesAccess`. [VERIFICATION](../VERIFICATION.md) phân biệt Maven/H2, MySQL, Playwright và provider thật chưa chạy.

## 3. Sơ đồ và minh chứng

Activity Mermaid ở phần đặc tả là nguồn thiết kế ban đầu; kiểm tra các nhánh lỗi/quyền theo Service và test ở mục 2. [Sơ đồ module](../assets/order-architecture.svg) và [nguồn Graphviz](../assets/order-architecture.dot) chỉ mô tả ranh giới ba tầng/transaction thực, không thay activity riêng của từng UC trong báo cáo cuối kỳ.

Ảnh dưới đây chụp ứng dụng JSP chạy với dữ liệu test, không phải mockup hoặc giao dịch thực:

- [ui-refunded-mobile.png](../assets/UC-ORD-09/ui-refunded-mobile.png).
- [ui-refunded.png](../assets/UC-ORD-09/ui-refunded.png).
- [ui-return-request.png](../assets/UC-ORD-09/ui-return-request.png).

## 4. Setup, giới hạn và bàn giao

Đọc [SETUP](../SETUP.md) về dữ liệu catalog/address/identity/session/loyalty/audit cần ghép. VNPAY hoãn; Cloudinary chưa cấu hình/test provider thật. Shipper cần `features.shipper.enabled=true`. Test fixtures chỉ dành cho H2 in-memory hoặc DB MySQL localhost `utetra_order_*` có opt-in, không chạy reset/seed trên utetra_db.

Review schema/version migration và contract cross-module trên integration trước khi ghép main; lần này chỉ thay feature/order-payment. DOCX, ảnh source có callout, sơ đồ activity xuất riêng và reviewer cá nhân cần nhóm hoàn thiện khi nộp báo cáo; không ghi đã có nếu chưa làm.
