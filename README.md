# LEVEL COMMERCE — Ecommerce Beta

E-commerce gamificado com catálogo, sacola, pedidos, passe por temporada, missões, XP e recompensas digitais.

**BETA para testes locais. Pagamentos desativados por padrão: o teste não cobra dinheiro.** A versão de desenvolvimento local está na branch `develop`; os comandos abaixo selecionam essa branch.

## Testar no computador

Você precisa de **Git, Python 3 e Docker com Compose**. Não precisa instalar Java ou PostgreSQL: o Docker cuida deles.

**Escolha apenas um caminho abaixo.** Se um comando der erro, pare e resolva antes de continuar. Copie somente os comandos, sem o texto do terminal, como `phzl0@chat:...$`.

### Linux — primeira instalação (recomendado)

Abra o terminal na sua pasta pessoal. Usamos a pasta **Ecommerce-beta** para não confundir com uma cópia antiga chamada Ecommerce.

**1. Baixe a beta e entre na pasta:**

```bash
cd ~
git clone --branch develop https://github.com/phzl0/Ecommerce.git Ecommerce-beta
cd ~/Ecommerce-beta
```

Se `Ecommerce-beta` já existir, use o caminho de atualização abaixo com essa pasta; não repita o clone.

**2. Gere a configuração local:**

```bash
python3 scripts/init_env.py --demo
```

O script cria o `.env` e uma senha aleatória para o banco. Para adicionar um administrador próprio, rode o script sem `--demo` na primeira configuração.

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
git switch develop
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
python3 scripts/init_env.py --demo
sudo docker compose up --build -d
sudo docker compose logs -f app
```

Se já existir um `.env`, o script o preserva. Uma configuração antiga pode precisar de ajustes; não apague suas credenciais ou seu banco para resolver um erro.

### Windows — primeira instalação (PowerShell)

Instale **Git, Python 3 e Docker Desktop**, com containers Linux/WSL 2. Abra o Docker Desktop e aguarde o mecanismo iniciar. Abra o **PowerShell**.

**1. Baixe a beta:**

```powershell
Set-Location $HOME
git clone --branch develop https://github.com/phzl0/Ecommerce.git Ecommerce-beta
Set-Location "$HOME/Ecommerce-beta"
```

**2. Gere a configuração local:**

```powershell
py -3 scripts/init_env.py --demo
```

Se `py` não existir, tente `python scripts/init_env.py --demo`. Para adicionar um administrador próprio, retire `--demo` na primeira configuração.

**3. Inicie e acompanhe:**

```powershell
docker compose up --build -d
docker compose logs -f app
```

Quando aparecer `Started EcommerceApplication`, abra **http://localhost:8080**. **Ctrl+C** sai dos logs sem desligar a loja.

**Já tem uma cópia no Windows?** Entre na pasta existente com `Set-Location "CAMINHO/DO/Ecommerce"`, rode `git fetch origin`, `git switch develop` e `git pull --ff-only`, um comando por vez. Depois execute as etapas 2 e 3. Não faça outro clone dentro da pasta existente.

## O que testar

- **Loja:** http://localhost:8080 — veja produtos demonstrativos, detalhes e sacola.
- **Administração:** http://localhost:8080/admin — use o e-mail e a senha definidos no script.
- **Compra demonstrativa:** informe um endereço e finalize em modo de demonstração; o pedido fica pendente, sem cobrança. Cancele pelo detalhe do pedido para liberar o estoque.
- **Gamificação:** abra o passe, faça check-in e explore as missões. Compras só geram XP após pagamento verificado; o modo demonstrativo não simula aprovação.
- **Conta:** teste perfil, pedidos e coleção de benefícios digitais.

O catálogo de demonstração vem habilitado na configuração inicial. A tela carrega bibliotecas externas, então mantenha acesso à internet. Os dados de entrega preenchidos no checkout ficam em memória enquanto a tela está aberta. Se fechar ou recarregar após uma falha de rede no envio, consulte seus pedidos antes de refazer a compra.

## Testar conta, notificações e evento semanal

A navegação inclui **Administração** quando você estiver deslogado no Docker local ou conectado como administrador. Acesse [localhost:8080/admin](http://localhost:8080/admin) com a conta local de demonstração:

| Campo | Valor |
| --- | --- |
| E-mail | `admin@level.local` |
| Senha inicial | `AdminLocal123!` |

A conta de demonstração é criada pelo Compose local. As credenciais administrativas existentes no `.env` continuam válidas. Se você alterar a senha da conta de demonstração, a aplicação preserva a mudança nas próximas inicializações.

1. No admin, abra **Eventos** e publique **Semana XP em Dobro**. Os campos já vêm preenchidos com um exemplo. O horário segue o fuso da loja, por padrão São Paulo.
2. Em outra sessão do navegador, crie uma conta de cliente e faça um pedido de demonstração.
3. No admin, abra o pedido e selecione **Simular pagamento aprovado**. A aprovação local atualiza o pedido e credita XP para as compras que atendem às regras do evento.
4. Na conta do cliente, abra **Eventos**, resgate o selo e confira o **sino de notificações**. Os selos válidos também aparecem no perfil.

O evento considera o valor líquido dos produtos, sem frete ou compra de passe. A cada R$ 10 completos elegíveis por pedido, o cliente recebe 1 XP de evento multiplicado pela campanha, até o limite configurado. O selo pode ser resgatado até sete dias após o encerramento. Um estorno recalcula a elegibilidade.

**Recuperar senha no Docker**. Na tela de login, clique em **Esqueci minha senha** e informe o e-mail da conta. Abra [Mailpit em localhost:8025](http://localhost:8025) e use o link recebido. A caixa captura mensagens de teste. O link expira em 15 minutos e só funciona uma vez. Se a porta 8025 estiver ocupada, ajuste `MAILPIT_PORT` no `.env`.

### Pendências para produção

- Configurar login com Google, incluindo credenciais OAuth, URLs HTTPS de retorno e regras de vínculo a contas existentes.
- Configurar um provedor de e-mail transacional, domínio autenticado, limites de envio e verificação de entrega antes de habilitar recuperação de senha comercial.
- Validar o checkout externo e o webhook público com credenciais de teste. A conta e o simulador de demonstração só são habilitados no perfil local com a configuração explícita do Compose.
- Empacotar as dependências da interface para servir os scripts da própria aplicação antes de ativar fluxos de autenticação comercial.

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
| Porta 8080 já está em uso | Adicione `APP_PORT=8081` ao `.env`, rode `docker compose up --build -d` e abra http://localhost:8081. |
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
| Passe | Temporadas, trilha gratuita, Premium por temporada, com preço configurável e histórico de XP |
| Missões | Check-in, produtos distintos, sacola, avaliações e compras aprovadas |
| Benefícios | Cupom pessoal de uso único, avatar/badge, wallpaper e tema |
| Administração | Produtos, imagens, categorias, pedidos, estados e consulta de pagamento no provedor |
| Infraestrutura | Compose local, volumes persistentes, migrações e testes automatizados |

## Pagamentos e demonstração

Com a configuração inicial, o checkout informa **modo de demonstração** e cria um pedido pendente; nenhuma cobrança é feita. O pedido pode ser cancelado para liberar o estoque. Não há rota pública que permita simular uma compra aprovada para ganhar XP.

Para configurar testes do Mercado Pago, consulte [Pagamentos e Passe](https://github.com/phzl0/Ecommerce/blob/develop/docs/PAGAMENTOS_E_PASSE.md). Credenciais ficam apenas no backend. O Webhook exige uma origem HTTPS pública; o provedor não acessa `localhost` diretamente.

Não basta colocar um token e considerar a loja validada: antes de ativar vendas, confira o fluxo real de aprovação/rejeição, assinatura, recebedor, frete, descontos, cancelamentos, notificações, disponibilidade do estoque e política de devoluções.

## Regras iniciais de gamificação

- Temporadas existentes mantêm os níveis e XP cadastrados; o padrão inicial é 30 níveis de 100 XP.
- O Passe Premium é uma compra única da temporada. O preço inicial demonstrativo é R$ 79, configurável no administrativo.
- Os prêmios Premium exigem acesso ativo e o nível indicado, inclusive a recompensa final.
- Compras só avançam missões após confirmação financeira no servidor.
- Avaliações exigem compra aprovada; editar a mesma avaliação não dá um novo prêmio.
- Cupons são individuais, vinculados à conta, de uso único e com condições registradas no resgate. O padrão é R$ 10 de desconto em produtos a partir de R$ 100.
- A coleção permite resgatar e utilizar os benefícios digitais. Recompensas físicas configuradas geram pedidos gratuitos mediante solicitação pelo Baú.

As regras de níveis, assinatura, XP, cupons e fuso estão descritas no [guia de regras e operação](https://github.com/phzl0/Ecommerce/blob/develop/docs/PAGAMENTOS_E_PASSE.md).

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

Esta entrega está na branch `develop`. Nenhuma publicação ou alteração do banco hospedado foi executada. Consulte [o guia operacional](https://github.com/phzl0/Ecommerce/blob/develop/docs/PAGAMENTOS_E_PASSE.md) antes de atualizar um ambiente existente.

Desenvolvimento e manutenção: [phzl0](https://github.com/phzl0).

### Estoque e produtos digitais (beta local)

- **Admin → Estoque e reservas:** saldos disponíveis, reservas de pedidos/Baú, mínimo por produto e últimas 100 movimentações. Ajustes exigem motivo e registram o responsável; não alteram unidades reservadas. O saldo de cadastros existentes é alterado por essa tela.
- **Admin → Produtos digitais:** selecione um produto do tipo Digital e configure arquivo (PDF/TXT/ZIP, até 5 MB) ou códigos/licenças (até 500 por importação). Para iniciar códigos, o saldo disponível deve ser zero; a importação aumenta o estoque e ignora duplicados. O formato fica fixo por SKU.
- **Conta → Meus produtos digitais:** arquivos e códigos são liberados apenas após aprovação, com acesso restrito ao comprador. Cada pedido guarda a versão adquirida. O conteúdo fica no banco privado e não em URLs públicas. Os backups do PostgreSQL incluem esses arquivos e códigos; trate esses backups como dados sensíveis.
- Cancelamentos antes da aprovação liberam reservas. Estornos revogam o acesso futuro; códigos já liberados nunca voltam à venda. Um arquivo já baixado ou código copiado não pode ser apagado do dispositivo do cliente.
- Produtos físicos e digitais podem coexistir no pedido: digitais são disponibilizados após pagamento; apenas os físicos entram no envio. Reservas físicas são consumidas no envio. Após envio, devoluções físicas exigem conferência e ajuste de estoque com motivo.
- A migração preserva saldos e reservas existentes; o histórico começa com o saldo anterior, sem inventar movimentações passadas. Pedidos digitais antigos não recebem conteúdo retroativamente.
- Exemplos locais: `DEMO-WALLPAPER` entrega um TXT demonstrativo; `DEMO-LICENCA` contém três códigos sem valor comercial. Use **Simular pagamento aprovado** no detalhe do pedido para testar. Dados demonstrativos dependem de `DEMO_DATA_ENABLED=true`.
- Antes de produção: escolher armazenamento de arquivos em escala, política de retenção, verificação antimalware, gestão de licenças no fornecedor e regras de devolução; conectar ERP, transportadora e pagamentos reais. Checkout digital ainda usa o formulário de dados do cliente/endereço compartilhado, sem cobrar frete em pedidos somente digitais.

### Separação, rastreamento e pós-venda (beta local)

1. Em **Admin → Separação e envios**, abra um pedido físico confirmado (compra ou prêmio do Baú).
2. Clique em **Iniciar separação** e informe a quantidade conferida de cada item físico. A quantidade deve corresponder exatamente ao pedido; os digitais não entram nessa conferência.
3. Informe transportadora, código e, opcionalmente, um link HTTPS. **Registrar envio** consome a reserva e notifica o cliente uma vez. O botão **Confirmar entrega** registra a entrega manualmente. Correções de rastreio também ficam no histórico.
4. Na conta do cliente, o detalhe do pedido mostra o acompanhamento físico e a situação digital separadamente, além do atendimento e de solicitações de troca/devolução de itens físicos enviados.
5. Em **Admin → Atendimento e devoluções**, acompanhe solicitações por etapa e as últimas 50 mensagens de clientes (inclui mensagens já respondidas). Responda dentro do pedido. Observações internas são visíveis apenas à equipe.
6. Autorize ou recuse com resposta ao cliente. Após receber e conferir todas as unidades solicitadas, informe quantas estão aptas para voltar ao estoque. A reposição é registrada uma única vez, com responsável e referência à solicitação.

**Limites desta etapa:** um envio completo por pedido, sem remessas parciais. Rastreamento e entrega são manuais; não há consulta automática à transportadora. Concluir uma solicitação registra a solução do atendimento, mas não executa estorno financeiro, compra de frete, etiqueta ou novo pedido de troca. Essas operações devem ser resolvidas separadamente e documentadas na resposta. Prazos/políticas comerciais e integrações serão definidos antes da produção. Pedidos enviados anteriores à migração preservam a etapa, sem inventar transportadora, código ou datas de despacho.

### Reembolsos, trocas e avisos de progresso

- No pedido administrativo, use **Solicitar reembolso → Calcular impacto**. Revise o valor e o aviso, registre o motivo e confirme a leitura. A solicitação fica pendente e ainda não retira pontos.
- Em **Revisar e confirmar reembolso**, confira novamente o impacto e use **Confirmar reembolso simulado**. Disponível somente no perfil local de demonstração, com pagamentos desativados e sem identificadores de pagamento do provedor. Não movimenta dinheiro.
- Reembolsos parciais usam proporção acumulada do total pago, incluindo frete e descontos. XP da compra e VIP vinculados são reduzidos proporcionalmente; missões de compra/gasto e eventos são recalculados sobre o valor líquido. O arredondamento acumulado evita diferenças ao dividir um reembolso em várias operações.
- O reembolso integral cancela o pedido, revoga o acesso digital futuro/Premium e libera apenas reservas ainda não consumidas. Produtos físicos enviados voltam ao estoque somente pela conferência de devolução. Reembolso parcial digital mantém o acesso; não há revogação externa de licenças ou arquivos baixados.
- Benefícios já resgatados geram uma análise manual em **Reembolsos e benefícios**. Registrar a análise preserva a justificativa e o responsável; não cobra o cliente nem remove automaticamente prêmios. O filtro de análise é conservador e pode incluir benefícios de outras compras, para conferência humana.
- Após receber uma solicitação de **Troca**, escolha o produto físico, confira o endereço original e gere a remessa. Ela reserva estoque, vincula o pedido original e segue separação/conferência/envio normalmente, sem cobrança e sem novos XP/VIP/eventos. Cada solicitação gera uma única remessa. Reembolso pendente ou confirmado impede a troca; remessa ativa impede reembolsar o pedido original até resolução manual.
- No pedido do cliente, a seleção de item e quantidade mostra uma estimativa do impacto (itens com desconto, sem frete). A leitura do aviso é obrigatória antes de enviar a solicitação. O valor final pode ser ajustado na análise administrativa. Se o progresso relevante mudar, a confirmação antiga é rejeitada e exige revisão do aviso.
- Integração de estorno real, etiqueta reversa, política de trocas com diferença de preço e logística de remessas canceladas continuam pendentes. Não use este fluxo simulado para confirmar estornos reais.
