package com.utetra.repository.catalog;
import com.utetra.entity.catalog.Store;
import org.springframework.data.jpa.repository.*;
public interface StoreRepository extends JpaRepository<Store,Long>, JpaSpecificationExecutor<Store> { java.util.Optional<Store> findBySlug(String slug);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id=:id")
    java.util.Optional<Store> locked(@org.springframework.data.repository.query.Param("id") Long id); }
