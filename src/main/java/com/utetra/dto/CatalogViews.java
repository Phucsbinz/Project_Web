package com.utetra.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class CatalogViews {
    private CatalogViews() {}
    public record CategoryView(Long id,String name,String slug,String description,int displayOrder,boolean active,String imageUrl) {}
    public record ToppingView(Long id,String name,BigDecimal price,boolean active) {}
    public record AreaView(Long id,String areaCode,String name,boolean active) {}
    public record DeliveryRateView(Long areaId,String areaName,Long carrierId,String carrierName,BigDecimal fee) {}
    public record CommissionPolicyView(Long id,String name,BigDecimal ratePercent) {}
    public record RegistrationView(StoreView store,String status,String note,Instant createdAt,Instant reviewedAt) {}
    public record StoreView(Long id,Long ownerUserId,String name,String slug,String address,String phone,String description,
        String imageUrl,String bannerUrl,LocalTime openTime,LocalTime closeTime,String status,boolean acceptingOrders,
        long version,List<AreaView> areas) {}
    public record VariantView(Long id,String sizeName,BigDecimal extraPrice,boolean active,Integer stockQuantity,long version) {}
    public record ProductView(Long id,Long storeId,Long categoryId,String name,String slug,String description,
        BigDecimal basePrice,String imageUrl,boolean active,boolean available,boolean featured,long version,
        Instant createdAt,List<VariantView> variants,List<ToppingView> toppings,Set<Long> toppingIds,long soldUnits,BigDecimal rating,long favorites,String catalogKey) {}
    public record PromotionView(Long id,Long storeId,String scope,String kind,String code,String name,
        BigDecimal percent,BigDecimal maxDiscount,BigDecimal minOrder,int usageLimit,int perUserLimit,
        Instant startsAt,Instant endsAt,boolean active,String fundedBy,long version,Set<Long> productIds) {}
    public record PageView<T>(List<T> items,int page,int size,long total,long pages) {}
    public record DrinkQuote(Long productId,Long storeId,Long variantId,long productVersion,long storeVersion,
        BigDecimal drinkUnitPrice,BigDecimal toppingUnitTotal,int quantity,BigDecimal total) {}
    public record PromotionQuote(Long promotionId,String kind,BigDecimal eligibleAmount,BigDecimal discount,long version) {}
}
