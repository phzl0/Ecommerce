package db.migration;
import org.flywaydb.core.api.migration.*;
public class V13__Editor_temporadas extends BaseJavaMigration {
 public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){
  s.execute("alter table passe_temporadas add column estado varchar(20) not null default 'PUBLICADO', add column gerenciada boolean not null default false, add column preco numeric(10,2), add column previsao_gratuitos integer not null default 0, add column previsao_premium integer not null default 0, add column publicado_por varchar(120), add column publicado_em timestamp");
  s.execute("create unique index if not exists uq_passe_competencia on passe_temporadas(ano,mes)");
  s.execute("alter table recompensa_opcoes add column custo numeric(10,2) not null default 0, add column frete numeric(10,2) not null default 0");
 }}
}
