# LEVEL COMMERCE — Ecommerce Beta

E-commerce gamificado com catálogo, sacola, pedidos, passe por temporada, missões, XP e recompensas digitais.

**BETA para testes locais. Pagamentos desativados por padrão: o teste não cobra dinheiro.** A versão de teste está na branch `feat/checkout-gamification`; os comandos abaixo selecionam essa branch automaticamente.

## Testar no computador

Você precisa de **Git, Python 3 e Docker com Compose**. Não precisa instalar Java ou PostgreSQL: o Docker cuida deles.

**Escolha apenas um caminho abaixo.** Se um comando der erro, pare e resolva antes de continuar. Copie somente os comandos, sem o texto do terminal, como `phzl0@chat:...$`.

### Linux — primeira instalação (recomendado)

Abra o terminal na sua pasta pessoal. Usamos a pasta **Ecommerce-beta** para não confundir com uma cópia antiga chamada Ecommerce.

**1. Baixe a beta e entre na pasta:**

```bash
cd ~
git clone --branch feat/checkout-gamification https://github.com/phzl0/Ecommerce.git Ecommerce-beta
cd ~/Ecommerce-beta
```

Se `Ecommerce-beta` já existir, use o caminho de atualização abaixo com essa pasta; não repita o clone.

**2. Crie sua conta de administrador:**

```bash
python3 scripts/init_env.py
```

Informe seu e-mail e uma senha de **12 a 72 caracteres**. A senha não aparece enquanto você digita; isso é normal. Confirme a mesma senha quando solicitado. O script cria o arquivo `.env`.

**3. Inicie a loja e o banco:**

```bash
sudo docker compose up --build -d
```

A primeira inicialização pode demorar para baixar e compilar os componentes.

**4. Acompanhe a inicialização:**

```bash
sudo docker compose logs -f app
```

Quando aparecer `Started EcommerceApplication`, abra **http://localhost:8080**. Para sair dos logs, pressione **Ctrl+C**; a loja continua ligada.

### Linux — já tenho o repositório antigo

Se sua cópia está em `~/Projetos/Ecommerce`, execute os passos abaixo. **Você já está na pasta do projeto depois do primeiro comando: não execute outro `cd Ecommerce`.**

**1. Atualize a lista de branches e selecione a beta:**

```bash
cd ~/Projetos/Ecommerce
git status --short
git fetch origin
git switch feat/checkout-gamification
git pull --ff-only
```

Se o Git avisar sobre alterações locais, pare e preserve seus arquivos antes de trocar a branch. Não use comandos para descartar alterações. Se sua pasta tiver outro nome, ajuste apenas o primeiro comando.

**2. Confira os arquivos antes de continuar:**

```bash
ls compose.yaml scripts/init_env.py
```

Os dois arquivos devem aparecer. Se não aparecerem, a troca de branch não foi concluída.

**3. Configure e inicie:**

```bash
python3 scripts/init_env.py
sudo docker compose up --build -d
sudo docker compose logs -f app
```

Se já existir um `.env`, o script o preserva. Uma configuração antiga pode precisar de ajustes; não apague suas credenciais ou seu banco para resolver um erro.

### Windows — primeira instalação (PowerShell)

Instale **Git, Python 3 e Docker Desktop**, com containers Linux/WSL 2. Abra o Docker Desktop e aguarde o mecanismo iniciar. Abra o **PowerShell**.

**1. Baixe a beta:**

```powershell
Set-Location $HOME
git clone --branch feat/checkout-gamification https://github.com/phzl0/Ecommerce.git Ecommerce-beta
Set-Location "$HOME/Ecommerce-beta"
```

**2. Crie a conta administrativa:**

```powershell
py -3 scripts/init_env.py
```

Informe e-mail, senha de 12 a 72 caracteres e confirmação. A senha não aparece enquanto você digita. Se `py` não existir, tente `python scripts/init_env.py`.

**3. Inicie e acompanhe:**

```powershell
docker compose up --build -d
docker compose logs -f app
```

Quando aparecer `Started EcommerceApplication`, abra **http://localhost:8080**. **Ctrl+C** sai dos logs sem desligar a loja.

**Já tem uma cópia no Windows?** Entre na pasta existente com `Set-Location "CAMINHO/DO/Ecommerce"`, rode `git fetch origin`, `git switch feat/checkout-gamification` e `git pull --ff-only`, um comando por vez. Depois execute as etapas 2 e 3. Não faça outro clone dentro da pasta existente.

## O que testar

- **Loja:** http://localhost:8080 — veja produtos demonstrativos, detalhes e sacola.
- **Administração:** http://localhost:8080/admin — use o e-mail e a senha definidos no script.
- **Compra demonstrativa:** informe um endereço e finalize em modo de demonstração; o pedido fica pendente, sem cobrança. Cancele pelo detalhe do pedido para liberar o estoque.
- **Gamificação:** abra o passe, faça check-in e explore as missões. Compras só geram XP após pagamento verificado; o modo demonstrativo não simula aprovação.
- **Conta:** teste perfil, pedidos e coleção de benefícios digitais.

O catálogo de demonstração vem habilitado na configuração inicial. A tela carrega bibliotecas externas, então mantenha acesso à internet.

## Atualizar ou parar

Sempre execute dentro da pasta onde aparecem `compose.yaml` e `scripts/init_env.py`.

Para atualizar uma instalação que **já está na branch beta**:

```bash
git pull --ff-only
docker compose up --build -d
```

No Linux, acrescente `sudo` antes dos comandos Docker se necessário.

Para parar preservando banco e imagens:

```bash
docker compose down
```

Para ligar novamente:

```bash
docker compose up -d
```

**Preserve o arquivo `.env` e os volumes. Não use `docker compose down -v`: essa opção apaga os dados locais.** Faça backup antes de atualizar um banco existente. Use dados fictícios para testar e não copie credenciais de produção.

## Problemas comuns

| Mensagem | Como resolver |
| --- | --- |
| `cd: Ecommerce: Arquivo ou diretório inexistente` | Se o terminal já mostra `/Ecommerce$`, você já está na pasta. Não entre novamente. |
| `can't open file ... scripts/init_env.py` ou `no configuration file provided` | Confira a pasta e execute o caminho “já tenho o repositório antigo” para selecionar a branch beta. |
| Pasta `Ecommerce-beta` já existe | Entre nela e use os comandos de atualização; não clone por cima. |
| `permission denied ... docker.sock` | No Linux, use `sudo docker compose ...`. |
| Não consegue conectar ao Docker | Inicie o serviço Docker no Linux ou abra o Docker Desktop no Windows. |
| Porta 8080 já está em uso | Pare o outro aplicativo que utiliza essa porta antes de iniciar a loja. |
| `localhost:8080` ainda não abre | Veja `docker compose ps` e `docker compose logs --tail=100 app`; aguarde a inicialização ou confira o erro. |
| A senha não aparece no terminal | Digite normalmente e pressione Enter; a entrada fica oculta por segurança. |

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

Para configurar testes do Mercado Pago, consulte [Pagamentos e Passe](https://github.com/phzl0/Ecommerce/blob/feat/checkout-gamification/docs/PAGAMENTOS_E_PASSE.md). Credenciais ficam apenas no backend. O Webhook exige uma origem HTTPS pública; o provedor não acessa `localhost` diretamente.

Não basta colocar um token e considerar a loja validada: antes de ativar vendas, confira o fluxo real de aprovação/rejeição, assinatura, recebedor, frete, descontos, cancelamentos, notificações, disponibilidade do estoque e política de devoluções.

## Regras iniciais de gamificação

- Temporadas existentes mantêm os níveis e XP cadastrados; o padrão inicial é 30 níveis de 100 XP.
- O premium custa R$ 197 por temporada por padrão, sem renovação automática; o preço é configurável para novos pedidos.
- A recompensa final exige R$ 1.200 em produtos aprovados na temporada, por padrão. Frete, descontos e compra do premium não contam nesse gasto.
- Compras e valores gastos só avançam missões após confirmação financeira no servidor.
- Avaliações exigem compra aprovada; editar a mesma avaliação não dá um novo prêmio.
- Cupons são individuais, vinculados à conta, de uso único e com condições registradas no resgate. O padrão é R$ 10 de desconto em produtos a partir de R$ 100.
- A coleção permite resgatar e utilizar os benefícios digitais. Não há sorteio nem entrega física automática de prêmios nesta beta.

As variáveis de níveis, preço, XP, cupons, fuso e gasto mínimo estão descritas no [guia de regras e operação](https://github.com/phzl0/Ecommerce/blob/feat/checkout-gamification/docs/PAGAMENTOS_E_PASSE.md).

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

Esta entrega está na branch `feat/checkout-gamification`. Nenhuma publicação ou alteração do banco hospedado foi executada. Consulte [o guia operacional](https://github.com/phzl0/Ecommerce/blob/feat/checkout-gamification/docs/PAGAMENTOS_E_PASSE.md) antes de atualizar um ambiente existente.

Desenvolvimento e manutenção: [phzl0](https://github.com/phzl0).
