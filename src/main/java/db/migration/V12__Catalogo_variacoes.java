package db.migration;
import org.flywaydb.core.api.migration.*;
public class V12__Catalogo_variacoes extends BaseJavaMigration {
 public void migrate(Context c)throws Exception{try(var s=c.getConnection().createStatement()){
  s.execute("alter table produtos add column publicacao varchar(20) not null default 'PUBLICADO'");
  s.execute("update produtos set publicacao='DESATIVADO' where ativo=false");
  s.execute("alter table produtos add column produto_principal_id bigint references produtos(id)");
  s.execute("alter table produtos add constraint ck_produto_familia check(produto_principal_id is null or produto_principal_id<>id)");
  s.execute("alter table produtos add column tamanho varchar(40), add column cor varchar(40), add column modelo varchar(80), add column galeria text");
  s.execute("alter table produtos add column peso_kg numeric(10,3), add column comprimento_cm numeric(10,2), add column largura_cm numeric(10,2), add column altura_cm numeric(10,2)");
  s.execute("alter table produtos add column xp_extra integer not null default 0 check(xp_extra between 0 and 10000), add column xp_extra_inicio timestamp, add column xp_extra_fim timestamp");
  s.execute("create unique index uq_produto_opcao on produtos(coalesce(produto_principal_id,id),lower(coalesce(tamanho,'')),lower(coalesce(cor,'')),lower(coalesce(modelo,'')))");
  s.execute("create index ix_produto_familia on produtos(produto_principal_id)");
  s.execute("alter table itens_pedido add column nome_snapshot varchar(500), add column sku_snapshot varchar(40), add column imagem_snapshot varchar(255), add column xp_extra_snapshot integer not null default 0");
  s.execute("update itens_pedido i set nome_snapshot=p.nome,sku_snapshot=p.sku,imagem_snapshot=p.imagem_url from produtos p where p.id=i.produto_id");
 }}
}
