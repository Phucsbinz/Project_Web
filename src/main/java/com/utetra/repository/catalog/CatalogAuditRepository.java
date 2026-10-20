package com.utetra.repository.catalog;
import com.utetra.entity.catalog.CatalogAudit;
import org.springframework.data.jpa.repository.*;
public interface CatalogAuditRepository extends JpaRepository<CatalogAudit,Long>, JpaSpecificationExecutor<CatalogAudit> {  }
