package com.utetra.identity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import jakarta.mail.MessagingException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
@Service
public class MailOtpDelivery implements OtpDelivery {
    private final ObjectProvider<JavaMailSender> sender; private final boolean enabled; private final String from,template,validity;
    public MailOtpDelivery(ObjectProvider<JavaMailSender> sender,@Value("${app.mail.enabled:false}") boolean enabled,@Value("${app.mail.from}") String from,@Value("${app.otp.ttl}") Duration ttl) throws IOException {
        this.sender=sender; this.enabled=enabled; this.from=from;
        this.validity=ttl.toSeconds()%60==0?ttl.toMinutes()+" phút":ttl.toSeconds()+" giây";
        this.template=new ClassPathResource("mail/otp.html").getContentAsString(StandardCharsets.UTF_8);
    }
    public void send(String email,String otp,String purpose) {
        if(!enabled || sender.getIfAvailable()==null) throw unavailable();
        if(otp==null || !otp.matches("[0-9]{6}") || !java.util.Set.of("REGISTER","PASSWORD_RESET").contains(purpose)) throw new IllegalArgumentException("Invalid OTP message");
        boolean registration="REGISTER".equals(purpose);
        String title=registration?"Chào bạn, mời vào nhà trà.":"Một bước để trở lại.";
        String action=registration?"Xác minh tài khoản":"Khôi phục mật khẩu";
        String intro=registration
            ?"Cảm ơn bạn đã chọn UTETra. Nhập mã bên dưới trên trang đăng ký để xác minh email và hoàn tất tài khoản của bạn."
            :"UTETra vừa nhận được yêu cầu đặt lại mật khẩu. Nhập mã bên dưới trên trang khôi phục để tiếp tục tạo mật khẩu mới.";
        String plain="UTETra — "+action+"\n\n"+intro+"\n\nMã OTP của bạn: "+otp
            +"\nHiệu lực: "+validity+". Mã chỉ dùng một lần."
            +"\nNếu đã yêu cầu gửi lại, hãy dùng mã trong email mới nhất."
            +"\nKhông chia sẻ mã này. UTETra không yêu cầu bạn cung cấp OTP qua điện thoại hoặc tin nhắn."
            +"\nNếu không thực hiện yêu cầu này, bạn có thể bỏ qua email.\n\nUTETra — Một chút trà. Một ngày vui.";
        String html=template.replace("{{title}}",title).replace("{{action}}",action)
            .replace("{{intro}}",intro).replace("{{code}}",otp).replace("{{validity}}",validity);
        try {
            var provider=sender.getObject(); var message=provider.createMimeMessage();
            var helper=new MimeMessageHelper(message,true,StandardCharsets.UTF_8.name());
            helper.setFrom(from,"UTETra"); helper.setTo(email); helper.setSubject("UTETra | "+action);
            helper.setText(plain,html); provider.send(message);
        } catch(MessagingException | IOException | MailException ex) { throw unavailable(); }
    }
    private IdentityException unavailable() { return new IdentityException(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ email chưa sẵn sàng. Vui lòng thử lại sau."); }
}
