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
        boolean acceptingOrders, @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {
        public StoreInput(String name,String slug,String address,String phone,String description,LocalTime openTime,LocalTime closeTime,boolean acceptingOrders,long version) {
            this(name,slug,address,phone,description,openTime,closeTime,acceptingOrders,Long.valueOf(version));
        }
    }
    public record CategoryInput(@NotBlank @Size(max=100) String name,
        @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=100) String slug,
        @Size(max=4000) String description, @Min(0) int displayOrder, boolean active,
        @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {
        public CategoryInput(String name,String slug,String description,int displayOrder,boolean active) {
            this(name,slug,description,displayOrder,active,null);
        }
    }
    public record ToppingInput(@NotBlank @Size(max=100) String name,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=0) BigDecimal price, boolean active,
        @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {
        public ToppingInput(String name,BigDecimal price,boolean active) { this(name,price,active,null); }
    }
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
        @NotNull @Size(max=50) Set<@NotNull Long> toppingIds, @PositiveOrZero Long version,
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
        boolean active, @NotNull @Size(max=200) Set<@NotNull Long> productIds, @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {
        public PromotionInput(Long storeId,String name,String code,String kind,BigDecimal percent,BigDecimal maxDiscount,BigDecimal minOrder,int usageLimit,int perUserLimit,Instant startsAt,Instant endsAt,boolean active,Set<Long> productIds,long version) {
            this(storeId,name,code,kind,percent,maxDiscount,minOrder,usageLimit,perUserLimit,startsAt,endsAt,active,productIds,Long.valueOf(version));
        }
    }
    public record DrinkInput(@NotNull Long variantId, @Min(1) @Max(20) int quantity,
        int sugarLevel, int iceLevel, @NotNull @Size(max=50) Set<@NotNull Long> toppingIds) {}
    public record Decision(@NotNull @Pattern(regexp="APPROVED|REJECTED") String decision,
        @NotBlank @Size(max=1000) String note, @PositiveOrZero long version, @Positive Long commissionPolicyId) {}
    public record StoreState(@NotNull @Pattern(regexp="ACTIVE|SUSPENDED") String status, @PositiveOrZero long version) {}
    public record AreaInput(@NotBlank @Size(max=50) String areaCode, @NotBlank @Size(max=150) String name, boolean active,
        @PositiveOrZero @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {
        public AreaInput(String areaCode,String name,boolean active) { this(areaCode,name,active,null); }
    }
    public record Availability(boolean available) {}
    public record OwnerInput(@NotNull @Positive Long userId, @NotNull @PositiveOrZero
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=com.utetra.identity.StrictNumbers.LongValue.class) Long version) {}
    public record Membership(@NotNull @Positive Long userId, boolean active, @Positive Long previousUserId, @Positive Long previousStoreId) {
        public Membership(Long userId,boolean active) {this(userId,active,null,null);}
    }
}
