package com.utetra.support;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.dao.DataAccessException;
import java.util.Map;
@RestControllerAdvice(basePackages="com.utetra.support")
public class SupportErrors {
 @ExceptionHandler(SupportProblem.class) public ResponseEntity<?> problem(SupportProblem e){return response(e.status,e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingServletRequestParameterException.class}) public ResponseEntity<?> invalid(Exception e){return response(HttpStatus.BAD_REQUEST,"Dữ liệu hội thoại không hợp lệ.");}
 @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> unavailable(Exception e){return response(HttpStatus.SERVICE_UNAVAILABLE,"CSKH chưa sẵn sàng. Vui lòng tải lại và thử sau.");}
 private ResponseEntity<?> response(HttpStatus status,String message){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message",message));}
}
