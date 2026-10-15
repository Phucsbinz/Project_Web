package com.utetra.repository.catalog;
import com.utetra.entity.catalog.StoreDeliveryArea;
import org.springframework.data.jpa.repository.*;
public interface StoreDeliveryAreaRepository extends JpaRepository<StoreDeliveryArea,Long>, JpaSpecificationExecutor<StoreDeliveryArea> { java.util.List<StoreDeliveryArea> findByStoreId(Long storeId); }
