package db.migration;
import org.flywaydb.core.api.migration.*;
public class V14__Gestao_missoes extends BaseJavaMigration {
 public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){
  s.execute("alter table passe_missoes_template add column produto_id bigint references produtos(id), add column categoria_id bigint references categorias(id), add column evento_id bigint references eventos_semanais(id), add column inicio_em date, add column fim_em date, add column especial boolean not null default false");
  s.execute("create table passe_missao_acoes(id bigserial primary key,usuario_id bigint not null,template_id bigint not null,periodo_inicio timestamp not null,referencia varchar(180) not null,unique(usuario_id,template_id,periodo_inicio,referencia))");
 }}
}
