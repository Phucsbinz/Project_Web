# UTETra — Triển khai role/workflow 29/10/2026

## Yêu cầu, baseline và giới hạn
Prompt thực: sửa ba lỗi audit; Vendor sở hữu kinh doanh, Manager chỉ vận hành, Admin phân công quyền/phạm vi và chuyến; Shipper giao theo scope/assignment; TDD, concurrency, một layout, full test/WAR/UI; không commit/push/merge/deploy hoặc ghi DB shared.
Baseline integration/utetra @8e1234b624282ed2e8a9c633695da3bb75b17baf. Workspace đã có sửa widget/app.jsp, ảnh/menu và docs/tools; giữ nguyên. Audit gần nhất có 35 test chọn lọc đạt và ba lỗi tái hiện; báo cáo khác nói 286 test không chứng minh mọi UC. Không làm theo đề xuất seed assignment giả hoặc coi manual refund/TTL là bug. Kiểm môi trường chỉ đọc; build/test cô lập, không restart runtime đang có.

## Thiết kế và bảng quyền mục tiêu
Dùng module hiện có; không đổi provider/payment/datasource. CATALOG_ADMIN là member_role trong store_memberships (capability gắn membership Manager), không phải role tài khoản mới. Giữ dữ liệu hiện có, chỉ cho phép định nghĩa Category/Topping chung; không cấp sửa giá món/promotion/settlement. Không backfill quyền hàng loạt.

| Role | Tài nguyên/hành động | Phạm vi/điều kiện | Backend |
|---|---|---|---|
| Admin | Catalog, business, phân công Manager/Shipper, điều phối, tài chính | Toàn hệ thống, active/verified; version/state/CSRF | Guard/Services/Security |
| Vendor | Business, giá/promotion, revenue/settlement, vận hành | stores.owner_user_id đúng actor; không lấy membership thay owner | CatalogGuard.business; Operations |
| Manager | Đơn, tồn, available, acceptingOrders, phân công chuyến, báo cáo vận hành | Membership còn hiệu lực; kiểm lại DB tại request/mutation | CatalogGuard.mutation; refs.requireOperatorMutation |
| Manager + CATALOG_ADMIN | Category/Topping chung | Admin cấp/thu hồi; không mở rộng business/finance | CatalogGuard.catalog |
| Shipper | Scope phục vụ và assignment hiện hành; lấy/giao/COD | Active role/account, scope hợp lệ, assignment; không finance/support | ShipperScopeService/OrderWorkflow |

Quyết định thu hồi: row users của Manager là mutex quyền. Mutation lấy khóa actor trước tài nguyên nghiệp vụ, kiểm membership bằng locking read; grant/revoke khóa cùng target user. Thao tác đã qua điểm quyết định trước revoke có thể hoàn thành trước revoke commit; sau revoke commit thao tác mới không được phép. Không dùng quyền session snapshot.

## State machine mục tiêu
| Trước | Hành động/actor | Điều kiện | Sau/tác động/retry |
|---|---|---|---|
| NEW | Xác nhận Manager/Admin (Vendor theo nghiệp vụ shop hiện có) | Online PAID; COD hợp lệ; version | CONFIRMED; không ghi thu COD |
| CONFIRMED | Phân công Admin/Manager | Shipper active trong scope; một assignment/order | ACTIVE assignment; history old/new/operator/reason; retry không nhân đôi |
| CONFIRMED | Tiếp nhận/lấy hàng Shipper | Assignment hiện hành; version; scope | PICKED_UP; trước lấy mới được đổi bình thường |
| PICKED_UP | Bắt đầu giao | Assignment/version đúng | DELIVERING; chặn đổi thông thường sau khi đã lấy |
| DELIVERING | Giao thành công Shipper | COD xác nhận thu; online PAID | DELIVERED/COMPLETED, thu/điểm/settlement đúng một lần |
| Đang giao | Thất bại Shipper | Lý do hợp lệ | Ghi event, không tự hủy/hoàn; Manager xử lý theo contract |
| Cho phép hủy/trả | Hủy/trả/hoàn theo contract cũ | Trạng thái, thời hạn, evidence và quyền | Không tự chuyển tiền ngoài; REFUND_PENDING đến đối soát có chứng từ |

Invariant: không nhảy bước/stale; một assignment active; không assign đơn cuối; shipper cũ mất PII/quyền; COD/điểm/ledger không hai lần. Giữ tổng/phần tiền/quyền host nhóm.

## Kế hoạch thực thi và nghiệm thu
- [x] G1: kiểm source/báo cáo/runtime; inventory quyền/state, baseline và schema. Chọn sửa trên workspace hiện tại vì cần giữ dirty UI; build riêng, không đổi nhánh/worktree.
- [x] G2: regression RED → GREEN cho time precision, category cũ inactive và missing/null/zero/stale version; test tạo mới/hồi quy.
- [x] G3: business guard/Admin delegation; Manager operational endpoints; kiểm role/scope/revoke/race và direct API.
- [x] G4: scope Shipper tách assignment; history; cấm reassignment sau lấy; version/idempotency/concurrency; migration mới kiểm fresh/legacy, không áp shared.
- [x] G5: workspace/login/empty states; Admin assignments; Vendor comparison; Manager operational UI; Shipper dashboard; một layout.
- [x] G6: review độc lập, targeted regressions, full suite + WAR cuối, render thực 390/768/1440, shared UI checker; ghi giới hạn thật.

## Tiến độ và bằng chứng
Đang triển khai. tools/check_agent_skills.py: 15 skill hợp lệ, không ghi cấu hình global. Phân công file độc lập: agent catalog sửa ba lỗi, agent shipping sửa module giao, agent workspace sửa điều hướng/JSP, agent chính guard/operational endpoints/tests/tích hợp. Không cùng ghi file dùng chung.

## Mapping yêu cầu / kiểm thử / trạng thái
| Yêu cầu | File trọng yếu | Test/bằng chứng | Trạng thái |
|---|---|---|---|
| 3 lỗi catalog | CatalogRequests, CatalogService, StoreService, PromotionService, catalog-management.js | CatalogWorkflowRegressionTest 12; RED/GREEN H2 + Edge editor | Đạt các ca đã nêu |
| Quyền giá/promotion/ownership | CatalogGuard, CatalogMediaService, CommerceReferenceRepository | RoleOperationsIntegrationTest; scope hai owner và session | Đạt |
| Manager vận hành/revoke | ManagerOperationsService/Controller; StoreMembershipRepository; UserManagementService | ManagerPermissionConcurrencyTest, RoleOperationsIntegrationTest | Đạt H2; TiDB chưa xác minh |
| Manager không settlement/commission | OperationsService/Controller, StoreService.commissionPolicies | Direct API/CSV denied, operational report tests | Đạt |
| Admin Manager assignment | ManagerOperationsService, StoreService.assign, manager-assignments.js, assignments.jsp | Role tests; grant/revoke/capability guards; UI đã kiểm phần nêu dưới | Backend đạt |
| Shipper scope/assignment/history | ShipperScopeService, OrderWorkflowService, ba entity mới, AdminShipperScopeController | ShippingScopeIntegrationTest 13; ShippingScopeMigrationTest 3 | Đạt H2 |
| Workspace/login/branch selector | WorkspaceController/Service/Repository, LoginDestination, NavigationAdvice, app.jsp, role module assets | 7 unit tests + identity login regression; UI đã kiểm phần nêu dưới | Backend đạt |
| Hồi quy tiền/nhóm/giỏ/callback/support | Các service cũ + guards có scope | Full suite 328, gồm OrderCommerce111, Support11, InventoryLock6, LoyaltyConcurrency4 | Đạt các test hiện có |

## Nguyên nhân và sửa đã kiểm chứng
1. Input time mặc định bước60s không nhận 23:59:59. Schema MySQL TIME không khai báo fsp: chốt precision nguyên giây, step=1, backend từ chối fraction rõ ràng (không cắt). Giữ HH:mm và HH:mm:ss; chính sách không qua đêm đã có từ trước được giữ. Null theo @NotNull contract, không tự cho rỗng. RED time/category UI thật JS trên Edge với HTTP stub; H2 backend thật. RED thêm4 fraction tests → GREEN12/12.
2. Public lookup không có category inactive nên select fallback đầu tiên. Editor giữ category hiện tại có nhãn inactive; backend cho giữ liên kết cũ khi đổi giá/trường khác, cấm tạo hoặc chuyển mới vào inactive. Không làm public món thuộc category inactive.
3. Primitive version thiếu mặc định0. Shop/promotion dùng nullable strict version + expectedVersion; create vẫn hợp lệ. Missing/null409 không ghi, zero đúng cập nhật, stale409. Không thay schema/version khởi tạo. New scope controller cũng yêu cầu active/version rõ ràng.
4. Manager trước đây được guard.store cấp cả business. Nay guard.business chỉ Admin/ownedVendor; operational endpoints chỉ nhận availability/accepting/quantity + version, không có price/promotion/owner. Membership CATALOG_ADMIN không mở finance. Manager vẫn giữ carrier/rate vận hành trong scope theo contract hiện có, không sửa commission.
5. Revoke và mutation dùng mutex user + current locking membership read. Area và topping availability cũng đi qua mutation. Grant/revoke Catalog/Identity dùng cùng target user; repository assignment locking read tránh snapshot cũ/insert trùng. Thu hồi Manager khóa/unverified vẫn được; grant mới phải active/verified. Identity regrant dormant CATALOG_ADMIN bị từ chối để Admin xác nhận capability qua form, không âm thầm khôi phục hoặc xóa quyền.
6. Shipper trước đây global directory/overwrite pointer. Scope riêng có Admin audit, assignment unique theo order giữ nguyên, lịch sử immutable old/new/operator/time/reason bổ sung. Dispatch chỉ Admin/Manager khi CONFIRMED và đúng payment; cấm đổi sau lấy. Thu hồi/đổi assignment chặn người cũ; completed chỉ history tối thiểu không PII/detail. COD success redirect history thay vì reload detail đã mất quyền.
7. Review tìm actor→buyer có thể ngược thứ tự. Nay toàn bộ tập user liên quan được khóa tăng ID trước order/resources (assignment gồm actor+buyer+targetshipper), không chỉ sort từng cặp. Test H2 điều phối bằng latch và kiểm blocker đã kiểm thứ tự, không kết luận hết deadlock TiDB.

## Bằng chứng chạy thực
- `py -3 tools/check_agent_skills.py`:15 hợp lệ; `py -3 tools/check_shared_ui.py`:1 layout/21 shared files; diff check không lỗi whitespace (có cảnh báo CRLF sẵn có ở tools/check_support_ui.py).
- Catalog RED:8 ca ban đầu có4 failure +1 error do inactive link bị chặn; GREEN8. Precision RED4 fail → GREEN tổng12. `.local/catalog-workflow-fix/{red-tests,green-tests,precision-red,precision-green}.log`.
- Role RED3 fail đúng Manager business vẫn được phép và operational endpoint404; revoke guard regression RED khi snapshot thay guard bằng check không khóa. GREEN6 ca root sau khôi phục và compile lại. Dormant capability regrant RED trước fix; final suite xác nhận GREEN.
- ShippingScopeIntegrationTest13: scope, state, COD/retry, history/PII, version, coordinated revoke/assignment và cross-owner lock; có Tomcat/SiteMesh render thật. ShippingScopeMigrationTest3: fresh/legacy, no automatic grants, snapshot assignment, FK/UQ trên H2 MySQL mode.
- Scope regression151 ca phát hiện2 kỳ vọng cũ: redirect không giữ next và buyer cancel sau CONFIRMED. Đã sửa test theo contract mới: kiểm next chính xác; buyer vẫn bị từ chối và operator hủy assignment trước pickup. Không skip/xóa assertions nghiệp vụ.
- Full lần đầu328 có1 failure: CatalogViewTest còn đòi Manager catalog price editor; cập nhật riêng Manager sang operations markup, giữ assertions layout/JSP và Vendor/Admin cũ. Full trung gian **328 tests,0 failure/error/skipped**, Maven package exit0; sau đó kiểm browser phát hiện hai lỗi tích hợp và đã sửa như dưới đây. `.local/role-workflow-main/final-package-confirmed.log`, kết thúc05:09:29+07. Java21, không skip test. Lần clean package đầu có failure đã ghi ở final-package.log.
- Một full snapshot phụ của agent catalog trước tích hợp có2 error do thiếu copy proposal SQL docs; đó là lỗi input test harness, đã copy đủ docs/sql ở final. Không dùng kết quả đó để kết luận code đạt.
- Review độc lập sau sửa: năm phát hiện đã được xử lý, không còn blocker trong phạm vi review; chỉ source review, không tính như test runtime.

## Migration và vận hành có kiểm soát
Migration mới duy nhất `src/main/resources/db/migration/mysql/V2026102901__shipper_store_scopes.sql`:3 bảng shipper_store_scopes/shipper_scope_events/delivery_assignment_changes. Không sửa migration cũ, không cấp scope tự động. Giữ assignment hiện tại làm snapshot lịch sử, không giả dựng quá khứ hoặc đổi trạng thái đơn đang giao.

Chưa áp lên DB shared. Trước triển khai thật: backup, kiểm danh sách assignment đang hiệu lực, xác minh người/chi nhánh/role; áp migration theo quy trình được cho phép; Admin cấp scope rõ ràng cho Shipper được duyệt, đặc biệt chuyến đang giữ hàng, rồi kiểm lại quyền/dispatch. Không dùng mọi đơn cũ làm căn cứ tự cấp quyền rộng. Không rollback bằng cách xóa history đã có nghiệp vụ mới; cần đánh giá export/forward migration và tương thích artifact cũ trước mọi rollback.

Completed history không mở lại khách hàng; shipper được thay không có toàn chi tiết lịch sử. Chưa xây quy trình bàn giao hàng giữa Shipper: chặn đổi bình thường sau PICKED_UP, Manager xử lý thất bại qua event và quy trình đơn hiện có. Tiếp nhận hiện là bước lấy hàng, không thêm trạng thái ACCEPTED/DECLINED trùng ý nghĩa; báo không thực hiện được bằng lý do/event để điều phối xem xét, không tự hủy/hoàn.

Hoàn ngoài: REFUND_PENDING là yêu cầu/chờ xử lý, không phải đã chuyển tiền. Admin đối chiếu khoản đã thu và phần được hoàn, thực hiện hoàn ngoài bằng kênh được phép, kiểm sao kê/chứng từ rồi dùng form xác nhận kèm reference theo contract; hệ thống chỉ ghi nhận hoàn thủ công, không tự gọi chuyển tiền thật. Không đánh dấu REFUNDED khi chưa có chứng từ.

## Giới hạn
Không có MySQL/TiDB kiểm thử cô lập, nên transaction/migration/concurrency mới chỉ xác minh H2/MySQL-mode, không chứng minh isolation/lock/DDL TiDB. Không gọi ngân hàng/VNPAY/SMTP/Cloudinary thật. Không sửa DB demo/shared, không deploy/restart8080; mọi fixture ghi riêng và đóng sau kiểm. Không khẳng định không regression tuyệt đối hoặc coverage đủ chỉ từ328 tests.



## Chốt kiểm chứng và bàn giao theo yêu cầu dừng mở rộng
- Browser lần đầu phát hiện JS Shipper thiếu groupOrder (projection đã bỏ trường này) và JSON directory Manager phụ thuộc uppercase/lowercase nhãn JDBC. Hai test mới/được tăng assertion **RED2/2**, sửa fallback boolean JSP và normalize DTO map JSON, **GREEN2/2**. Không mở lại trường nhóm hoặc PII để sửa UI.
- UI cuối:36 render ở390/768/1440 và5 luồng thao tác (Manager nhận đơn/tồn + boundary tài chính/ownership; shop lưu giờ có giây; dispatch→pickup→COD→history; Admin cấp/thu scope Shipper; Admin phân công/thu hồi Manager). **41 nhóm kiểm đã đạt**,0 pageerror,0 HTTP5xx trong phạm vi thu thập. Bằng chứng `.local/role-workflow-main/ui/browser-confirmed.log` (40 PASS,1 lỗi harness do form reset lựa chọn sau Save), `.local/role-workflow-main/ui/assignment-only.log` (1/1 PASS sau chọn lại tài khoản). Ba lỗi assertion chữ hoa/thường empty state ban đầu là harness; đã kiểm lại. Không cộng số UI check vào số backend tests.
- Native Manager assignment Save/reload và request từ phiên cũ đã bị chặn theo membership mới. Native Shipper sau DELIVERED đi về history không PII thay vì reload detail404. Ảnh/widget hiện có được giữ; không tạo bộ layout mới.
- Full/WAR trên code cuối: xem final-verified-package.log; không skip. So sánh330 file src/main root và bản build:0 khác biệt tại lúc kiểm. Các sửa cuối chỉ ở ManagerOperationsService, detail.jsp và regression tests tương ứng; không datasource/provider/JWT/schema ngoài migration mới.
- HEAD/nhánh không đổi8e1234b624282ed2e8a9c633695da3bb75b17baf / integration/utetra. Chưa commit/push/merge/deploy; chưa áp migration hoặc seed lên DB demo. Runtime UI thử8898 đã đóng; không dừng/restart8080.

### Phần bàn giao chưa kiểm/chưa bổ sung
1. **TiDB/MySQL thật:** chưa chạy migration/locking/race trên DB cùng engine/isolation. Cần DB kiểm thử riêng; chạy ca phân công tranh chấp, revoke/mutation, giao/hủy, callback/refund/điểm và kiểm DB cuối. H2 không thay thế nghiệm thu này.
2. **Provider thật:** chưa SMTP/Cloudinary upload/xóa, ngân hàng hoặc VNPAY sandbox/merchant; dùng adapter/stub trong tests. Runtime8080 và WAR trên máy báo cáo chưa được chạy nghiệm thu trong lượt này.
3. **UI ngoài luồng đã chạy:** chưa thử đủ từng form refund/QR/callback, media/review video, voucher/cuộn Top20/cart/support trên cả ba viewport sau bản sửa; full suite có kiểm backend liên module nhưng không thay toàn bộ click-through UI.
4. **Admin giám sát Shipper:** đã có scope và phân công từng đơn; chưa có dashboard tổng hợp trạng thái tài khoản cùng số/chuyến đang phụ trách theo từng Shipper. Đây là phần cần bổ sung UI/query đúng scope, không seed dữ liệu giả.
5. **Manager topping vận hành:** backend availability topping có guard mutation; workspace mới có món/variant tồn nhưng chưa có công tắc topping theo từng chi nhánh. Cần thêm readback trạng thái thật và UI scoped; không dùng quyền CATALOG_ADMIN thay khả năng vận hành.
6. **Admin/Vendor form topping tồn:** màn hình cũ còn dạng ghi cấu hình, chưa đọc lại đầy đủ trạng thái đã lưu. Là hạn chế UX/readback đã biết, không khẳng định reload tự làm mất dữ liệu. Không lấp bằng giá trị mặc định.
7. **Điều phối sau pickup:** cố ý chặn đổi bình thường, chưa xây nghiệp vụ bàn giao hàng giữa Shipper. Nhận chuyến đang gộp vào pickup; chưa có bước nhận/từ chối riêng. Nếu nhóm muốn tách, cần contract/state rõ và test; không tự thêm trạng thái trùng.
8. **Dữ liệu lớn/ca mạng:** chưa nghiệm thu phân trang/tìm kiếm khối lượng lớn của danh sách phân công mới, mọi trạng thái mất mạng/retry/focus/accessibility. Các thao tác native và retry backend đã kiểm chỉ chứng minh ca cụ thể.
9. **Triển khai dữ liệu quyền:** Admin cần xác nhận và cấp scope Shipper rõ ràng sau migration theo runbook trên, nhất là chuyến đang giao; không có backfill quyền rộng. Không sửa dữ liệu shared trong phiên này. Phân quyền Manager mới khác tài liệu cũ; nhóm cần đồng bộ báo cáo thành viên theo contract29/10.
10. **Đối soát/chuyển tiền ngoài:** manual refund vẫn REFUND_PENDING đến có chứng từ, không tự chuyển tiền. Dashboard Vendor dùng goods/commission/refund/storeNet theo công thức hiện hành; chưa thêm hệ thống chi trả Vendor hoặc lương Shipper, không gọi số net/COD là tiền đã được ngân hàng chi trả.

Không còn lỗi đã xác nhận nào được để ở trạng thái chờ sửa trong ba lỗi ban đầu và hai lỗi tích hợp vừa phát hiện. Các mục trên tách kiểm chứng môi trường, phần UI/tính năng chưa bổ sung và nghiệp vụ chủ động chưa triển khai; không coi tất cả là bug.

Kết quả cuối: **Tests run: 329, Failures: 0, Errors: 0, Skipped: 0**, BUILD SUCCESS; WAR 151320652 bytes, SHA256 `5cda777a01fc227df2be1467347ae718dcbfa514e67233865dddb371c0f4edb6`. Artifact local: `.local/role-workflow-main/target/utetra.war`.
