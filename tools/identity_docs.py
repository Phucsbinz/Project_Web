"""Build reviewable identity evidence from current source, without changing historical logs."""
from pathlib import Path
import hashlib, html, json, re, subprocess, sys, textwrap

root=Path('docs/features/identity-membership')
source=Path('src/main/java/com/utetra/identity')
cases={
'UC-AUTH-01':('AuthService','register','Chuẩn hóa username/email; kiểm unique và giới hạn BCrypt; tạo User chưa verified; lưu HMAC và gửi SMTP','registrationHashesPasswordAndRequiresActivation; smtpFailureRollsBackRegistration','register-desktop'),
'UC-AUTH-02':('OtpService','verify','Khóa User và challenge REGISTER; kiểm TTL/attempts/HMAC; consume và kích hoạt; từ chối replay','wrongOtpAttemptsCommitAndBlockTheCorrectCode; expiredWrongPurposeAndResendAreRejected','otp-desktop'),
'UC-AUTH-03':('AuthService','login','Kiểm BCrypt/active/verified; lưu refresh hash; ký JWT sid; đặt HttpOnly cookie và chuyển hồ sơ','registrationHashesPasswordAndRequiresActivation; csrfCookiesAndProfileRoleRulesWorkThroughHttp','login-desktop'),
'UC-AUTH-04':('AuthService','logout','Thu hồi sid hoặc refresh hợp lệ; xóa cả hai cookie đúng path; scheduler ngắt idle socket; chuyển đăng nhập','refreshRotatesAndKeepsAbsoluteExpiration; IdentitySocketLifecycleTest','profile-desktop'),
'UC-AUTH-05':('OtpService','request','Giới hạn IP/email/cooldown; challenge PASSWORD_RESET kể cả email không tồn tại; gửi SMTP; trả phản hồi chung','otpResendRevokesOldChallengeAndUnknownEmailUsesSameDelivery','forgot-desktop'),
'UC-AUTH-06':('AuthService','reset','Verify OTP và cấp grant hash 5 phút; khóa và kiểm grant; đổi BCrypt/consume; thu hồi tất cả phiên','resetGrantIsOneUseAndRevokesAllSessions','reset-desktop'),
'UC-AUTH-07':('AuthService','refresh','Khóa User rồi session; kiểm hash/hạn tuyệt đối/active; xoay hash; ký JWT mới','concurrentRefreshHasExactlyOneWinner; refreshRotatesAndKeepsAbsoluteExpiration','profile-desktop'),
'UC-USER-01':('AccountService','profile','Kiểm phiên hiện tại; kiểm version khi sửa hồ sơ; upload avatar và bù rollback; đổi mật khẩu và revoke','passwordByteLimitAndStaleProfileAreRejected; avatarUploadIsCompensatedOnRollbackAndOldAssetDeletedAfterCommit','profile-desktop'),
'UC-USER-02':('AccountService','saveAddress','Kiểm User/Vendor và owner; khóa User; CRUD địa chỉ và tối đa một mặc định; trả snapshot riêng cho Order','addressesAreOwnedAndHaveOneDefault; concurrentDefaultAddressWritesSerialize','addresses-desktop'),
'UC-USER-03':('CommunityService','favorite','Kiểm User/Vendor và product public; khóa User chống concurrent duplicate; thêm/xóa theo owner; trả tổng like','vendorCanLikeAndViewIdempotently','favorites-desktop'),
'UC-USER-04':('CommunityService','viewed','Kiểm current principal và product; khóa User; upsert timestamp/count; danh sách riêng theo timestamp và id','vendorCanLikeAndViewIdempotently','viewed-desktop'),
'UC-USER-05':('CommunityService','review','Kiểm order item own DELIVERED+PAID; sao 1–5 và unique item; lưu review/media; public rating loại HIDDEN','purchaseEvidenceAndCommentLengthAreEnforced; mediaRollbackCleansUploadedAssetAndPublicContentHidesModeratedRows','reviews-desktop'),
'UC-USER-06':('CommunityService','comment','Kiểm đã mua đúng product/item; trim 50–2000 codepoints; validate JPEG/PNG/MP4 và upload; lưu rồi hiển thị textContent','purchaseEvidenceAndCommentLengthAreEnforced; fakeVideoAndExcessMediaAreRejectedBeforeUpload','comments-desktop'),
'UC-USER-07':('LoyaltyService','apply','Khóa order rồi User/account trong transaction caller; unique order/type; REDEEM/REFUND/EARN/REVERSAL; cập nhật balance/debt/tier','loyaltyEarnRedeemReversalCreatesDebtAndRepaysIt; loyaltyRollsBackWithOrderTransactionAndRefusesUnpaidEarn','loyalty-desktop'),
'UC-MGR-01':('UserManagementService','manage','Kiểm Manager hiện tại; scope membership và customer/owner shop; chỉ đổi active của User/Vendor; revoke và audit','managerCannotEscalateOrReadUnassignedCustomers; managerMembershipControlsStoreAccessAndAdminAssignmentRevokesSessions','manager-desktop'),
'UC-ADM-01':('UserManagementService','manage','Kiểm Admin; khóa role Admin giữ Admin cuối; kiểm version và đổi role/active/assignment; revoke và audit','lastAdminIsProtected; managerCannotEscalateOrReadUnassignedCustomers','admin-desktop')
}

def excerpt(file,method):
    lines=file.read_text(encoding='utf-8').splitlines()
    matches=[i for i,line in enumerate(lines) if re.search(r'public .*\b'+method+r'\(',line)]
    start=matches[-1] if matches else 0
    start=max(0,start-1)
    end=min(len(lines),start+24)
    return [(i+1,lines[i]) for i in range(start,end)]

sys.path.insert(0,str(Path('.tools/browser').resolve()))
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    browser=p.chromium.launch(channel='msedge',headless=True)
    page=browser.new_page(viewport={'width':1500,'height':1050},device_scale_factor=1)
    for uc,(service,method,flow,tests,ui) in cases.items():
        dest=root/'assets'/uc; dest.mkdir(parents=True,exist_ok=True)
        steps=flow.split('; ')
        mmd='flowchart TD\n  Start([Bắt đầu]) --> Permission{Input, phiên và quyền hợp lệ?}\n  Permission -->|Không| Deny[Từ chối, không ghi dở]\n  Permission -->|Có| S0\n'
        for i,step in enumerate(steps):
            mmd+=f'  S{i}["{step}"]\n'
            if i+1<len(steps): mmd+=f'  S{i} --> S{i+1}\n'
        mmd+=f'  S{len(steps)-1} --> End([Trả DTO/View])\n  Deny --> End\n'
        (dest/'activity.mmd').write_text(mmd,encoding='utf-8')
        height=160+len(steps)*115
        svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="{height}" viewBox="0 0 1000 {height}"><rect width="1000" height="{height}" fill="#faf8f2"/><g font-family="Arial,sans-serif" font-size="20" fill="#153d30"><text x="35" y="40" font-weight="bold">{uc} · {service}.{method}</text><text x="35" y="75" font-size="17">Kiểm input/phiên/quyền → lỗi: từ chối và rollback (OTP sai: lưu attempts)</text>'
        for i,step in enumerate(steps):
            y=100+i*115
            svg+=f'<rect x="35" y="{y}" width="930" height="88" rx="12" fill="white" stroke="#198754"/>'
            for j,line in enumerate(textwrap.wrap(f'{i+1}. {step}',90)):
                svg+=f'<text x="55" y="{y+32+j*26}">{html.escape(line)}</text>'
            if i+1<len(steps): svg+=f'<path d="M500 {y+88}v22m-6-7 6 7 6-7" fill="none" stroke="#198754" stroke-width="2"/>'
        svg+=f'<text x="35" y="{height-18}" font-size="16">Nguồn code thực · 17/10/2026 · H2 test; provider/integration nghiệm thu riêng</text></g></svg>'
        (dest/'activity.svg').write_text(svg,encoding='utf-8')
        snippet=excerpt(source/(service+'.java'),method)
        code=''.join(f'<div><span class="number">{number:3}</span> {html.escape(line)}</div>' for number,line in snippet)
        page.set_content(f'<html><meta charset="UTF-8"><style>body{{margin:0;background:#10251f;color:#edf5f1;padding:32px;font:18px Consolas,monospace}}h1{{font:24px Arial;color:#b1eacb}}.number{{color:#829f94}}pre{{line-height:1.5;white-space:pre-wrap;overflow-wrap:anywhere}}footer{{font:16px Arial;color:#b1eacb}}</style><h1>{uc} · {service}.{method}</h1><pre>{code}</pre><footer>1. Validation/quyền  ·  2. Khóa/transaction  ·  3. Cập nhật có điều kiện — trích source hiện tại</footer></html>')
        page.screenshot(path=str(dest/'code.png'),full_page=True)
        note=root/'use-cases'/(uc+'.md'); old=note.read_text(encoding='utf-8')
        update=f'''
## Cập nhật triển khai — 17/10/2026

Baseline sau đồng bộ: `8a18438`. Code hiện chưa commit/push theo yêu cầu phiên này; chưa có reviewer do nhóm xác nhận. Các bảng tiêu chí “chưa chạy” phía trên là hồ sơ thiết kế lịch sử; kết quả thực hiện hành ở phần này và [VERIFICATION](../VERIFICATION.md).

Luồng code thực: {flow}.

1. Entity metadata tại `src/main/java/com/utetra/identity/entity`, giữ User/Role/AuthSession nền. Migration `V2026101701__identity_membership.sql` không sửa V1; FK liên module là đề xuất riêng chờ main review.
2. `IdentityRepository` dùng EntityManager/native projections và generated keys trên cùng Hibernate connection; Service giữ validation, current principal, transaction và row lock. Không đưa SQL vào Controller/JSP.
3. Source trọng tâm: `src/main/java/com/utetra/identity/{service}.java` → `{method}`. DTO đầu vào ở `IdentityForms`; controller tương ứng không trả Entity hoặc secret.
4. View `WEB-INF/views/identity/page.jsp` (login ở `auth/login.jsp`) và `assets/js/identity.js`: CSRF, loading, lỗi/rỗng, paging và textContent. Các đường dẫn route thực xem [RUNBOOK](../RUNBOOK.md).
5. Test đã chạy: `{tests}` trong `IdentityIntegrationTest` hoặc lớp test được ghi tên. Ảnh UI dùng dữ liệu giả lập H2, không chứng minh SMTP/Cloudinary/TiDB hoặc checkout tích hợp.

![Activity từ code hiện tại](../assets/{uc}/activity.svg)

Nguồn sơ đồ có thể sửa: [activity.mmd](../assets/{uc}/activity.mmd). Các nhánh lỗi và ranh giới transaction cụ thể xem source/test; đây là diagram code, không phải ảnh chạy provider.

![Đoạn source thực và chú giải](../assets/{uc}/code.png)

Ảnh giao diện: {'[màn hình thật từ fixture H2](../assets/browser/'+ui+'.png)' if (root/'assets/browser'/(ui+'.png')).exists() else 'chưa chụp được màn hình riêng; không thay bằng mockup hoặc ảnh giả.'}

Setup/demo: [RUNBOOK](../RUNBOOK.md). Prompt thật: `IDN-AI-001` trong [AI_PROMPT_HISTORY](../../../AI_PROMPT_HISTORY.md); phản hồi và nhóm review tại [AI_LOG](../AI_LOG.md). Chưa có commit chức năng hoặc reviewer; phần phụ thuộc cần ghép thật được ghi trong CONTRACT_REVIEW và VERIFICATION.
'''
        old=old.split('\n## Cập nhật triển khai — 17/10/2026')[0]
        old=old.replace('Đặc tả thiết kế; chưa có code, ảnh chạy hoặc test nghiệp vụ của UC này.',
            'Đặc tả ban đầu; code/test và ảnh thực được cập nhật ngày 17/10/2026 ở cuối hồ sơ.')
        old=old.replace('Đặc tả bổ sung 14/10/2026; chức năng chưa triển khai',
            'Đặc tả 14/10/2026; code/test cập nhật 17/10/2026, xem phần cuối')
        note.write_text(old+'\n'+update,encoding='utf-8')
    browser.close()

# Read the complete tracked Markdown corpus and retain a reproducible inventory.
paths=subprocess.check_output(['git','ls-files','--','*.md'],text=True,encoding='utf-8').splitlines()
inventory=[]
for path in paths:
    data=Path(path).read_bytes(); content=data.decode('utf-8-sig')
    inventory.append({'path':path,'lines':len(content.splitlines()),'sha256':hashlib.sha256(data).hexdigest()})
(root/'DOCUMENT_INVENTORY.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'markdown_files_read':len(inventory),'uc_evidence_sets':len(cases)}))
