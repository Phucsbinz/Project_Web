package com.utetra.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class MailOtpDeliveryTest {
    private final JavaMailSender sender=mock(JavaMailSender.class);
    private MailOtpDelivery delivery(boolean enabled,Duration ttl) throws Exception {
        var beans=new StaticListableBeanFactory(); beans.addBean("mail",sender);
        return new MailOtpDelivery(beans.getBeanProvider(JavaMailSender.class),enabled,"sender@example.test",ttl);
    }
    private void bodies(Part part,List<String> text,List<String> html) throws Exception {
        if(part.isMimeType("text/plain")) text.add((String)part.getContent());
        else if(part.isMimeType("text/html")) html.add((String)part.getContent());
        else if(part.getContent() instanceof Multipart multipart)
            for(int i=0;i<multipart.getCount();i++) bodies(multipart.getBodyPart(i),text,html);
    }
    @Test void registrationEmailHasBrandedHtmlAndMatchingPlainCodeWithLeadingZeros() throws Exception {
        var message=new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        delivery(true,Duration.ofMinutes(5)).send("recipient@example.test","000123","REGISTER");
        verify(sender).send(message); message.saveChanges();
        assertThat(((InternetAddress)message.getFrom()[0]).getPersonal()).isEqualTo("UTETra");
        assertThat(message.getSubject()).isEqualTo("UTETra | Xác minh tài khoản");
        var plain=new ArrayList<String>();var html=new ArrayList<String>();bodies(message,plain,html);
        assertThat(plain).hasSize(1);assertThat(html).hasSize(1);
        assertThat(plain.getFirst()).contains("000123","5 phút","email mới nhất");
        assertThat(html.getFirst()).contains("000123","5 phút","Chào bạn, mời vào nhà trà.","#173c32").doesNotContain("{{","http://localhost");
        assertThat(message.getSubject()).doesNotContain("000123");
    }
    @Test void recoveryEmailUsesRecoveryCopyAndConfiguredLifetime() throws Exception {
        var message=new MimeMessage(Session.getInstance(new Properties()));when(sender.createMimeMessage()).thenReturn(message);
        delivery(true,Duration.ofSeconds(90)).send("recipient@example.test","123456","PASSWORD_RESET");
        message.saveChanges();var plain=new ArrayList<String>();var html=new ArrayList<String>();bodies(message,plain,html);
        assertThat(message.getSubject()).isEqualTo("UTETra | Khôi phục mật khẩu");
        assertThat(html.getFirst()).contains("Một bước để trở lại.","90 giây","123456").doesNotContain("5 phút","hoàn tất tài khoản");
    }
    @Test void disabledProviderDoesNotSendAndSmtpFailureRemainsUnavailable() throws Exception {
        assertThatThrownBy(()->delivery(false,Duration.ofMinutes(5)).send("recipient@example.test","123456","REGISTER")).isInstanceOf(IdentityException.class);
        verifyNoInteractions(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("provider unavailable")).when(sender).send(any(MimeMessage.class));
        assertThatThrownBy(()->delivery(true,Duration.ofMinutes(5)).send("recipient@example.test","123456","REGISTER"))
            .isInstanceOf(IdentityException.class).hasMessage("Dịch vụ email chưa sẵn sàng. Vui lòng thử lại sau.");
    }
}
