package com.utetra.repository.catalog;
import com.utetra.entity.catalog.StoreRegistrationRequest;
import org.springframework.data.jpa.repository.*;
public interface StoreRegistrationRequestRepository extends JpaRepository<StoreRegistrationRequest,Long>, JpaSpecificationExecutor<StoreRegistrationRequest> {
    java.util.Optional<StoreRegistrationRequest> findFirstByStoreIdOrderByIdDesc(Long storeId);
    java.util.List<StoreRegistrationRequest> findByApplicantIdOrderByIdDesc(Long applicantId);
}
