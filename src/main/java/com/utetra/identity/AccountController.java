package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/account")
public class AccountController {
    private final AccountService accounts; private final LoyaltyService loyalty; private final AuthCookies cookies;
    public AccountController(AccountService accounts,LoyaltyService loyalty,AuthCookies cookies) { this.accounts=accounts; this.loyalty=loyalty; this.cookies=cookies; }
    @GetMapping("/profile/data") public Map<String,Object> profile(@AuthenticationPrincipal UtetraPrincipal p) { return accounts.profile(p); }
    @PostMapping("/profile") public Map<String,Object> save(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody IdentityForms.Profile f) { accounts.profile(p,f); return ok(); }
    @PostMapping("/profile/save") public Map<String,Object> saveWithAvatar(@AuthenticationPrincipal UtetraPrincipal p,
        @RequestParam String fullName,@RequestParam(required=false) String phone,@RequestParam Long version,
        @RequestParam(required=false) MultipartFile file) {
        accounts.profileWithAvatar(p,new IdentityForms.Profile(fullName,phone,version),file); return ok();
    }
    @PostMapping("/profile/avatar") public Map<String,Object> avatar(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam MultipartFile file) { accounts.avatar(p,file); return ok(); }
    @PostMapping("/password") public Map<String,Object> password(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody IdentityForms.Password f,HttpServletRequest req,HttpServletResponse res) { accounts.password(p,f); cookies.clear(req,res); return Map.of("message","Đã đổi mật khẩu. Vui lòng đăng nhập lại.","redirect",req.getContextPath()+"/auth/login"); }
    @GetMapping("/addresses/data") public List<Map<String,Object>> addresses(@AuthenticationPrincipal UtetraPrincipal p) { return accounts.addresses(p); }
    @PostMapping("/addresses") public Map<String,Object> add(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody IdentityForms.Address f) { return Map.of("id",accounts.saveAddress(p,null,f),"message","Đã thêm địa chỉ."); }
    @PutMapping("/addresses/{id}") public Map<String,Object> edit(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody IdentityForms.Address f) { accounts.saveAddress(p,id,f); return ok(); }
    @DeleteMapping("/addresses/{id}") public Map<String,Object> delete(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id) { accounts.deleteAddress(p,id); return ok(); }
    @GetMapping("/loyalty/data") public Map<String,Object> loyalty(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return loyalty.summary(p,page,size); }
    private Map<String,Object> ok() { return Map.of("message","Đã lưu thay đổi."); }
}
