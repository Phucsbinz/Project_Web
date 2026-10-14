package com.utetra.order.service;

import org.springframework.http.HttpStatus;

public class OrderProblem extends RuntimeException {
  private final HttpStatus status;

  public OrderProblem(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }

  public static OrderProblem invalid(String text) {
    return new OrderProblem(HttpStatus.BAD_REQUEST, text);
  }

  public static OrderProblem forbidden() {
    return new OrderProblem(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
  }

  public static OrderProblem conflict(String text) {
    return new OrderProblem(HttpStatus.CONFLICT, text);
  }

  public static OrderProblem missing() {
    return new OrderProblem(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu trong phạm vi của bạn.");
  }
}
