package com.utetra.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public final class OrderRequests {
  private OrderRequests() {}

  public record Actor(long userId, String role) {
    public boolean is(String value) {
      return ("ROLE_" + value).equals(role);
    }

    public boolean buyer() {
      return is("USER") || is("VENDOR");
    }
  }

  public record Item(
      @Positive long variantId,
      @Min(0) @Max(100) int sugar,
      @Min(0) @Max(100) int ice,
      List<@Positive Long> toppingIds,
      @Min(1) @Max(20) int quantity,
      @Size(max = 255) String note)
      implements java.io.Serializable {}

  public record Quantity(@Min(1) @Max(20) int quantity, @Min(0) long version) {}

  public record Checkout(
      @NotBlank @Size(max = 36) String key,
      @Positive long addressId,
      @Positive long areaId,
      @Positive long carrierId,
      Long productPromotionId,
      Long shippingPromotionId,
      @Min(0) int points,
      @NotBlank String quoteHash,
      @Size(max = 500) String note) {}

  public record Transition(
      @NotBlank String status,
      @Min(0) long version,
      @Size(max = 500) String reason,
      boolean cashCollected) {}

  public record ReturnInput(@NotBlank @Size(max = 2000) String reason) {}

  public record VersionInput(@NotNull @Min(0) Long version) {}

  public record Decision(
      boolean approve,
      boolean waivePhysicalReturn,
      @NotBlank @Size(max = 1000) String reason,
      @Min(0) long version) {}

  public record RefundInput(@NotBlank @Size(max = 500) String evidence, @Min(0) long version) {}

  public record Assignment(@Positive long shipperId, @Min(0) long version) {}

  public record CarrierInput(
      Long id,
      @NotBlank @Size(max = 150) String name,
      @PositiveOrZero java.math.BigDecimal baseFee,
      boolean active,
      @Min(0) long version) {}

  public record RateInput(
      @Positive long carrierId,
      @Positive long areaId,
      @PositiveOrZero java.math.BigDecimal fee,
      boolean active) {}

  public record GlobalCommissionInput(
      @DecimalMin("0") @DecimalMax("100") java.math.BigDecimal percent) {}

  public record CommissionInput(
      @Positive long storeId, @DecimalMin("0") @DecimalMax("100") java.math.BigDecimal percent) {}
}
