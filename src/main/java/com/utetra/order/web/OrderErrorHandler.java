package com.utetra.order.web;

import com.utetra.order.service.OrderProblem;
import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages = "com.utetra.order.web")
public class OrderErrorHandler {
  @ExceptionHandler(OrderProblem.class)
  public ResponseEntity<?> problem(OrderProblem e) {
    return ResponseEntity.status(e.status()).body(Map.of("error", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Dữ liệu không hợp lệ; kiểm tra các trường và giới hạn."));
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    NullPointerException.class,
    org.springframework.http.converter.HttpMessageNotReadableException.class
  })
  public ResponseEntity<?> input(Exception e) {
    return ResponseEntity.badRequest().body(Map.of("error", "Yêu cầu thiếu hoặc sai dữ liệu."));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class,
    org.springframework.dao.PessimisticLockingFailureException.class
  })
  public ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "error",
                "Dữ liệu đã thay đổi hoặc thao tác trùng; hãy tải lại và kiểm tra trạng thái."));
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<?> dependencies(Exception e) {
    return ResponseEntity.status(503)
        .body(
            Map.of("error", "Dữ liệu phục vụ chức năng hiện chưa sẵn sàng. Vui lòng thử lại sau."));
  }
}
