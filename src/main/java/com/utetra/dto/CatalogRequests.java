package com.utetra.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Write DTOs deliberately exclude ownership, approval, counters and provider asset IDs. */
public final class CatalogRequests {
    private CatalogRequests() {}
    public record StoreInput(@NotBlank @Size(max=150) String name,
        @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=150) String slug,
        @NotBlank @Size(max=500) String address, @NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}") String phone,
        @Size(max=4000) String description, @NotNull LocalTime openTime, @NotNull LocalTime closeTime,
        boolean acceptingOrders, @PositiveOrZero long version) {}
    public record CategoryInput(@NotBlank @Size(max=100) String name,
        @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=100) String slug,
        @Size(max=4000) String description, @Min(0) int displayOrder, boolean active) {}
    public record ToppingInput(@NotBlank @Size(max=100) String name,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal price, boolean active) {}
    public record VariantInput(@NotBlank @Pattern(regexp="S|M|L") String sizeName,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal extraPrice, boolean active,
        @Min(0) @Max(1000000) @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.IntValue.class) Integer stockQuantity,
        @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long stockVersion) {
        public VariantInput(String sizeName,BigDecimal extraPrice,boolean active) { this(sizeName,extraPrice,active,null,null); }
    }
    public record ProductInput(@NotNull Long storeId, @NotNull Long categoryId,
        @NotBlank @Size(max=150) String name,
        @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=150) String slug,
        @Size(max=4000) String description,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal basePrice,
        boolean active, boolean available, boolean featured,
        @NotEmpty @Size(max=3) List<@jakarta.validation.Valid VariantInput> variants,
        @NotNull @Size(max=50) Set<@NotNull Long> toppingIds, @PositiveOrZero long version,
        @Size(max=150) @Pattern(regexp="|[a-z0-9]+(?:-[a-z0-9]+)*") String catalogKey) {
        public ProductInput(Long storeId,Long categoryId,String name,String slug,String description,BigDecimal basePrice,
            boolean active,boolean available,boolean featured,List<VariantInput> variants,Set<Long> toppingIds,long version) {
            this(storeId,categoryId,name,slug,description,basePrice,active,available,featured,variants,toppingIds,version,null);
        }
    }
    public record PromotionInput(Long storeId, @NotBlank @Size(max=150) String name,
        @Pattern(regexp="[A-Za-z0-9_-]{3,50}") String code,
        @NotNull @Pattern(regexp="PRODUCT_PERCENT|SHIPPING_PERCENT") String kind,
        @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal percent,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal maxDiscount,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal minOrder,
        @Min(1) int usageLimit, @Min(1) int perUserLimit, @NotNull Instant startsAt, @NotNull Instant endsAt,
        boolean active, @NotNull @Size(max=200) Set<@NotNull Long> productIds, @PositiveOrZero long version) {}
    public record DrinkInput(@NotNull Long variantId, @Min(1) @Max(20) int quantity,
        int sugarLevel, int iceLevel, @NotNull @Size(max=50) Set<@NotNull Long> toppingIds) {}
    public record Decision(@NotNull @Pattern(regexp="APPROVED|REJECTED") String decision,
        @NotBlank @Size(max=1000) String note, @PositiveOrZero long version, @Positive Long commissionPolicyId) {}
    public record StoreState(@NotNull @Pattern(regexp="ACTIVE|SUSPENDED") String status, @PositiveOrZero long version) {}
    public record AreaInput(@NotBlank @Size(max=50) String areaCode, @NotBlank @Size(max=150) String name, boolean active) {}
    public record Availability(boolean available) {}
    public record Membership(@NotNull Long userId, boolean active) {}
}
