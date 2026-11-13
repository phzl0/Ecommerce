package com.paulo.ecommerce;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
      var migrations =
          Flyway.configure()
              .dataSource(url, user, password)
              .schemas(schema)
              .defaultSchema(schema)
              .baselineOnMigrate(true)
              .baselineVersion("1");
      migrations.target("4").load().migrate();
      sql.execute("set search_path to " + schema);
      sql.execute(
          "insert into usuarios(nome,email,senha,role) values"
              + " ('Preservado','preserved@example.test','test-password','CLIENTE')");
      sql.execute("drop table flyway_schema_history");
      sql.execute("drop table passe_eventos");
      sql.execute("alter table pagamentos drop column checkout_url");
      migrations.target("8").load().migrate();
      sql.execute("insert into produtos(id,nome,preco,estoque,ativo,destaque,tipo) values (90001,'Preservado',10,7,true,false,'FISICO')");
      sql.execute("insert into pedidos(id,usuario_id,numero,total,subtotal,frete,desconto,data_pedido,status,passe_mensal,recompensa) select 90001,id,'RESERVA-MIGRACAO',20,20,0,0,current_timestamp,'PENDENTE',false,false from usuarios where email='preserved@example.test'");
      sql.execute("insert into itens_pedido(pedido_id,produto_id,quantidade,preco_unitario) values (90001,90001,2,10)");
      sql.execute("insert into bau_itens(usuario_id,origem,titulo,produto_id,criado_em) select id,'TESTE','Premio',90001,current_timestamp from usuarios where email='preserved@example.test'");
      migrations.target("9").load().migrate();
      sql.execute("update pedidos set status='ENVIADO' where id=90001");
      migrations.target(MigrationVersion.LATEST).load().migrate();
      try(var row=sql.executeQuery("select etapa,codigo,enviado_em from pedido_expedicoes where pedido_id=90001")) {
        assertTrue(row.next());assertEquals("ENVIADO",row.getString(1));
        assertNull(row.getString(2));assertNull(row.getTimestamp(3));
      }
      try(var row=sql.executeQuery("select estoque from produtos where id=90001")) {
        assertTrue(row.next());assertEquals(7,row.getInt(1));
      }
      try(var row=sql.executeQuery("select sum(quantidade) from estoque_reservas where produto_id=90001 and estado='RESERVADO'")) {
        assertTrue(row.next());assertEquals(3,row.getInt(1));
      }
      try (var result =
          sql.executeQuery("select count(*) from usuarios where email='preserved@example.test'")) {
        assertTrue(result.next());
        assertEquals(1, result.getInt(1));
      }
      assertTrue(sql.execute("select checkout_url from pagamentos limit 0"));
      assertTrue(sql.execute("select id from eventos_semanais limit 0"));
      assertTrue(sql.execute("select id from notificacoes limit 0"));
      assertTrue(sql.execute("select usuario_id from recuperacao_senha limit 0"));
    } finally {
      try (var conn = DriverManager.getConnection(url, user, password);
          var sql = conn.createStatement()) {
        sql.execute("drop schema if exists " + schema + " cascade");
      }
    }
  }
}
