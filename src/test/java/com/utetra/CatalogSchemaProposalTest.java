package com.utetra;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

/** Isolated relational smoke check. Does not establish MySQL/TiDB dialect compatibility. */
class CatalogSchemaProposalTest {
    @Test void catalogOnlyHandoffComposesWithOwnerPrerequisitesWithoutSeedingAccounts() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:catalog_only_handoff;MODE=MySQL","sa","");var stmt=c.createStatement()) {
            // A disposable prerequisite contract, not an implementation of Order's schema.
            stmt.execute("create table commission_policies(id bigint primary key)");
            for(String file:new String[]{"src/main/resources/db/migration/mysql/V1__identity_baseline.sql","docs/features/catalog-sales/proposals/V3__catalog_sales.sql"}) {
                String sql=Files.readString(Path.of(file)).replaceAll("(?m)^--.*$","")
                    .replaceAll("(?i) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
                for(String statement:sql.split(";")) if(!statement.isBlank()) stmt.execute(statement);
            }
            assertThatThrownBy(() -> stmt.execute("insert into promotion_products(promotion_id,product_id) values(999,999)")).isInstanceOf(SQLException.class);
            try(var result=stmt.executeQuery("select count(*) from users")) { result.next(); assertThat(result.getInt(1)).isZero(); }
            try(var result=stmt.executeQuery("select count(*) from information_schema.tables where table_schema='PUBLIC'")) { result.next(); assertThat(result.getInt(1)).isEqualTo(15); }
        }
    }
    @Test void proposalHasConsistentForeignKeysAndChecks() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:h2:mem:catalog_proposal;MODE=MySQL","sa","");var stmt=c.createStatement()) {
            for(String file:new String[]{"src/main/resources/db/migration/mysql/V1__identity_baseline.sql","docs/features/catalog-sales/proposals/catalog-sales.sql"}) {
                String sql=Files.readString(Path.of(file)).replaceAll("(?m)^--.*$","")
                    .replaceAll("(?i) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","")
                    .replaceAll("(?i) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci","");
                for(String statement:sql.split(";")) if(!statement.isBlank()) stmt.execute(statement);
            }
            assertThatThrownBy(() -> stmt.execute("insert into product_toppings(product_id,topping_id) values(999,999)")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> stmt.execute("insert into toppings(name,price,is_active,created_at,updated_at) values('Invalid',-1,true,current_timestamp,current_timestamp)")).isInstanceOf(SQLException.class);
            try(var result=stmt.executeQuery("select count(*) from information_schema.tables where table_schema='PUBLIC'")) { result.next(); assertThat(result.getInt(1)).isEqualTo(17); }
        }
    }
}
