package com.utetra.repository.catalog;
import com.utetra.entity.catalog.StoreMembership;
import org.springframework.data.jpa.repository.*;
public interface StoreMembershipRepository extends JpaRepository<StoreMembership,Long>, JpaSpecificationExecutor<StoreMembership> {
    boolean existsByUserIdAndStoreIdAndMemberRoleAndActiveTrue(Long userId,Long storeId,String memberRole);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from StoreMembership m where m.userId=:userId and m.storeId=:storeId")
    java.util.Optional<StoreMembership> assignment(@org.springframework.data.repository.query.Param("userId") Long userId,@org.springframework.data.repository.query.Param("storeId") Long storeId);
}
