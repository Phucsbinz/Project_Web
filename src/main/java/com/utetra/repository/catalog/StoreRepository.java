package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Store;
import org.springframework.data.jpa.repository.*;
public interface StoreRepository extends JpaRepository<Store,Long>, JpaSpecificationExecutor<Store> { java.util.Optional<Store> findBySlug(String slug); }
