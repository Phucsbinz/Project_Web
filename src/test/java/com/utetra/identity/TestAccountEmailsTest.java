package com.utetra.identity;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class TestAccountEmailsTest {
 @Test void vietnameseNamePrecedesRole(){assertThat(TestAccountEmails.email("Phan Tuấn Thanh","ROLE_USER")).isEqualTo("thanhpt.guest@example.test");assertThat(TestAccountEmails.email("Đỗ Quang Vinh","ROLE_SHIPPER")).isEqualTo("vinhdq.shipper@example.test");assertThat(TestAccountEmails.email("Nguyễn Đức Minh","ROLE_ADMIN")).isEqualTo("minhnd.admin@example.test");}
 @Test void realAddressesAreExcluded(){assertThat(TestAccountEmails.isTest("test@utetra.example")).isTrue();assertThat(TestAccountEmails.isTest("demo@example.test")).isTrue();assertThat(TestAccountEmails.isTest("real@gmail.com")).isFalse();assertThat(TestAccountEmails.isTest("student@student.hcmute.edu.vn")).isFalse();}
}
