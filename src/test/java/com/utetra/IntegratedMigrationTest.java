package com.utetra;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class IntegratedMigrationTest {
 @Test void allModuleMigrationsComposeWithoutDuplicateTablesOrBrokenForeignKeys() throws Exception {
  try(var c=DriverManager.getConnection("jdbc:h2:mem:integrated_migrations;MODE=MySQL","sa","");var stmt=c.createStatement()) {
   var files=Files.list(Path.of("src/main/resources/db/migration/mysql")).filter(p->p.toString().endsWith(".sql")).sorted(Comparator.comparingLong(p->Long.parseLong(p.getFileName().toString().split("__")[0].substring(1)))).toList();
   for(var file:files) {
    String sql=Files.readString(file).replaceAll("(?m)^--.*$","").replaceAll("(?i) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","").replaceAll("(?i) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci","");
    for(String statement:sql.split(";"))if(!statement.isBlank())stmt.execute(statement);
   }
   for(String table:List.of("CATEGORIES","TOPPINGS","STORE_DELIVERY_AREAS")) {
    try(var columns=c.getMetaData().getColumns(null,"PUBLIC",table,"VERSION")){assertThat(columns.next()).as(table+" version column").isTrue();assertThat(columns.getInt("NULLABLE")).isEqualTo(DatabaseMetaData.columnNoNulls);}
   }
   try(var columns=c.getMetaData().getColumns(null,"PUBLIC","USERS","PASSWORD_PLAINTEXT")){assertThat(columns.next()).isFalse();}
   assertThatThrownBy(()->stmt.execute("insert into product_favorites(user_id,product_id,created_at) values(999,999,current_timestamp)")).isInstanceOf(SQLException.class);
   try(var r=stmt.executeQuery("select count(*) from users")){r.next();assertThat(r.getLong(1)).isZero();}
  }
 }
}
