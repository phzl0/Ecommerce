import { expect, test } from "@playwright/test";

async function login(page, email, password, destination = "/conta") {
  await page.goto("/login?returnUrl=" + encodeURIComponent(destination));
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(destination + "$"));
}

test("registers, earns event XP from a local purchase and claims a badge", async ({ page, browser }) => {
  test.setTimeout(150_000);
  const email = "evento@example.test";
  const password = "a-memorable-test-password";
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/cadastro");
  await expect(page.getByRole("heading", { name: "Criar conta" })).toBeVisible();
  await expect(page.getByRole("search")).toHaveCount(0);
  await page.screenshot({ path: "test-results/registro-mobile.png", fullPage: true });
  await page.getByLabel("Nome completo", { exact: true }).fill("Cliente Evento");
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByLabel("Confirme a senha", { exact: true }).fill("outra-senha-de-teste");
  await page.getByRole("button", { name: "Criar conta", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("A confirmação deve ser igual");
  await page.getByLabel("Confirme a senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Criar conta", exact: true }).click();
  await expect(page).toHaveURL(/\/login\?.*sucesso=true/);
  await login(page, email, password);

  const drawer = page.getByRole("dialog", { name: "Menu da loja" });
  await expect(page.locator(".lc-nav-bottom")).toBeHidden();
  await page.getByRole("button", { name: "Abrir menu da loja" }).click();
  await expect(drawer.getByRole("link", { name: "Passe mensal" })).toBeVisible();
  await expect(drawer.getByRole("link", { name: "Eventos", exact: true })).toHaveCount(0);
  await page.keyboard.press("Escape");
  await expect(drawer).toBeHidden();

  await page.getByRole("link", { name: /Entregar em Adicionar endereço/ }).click();
  for (const [label, value] of [
    ["CEP", "01001000"], ["Rua ou avenida", "Rua Teste"], ["Número", "10"],
    ["Bairro", "Centro"], ["Cidade", "São Paulo"], ["UF", "SP"],
  ]) {
    await page.getByLabel(label, { exact: true }).fill(value);
  }
  await page.getByRole("button", { name: "Salvar endereço" }).click();
  const deliveryLink = page.getByRole("navigation", { name: "Navegação da loja" })
    .getByRole("link", { name: /São Paulo · 01001-000/ });
  await expect(deliveryLink).toBeVisible();
  await page.reload();
  await expect(deliveryLink).toBeVisible();

  const admin = await browser.newPage();
  try {
    await login(admin, "admin@level.local", "AdminLocal123!", "/admin");
    await expect(admin.getByRole("heading", { name: "Atenção agora" })).toBeVisible();
    await expect(admin.getByRole("link", { name: "Novo produto" })).toBeVisible();
    await admin.getByRole("link", { name: /Produtos sem estoque/ }).click();
    await expect(admin.getByRole("checkbox", { name: "Apenas sem estoque" })).toBeChecked();
    await admin.getByRole("link", { name: "Integrações", exact: true }).click();
    await expect(admin.getByText("Aguardando provedor")).toBeVisible();
    expect((await page.request.get("/admin/integracoes")).status()).toBe(403);
    await admin.getByRole("link", { name: "Eventos", exact: true }).click();
    await admin.getByRole("button", { name: "Prévia", exact: true }).click();
    await expect(admin.getByRole("heading", { name: "Prévia do evento" })).toBeVisible();
    expect(await (await page.request.get("/api/events")).json()).toEqual([]);
    await admin.getByRole("button", { name: "Publicar evento" }).click();
    await expect(admin.getByRole("status")).toHaveText("Evento publicado.");

    await page.goto("/eventos");
    await expect(page).toHaveURL(/\/passe#eventos$/);
    await expect(page.getByRole("region", { name: "Eventos da loja" })).toBeVisible();
    const event = page.getByRole("article").filter({ hasText: "Semana XP em Dobro" });
    await expect(event.getByRole("button", { name: "Resgatar selo" })).toBeDisabled();

    const catalog = await (await page.request.get("/api/store/catalog")).json();
    const product = catalog.produtos.find((item) => item.estoque && item.preco >= 50);
    const productPath = "/produto/" + product.id;
    const reviewEndpoint = "/api/pass/missions/review/" + product.id;
    await page.goto(productPath);
    await expect(deliveryLink).toBeVisible();
    expect(await (await page.request.get(reviewEndpoint)).json()).toEqual([]);
    await page.locator(".lc-detail-actions").getByRole("button", { name: "Adicionar à sacola", exact: true }).click();
    await expect(page.getByText("Adicionado à sacola", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "Comprar agora", exact: true }).first().click();
    await expect(page.getByLabel("CEP", { exact: true })).toHaveValue("01001000");
    for (const [label, value] of [
      ["Nome completo", "Cliente Evento"], ["Telefone", "11999999999"],
    ]) {
      await page.getByLabel(label, { exact: true }).fill(value);
    }
    await page.getByRole("button", { name: "Criar pedido de demonstração" }).click();
    await expect(page).toHaveURL(/\/pedidos\/\d+/);
    const orderId = new URL(page.url()).pathname.split("/").pop();
    await page.locator(".lc-account-popover summary").click();
    const accountMenu = page.getByRole("navigation", { name: "Menu da conta" });
    await expect(accountMenu.getByRole("link", { name: "Meu perfil" })).toBeVisible();
    await accountMenu.getByRole("link", { name: "Meus pedidos" }).click();
    const orderCard = page.locator(".lc-orders-page .lc-order-card").first();
    await expect(orderCard.getByRole("heading", { name: /^Pedido / })).toBeVisible();
    await expect(orderCard.locator(".lc-order-product img")).toBeVisible();
    await expect(orderCard.locator(".lc-order-product p")).toBeVisible();
    await expect(orderCard.getByRole("link", { name: "Ver produto" })).toHaveAttribute("href", productPath);
    await expect(orderCard.getByRole("link", { name: "Ver detalhes do pedido" })).toHaveAttribute("href", "/pedidos/" + orderId);
    await page.getByRole("link", { name: /Sacola/ }).click();
    const cartItems = page.getByRole("region", { name: "Produtos da sacola" });
    await expect(cartItems.getByText(product.nome, { exact: true })).toBeVisible();
    await expect(cartItems).toHaveCSS("grid-template-columns", /^\d+px \d+px$/);
    await expect(page.getByRole("region", { name: "Endereço de entrega" })).toContainText("Rua Teste");
    await cartItems.getByRole("button", { name: "Remover", exact: true }).click();
    await expect(cartItems).toHaveCount(0);
    await page.getByRole("button", { name: "Desfazer", exact: true }).click();
    await expect(cartItems.getByText(product.nome, { exact: true })).toBeVisible();
    await page.getByLabel("Cupom de desconto").fill("INVALIDO");
    await page.getByRole("button", { name: "Aplicar cupom" }).click();
    await expect(page.getByRole("alert")).toHaveText("Cupom não encontrado nesta conta.");
    await page.getByRole("button", { name: "Limpar cupom" }).click();
    expect((await page.request.get("/admin/eventos")).status()).toBe(403);
    await admin.goto("/admin");
    await admin.getByRole("link", { name: /Aguardando pagamento/ }).click();
    await expect(admin.getByRole("combobox", { name: "Status", exact: true })).toHaveValue("PENDENTE");
    await admin.getByLabel("Buscar pedidos").fill(orderId);
    await admin.getByRole("button", { name: "Filtrar", exact: true }).click();
    await expect(admin.getByRole("row").filter({ hasText: "Cliente Evento" })).toHaveCount(1);
    await admin.getByRole("link", { name: "Detalhes →" }).click();
    await admin.getByRole("button", { name: "Simular pagamento aprovado" }).click();
    await expect(admin.getByText("Pagamento simulado. Nenhuma cobrança foi realizada.")).toBeVisible();
    await page.goto("/pedidos/" + orderId);
    const purchaseGains = page.locator(".lc-gain-region");
    await expect(purchaseGains.getByText(/Compra aprovada do pedido/)).toBeVisible();
    await expect(purchaseGains.getByRole("link", { name: "Ver passe" })).toBeVisible();
    await expect(purchaseGains.locator(".lc-gain-toast")).toHaveCSS("animation-name", "lc-gain-enter");
    await purchaseGains.getByRole("button", { name: "Fechar aviso" }).click();
    await expect(purchaseGains.getByText(/Recompensa disponível/)).toBeVisible();
    const history = admin.getByRole("region", { name: "Histórico do pedido" });
    await expect(history.getByText("Pagamento confirmado", { exact: true })).toBeVisible();
    await admin.getByRole("button", { name: "Iniciar separação" }).click();
    const quantities = admin.locator('form[action$="/conferir"] input[type="number"]');
    for (const input of await quantities.all()) await input.fill(await input.getAttribute("max"));
    await admin.getByRole("button", { name: "Confirmar conferência" }).click();
    await admin.getByLabel("Transportadora", { exact: true }).fill("Transportadora demo");
    await admin.getByLabel("Código de rastreio", { exact: true }).fill("EVENTO-DEMO-123");
    await admin.getByRole("button", { name: "Registrar envio" }).click();
    await expect(history.getByText("Pedido enviado", { exact: true })).toBeVisible();
    await admin.goto("/admin/pedidos?pagamento=APROVADO&busca=" + orderId);
    await expect(admin.getByRole("combobox", { name: "Pagamento", exact: true })).toHaveValue("APROVADO");
    await expect(admin.getByRole("row").filter({ hasText: "Cliente Evento" })).toContainText("Enviado");

    await page.goto("/passe/missoes");
    await expect(page.getByRole("link", { name: /Avalie e ganhe 90 XP/ }))
      .toHaveAttribute("href", productPath + "?avaliar=1");
    await expect(page.getByText("XP extra · exemplo").first()).toBeVisible();

    const hour = 60 * 60 * 1000;
    const start = Date.now();
    const openProduct = async offset => {
      await page.clock.setFixedTime(start + offset);
      const response = page.waitForResponse(value => value.url().endsWith(reviewEndpoint));
      await page.goto(productPath);
      expect((await response).ok()).toBeTruthy();
      await expect(page.locator(".lc-review-form").getByText("Avalie e ganhe 90 XP")).toBeVisible();
    };
    const reminder = page.locator(".lc-toast-region").getByText("Missão de avaliação", { exact: true });
    await openProduct(0);
    await expect(reminder).toBeVisible();
    await openProduct(3 * hour - 1);
    await expect(reminder).toHaveCount(0);
    await openProduct(3 * hour);
    await expect(reminder).toBeVisible();
    await openProduct(9 * hour - 1);
    await expect(reminder).toHaveCount(0);
    await openProduct(9 * hour);
    await expect(reminder).toBeVisible();
    await openProduct(21 * hour - 1);
    await expect(reminder).toHaveCount(0);
    await openProduct(21 * hour);
    await expect(reminder).toBeVisible();
    await page.getByRole("link", { name: "Ir para avaliação →" }).click();
    await expect(page.locator(".lc-review-form")).toBeFocused();
    await page.getByRole("button", { name: "Enviar avaliação" }).click();
    const gains = page.locator(".lc-gain-region");
    await expect(gains.getByText(/\+90 XP/)).toBeVisible();
    expect(await (await page.request.get(reviewEndpoint)).json()).toEqual([]);

    await page.goto("/");
    const claim = page.getByRole("button", { name: /^Receber · nível 1 ·/ });
    await expect(claim).toBeEnabled();
    await claim.click();
    await expect(page.getByRole("button", { name: /^Recebido · nível 1 ·/ })).toBeDisabled();
    await expect(gains.getByText("Recompensa recebida", { exact: true })).toBeVisible();
    const rewards = await (await page.request.get("/api/pass/rewards")).json();
    const coupon = rewards.find(item => item.tipo === "Cupom");
    await page.goto("/sacola");
    const quantity = Math.ceil(Number(coupon.minimo) / Number(product.preco));
    for (let count = 1; count < quantity; count++) {
      await page.getByRole("button", { name: /^Aumentar quantidade/ }).click();
    }
    await page.getByLabel("Cupom de desconto").fill(coupon.codigo);
    await page.getByRole("button", { name: "Aplicar cupom" }).click();
    await expect(page.getByText(/Cupom aplicado: LC-/)).toBeVisible();
    const summary = page.getByRole("complementary", { name: "Resumo da compra" });
    await expect(summary).toContainText("Desconto");
    await summary.getByRole("button", { name: "Continuar para pagamento" }).click();
    await expect(page).toHaveURL(/\/checkout$/);
    await expect(page.getByLabel("Cupom da sua coleção")).toHaveValue(coupon.codigo);
    await expect(summary).toContainText("Desconto");

    await page.goto("/eventos");
    await event.getByRole("button", { name: "Resgatar selo" }).click();
    await expect(event.getByText("Selo na sua coleção")).toBeVisible();
    await page.getByLabel(/Notificações, \d+ não lidas/).click();
    await expect(page.getByText("Selo resgatado", { exact: true }).last()).toBeVisible();
    await page.getByRole("button", { name: "Marcar como lidas" }).click();
    await expect(page.getByLabel("Notificações", { exact: true })).toBeVisible();

    await page.goto("/perfil");
    await expect(page.getByRole("heading", { name: "Meu perfil" })).toBeVisible();
    await expect(page.getByLabel("Tipo de conta")).toHaveCount(0);
    await page.getByRole("link", { name: "Progresso e XP" }).click();
    await expect(page.getByRole("heading", { name: "Últimos ganhos de XP" })).toBeVisible();
    await expect(page.getByText("XP na temporada")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Selos de eventos" })).toBeVisible();
    await expect(page.locator(".lc-event-badges").getByText("Veterano da Semana LEVEL")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();

    await page.goto("/passe/missoes");
    await expect(page.getByRole("heading", { name: "Missões do passe" })).toBeVisible();
    for (const tab of ["Diárias", "Semanais", "Mensais"]) {
      await page.getByRole("tab", { name: tab }).click();
      await expect(page.getByRole("tabpanel").getByRole("article")).toHaveCount(4);
    }
    await page.getByRole("tab", { name: "Especiais" }).click();
    await expect(page.getByRole("tabpanel").getByText("Semana XP em Dobro")).toBeVisible();

    await page.goto("/passe");
    await expect(page.getByText(/R\$\s?79,00 nesta temporada/)).toBeVisible();
    await page.getByRole("button", { name: "Liberar Premium" }).click();
    await expect(page).toHaveURL(/\/pedidos\/\d+/);
    const premiumId = new URL(page.url()).pathname.split("/").pop();
    const subscription = await (await page.request.get("/api/account/orders/" + premiumId)).json();
    expect(Number(subscription.total)).toBe(79);
    await admin.goto("/admin/pedidos/" + premiumId);
    await admin.getByRole("button", { name: "Simular pagamento aprovado" }).click();
    await expect(admin.getByText("Pagamento simulado. Nenhuma cobrança foi realizada.")).toBeVisible();
    await page.goto("/passe");
    await expect(page.getByText("Premium ativo", { exact: true })).toBeVisible();
    await expect(page.locator(".lc-premium-overlay")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Liberar Premium" })).toHaveCount(0);

    await admin.goto("/admin/produtos/" + product.id + "/editar");
    const preview = admin.getByRole("img", { name: "Imagem atual" });
    await expect(preview).toHaveCSS("height", "64px");
    const originalImage = await preview.getAttribute("src");
    await admin.getByLabel("Descrição curta", { exact: true }).fill("Descrição atualizada no painel.");
    await admin.getByRole("button", { name: "Salvar produto" }).click();
    await expect(admin.getByText("Produto salvo com sucesso!")).toBeVisible();
    expect((await (await page.request.get("/api/store/products/" + product.id)).json()).imagemUrl).toBe(originalImage);
    await admin.goto("/admin/produtos?busca=" + encodeURIComponent(product.nome));
    await admin.getByRole("button", { name: "Desativar", exact: true }).click();
    await admin.goto("/admin/produtos?visibilidade=inativos&busca=" + encodeURIComponent(product.nome));
    await expect(admin.getByRole("row").filter({ hasText: product.nome })).toContainText("Inativo");
    await admin.getByRole("button", { name: "Ativar", exact: true }).click();
    await expect(admin.getByText("Produto ativado.", { exact: true })).toBeVisible();
  } finally {
    await admin.close();
  }
});

test("recovers a password by local email and rejects a reused link", async ({ page }) => {
  const mailpit = process.env.TEST_MAILPIT_URL;
  test.skip(!mailpit, "This journey requires the Compose mail inbox.");
  const email = "recuperacao@example.test";
  const original = "original-test-password";
  const replacement = "replacement-test-password";
  await page.goto("/cadastro");
  await page.getByLabel("Nome completo", { exact: true }).fill("Cliente Recuperação");
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(original);
  await page.getByLabel("Confirme a senha", { exact: true }).fill(original);
  await page.getByRole("button", { name: "Criar conta", exact: true }).click();
  await page.getByRole("link", { name: "Esqueci minha senha" }).click();
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByRole("button", { name: "Enviar link" }).click();
  await expect(page.getByText(/Se o e-mail estiver cadastrado/)).toBeVisible();

  let messageId;
  await expect.poll(async () => {
    const response = await page.request.get(mailpit + "/api/v1/messages");
    const { messages } = await response.json();
    messageId = messages.find((item) => item.Subject.startsWith("Redefina sua senha")
      && item.To.some((recipient) => recipient.Address === email))?.ID;
    return messageId;
  }).toBeTruthy();
  const response = await page.request.get(mailpit + "/api/v1/message/" + messageId);
  const { Text } = await response.json();
  const link = Text.match(/http[^\s]+\/redefinir-senha#token=[A-Za-z0-9_-]+/)?.[0];
  expect(link).toBeTruthy();
  const setPassword = async () => {
    await page.goto(link);
    await page.getByLabel("Nova senha", { exact: true }).fill(replacement);
    await page.getByLabel("Confirme a senha", { exact: true }).fill(replacement);
    await page.getByRole("button", { name: "Redefinir senha", exact: true }).click();
  };
  await setPassword();
  await expect(page.getByText("Senha atualizada. Entre com sua nova senha.")).toBeVisible();
  await setPassword();
  await expect(page.getByRole("alert")).toContainText("expirou ou já foi usado");
  await login(page, email, replacement);
});
