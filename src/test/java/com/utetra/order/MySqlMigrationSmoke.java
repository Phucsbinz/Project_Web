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
    if (!url.matches("jdbc:mysql://(127\\.0\\.0\\.1|localhost):[0-9]+/utetra_order_[a-z0-9_]+(?:\\?.*)?"))
      throw new IllegalArgumentException("Use a disposable localhost utetra_order_* database only");
    DriverManagerDataSource ds = new DriverManagerDataSource(url, "root", password);
    Flyway flyway =
        Flyway.configure().dataSource(ds).locations("classpath:db/migration/mysql").load();
    int expected = flyway.info().pending().length;
    if (expected == 0 || flyway.info().applied().length != 0)
      throw new IllegalStateException("Expected a fresh disposable database");
    int applied = flyway.migrate().migrationsExecuted;
    if (applied != expected) throw new IllegalStateException("Expected all integrated migrations");
    if (flyway.migrate().migrationsExecuted != 0)
      throw new IllegalStateException("Migration replay changed the schema");
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(ds);
    factory.setPackagesToScan("com.utetra.entity", "com.utetra.order.entity", "com.utetra.identity.entity");
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
    System.out.println("MYSQL_FLYWAY_ALL_MIGRATIONS_REPLAY_AND_JPA_VALIDATE_PASSED_WITH_TLS");
  }
}
