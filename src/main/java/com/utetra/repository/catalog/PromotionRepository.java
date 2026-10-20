package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Promotion;
import org.springframework.data.jpa.repository.*;
public interface PromotionRepository extends JpaRepository<Promotion,Long>, JpaSpecificationExecutor<Promotion> {  }
