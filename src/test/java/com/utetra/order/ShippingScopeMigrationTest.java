package com.utetra.order;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Executes the production SQL against disposable H2 MySQL-mode legacy table shapes. */
class ShippingScopeMigrationTest {
  private DriverManagerDataSource source;
  private JdbcTemplate jdbc;

  @BeforeEach
  void legacySchema() {
    source = new DriverManagerDataSource(
        "jdbc:h2:mem:utetra_shipping_migration_" + UUID.randomUUID()
            + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    jdbc = new JdbcTemplate(source);
    jdbc.execute("create table users(id bigint primary key,role_id bigint not null,is_active boolean not null)");
    jdbc.execute("create table stores(id bigint primary key,owner_user_id bigint not null,status varchar(20) not null)");
    jdbc.execute("create table delivery_assignments(id bigint primary key,order_id bigint not null unique,shipper_id bigint not null,assigned_by bigint not null,status varchar(20) not null,assigned_at datetime(6) not null)");
  }

  @AfterEach
  void closeDisposableDatabase() {
    jdbc.execute("shutdown");
  }

  private void migrate() {
    new ResourceDatabasePopulator(new ClassPathResource(
        "db/migration/mysql/V2026102901__shipper_store_scopes.sql")).execute(source);
  }

  private void legacyAssignment() {
    jdbc.update("insert into users values(4,4,true),(5,5,true),(6,5,false)");
    jdbc.update("insert into stores values(10,4,'ACTIVE')");
    jdbc.update("insert into delivery_assignments values(70,100,5,4,'ACTIVE',?)",
        Timestamp.valueOf("2026-10-14 03:00:00.123456"));
  }

  @Test
  void freshSchemaMigratesWithoutCreatingRightsOrAssignments() {
    migrate();
    for (String table : new String[] {"users", "stores", "delivery_assignments",
        "shipper_store_scopes", "shipper_scope_events", "delivery_assignment_changes"}) {
      assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isZero();
    }
  }

  @Test
  void legacyAssignmentGetsExactlyOneHistoricalSnapshotAndNoScopeGrant() {
    legacyAssignment();
    var users = jdbc.queryForList("select * from users order by id");
    var stores = jdbc.queryForList("select * from stores order by id");
    var assignments = jdbc.queryForList("select * from delivery_assignments order by id");
    migrate();
    assertThat(jdbc.queryForList("select * from users order by id")).isEqualTo(users);
    assertThat(jdbc.queryForList("select * from stores order by id")).isEqualTo(stores);
    assertThat(jdbc.queryForList("select * from delivery_assignments order by id")).isEqualTo(assignments);
    assertThat(jdbc.queryForObject("select count(*) from shipper_store_scopes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from shipper_scope_events", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from delivery_assignment_changes", Long.class)).isEqualTo(1);
    var history = jdbc.queryForMap("select * from delivery_assignment_changes");
    assertThat(((Number) history.get("assignment_id")).longValue()).isEqualTo(70);
    assertThat(history.get("previous_shipper_id")).isNull();
    assertThat(((Number) history.get("shipper_id")).longValue()).isEqualTo(5);
    assertThat(((Number) history.get("actor_id")).longValue()).isEqualTo(4);
    assertThat(history.get("reason")).isEqualTo("Existing assignment at migration");
    assertThat(((Timestamp) history.get("created_at")).toLocalDateTime())
        .isEqualTo(LocalDateTime.parse("2026-10-14T03:00:00.123456"));
  }

  @Test
  void scopeUniquenessAndAuditForeignKeysRejectDuplicatesAndOrphans() {
    legacyAssignment();
    migrate();
    jdbc.update("insert into shipper_store_scopes(shipper_id,store_id,is_active,updated_at) values(5,10,true,current_timestamp)");
    assertThatThrownBy(() -> jdbc.update("insert into shipper_store_scopes(shipper_id,store_id,is_active,updated_at) values(5,10,false,current_timestamp)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    for (String values : new String[] {"999,10", "5,999"}) {
      assertThatThrownBy(() -> jdbc.update("insert into shipper_store_scopes(shipper_id,store_id,is_active,updated_at) values(" + values + ",true,current_timestamp)"))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    for (String values : new String[] {"999,10,4", "5,999,4", "5,10,999"}) {
      assertThatThrownBy(() -> jdbc.update("insert into shipper_scope_events(shipper_id,store_id,actor_id,is_active,reason,created_at) values(" + values + ",false,'Revoke',current_timestamp)"))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    for (String values : new String[] {"999,null,5,4", "70,999,5,4", "70,null,999,4", "70,null,5,999"}) {
      assertThatThrownBy(() -> jdbc.update("insert into delivery_assignment_changes(assignment_id,previous_shipper_id,shipper_id,actor_id,reason,created_at) values(" + values + ",'Change',current_timestamp)"))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    assertThat(jdbc.queryForObject("select count(*) from shipper_store_scopes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from shipper_scope_events", Long.class)).isZero();
    assertThat(jdbc.queryForObject("select count(*) from delivery_assignment_changes", Long.class)).isEqualTo(1);
  }
}
