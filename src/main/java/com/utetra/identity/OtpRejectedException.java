package com.utetra.identity;
import org.springframework.http.HttpStatus;
/** Only this exception commits a failed OTP attempt. */
public final class OtpRejectedException extends IdentityException {
    public OtpRejectedException() { this("Mã OTP chưa chính xác hoặc không còn hiệu lực. Vui lòng kiểm tra email và dùng mã trong thư mới nhất."); }
    public OtpRejectedException(String message) { super(HttpStatus.BAD_REQUEST,message); }
}
