package com.utetra.identity;
import com.utetra.security.UtetraPrincipal;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CommunityController {
    private final CommunityService community;
    public CommunityController(CommunityService community) { this.community=community; }
    @GetMapping("/products/{id}/community") public Map<String,Object> publicContent(@PathVariable long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return community.publicContent(id,page,size); }
    @PostMapping("/favorites/{id}") public Map<String,Object> like(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id) { return Map.of("count",community.favorite(p,id,true)); }
    @DeleteMapping("/favorites/{id}") public Map<String,Object> unlike(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id) { return Map.of("count",community.favorite(p,id,false)); }
    @PostMapping("/viewed/{id}") public Map<String,Object> view(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id) { community.viewed(p,id); return Map.of("message","Đã ghi nhận lượt xem."); }
    @GetMapping({"/favorites/data","/viewed/data"}) public List<Map<String,Object>> products(@AuthenticationPrincipal UtetraPrincipal p,jakarta.servlet.http.HttpServletRequest req,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return community.products(p,req.getServletPath().startsWith("/viewed"),page,size); }
    @GetMapping("/reviews/purchases") public List<Map<String,Object>> purchases(@AuthenticationPrincipal UtetraPrincipal p) {
        return community.purchases(p);
    }
    @GetMapping({"/reviews/data","/comments/data"}) public List<Map<String,Object>> entries(@AuthenticationPrincipal UtetraPrincipal p,jakarta.servlet.http.HttpServletRequest req,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return community.entries(p,req.getServletPath().startsWith("/reviews"),page,size); }
    @PostMapping(value="/reviews",consumes="multipart/form-data") public Map<String,Object> review(@AuthenticationPrincipal UtetraPrincipal p,@Valid @ModelAttribute IdentityForms.Review form,@RequestParam(required=false) List<MultipartFile> files) { return Map.of("id",community.review(p,form,files),"message","Đã gửi đánh giá."); }
    @PostMapping(value="/comments",consumes="multipart/form-data") public Map<String,Object> comment(@AuthenticationPrincipal UtetraPrincipal p,@Valid @ModelAttribute IdentityForms.Comment form,@RequestParam(required=false) List<MultipartFile> files) { return Map.of("id",community.comment(p,form,files),"message","Đã gửi bình luận."); }
    public record Edit(@CodePointLength(max=2000) String content,@jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(5) @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.IntValue.class) int stars,@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=StrictNumbers.LongValue.class) Long version) {}
    @PutMapping({"/reviews/{id}","/comments/{id}"}) public Map<String,Object> edit(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,@Valid @RequestBody Edit f,jakarta.servlet.http.HttpServletRequest req) { community.edit(p,req.getServletPath().startsWith("/reviews"),id,f.content(),f.stars(),f.version()); return Map.of("message","Đã cập nhật."); }
    @DeleteMapping({"/reviews/{id}","/comments/{id}"}) public Map<String,Object> delete(@AuthenticationPrincipal UtetraPrincipal p,@PathVariable long id,jakarta.servlet.http.HttpServletRequest req) { community.delete(p,req.getServletPath().startsWith("/reviews"),id); return Map.of("message","Đã gỡ nội dung."); }
}
