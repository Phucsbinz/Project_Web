package com.utetra.repository.catalog;
import com.utetra.entity.catalog.ProductTopping;
import org.springframework.data.jpa.repository.*;
public interface ProductToppingRepository extends JpaRepository<ProductTopping,Long>, JpaSpecificationExecutor<ProductTopping> { java.util.List<ProductTopping> findByProductId(Long productId); void deleteByProductId(Long productId); }
