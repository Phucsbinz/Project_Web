package com.utetra.support;
import org.springframework.http.HttpStatus;
public class SupportProblem extends RuntimeException {
 public final HttpStatus status;
 public SupportProblem(HttpStatus status,String message){super(message);this.status=status;}
 static SupportProblem invalid(String text){return new SupportProblem(HttpStatus.BAD_REQUEST,text);}
 static SupportProblem denied(){return new SupportProblem(HttpStatus.FORBIDDEN,"Bạn không có quyền sử dụng kênh CSKH này.");}
 static SupportProblem missing(){return new SupportProblem(HttpStatus.NOT_FOUND,"Không tìm thấy hội thoại trong phạm vi của bạn.");}
}
