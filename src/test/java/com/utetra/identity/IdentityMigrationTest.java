package com.utetra.identity;
import static org.assertj.core.api.Assertions.*;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
class IdentityMigrationTest {
    @Test void actualMigrationCreatesOwnedSchemaAndRejectsDuplicateFavorites() throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:identity_migration;MODE=MySQL","sa",""); var sql=connection.createStatement()) {
            sql.execute("create table users(id bigint primary key)"); sql.execute("insert into users(id) values(1)");
            ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migration/mysql/V2026101701__identity_membership.sql"));
            ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migration/mysql/V2026102001__identity_validation_guards.sql"));
            sql.execute("insert into product_favorites(user_id,product_id,created_at) values(1,10,current_timestamp)");
            assertThatThrownBy(() -> sql.execute("insert into product_favorites(user_id,product_id,created_at) values(1,10,current_timestamp)")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("insert into loyalty_accounts(user_id,balance,debt,lifetime_earned,updated_at) values(1,-1,0,0,current_timestamp)")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("insert into product_views(user_id,product_id,last_viewed_at,view_count) values(1,10,current_timestamp,0)")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sql.execute("insert into product_comments(author_id,product_id,order_item_id,content,status,created_at,updated_at) values(1,10,1,'too short','VISIBLE',current_timestamp,current_timestamp)")).isInstanceOf(java.sql.SQLException.class);
        }
    }
}
