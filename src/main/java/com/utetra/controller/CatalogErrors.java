package com.utetra.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes={CatalogManagementController.class,com.utetra.controller.publicweb.CatalogController.class})
public class CatalogErrors {
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class})
    public ResponseEntity<?> conflict(RuntimeException ex) { return ResponseEntity.status(409).body(Map.of("message","Dữ liệu trùng, đã thay đổi hoặc đang được tham chiếu. Hãy tải lại.")); }
    @ExceptionHandler({ConstraintViolationException.class,MethodArgumentNotValidException.class})
    public ResponseEntity<?> invalid(Exception ex) { return ResponseEntity.badRequest().body(Map.of("message","Dữ liệu không hợp lệ. Kiểm tra các trường bắt buộc, giá, thời hạn và hạn mức.")); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException ex) { return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Không thể xử lý yêu cầu":ex.getReason())); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> provider(IllegalStateException ex) { return ResponseEntity.status(503).body(Map.of("message","Dịch vụ ảnh chưa sẵn sàng. Dữ liệu chưa được cập nhật.")); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> argument(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("message","Ảnh hoặc dữ liệu đầu vào không hợp lệ.")); }
}
