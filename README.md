# LEVEL COMMERCE — Ecommerce Beta

Loja com catálogo, checkout, pedidos e gamificação por passe mensal, missões, XP e recompensas. **Projeto beta: pagamentos vêm desativados e esta entrega não habilita cobranças reais.**

## Rodar localmente

Requisitos: Git, Docker com Compose e Python 3 para a primeira configuração. Java 17 e PostgreSQL 17 rodam nos containers.

### Linux

Na primeira instalação, baixe a branch desta entrega, configure o administrador e inicie:

```bash
git clone --branch feat/checkout-gamification https://github.com/phzl0/Ecommerce.git
cd Ecommerce
python3 scripts/init_env.py
sudo docker compose up --build -d
sudo docker compose logs -f app
```

Aguarde a inicialização e use `Ctrl+C` para sair dos logs. Abra **http://localhost:8080**. Entre com a conta criada no script; administração em **http://localhost:8080/admin**. Se seu usuário já pode acessar o Docker, omita `sudo`.

### Windows — PowerShell

Instale Git, Python 3 e Docker Desktop com containers Linux/WSL 2. Abra o Docker Desktop e aguarde o mecanismo iniciar.

```powershell
git clone --branch feat/checkout-gamification https://github.com/phzl0/Ecommerce.git
cd Ecommerce
py -3 scripts/init_env.py
docker compose up --build -d
docker compose logs -f app
```

Se o Python estiver disponível apenas como `python`, use esse comando no lugar de `py -3`. Abra **http://localhost:8080**. `Ctrl+C` sai dos logs sem desligar a loja.

O script cria `.env` com senha aleatória para o banco e solicita a conta administrativa. Ele não sobrescreve uma configuração existente. Produtos demonstrativos são criados somente quando `DEMO_DATA_ENABLED=true`; desative essa opção ao preparar seu próprio catálogo.

### Atualizar, parar e preservar dados

Na pasta do projeto e na branch da entrega:

```bash
git pull --ff-only
docker compose up --build -d
```

Use `sudo` no Linux se necessário. Para parar preservando os dados:

```bash
docker compose down
```

**Não use `down -v` para atualizar:** isso remove os volumes. `postgres_data` guarda o banco e `product_storage` guarda as imagens novas. Preserve também o `.env`.

Para uma instalação anterior, faça backup do PostgreSQL e dos arquivos antes da atualização. As migrações Flyway são aditivas; a primeira migração em um banco existente reconhece a base anterior e valida sua compatibilidade. Não copie credenciais do servidor publicado para uma demonstração local.

## Sobre o projeto

A LEVEL COMMERCE explora uma experiência de e-commerce gamificada: comprar e participar de missões pode gerar progresso em uma temporada, com níveis e benefícios. O objetivo é reunir a operação básica de loja com engajamento e recompensas verificáveis.

O backend é um monólito modular em **Java 17 e Spring Boot**, com **PostgreSQL, JPA e Flyway**. A vitrine usa **React/JavaScript**; o painel administrativo mantém **Thymeleaf**. Não há exigência de IA para operar os recursos centrais.

## Funcionalidades da beta

| Área | Recursos |
| --- | --- |
| Loja | Catálogo, busca/categoria, detalhes, sacola e endereço de entrega |
| Conta | Cadastro, login, perfil e pedidos privados |
| Pedidos | Preços recalculados no banco, frete por faixa, estoque reservado e cancelamento |
| Pagamento | Integração Checkout Pro, retorno, Webhook validado e conciliação administrativa |
| Passe | Temporadas, trilha gratuita, premium por temporada e histórico de XP |
| Missões | Check-in, produtos distintos, sacola, avaliações e compras aprovadas |
| Benefícios | Cupom pessoal de uso único, avatar/badge, wallpaper e tema |
| Administração | Produtos, imagens, categorias, pedidos, estados e consulta de pagamento no provedor |
| Infraestrutura | Compose local, volumes persistentes, migrações e testes automatizados |

## Pagamentos e demonstração

Com a configuração inicial, o checkout informa **modo de demonstração** e cria um pedido pendente; nenhuma cobrança é feita. O pedido pode ser cancelado para liberar o estoque. Não há rota pública que permita simular uma compra aprovada para ganhar XP.

Para configurar testes do Mercado Pago, consulte [Pagamentos e Passe](docs/PAGAMENTOS_E_PASSE.md). Credenciais ficam apenas no backend. O Webhook exige uma origem HTTPS pública; o provedor não acessa `localhost` diretamente.

Não basta colocar um token e considerar a loja validada: antes de ativar vendas, confira o fluxo real de aprovação/rejeição, assinatura, recebedor, frete, descontos, cancelamentos, notificações, disponibilidade do estoque e política de devoluções.

## Regras iniciais de gamificação

- Temporadas existentes mantêm os níveis e XP cadastrados; o padrão inicial é 30 níveis de 100 XP.
- O premium custa R$ 197 por temporada por padrão, sem renovação automática; o preço é configurável para novos pedidos.
- A recompensa final exige R$ 1.200 em produtos aprovados na temporada, por padrão. Frete, descontos e compra do premium não contam nesse gasto.
- Compras e valores gastos só avançam missões após confirmação financeira no servidor.
- Avaliações exigem compra aprovada; editar a mesma avaliação não dá um novo prêmio.
- Cupons são individuais, vinculados à conta, de uso único e com condições registradas no resgate. O padrão é R$ 10 de desconto em produtos a partir de R$ 100.
- A coleção permite resgatar e utilizar os benefícios digitais. Não há sorteio nem entrega física automática de prêmios nesta beta.

As variáveis de níveis, preço, XP, cupons, fuso e gasto mínimo estão descritas no [guia de regras e operação](docs/PAGAMENTOS_E_PASSE.md).

## Limites e próximos cuidados

- Frete ainda usa faixas fixas; não é uma cotação de transportadora.
- Reserva de estoque expira em 24 horas. Aprovação recebida depois de cancelamento/expiração requer conciliação.
- Reembolso integral/contestação recalcula XP e missões de compra. Estorno financeiro não é disparado automaticamente pela loja.
- Reembolso parcial, cobrança duplicada, mercadoria já enviada e benefício já consumido precisam de tratamento operacional.
- Imagens precisam de armazenamento persistente no servidor; disco efêmero de hospedagem não substitui o volume local.
- O frontend ainda carrega React/HTM por CDN; sua disponibilidade depende desses serviços.
- E-mails exigem SMTP próprio. O fluxo não promete entrega de mensagem sem configuração.
- Recuperação de senha por e-mail, integração logística e editor administrativo completo das regras do passe ficam para evolução posterior.

## Verificação e desenvolvimento

```bash
sh mvnw test
sh mvnw package
```

Os testes padrão usam H2 descartável. O workflow também roda com PostgreSQL 17, testa migração nova/atualização e usa Chromium para percorrer passe, catálogo, checkout sem cobrança e cancelamento. O teste de pagamento usa resposta simulada do provedor; não valida credenciais comerciais nem cobra cartões.

Para integração PostgreSQL, use `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` e `TEST_DB_DRIVER` apontando exclusivamente para um banco descartável terminado em `_test`. Os testes criam/removem estruturas nesse banco. Nunca use dados pessoais ou o banco publicado.

Esta entrega está na branch `feat/checkout-gamification`. Nenhuma publicação ou alteração do banco hospedado foi executada. Consulte [o guia operacional](docs/PAGAMENTOS_E_PASSE.md) antes de atualizar um ambiente existente.

Desenvolvimento e manutenção: [phzl0](https://github.com/phzl0).
