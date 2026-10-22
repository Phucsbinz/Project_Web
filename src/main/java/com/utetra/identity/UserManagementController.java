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
    @GetMapping({"/admin/users/data","/manager/users/data"}) public List<Map<String,Object>> search(@AuthenticationPrincipal UtetraPrincipal p,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return users.search(p,q,page,size); }
    @PutMapping({"/admin/users/{id}","/manager/users/{id}"}) public Map<String,Object> manage(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody IdentityForms.Manage f) { users.manage(p,id,f); return Map.of("message","Đã cập nhật tài khoản và thu hồi phiên."); }
    public record Assignment(@jakarta.validation.constraints.Positive @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.LongValue.class) long storeId,@jakarta.validation.constraints.NotNull @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.BooleanValue.class) Boolean active) {}
    @PostMapping("/admin/users/{id}/assignments") public Map<String,Object> assign(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Assignment f) { users.assign(p,id,f.storeId(),f.active()); return Map.of("message","Đã cập nhật phân công."); }
    @PostMapping({"/admin/reviews/{id}/hide","/admin/comments/{id}/hide"}) public Map<String,Object> hide(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,jakarta.servlet.http.HttpServletRequest req) { users.hide(p,req.getServletPath().startsWith("/admin/reviews"),id); return Map.of("message","Đã ẩn nội dung."); }
}
