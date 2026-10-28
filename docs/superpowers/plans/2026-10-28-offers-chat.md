# Offers and floating support Implementation Plan

> Execute inline using executing-plans; user approved triển khai luôn. No delegation or commit/push requested.

**Goal:** Ví ưu đãi cá nhân, quà theo mốc đã duyệt, chat nổi dùng hộp thư thật.

**Architecture:** promotion_wallet persists ownership and milestone grants; personalized promotions retain existing quote/reserve and locks. Support widget consumes existing authenticated support endpoints; DB is message source.

**Tech Stack:** Java 21/Spring/JPA/MySQL/JSP/Bootstrap 5.3.3.

**Spec:** ../specs/2026-10-28-offers-chat-design.md

## Global Constraints
Một SiteMesh/app.jsp, scope CSS; không đổi datasource/JWT/provider. Tất cả checkout và phương thức dùng cùng eligibility. Không push.

## Review Focus
- Một người không dùng/xem voucher quà của người khác.
- Cấp quà lặp hoặc đồng thời không nhân đôi, hoàn/hủy không đếm đơn.
- Quà hết hạn/đã dùng khóa ở checkout và ví.
- Chat không lộ tin giữa khách khác hoặc mất tin khi gửi lại/đổi trang.
- Mobile popup không che nút gửi/áp dụng.

## Tasks
- [x] Viết test reward ownership/idempotency/quota/expiry và widget rendering, chạy RED.
- [x] Tạo migration/entity promotion_wallet, service/controller ví; nối delivery reward và eligibility/choices. Ruling: after-commit delivery thay EARN để bao gồm zero-point và tất cả thành viên.
- [x] Tạo popup ví và chat nổi, chỉnh menu chung; admin inbox giữ nguyên.
- [x] Chạy test backend (278 qua), browser hai contexts/responsive (3 nhóm + shopping18); kiểm shared UI.
- [x] Ghi kết quả thực; mở server lại với bản đã build, không push.
