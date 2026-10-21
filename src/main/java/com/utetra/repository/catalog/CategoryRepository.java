package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Category;
import org.springframework.data.jpa.repository.*;
public interface CategoryRepository extends JpaRepository<Category,Long>, JpaSpecificationExecutor<Category> {  }
