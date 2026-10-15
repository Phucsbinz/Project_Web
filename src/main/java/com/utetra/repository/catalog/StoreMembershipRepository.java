package com.utetra.repository.catalog;
import com.utetra.entity.catalog.StoreMembership;
import org.springframework.data.jpa.repository.*;
public interface StoreMembershipRepository extends JpaRepository<StoreMembership,Long>, JpaSpecificationExecutor<StoreMembership> { boolean existsByUserIdAndStoreIdAndMemberRoleAndActiveTrue(Long userId,Long storeId,String memberRole); }
