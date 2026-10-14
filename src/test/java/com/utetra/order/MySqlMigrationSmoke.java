package com.utetra.order;

import java.sql.Connection;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Explicit optional MySQL test; receives disposable DB credentials through the process environment.
 */
public final class MySqlMigrationSmoke {
  public static void main(String[] args) throws Exception {
    String url = System.getenv("ORDER_MYSQL_TEST_URL"),
        password = System.getenv("ORDER_MYSQL_TEST_PASSWORD");
    if (url == null || password == null)
      throw new IllegalArgumentException("Provide disposable MySQL test bindings");
    DriverManagerDataSource ds = new DriverManagerDataSource(url, "root", password);
    Flyway flyway =
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load();
    int applied = flyway.migrate().migrationsExecuted;
    if (applied != 2) throw new IllegalStateException("Expected both V1 and V2 on a fresh test DB");
    if (flyway.migrate().migrationsExecuted != 0)
      throw new IllegalStateException("Migration replay changed the schema");
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(ds);
    factory.setPackagesToScan("com.utetra.entity", "com.utetra.order.entity");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of("hibernate.hbm2ddl.auto", "validate", "hibernate.jdbc.time_zone", "UTC"));
    factory.afterPropertiesSet();
    try (Connection connection = ds.getConnection();
        var statement = connection.createStatement();
        var tls = statement.executeQuery("SHOW SESSION STATUS LIKE 'Ssl_cipher'")) {
      if (!tls.next() || tls.getString(2).isBlank())
        throw new IllegalStateException("Expected a verified TLS MySQL connection");
    } finally {
      factory.destroy();
    }
    System.out.println("MYSQL_FLYWAY_TWO_MIGRATIONS_REPLAY_AND_JPA_VALIDATE_PASSED_WITH_TLS");
  }
}
