# Checkout e gamificação — beta

## O que funciona nesta entrega

- Catálogo, cadastro/login, sacola local, checkout com endereço, pedidos e administração.
- Totais recalculados com preços do banco; frete fixo por faixa e desconto de cupom.
- Reserva de estoque por 24 horas, cancelamento de pedido pendente e expiração automática.
- Reenvio do mesmo checkout retorna o pedido existente, sem descontar estoque novamente. A chave é preservada no navegador para repetir a mesma solicitação após recarregar.
- Checkout Pro do Mercado Pago com frete/desconto, URL de retorno, assinatura HMAC das notificações, consulta autenticada do pagamento e conferência de valor, moeda, conta recebedora e ambiente.
- Tentativas repetidas reutilizam a preferência enquanto o pedido está válido. Eventos aprovados repetidos não concedem XP novamente nem reconfirmam o pedido.
- Histórico de XP, missões, produtos distintos, resgate por temporada e coleção privada.
- Premium comprado por temporada, sem renovação automática. O pagamento do premium não concede XP de compra nem entra no gasto mínimo de produtos.
- Cupons pessoais de uso único, avatar/badge em SVG, wallpaper e tema aplicável à conta. Não há premiação física automática nesta versão.

## Demonstração e pagamento real são configurações diferentes

O padrão é **APP_PAYMENTS_ENABLED=false**. O checkout avisa que nenhuma cobrança será feita e salva o pedido pendente para testar a jornada. Não há botão público para fingir pagamento aprovado. O painel de administração também não deve marcar um pedido como pago sem comprovação do provedor.

Para testes externos do Checkout Pro, configure credenciais da aplicação de teste no ambiente do backend:

| Variável | Finalidade |
| --- | --- |
| `APP_PAYMENTS_ENABLED` | `true` para habilitar a integração |
| `APP_PAYMENTS_LIVE_MODE` | `false` para testes; produção exige ativação deliberada |
| `MERCADOPAGO_ACCESS_TOKEN` | Token privado do ambiente correspondente |
| `APP_PAYMENTS_COLLECTOR_ID` | ID numérico da conta recebedora desse ambiente |
| `APP_PAYMENTS_WEBHOOK_SECRET` | Segredo da configuração de Webhooks |
| `APP_URL` | Origem HTTPS pública da aplicação para notificações e retornos |

Cadastre `/pagamento/webhook` como URL de notificações. O provedor não acessa `localhost`: os testes externos exigem uma origem HTTPS alcançável. Não use dados de cartão reais nos testes. Nenhuma credencial foi adicionada ao repositório e nenhum pagamento real foi executado nesta entrega.

A assinatura usa `x-signature`, `x-request-id` e `data.id`, com janela de cinco minutos para o timestamp. Notificações inválidas retornam 401. Falhas no processamento retornam 503 para permitir nova tentativa, em vez de confirmar um processamento que não terminou.

Referência oficial: https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-preferences/payment-notifications .

## Estados, estoque e conciliação

Pedido pendente pode ser cancelado pelo titular. O estoque é protegido com bloqueio de escrita e liberado uma vez no cancelamento/expiração. Confirmação exige pagamento aprovado; envio exige confirmação; entrega exige envio.

A reserva e a preferência expiram 24 horas após a criação do pedido. Uma aprovação recebida depois do cancelamento/expiração exige conciliação: o sistema não reabre o pedido automaticamente nem promete estoque que já foi liberado. No boleto, a aprovação pode ocorrer após essa janela; revise essa política antes de oferecer o meio em uma operação comercial.

Reembolso integral/contestação confirmado pelo provedor cancela o pedido e recalcula o XP e as missões de compra afetadas. O estoque só retorna automaticamente se o pedido ainda estava pendente ou confirmado; mercadoria já enviada/entregue depende de conferência física. Atualizações pendentes ou rejeitadas não rebaixam um pagamento aprovado.

Em **Administração → Pedidos → Conciliar pagamento**, o administrador pode informar o ID obtido no Mercado Pago. A aplicação consulta o provedor e executa as mesmas conferências; não é uma aprovação financeira manual.

Pagamentos duplicados, reembolso parcial, aprovações tardias, estorno de benefício já consumido e devolução de mercadoria enviada precisam de tratamento operacional no painel do provedor. Esta beta não dispara estornos financeiros automaticamente. Arquivos digitais já baixados não podem ser recolhidos.

## Regras do passe

O passe existente tem 30 níveis de 100 XP. As missões atuais incluem check-in, produtos distintos, sacola, avaliações e compras aprovadas. Compras e gastos só são registrados pelo backend após confirmação financeira. Avaliação exige compra aprovada do produto; editar a mesma avaliação não gera novo prêmio.

O fuso padrão de negócio é `America/Sao_Paulo`, configurável por `APP_TIMEZONE`. Mudanças de fuso em um banco existente exigem avaliação dos registros históricos.

| Variável | Padrão | Efeito |
| --- | --- | --- |
| `APP_PASS_PREMIUM_PRICE` | `197.00` | Preço dos novos pedidos premium |
| `APP_PASS_TOTAL_LEVELS` | `30` | Quantidade de níveis para novas temporadas; permite configurar 50 |
| `APP_PASS_XP_PER_LEVEL` | `100` | XP por nível para novas temporadas |
| `APP_PASS_REQUIRED_SPEND` | `1200.00` | Gasto líquido mínimo em produtos aprovados para o resgate final |
| `APP_PASS_COUPON_VALUE` | `10.00` | Desconto de cupom validado no checkout |
| `APP_PASS_COUPON_MINIMUM` | `100.00` | Subtotal mínimo de produtos para usar cupom |

Temporadas existentes mantêm quantidade de níveis e XP já definidos. O preço fica registrado no pedido premium. Valor e mínimo de compra de cada cupom são registrados no resgate: alterar os parâmetros afeta novas emissões, preservando cupons já emitidos. A beta ainda não oferece um editor administrativo dessas regras.

A trilha gratuita libera níveis selecionados; os outros exigem premium aprovado da temporada. O resgate final também exige o gasto mínimo informado na tela. Frete, descontos e compra do próprio premium não aumentam esse gasto.

Cada recompensa é resgatada uma vez por usuário. Códigos pessoais são emitidos no resgate, não expostos antes do desbloqueio. Cupons têm validade até 30 dias após o fim da temporada, pertencem à conta e ficam reservados ao pedido que os usa. Cancelar esse pedido permite nova utilização, se o benefício continuar válido.

A coleção permite aplicar tema/avatar/badge e baixar os arquivos disponíveis. O saldo de XP e a elegibilidade são reavaliados após reembolso. Não há sorteio, entrega de cesta, frete de prêmio ou promessa de recompensa física implementada nesta entrega.

## Banco, arquivos e atualização

Flyway cria o banco novo e aplica as atualizações versionadas. Em banco anterior sem histórico Flyway, registra a versão-base 1 e aplica migrações aditivas. Isso pressupõe o esquema do projeto anterior compatível: `validate` interrompe a inicialização se a estrutura real divergir.

Faça backup antes de atualizar uma instalação existente. As migrações não removem usuários, pedidos, XP ou resgates; não houve acesso ao banco publicado. O teste de migração cria um schema isolado e verifica preservação de registros.

Imagens novas ficam em `APP_STORAGE_DIR/products`, com nome aleatório, validação JPG/PNG, limite de 5 MB e 20 megapixels. O Compose persiste essa pasta em volume. Em Render com disco efêmero, configure armazenamento persistente ou mantenha `APP_UPLOADS_ENABLED=false`; o Compose local não transforma o disco do Render em persistente.

## Verificação

O workflow executa compilação e testes em Java 17, H2, PostgreSQL 17, migração nova/atualização e navegador Chromium. A jornada de navegador usa banco descartável e pagamentos desativados. O teste de conciliação usa resposta simulada do provedor; aprovação real, credenciais, Webhook público e cookies do domínio hospedado precisam de validação de integração antes da ativação comercial.
