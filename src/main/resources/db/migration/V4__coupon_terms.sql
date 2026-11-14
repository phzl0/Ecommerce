alter table passe_recompensa_resgates add column if not exists valor_cupom numeric(10,2);
alter table passe_recompensa_resgates add column if not exists minimo_cupom numeric(10,2);
-- Previously issued beta coupons used these fixed conditions.
update passe_recompensa_resgates set valor_cupom=10.00, minimo_cupom=100.00
where valor_cupom is null and codigo_entrega is not null and recompensa_id in (select id from passe_recompensas_nivel where tipo_digital='Cupom');
