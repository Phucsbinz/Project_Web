package com.utetra.repository.catalog;
import com.utetra.entity.catalog.StoreTopping;
import org.springframework.data.jpa.repository.*;
public interface StoreToppingRepository extends JpaRepository<StoreTopping,Long>, JpaSpecificationExecutor<StoreTopping> { java.util.List<StoreTopping> findByStoreId(Long storeId); java.util.Optional<StoreTopping> findByStoreIdAndToppingId(Long storeId,Long toppingId); }
