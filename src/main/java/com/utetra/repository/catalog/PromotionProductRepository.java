package com.utetra.repository.catalog;
import com.utetra.entity.catalog.PromotionProduct;
import org.springframework.data.jpa.repository.*;
public interface PromotionProductRepository extends JpaRepository<PromotionProduct,Long>, JpaSpecificationExecutor<PromotionProduct> { java.util.List<PromotionProduct> findByPromotionId(Long promotionId); void deleteByPromotionId(Long promotionId); }
