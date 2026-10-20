package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Topping;
import org.springframework.data.jpa.repository.*;
public interface ToppingRepository extends JpaRepository<Topping,Long>, JpaSpecificationExecutor<Topping> {  }
