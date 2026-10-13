# Hướng dẫn nhánh feature/order-payment

**Người nhận:** TV3, điền họ tên trong docs/features/order-payment/IMPLEMENTATION.md. **Phạm vi:** UC-ORD-01–08, UC-NOT-01, UC-RPT-01; giỏ → checkout → COD → xử lý/hủy/hoàn tất → realtime và báo cáo.

Đọc PROJECT_PLAN.md, DATABASE_DESIGN.md, USE_CASES.md, docs/README.md, docs/COMMON_BASELINE.md và docs/SETUP.md. Hướng dẫn: [IMPLEMENTATION](docs/features/order-payment/IMPLEMENTATION.md). Prompt thật: [AI_LOG](docs/features/order-payment/AI_LOG.md).

Cấu hình theo main: Java 21/Boot 3.5.x/WAR/JSP/SiteMesh/JPA, ba profile DB, JWT/CSRF dùng chung; WebSocket /ws và destinations theo contract. Owner 8 bảng giỏ/đơn/notification. Dùng giá/store/media TV1 và quyền/địa chỉ/điểm/audit TV2. Không sửa ledger ngoài LoyaltyService; giá/phí/tổng từ server; COD và transaction theo đặc tả.

Làm đủ mọi lớp và hồ sơ từng UC; không tự tính khoảng cách hay chọn chi nhánh gần nhất. Giữ các chức năng khác, snapshot, replay/locking, điểm rollback cùng đơn. Ghi kiến trúc/activity/code/UI/setup và prompt thật, test AT-01–16 khi ghép qua integration/utetra. B0 phải đồng bộ trước lập trình.
