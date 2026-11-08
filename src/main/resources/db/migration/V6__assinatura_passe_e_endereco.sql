alter table pedidos add column passe_mensal boolean not null default false;
alter table passe_recompensa_resgates add column premium_necessario boolean;
update passe_missoes_template set ativa = false
where codigo in ('weekly-spend-500', 'monthly-spend-1200');

create table enderecos_entrega (
  usuario_id bigint primary key references usuarios(id) on delete cascade,
  cep varchar(9) not null,
  endereco varchar(120) not null,
  numero varchar(20) not null,
  complemento varchar(80),
  bairro varchar(80) not null,
  cidade varchar(80) not null,
  estado varchar(2) not null
);
