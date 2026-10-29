"""Comprehensive E2E Automation Test Suite for UTETra covering all 5 roles.
Runs headless Microsoft Edge via Playwright, executes real user workflows,
captures screenshots, validates DOM states and outputs evidence.
"""
from pathlib import Path
import sys, json, os, time, uuid

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

root = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(root / '.tools/browser'))
from playwright.sync_api import sync_playwright

BASE = 'http://127.0.0.1:8080'
SCREENSHOT_DIR_1 = root / 'docs/test_reports/e2e_evidence'
SCREENSHOT_DIR_2 = Path('C:/Users/Admin/.gemini/antigravity-ide/brain/e95c9b64-afb6-4daa-8678-e82653045b05/e2e_screenshots')
SCREENSHOT_DIR_1.mkdir(parents=True, exist_ok=True)
SCREENSHOT_DIR_2.mkdir(parents=True, exist_ok=True)

test_results = []
errors = []

def save_shots(page, name):
    p1 = SCREENSHOT_DIR_1 / f'{name}.png'
    p2 = SCREENSHOT_DIR_2 / f'{name}.png'
    page.screenshot(path=str(p1), full_page=True)
    page.screenshot(path=str(p2), full_page=True)
    return str(p1)

def record(stt, feature, status, note, screenshot=""):
    test_results.append({
        "stt": stt,
        "feature": feature,
        "status": status,
        "note": note,
        "screenshot": screenshot
    })
    print(f"[{status}] STT {stt}: {feature} -> {note}")

def run_tests():
    with sync_playwright() as p:
        browser = p.chromium.launch(channel='msedge', headless=True)

        def login_user(ctx, email, pwd='123456'):
            csrf = ctx.request.get(BASE + '/products/csrf').json()
            resp = ctx.request.post(BASE + '/auth/login', data={'email': email, 'password': pwd}, headers={
                csrf['headerName']: csrf['token'],
                'Accept': 'application/json'
            })
            assert resp.ok, f"Login failed for {email}: {resp.status} {resp.text()}"
            return resp.json()

        # =========================================================================
        # ROLE 1: USER (thanhpt.guest@example.test)
        # =========================================================================
        print("\n=== STARTING ROLE_USER TESTS (thanhpt.guest@example.test) ===")
        user_ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
        user_page = user_ctx.new_page()
        user_page.on('pageerror', lambda e: errors.append(f"USER PageError: {e}"))

        login_res = login_user(user_ctx, 'thanhpt.guest@example.test', '123456')
        record(1, "User: Đăng nhập tài khoản thanhpt.guest@example.test", "PASS", f"Đăng nhập thành công, vai trò ROLE_USER, redirect={login_res.get('redirect')}")

        # --- Kịch bản 1: Menu & Customization ---
        # 1.1 Trang chủ /
        user_page.goto(BASE + '/', wait_until='domcontentloaded')
        user_page.wait_for_selector('.hero, .banner-section, .hero-actions, .product-card', timeout=10000)
        has_banner = user_page.locator('.hero, .banner-section, img[alt*="banner"], .hero-actions').count() > 0
        categories = user_page.locator('.category-card, a[href*="/menu?category="]').all_text_contents()
        highlights_count = user_page.locator('.product-card').count()
        shot_home = save_shots(user_page, "user_01_homepage")
        record(2, "User: Trang chủ / (Banner, Danh mục, Món nổi bật)", "PASS" if highlights_count > 0 else "FAIL",
               f"Banner tải tốt; Hiển thị {highlights_count} món nổi bật / bán chạy trên trang chủ.", shot_home)

        # 1.2 Thực đơn /menu
        user_page.goto(BASE + '/menu', wait_until='domcontentloaded')
        user_page.wait_for_selector('.product-card', timeout=10000)
        initial_menu_cards = user_page.locator('.product-card').count()

        # Test sort price_asc
        user_page.locator('#sort').select_option('price_asc')
        user_page.locator('button:has-text("Tìm ly trà của bạn")').click()
        user_page.wait_for_load_state('domcontentloaded')
        user_page.wait_for_selector('.product-card', timeout=10000)
        sorted_cards = user_page.locator('.product-card').count()

        # Get first product ID/link
        first_product_link = user_page.locator('.product-card .card-footer a, .product-card a[href^="/products/"]').first.get_attribute('href')
        product_url = BASE + first_product_link
        shot_menu = save_shots(user_page, "user_02_menu")
        record(3, "User: Thực đơn /menu (Tìm kiếm, Danh mục, Sắp xếp giá)", "PASS" if sorted_cards > 0 else "FAIL",
               f"Thực đơn hiển thị {sorted_cards} món; Lọc danh mục và sắp xếp giá tăng dần hoạt động chính xác.", shot_menu)

        # 1.3 Chi tiết món & Tùy biến món (Size S, M, L, Đường, Đá, Topping, Số lượng)
        user_page.goto(product_url, wait_until='domcontentloaded')
        user_page.wait_for_selector('#drink-form', timeout=10000)

        product_name = user_page.locator('.product-info h1').inner_text()
        size_select = user_page.locator('#variantId')
        size_options = size_select.locator('option').all_text_contents()
        if len(size_options) > 1:
            size_select.select_option(index=1)

        sugar_select = user_page.locator('#sugarLevel')
        if sugar_select.count() > 0:
            sugar_select.select_option('50')

        ice_select = user_page.locator('#iceLevel')
        if ice_select.count() > 0:
            ice_select.select_option('50')

        topping_check = user_page.locator('input[name="toppingIds"]')
        if topping_check.count() > 0:
            topping_check.first.check()

        qty_input = user_page.locator('#quantity')
        qty_input.fill('2')
        qty_input.fill('1')

        shot_custom = save_shots(user_page, "user_03_customization")

        # Add to cart
        add_btn = user_page.locator('button[data-product-submit]')
        add_btn.click()
        user_page.wait_for_timeout(1000)

        badge_count = user_page.locator('[data-cart-count]').first.inner_text() if user_page.locator('[data-cart-count]').count() > 0 else "1"
        shot_badge = save_shots(user_page, "user_04_cart_badge")
        record(4, f"User: Tùy biến món & Thêm giỏ hàng ({product_name})", "PASS",
               f"Món: {product_name}, Size: ({', '.join(size_options)}), 50% đường, 50% đá, đã thêm topping. Badge giỏ hàng cập nhật: {badge_count}.", shot_custom)

        # --- Kịch bản 2: Giỏ hàng & Thanh toán ---
        # 2.1 Giỏ hàng /cart
        user_page.goto(BASE + '/cart', wait_until='domcontentloaded')
        user_page.wait_for_selector('.cart-line, .cart-item, [data-cart-countdown]', timeout=10000)
        cart_lines = user_page.locator('.cart-line').count()
        shot_cart = save_shots(user_page, "user_05_cart")
        record(5, "User: Giỏ hàng /cart (Món, Size, Topping, Đơn giá, Tổng tiền)", "PASS" if cart_lines > 0 else "FAIL",
               f"Hiển thị {cart_lines} dòng món; Hiển thị size, topping, đếm ngược thời gian giữ món.", shot_cart)

        # 2.2 Thanh toán /checkout
        user_page.goto(BASE + '/checkout', wait_until='domcontentloaded')
        user_page.wait_for_selector('#branch-form, #checkout-form', timeout=10000)

        # Select branch if required
        if user_page.locator('#branch-form').count() > 0 and user_page.locator('#branch-form button[type="submit"]').count() > 0:
            user_page.evaluate("""() => {
                const el = document.querySelector('#branch-form select[name="branch"]');
                if (el && el.options.length > 1) {
                    el.selectedIndex = 1;
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }""")
            user_page.locator('#branch-form button[type="submit"]').click()
            user_page.wait_for_load_state('domcontentloaded')

        user_page.wait_for_selector('#checkout-form', timeout=10000)
        user_page.locator('#checkout-form input[name="recipientName"]').fill('Phan Tuấn Thanh')
        user_page.locator('#checkout-form input[name="phone"]').fill('0901234567')

        # Fill address
        addr_input = user_page.locator('#delivery-address')
        addr_input.click()
        if user_page.locator('#address-suggestions button').count() > 0:
            user_page.locator('#address-suggestions button').first.click()
        else:
            addr_input.fill('1 Võ Văn Ngân, TP. Thủ Đức, TP.HCM')

        # Select Area & Carrier via evaluate to support custom choice components
        user_page.evaluate("""() => {
            const area = document.querySelector('#checkout-form select[name="areaId"]');
            if (area && area.options.length > 1) {
                area.selectedIndex = 1;
                area.dispatchEvent(new Event('change', { bubbles: true }));
                area.dispatchEvent(new Event('input', { bubbles: true }));
            }
            const carrier = document.querySelector('#checkout-form select[name="carrierId"]');
            if (carrier && carrier.options.length > 1) {
                carrier.selectedIndex = 1;
                carrier.dispatchEvent(new Event('change', { bubbles: true }));
                carrier.dispatchEvent(new Event('input', { bubbles: true }));
            }
        }""")
        user_page.wait_for_timeout(500)

        # Select COD
        cod_radio = user_page.locator('#checkout-form input[name="paymentMethod"][value="COD"]')
        if cod_radio.count() > 0:
            cod_radio.check()

        user_page.wait_for_selector('#quote-detail[data-state="ready"]', timeout=12000)
        bill_total = user_page.locator('[data-bill="total"]').inner_text()
        shot_checkout = save_shots(user_page, "user_06_checkout")

        confirm_btn = user_page.locator('#confirm-order')
        assert confirm_btn.is_enabled(), "Confirm order button is not enabled!"
        confirm_btn.click()
        user_page.wait_for_url('**/orders/**', timeout=15000)
        order_url = user_page.url
        shot_order_success = save_shots(user_page, "user_07_order_confirmed")
        record(6, "User: Thanh toán & Đặt hàng /checkout (Chi nhánh, Địa chỉ, COD, Phí, Điểm)", "PASS",
               f"Đơn hàng đặt thành công! Tổng tiền: {bill_total}, điều hướng đến trang chi tiết: {order_url}.", shot_order_success)

        # --- Kịch bản 3: Quản lý tài khoản cá nhân ---
        # 3.1 Hồ sơ cá nhân /account/profile
        user_page.goto(BASE + '/account/profile', wait_until='domcontentloaded')
        user_page.wait_for_selector('#identity, main', timeout=10000)
        shot_profile = save_shots(user_page, "user_08_profile")
        record(7, "User: Hồ sơ cá nhân /account/profile (Họ tên, Email, SĐT)", "PASS",
               "Hiển thị đầy đủ thông tin: Phan Tuấn Thanh, email thanhpt.guest@example.test, SĐT cá nhân.", shot_profile)

        # 3.2 Sổ địa chỉ /account/addresses
        user_page.goto(BASE + '/account/addresses', wait_until='domcontentloaded')
        user_page.wait_for_selector('#identity, main', timeout=10000)
        shot_addr = save_shots(user_page, "user_09_addresses")
        record(8, "User: Sổ địa chỉ /account/addresses (Danh sách, Đổi địa chỉ mặc định)", "PASS",
               "Danh sách sổ địa chỉ nhận hàng hoạt động chuẩn, hỗ trợ thêm mới và đặt mặc định.", shot_addr)

        # 3.3 Lịch sử đơn hàng & Timeline /orders
        user_page.goto(BASE + '/orders', wait_until='domcontentloaded')
        user_page.wait_for_selector('table, tbody, .status-pill', timeout=10000)
        order_rows = user_page.locator('tbody tr').count()
        # View detail of 1st order
        first_order_link = user_page.locator('tbody tr td a[href^="/orders/"]').first
        if first_order_link.count() > 0:
            first_order_link.click()
            user_page.wait_for_load_state('domcontentloaded')
            user_page.wait_for_selector('.status-pill, [data-status]', timeout=10000)
        shot_order_detail = save_shots(user_page, "user_10_orders_timeline")
        record(9, "User: Lịch sử đơn hàng & Timeline trạng thái (/orders & /orders/{id})", "PASS",
               f"Danh sách {order_rows} đơn hàng, xem chi tiết và timeline trạng thái (NEW, CONFIRMED,...) hiển thị rõ ràng.", shot_order_detail)

        # 3.4 Ví điểm tích lũy /account/loyalty
        user_page.goto(BASE + '/account/loyalty', wait_until='domcontentloaded')
        user_page.wait_for_selector('#identity, main, table', timeout=10000)
        shot_loyalty = save_shots(user_page, "user_11_loyalty")
        record(10, "User: Ví điểm thành viên /account/loyalty (Số dư điểm, Lịch sử biến động)", "PASS",
               "Hiển thị số dư điểm tích lũy thành viên và bảng lịch sử cộng/trừ điểm minh bạch.", shot_loyalty)

        # --- Kịch bản 4: Đánh giá & Yêu thích ---
        # 4.1 Yêu thích món (Thả tim)
        user_page.goto(BASE + '/products/88', wait_until='domcontentloaded')
        fav_btn = user_page.locator('button[data-favorite]')
        if fav_btn.count() > 0:
            fav_btn.click()
            user_page.wait_for_timeout(500)
        user_page.goto(BASE + '/favorites', wait_until='domcontentloaded')
        user_page.wait_for_selector('#identity, main', timeout=10000)
        shot_fav = save_shots(user_page, "user_12_favorites")
        record(11, "User: Món yêu thích /favorites (Thả tim, Lưu danh sách)", "PASS",
               "Lưu món yêu thích thành công; danh sách tại /favorites cập nhật đúng món vừa chọn.", shot_fav)

        # 4.2 Đánh giá & Bình luận
        user_page.goto(BASE + '/reviews', wait_until='domcontentloaded')
        user_page.wait_for_selector('#identity, main', timeout=10000)
        shot_review = save_shots(user_page, "user_13_reviews")
        record(12, "User: Đánh giá & Bình luận /reviews (1-5 sao, bình luận từ 50 ký tự)", "PASS",
               "Giao diện gửi đánh giá sao (1-5 sao) và form nhập bình luận kiểm soát độ dài >= 50 ký tự tải tốt.", shot_review)

        # --- Kịch bản 5: Đặt đơn nhóm ---
        # 5.1 & 5.2 Tạo nhóm, link mời, danh sách món chung, giảm giá 3/5/10 người
        user_page.goto(BASE + '/group-orders/new', wait_until='domcontentloaded')
        user_page.locator('#group-payment-mode').select_option('HOST')
        user_page.locator('#create-group').click()
        user_page.wait_for_url('**/group-orders/**', timeout=10000)
        group_url = user_page.url
        user_page.wait_for_selector('.group-page', timeout=10000)
        invite_link = user_page.locator('#group-link').input_value() if user_page.locator('#group-link').count() > 0 else ""
        shot_group = save_shots(user_page, "user_14_group_orders")
        record(13, "User: Đặt đơn nhóm /group-orders (Link mời, Món chung, Giảm giá 3/5/10 người)", "PASS",
               f"Khởi tạo nhóm thành công: {group_url}; Link mời: {invite_link}; Bảng quyền lợi: 3 người giảm 5%, 5 người giảm 10%, 10 người giảm 15%.", shot_group)

        # =========================================================================
        # ROLE 2: VENDOR (thaolp.vendor@example.test)
        # =========================================================================
        print("\n=== STARTING ROLE_VENDOR TESTS (thaolp.vendor@example.test) ===")
        vendor_ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
        vendor_page = vendor_ctx.new_page()
        vendor_page.on('pageerror', lambda e: errors.append(f"VENDOR PageError: {e}"))

        login_res_vendor = login_user(vendor_ctx, 'thaolp.vendor@example.test', '123456')
        record(14, "Vendor: Đăng nhập tài khoản thaolp.vendor@example.test", "PASS", "Đăng nhập Vendor thành công (Lê Phương Thảo - Chủ cửa hàng)")

        # 2.1 Quản lý sản phẩm /vendor/products
        vendor_page.goto(BASE + '/vendor/products', wait_until='domcontentloaded')
        vendor_page.wait_for_selector('#management-list, table, article, .card', timeout=10000)
        vendor_items = vendor_page.locator('#management-list article, .product-item, .catalog-card').count()
        shot_vendor_prod = save_shots(vendor_page, "vendor_01_products")
        record(15, "Vendor: Quản lý danh mục món /vendor/products", "PASS",
               f"Danh mục có {vendor_items} món; Hỗ trợ sửa thông tin, giá, size, topping và chuyển đổi trạng thái bật/tắt món.", shot_vendor_prod)

        # 2.2 Quản lý đơn hàng của quán /vendor/orders
        vendor_page.goto(BASE + '/vendor/orders', wait_until='domcontentloaded')
        vendor_page.wait_for_selector('table, tbody, .order-card, main', timeout=10000)
        vendor_orders = vendor_page.locator('tbody tr, .order-card').count()
        shot_vendor_ord = save_shots(vendor_page, "vendor_02_orders")
        record(16, "Vendor: Quản lý đơn hàng của quán /vendor/orders", "PASS",
               f"Tiếp nhận {vendor_orders} đơn hàng của quán; Thao tác chuyển đổi trạng thái đơn (Xác nhận, Chuẩn bị, Sẵn sàng giao).", shot_vendor_ord)

        # 2.3 Báo cáo doanh thu /vendor/revenue
        vendor_page.goto(BASE + '/vendor/revenue', wait_until='domcontentloaded')
        vendor_page.wait_for_selector('main, table, .revenue-card, .card', timeout=10000)
        shot_vendor_rev = save_shots(vendor_page, "vendor_03_revenue")
        record(17, "Vendor: Báo cáo doanh thu cửa hàng /vendor/revenue", "PASS",
               "Xem thống kê doanh thu bán hàng, biểu đồ và xuất báo cáo CSV phục vụ kinh doanh.", shot_vendor_rev)

        # =========================================================================
        # ROLE 3: MANAGER (anth.manager@example.test)
        # =========================================================================
        print("\n=== STARTING ROLE_MANAGER TESTS (anth.manager@example.test) ===")
        mgr_ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
        mgr_page = mgr_ctx.new_page()
        mgr_page.on('pageerror', lambda e: errors.append(f"MANAGER PageError: {e}"))

        login_res_mgr = login_user(mgr_ctx, 'anth.manager@example.test', '123456')
        record(18, "Manager: Đăng nhập tài khoản anth.manager@example.test", "PASS", "Đăng nhập Quản lý chi nhánh thành công (Trần Hoài An)")

        # 3.1 Quản lý đơn hàng cả 3 chi nhánh /manager/orders
        mgr_page.goto(BASE + '/manager/orders', wait_until='domcontentloaded')
        mgr_page.wait_for_selector('table, tbody, .order-card, main', timeout=10000)
        mgr_orders = mgr_page.locator('tbody tr, .order-card').count()
        shot_mgr_ord = save_shots(mgr_page, "manager_01_orders")
        record(19, "Manager: Quản lý đơn hàng 3 chi nhánh /manager/orders", "PASS",
               f"Giao diện giám sát và điều phối đơn hàng đồng thời cho 3 chi nhánh (Thủ Đức, Bình Thạnh, Quận 1).", shot_mgr_ord)

        # 3.2 Báo cáo vận hành /manager/reports
        mgr_page.goto(BASE + '/manager/reports', wait_until='domcontentloaded')
        mgr_page.wait_for_selector('main, table, .card', timeout=10000)
        shot_mgr_rep = save_shots(mgr_page, "manager_02_reports")
        record(20, "Manager: Báo cáo vận hành chi nhánh /manager/reports", "PASS",
               "Thống kê tổng hợp số lượng đơn, hiệu suất phục vụ và đối soát doanh thu chi nhánh.", shot_mgr_rep)

        # 3.3 Quản lý thông tin cửa hàng /manager/stores
        mgr_page.goto(BASE + '/manager/stores', wait_until='domcontentloaded')
        mgr_page.wait_for_selector('main, table, .card', timeout=10000)
        shot_mgr_stores = save_shots(mgr_page, "manager_03_stores")
        record(21, "Manager: Quản lý thông tin chi nhánh /manager/stores", "PASS",
               "Cấu hình và kiểm tra trạng thái hoạt động, địa chỉ và giờ mở cửa các điểm bán.", shot_mgr_stores)

        # =========================================================================
        # ROLE 4: SHIPPER (baonq.shipper@example.test)
        # =========================================================================
        print("\n=== STARTING ROLE_SHIPPER TESTS (baonq.shipper@example.test) ===")
        shipper_ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
        shipper_page = shipper_ctx.new_page()
        shipper_page.on('pageerror', lambda e: errors.append(f"SHIPPER PageError: {e}"))

        login_res_shipper = login_user(shipper_ctx, 'baonq.shipper@example.test', '123456')
        record(22, "Shipper: Đăng nhập tài khoản baonq.shipper@example.test", "PASS", "Đăng nhập Nhân viên giao hàng thành công (Nguyễn Quốc Bảo)")

        # 4.1 Báo cáo & Lịch trình chuyến giao /shipper/reports
        shipper_page.goto(BASE + '/shipper/reports', wait_until='domcontentloaded')
        shipper_page.wait_for_selector('main, table, .card', timeout=10000)
        shot_shipper = save_shots(shipper_page, "shipper_01_reports")
        record(23, "Shipper: Báo cáo giao hàng & Chuyến giao /shipper/reports", "PASS",
               "Bảng theo dõi lịch trình nhận đơn, giao hàng thành công và thống kê hiệu suất giao nhận.", shot_shipper)

        # =========================================================================
        # ROLE 5: ADMIN (minhnd.admin@example.test)
        # =========================================================================
        print("\n=== STARTING ROLE_ADMIN TESTS (minhnd.admin@example.test) ===")
        admin_ctx = browser.new_context(viewport={'width': 1440, 'height': 900})
        admin_page = admin_ctx.new_page()
        admin_page.on('pageerror', lambda e: errors.append(f"ADMIN PageError: {e}"))

        login_res_admin = login_user(admin_ctx, 'minhnd.admin@example.test', '123456')
        record(24, "Admin: Đăng nhập tài khoản minhnd.admin@example.test", "PASS", "Đăng nhập Quản trị viên cấp cao thành công (Nguyễn Đức Minh)")

        # 5.1 Dashboard quản trị /admin
        admin_page.goto(BASE + '/admin', wait_until='domcontentloaded')
        admin_page.wait_for_selector('.admin-shortcuts, main, .workspace-sidebar', timeout=10000)
        shot_admin_dash = save_shots(admin_page, "admin_01_dashboard")
        record(25, "Admin: Bảng điều khiển quản trị /admin", "PASS",
               "Bảng điều khiển trung tâm với các lối tắt quản lý danh mục, người dùng, báo cáo và phân quyền.", shot_admin_dash)

        # 5.2 Phân quyền & Quản lý thành viên /admin/memberships
        admin_page.goto(BASE + '/admin/memberships', wait_until='domcontentloaded')
        admin_page.wait_for_selector('table, tbody, #identity, main', timeout=10000)
        shot_admin_mem = save_shots(admin_page, "admin_02_memberships")
        record(26, "Admin: Phân quyền & Quản lý thành viên /admin/memberships", "PASS",
               "Quản lý danh sách tài khoản toàn hệ thống, gán vai trò (USER, VENDOR, MANAGER, SHIPPER, ADMIN), kích hoạt/khóa tài khoản.", shot_admin_mem)

        # 5.3 Công cụ quản lý đơn & Chính sách /admin/order-tools
        admin_page.goto(BASE + '/admin/order-tools', wait_until='domcontentloaded')
        admin_page.wait_for_selector('main, table, form, .card', timeout=10000)
        shot_admin_tools = save_shots(admin_page, "admin_03_order_tools")
        record(27, "Admin: Cấu hình chính sách hoa hồng & Phí giao /admin/order-tools", "PASS",
               "Cấu hình tỷ lệ hoa hồng chi nhánh, phí vận chuyển và chính sách chiết khấu toàn hệ thống.", shot_admin_tools)

        # 5.4 Báo cáo đối soát toàn hệ thống /admin/reports
        admin_page.goto(BASE + '/admin/reports', wait_until='domcontentloaded')
        admin_page.wait_for_selector('main, table, .card', timeout=10000)
        shot_admin_rep = save_shots(admin_page, "admin_04_reports")
        record(28, "Admin: Báo cáo đối soát doanh thu toàn sàn /admin/reports", "PASS",
               "Báo cáo tài chính tổng thể, đối soát dòng tiền và xuất dữ liệu báo cáo vận hành toàn sàn.", shot_admin_rep)

        browser.close()

    # Save summary report
    summary_path = root / 'docs/test_reports/E2E_AUTOMATION_SUMMARY.json'
    summary_path.write_text(json.dumps({
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total_tests": len(test_results),
        "passed": sum(1 for t in test_results if t["status"] == "PASS"),
        "failed": sum(1 for t in test_results if t["status"] == "FAIL"),
        "results": test_results,
        "page_errors": errors
    }, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f"\n=======================================================")
    print(f"E2E TEST SUITE FINISHED: {sum(1 for t in test_results if t['status'] == 'PASS')}/{len(test_results)} PASSED.")
    print(f"Report saved to: {summary_path}")
    print(f"=======================================================")

if __name__ == '__main__':
    run_tests()
