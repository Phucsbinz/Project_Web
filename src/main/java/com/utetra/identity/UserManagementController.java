package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
public class UserManagementController {
    private final UserManagementService users;
    public UserManagementController(UserManagementService users) { this.users=users; }
    @GetMapping({"/admin/users/data","/manager/users/data"}) public List<Map<String,Object>> search(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String role,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return users.search(p,q,role,page,size); }
    @PostMapping("/admin/users") public Map<String,Object> create(@AuthenticationPrincipal UtetraPrincipal p,@Valid @RequestBody IdentityForms.CreateAccount form) {
        long id=users.create(p,form);
        return Map.of("id",id,"message","Đã tạo tài khoản và gửi mã OTP qua email. Chủ tài khoản cần xác minh email trước khi đăng nhập.");
    }
    @PutMapping({"/admin/users/{id}","/manager/users/{id}"}) public Map<String,Object> manage(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody IdentityForms.Manage f) { users.manage(p,id,f); return Map.of("message","Đã cập nhật tài khoản và thu hồi phiên."); }
    public record Assignment(@jakarta.validation.constraints.Positive @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.LongValue.class) long storeId,@jakarta.validation.constraints.NotNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.BooleanValue.class) Boolean active) {}
    @PostMapping("/admin/users/{id}/assignments") public Map<String,Object> assign(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Assignment f) { users.assign(p,id,f.storeId(),f.active()); return Map.of("message","Đã cập nhật phân công."); }
    private boolean reviews(String kind) {
        if(!Set.of("reviews","comments").contains(kind)) throw IdentityException.invalid("Loại nội dung không hợp lệ.");
        return "reviews".equals(kind);
    }
    @GetMapping("/admin/moderation/data") public List<Map<String,Object>> moderation(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(defaultValue="reviews") String kind,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return users.moderation(p,reviews(kind),q,status,page,size);
    }
    public record Visibility(@jakarta.validation.constraints.NotNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.BooleanValue.class) Boolean visible,@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.LongValue.class) Long version) {}
    @PutMapping("/admin/moderation/{kind}/{id}") public Map<String,Object> visibility(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable String kind,@PathVariable long id,@Valid @RequestBody Visibility form) {
        users.visibility(p,reviews(kind),id,form.visible(),form.version()); return Map.of("message","Đã cập nhật hiển thị nội dung.");
    }
    @PostMapping({"/admin/reviews/{id}/hide","/admin/comments/{id}/hide"}) public Map<String,Object> hide(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,jakarta.servlet.http.HttpServletRequest req) { users.hide(p,req.getServletPath().startsWith("/admin/reviews"),id); return Map.of("message","Đã ẩn nội dung."); }
}
