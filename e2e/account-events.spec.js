import { expect, test } from "@playwright/test";

async function login(page, email, password, destination = "/conta") {
  await page.goto("/login?returnUrl=" + encodeURIComponent(destination));
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(destination + "$"));
}

test("registers, earns event XP from a local purchase and claims a badge", async ({ page, browser }) => {
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
    await admin.goto("/admin/eventos");
    await admin.getByRole("button", { name: "Publicar evento" }).click();
    await expect(admin.getByRole("status")).toHaveText("Evento publicado.");

    await page.goto("/eventos");
    const event = page.getByRole("article").filter({ hasText: "Semana XP em Dobro" });
    await expect(event.getByRole("button", { name: "Resgatar selo" })).toBeDisabled();

    const catalog = await (await page.request.get("/api/store/catalog")).json();
    const product = catalog.produtos.find((item) => item.estoque && item.preco >= 50);
    await page.goto("/produto/" + product.id);
    await expect(deliveryLink).toBeVisible();
    await page.getByRole("button", { name: "Adicionar à sacola", exact: true }).click();
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
    await page.getByRole("link", { name: /Sacola/ }).click();
    await expect(page.getByText(product.nome, { exact: true })).toBeVisible();
    expect((await page.request.get("/admin/eventos")).status()).toBe(403);
    await admin.goto("/admin/pedidos/" + orderId);
    await admin.getByRole("button", { name: "Simular pagamento aprovado" }).click();
    await expect(admin.getByText("Pagamento simulado. Nenhuma cobrança foi realizada.")).toBeVisible();

    await page.goto("/eventos");
    await event.getByRole("button", { name: "Resgatar selo" }).click();
    await expect(event.getByText("Selo na sua coleção")).toBeVisible();
    await page.getByLabel(/Notificações, \d+ não lidas/).click();
    await expect(page.getByText("Selo resgatado", { exact: true }).last()).toBeVisible();
    await page.getByRole("button", { name: "Marcar como lidas" }).click();
    await expect(page.getByLabel("Notificações", { exact: true })).toBeVisible();

    await page.goto("/perfil");
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
    await expect(page.getByText(/R\$\s?79,00 por mês/)).toBeVisible();
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
    await expect(page.getByRole("button", { name: "Liberar Premium" })).toHaveCount(0);
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
