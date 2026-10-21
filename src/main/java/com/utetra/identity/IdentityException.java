package com.utetra.identity;
import org.springframework.http.HttpStatus;
public class IdentityException extends RuntimeException {
    private final HttpStatus status;
    public IdentityException(HttpStatus status, String message) { super(message); this.status=status; }
    public HttpStatus status() { return status; }
    public static IdentityException invalid(String message) { return new IdentityException(HttpStatus.BAD_REQUEST,message); }
    public static IdentityException denied() { return new IdentityException(HttpStatus.FORBIDDEN,"Bạn không có quyền thực hiện thao tác này."); }
    public static IdentityException missing() { return new IdentityException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu."); }
    public static IdentityException conflict() { return new IdentityException(HttpStatus.CONFLICT,"Dữ liệu đã thay đổi; vui lòng tải lại."); }
}
