package com.utetra.repository.catalog;
import com.utetra.entity.catalog.ProductVariant;
import org.springframework.data.jpa.repository.*;
public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long>, JpaSpecificationExecutor<ProductVariant> { java.util.List<ProductVariant> findByProductId(Long productId); }
