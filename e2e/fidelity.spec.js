import { test, expect } from "@playwright/test";

async function login(page, email, password) {
  await page.goto("/login");
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

test("premium reward enters the chest and ships as a normal free order", async ({ page, browser }) => {
  test.setTimeout(150000);
  const admin = await browser.newPage();
  try {
    await login(admin, "admin@level.local", "AdminLocal123!");
    await admin.goto("/admin/fidelidade");
    await expect(admin.getByRole("heading", { name: "Passe, VIP e Baú", exact: true })).toBeVisible();
    const missionForm = admin.locator('form[action="/admin/fidelidade/missao"]');
    await missionForm.getByLabel("Título", { exact: true }).fill("Exploração de teste");
    await missionForm.getByLabel("Ação", { exact: true }).selectOption("VIEW_PRODUCT");
    await missionForm.getByLabel("XP", { exact: true }).fill("3000");
    await missionForm.getByLabel("Exclusiva Premium").check();
    await missionForm.getByRole("button", { name: "Criar missão" }).click();

    await page.goto("/login");
    const token = (await page.context().cookies()).find(c => c.name === "XSRF-TOKEN").value;
    const email = "bau@example.test", password = "disposable-chest-password";
    const signup = await page.request.post("/auth/cadastro", {
      form: { nome: "Cliente Baú", email, senha: password, _csrf: decodeURIComponent(token) },
      maxRedirects: 0,
    });
    expect(signup.status()).toBe(302);
    await login(page, email, password);
    await page.goto("/passe/missoes");
    await page.getByRole("tab", { name: "Premium", exact: true }).click();
    await expect(page.getByText("🔒 Requer Premium").first()).toBeVisible();

    await page.goto("/passe");
    await page.getByRole("button", { name: "Liberar Premium" }).click();
    await expect(page).toHaveURL(/\/pedidos\/\d+/);
    const paymentOrder = new URL(page.url()).pathname.split("/").pop();
    await admin.goto("/admin/pedidos/" + paymentOrder);
    await admin.getByRole("button", { name: "Simular pagamento aprovado" }).click();

    await page.goto("/catalogo");
    await page.locator(".lc-product-card").first().getByRole("link").click();
    await expect(page).toHaveURL(/\/produto\/\d+/);
    await expect.poll(async () => (await (await page.request.get("/api/pass/current")).json()).nivelAtual).toBe(30);
    await page.goto("/passe");
    const track = page.getByRole("region", { name: "Níveis e recompensas do passe" });
    await track.evaluate(el => { el.scrollLeft = el.scrollWidth; });
    const choice = page.getByLabel("Escolher prêmio do nível 30");
    const option = await choice.locator("option").nth(1).getAttribute("value");
    await choice.selectOption(option);
    await page.getByRole("button", { name: /^Receber · nível 30 · Escolha/ }).click();
    await page.getByRole("link", { name: "Receber agora" }).click();
    await expect(page.getByRole("heading", { name: "Receber recompensa" })).toBeVisible();
    for (const [label, value] of [["Destinatário", "Cliente Baú"], ["Telefone", "11999999999"],
      ["CEP", "06400-000"], ["Endereço", "Rua Teste"], ["Número", "10"], ["Bairro", "Centro"],
      ["Cidade", "Barueri"], ["Estado (UF)", "SP"]]) {
      await page.getByLabel(label, { exact: true }).fill(value);
    }
    await page.getByRole("button", { name: "Confirmar entrega gratuita" }).click();
    await expect(page).toHaveURL(/\/pedidos\/\d+/);
    const orderId = new URL(page.url()).pathname.split("/").pop();
    const order = await (await page.request.get("/api/account/orders/" + orderId)).json();
    expect(order.status).toBe("CONFIRMADO");
    expect(Number(order.total)).toBe(0);
    expect(Number(order.frete)).toBe(0);
    await admin.goto("/admin/pedidos/" + orderId);
    await expect(admin.getByText("Recompensa do Baú · entrega gratuita")).toBeVisible();
    await admin.getByRole("button", { name: "Iniciar separação" }).click();
    await admin.locator('form[action$="/conferir"] input[type="number"]').fill("1");
    await admin.getByRole("button", { name: "Confirmar conferência" }).click();
    await admin.getByLabel("Transportadora", { exact: true }).fill("Transportadora demo");
    await admin.getByLabel("Código de rastreio", { exact: true }).fill("BAU-DEMO-123");
    await admin.getByLabel("Link HTTPS de rastreamento (opcional)").fill("https://example.com/rastreio");
    await admin.getByRole("button", { name: "Registrar envio" }).click();
    await page.goto("/pedidos/" + orderId);
    await expect(page.getByRole("link", { name: "Rastrear entrega" })).toHaveAttribute("href", "https://example.com/rastreio");
    await page.getByLabel("Mensagem para a loja").fill("Quero confirmar o andamento do meu pedido.");
    await page.getByRole("button", { name: "Enviar mensagem", exact: true }).click();
    await expect(page.getByText("Quero confirmar o andamento do meu pedido.", { exact: true })).toBeVisible();
    await admin.getByLabel("Mensagem", { exact: true }).fill("Observação interna que não pode aparecer ao cliente.");
    await admin.getByRole("button", { name: "Registrar mensagem" }).click();
    await page.reload();
    await expect(page.getByText("Observação interna que não pode aparecer ao cliente.", { exact: true })).toHaveCount(0);
    await page.getByText("Troca ou devolução de produto físico", { exact: true }).click();
    await page.getByLabel("Produto", { exact: true }).selectOption({index:1});
    await page.getByLabel("Motivo", { exact: true }).fill("Produto recebido apresentou divergência de tamanho.");
    await page.getByRole("button", { name: "Enviar solicitação" }).click();
    await expect(page.getByText(/Solicitação #.*Em análise/)).toBeVisible();
    await admin.reload();
    await admin.getByLabel("Resposta ao cliente", { exact: true }).fill("Retorno autorizado para conferência.");
    await admin.getByRole("button", { name: "Atualizar solicitação" }).click();
    await admin.getByLabel("Unidades aptas para voltar ao estoque").fill("1");
    await admin.getByLabel("Resposta ao cliente", { exact: true }).fill("Item recebido e conferido em boas condições.");
    await admin.getByRole("button", { name: "Atualizar solicitação" }).click();
    await page.reload();
    await page.getByText("Troca ou devolução de produto físico", { exact: true }).click();
    await expect(page.getByText(/Solicitação #.*Recebido e conferido/)).toBeVisible();
    await page.screenshot({ path: "test-results/shipping-return-customer.png", fullPage: true });
    await admin.screenshot({ path: "test-results/shipping-return-admin.png", fullPage: true });
    await page.goto("/conta/beneficios");
    await expect(page.getByRole("region", { name: "Itens guardados no Baú" })).toContainText("ENVIADO");
    await page.goto("/conta/vip");
    await expect(page.getByRole("heading", { name: "Meu VIP", exact: true })).toBeVisible();
    await page.screenshot({ path: "test-results/vip-desktop.png", fullPage: true });
  } finally {
    await admin.close();
  }
});
