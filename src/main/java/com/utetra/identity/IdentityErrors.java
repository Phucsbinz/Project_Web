package com.utetra.identity;
import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice(basePackages="com.utetra.identity")
public class IdentityErrors {
    @ExceptionHandler(IdentityException.class) public ResponseEntity<?> identity(IdentityException ex) { return error(ex.status(),ex.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.validation.BindException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class})
    public ResponseEntity<?> validation(Exception ex) { return error(HttpStatus.BAD_REQUEST,"Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường, mã dữ liệu và định dạng tệp."); }
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class}) public ResponseEntity<?> conflict(Exception ex) { return error(HttpStatus.CONFLICT,"Dữ liệu trùng lặp hoặc đã thay đổi. Vui lòng tải lại."); }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> database(DataAccessException ex) { return error(HttpStatus.SERVICE_UNAVAILABLE,"Dữ liệu chưa sẵn sàng; vui lòng thử lại sau khi tích hợp module phụ thuộc."); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) public ResponseEntity<?> upload(MaxUploadSizeExceededException ex) { return error(HttpStatus.PAYLOAD_TOO_LARGE,"Ảnh tối đa 5MB; video tối đa 20MB; tổng request tối đa 25MB."); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class) public ResponseEntity<?> mediaType(Exception ex) { return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Content-Type của yêu cầu không được hỗ trợ."); }
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<?> provider(Exception ex) { return error(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ chưa sẵn sàng. Vui lòng thử lại sau."); }
    private ResponseEntity<?> error(HttpStatus status,String message) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message",message)); }
}
