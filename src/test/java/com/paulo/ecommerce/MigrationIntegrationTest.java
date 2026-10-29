package com.paulo.ecommerce;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = "jdbc:postgresql:.*")
class MigrationIntegrationTest {
  @Test
  void freshSchemaAndExistingRowsSurviveUpgrade() throws Exception {
    String url = System.getenv("TEST_DB_URL"),
        user = System.getenv("TEST_DB_USER"),
        password = System.getenv("TEST_DB_PASSWORD");
    if (!url.endsWith("_test"))
      throw new IllegalStateException("Disposable test database required.");
    String schema = "migration_" + java.util.UUID.randomUUID().toString().replace("-", "");
    try (var conn = DriverManager.getConnection(url, user, password);
        var sql = conn.createStatement()) {
      var flyway =
          Flyway.configure()
              .dataSource(url, user, password)
              .schemas(schema)
              .defaultSchema(schema)
              .baselineOnMigrate(true)
              .baselineVersion("1")
              .load();
      flyway.migrate();
      sql.execute("set search_path to " + schema);
      sql.execute(
          "insert into usuarios(nome,email,senha,role) values"
              + " ('Preservado','preserved@example.test','test-password','CLIENTE')");
      sql.execute("drop table flyway_schema_history");
      sql.execute("drop table passe_eventos");
      sql.execute("alter table pagamentos drop column checkout_url");
      flyway.migrate();
      try (var result =
          sql.executeQuery("select count(*) from usuarios where email='preserved@example.test'")) {
        assertTrue(result.next());
        assertEquals(1, result.getInt(1));
      }
      try (var result = sql.executeQuery("select checkout_url from pagamentos limit 0")) {
        assertNotNull(result);
      }
    } finally {
      try (var conn = DriverManager.getConnection(url, user, password);
          var sql = conn.createStatement()) {
        sql.execute("drop schema if exists " + schema + " cascade");
      }
    }
  }
}
