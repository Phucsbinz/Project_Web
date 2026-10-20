package com.utetra.identity;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
public final class IdentityValidation {
    private IdentityValidation() {}
    public static String email(String value) {
        if(value==null) throw IdentityException.invalid("Email không hợp lệ.");
        String normalized=value.strip().toLowerCase(Locale.ROOT);
        if(normalized.isEmpty() || normalized.length()>254 || normalized.chars().anyMatch(Character::isISOControl)) throw IdentityException.invalid("Email không hợp lệ.");
        try {
            var address=new jakarta.mail.internet.InternetAddress(normalized,true); address.validate();
            if(address.isGroup() || address.getPersonal()!=null || !normalized.equals(address.getAddress()) || normalized.indexOf('@')>64) throw new jakarta.mail.internet.AddressException();
        }
        catch(jakarta.mail.internet.AddressException ex) { throw IdentityException.invalid("Email không hợp lệ."); }
        return normalized;
    }
    public static boolean validPhone(String value) {
        if(value==null || value.isBlank()) return true;
        String s=value.strip(); if(s.length()>20 || !s.matches("\\+?[0-9 ()-]+")) return false;
        int digits=(int)s.chars().filter(c -> c>='0' && c<='9').count();
        return digits>=8 && digits<=15;
    }
    public static String phone(String value,boolean required) {
        if(!validPhone(value) || required && (value==null || value.isBlank())) throw IdentityException.invalid("Số điện thoại cần 8–15 chữ số và tối đa 20 ký tự.");
        return value==null || value.isBlank()?null:value.strip();
    }
    public static void id(long value) { if(value<=0) throw IdentityException.invalid("Mã dữ liệu phải là số nguyên dương."); }
    public static void version(Long value) { if(value==null || value<0) throw IdentityException.invalid("Cần gửi version hợp lệ. Vui lòng tải lại dữ liệu."); }
    public static void passwordInput(String value) {
        if(value==null || value.isBlank() || value.getBytes(StandardCharsets.UTF_8).length>72 || value.codePoints().anyMatch(Character::isISOControl))
            throw IdentityException.invalid("Mật khẩu không được trống, chứa ký tự điều khiển hoặc vượt 72 byte UTF-8.");
    }
    public static void password(String value) {
        passwordInput(value);
        if(value.codePointCount(0,value.length())<8)
            throw IdentityException.invalid("Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
    }
    public static String text(String value,int min,int max) {
        String s=value==null?"":value.strip();
        int length=s.codePointCount(0,s.length());
        if(length<min || length>max || s.codePoints().anyMatch(c -> Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t')) throw IdentityException.invalid("Nội dung cần từ "+min+" đến "+max+" ký tự hợp lệ.");
        return s;
    }
    public static int offset(int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) throw IdentityException.invalid("Phân trang không hợp lệ.");
        return page*size;
    }
}
