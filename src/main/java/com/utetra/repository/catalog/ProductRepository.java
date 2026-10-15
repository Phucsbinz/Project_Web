package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Product;
import org.springframework.data.jpa.repository.*;
public interface ProductRepository extends JpaRepository<Product,Long>, JpaSpecificationExecutor<Product> {  }
