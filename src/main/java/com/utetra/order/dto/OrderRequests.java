package com.utetra.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.utetra.identity.StrictNumbers;

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
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long variantId,
      @Min(0) @Max(100) @JsonDeserialize(using = StrictNumbers.IntValue.class) int sugar,
      @Min(0) @Max(100) @JsonDeserialize(using = StrictNumbers.IntValue.class) int ice,
      @JsonDeserialize(contentUsing = StrictNumbers.LongValue.class) List<@Positive Long> toppingIds,
      @Min(1) @Max(20) @JsonDeserialize(using = StrictNumbers.IntValue.class) int quantity,
      @Size(max = 255) String note)
      implements java.io.Serializable {}

  public record Quantity(
      @Min(1) @Max(20) @JsonDeserialize(using = StrictNumbers.IntValue.class) int quantity,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version) {}

  public record Checkout(
      @NotBlank @Size(max = 36) String key,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long addressId,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long areaId,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long carrierId,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) Long productPromotionId,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) Long shippingPromotionId,
      @Min(0) @JsonDeserialize(using = StrictNumbers.IntValue.class) int points,
      @NotBlank String quoteHash,
      @Size(max = 500) String note) {}

  public record Transition(
      @NotBlank String status,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version,
      @Size(max = 500) String reason,
      @JsonDeserialize(using = StrictNumbers.BooleanValue.class) boolean cashCollected) {}

  public record ReturnInput(@NotBlank @Size(max = 2000) String reason) {}

  public record VersionInput(@NotNull @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) Long version) {}

  public record Decision(
      @JsonDeserialize(using = StrictNumbers.BooleanValue.class) boolean approve,
      @JsonDeserialize(using = StrictNumbers.BooleanValue.class) boolean waivePhysicalReturn,
      @NotBlank @Size(max = 1000) String reason,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version) {}

  public record RefundInput(@NotBlank @Size(max = 500) String evidence,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version) {}

  public record Assignment(
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long shipperId,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version) {}

  public record CarrierInput(
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) Long id,
      @NotBlank @Size(max = 150) String name,
      @NotNull @PositiveOrZero java.math.BigDecimal baseFee,
      @JsonDeserialize(using = StrictNumbers.BooleanValue.class) boolean active,
      @Min(0) @JsonDeserialize(using = StrictNumbers.LongValue.class) long version) {}

  public record RateInput(
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long carrierId,
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long areaId,
      @NotNull @PositiveOrZero java.math.BigDecimal fee,
      @JsonDeserialize(using = StrictNumbers.BooleanValue.class) boolean active) {}

  public record GlobalCommissionInput(
      @NotNull @DecimalMin("0") @DecimalMax("100") java.math.BigDecimal percent) {}

  public record CommissionInput(
      @Positive @JsonDeserialize(using = StrictNumbers.LongValue.class) long storeId,
      @NotNull @DecimalMin("0") @DecimalMax("100") java.math.BigDecimal percent) {}
}
