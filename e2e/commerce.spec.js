import { test, expect } from "@playwright/test";
test("catalog, private pass, checkout without live charges and profile", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/login");
  let csrf = (await page.context().cookies()).find(
    (c) => c.name === "XSRF-TOKEN",
  )?.value;
  expect(csrf).toBeTruthy();
  const email = "browser@example.test",
    password = "disposable-browser-password";
  const signup = await page.request.post("/auth/cadastro", {
    form: {
      nome: "Cliente navegador",
      email,
      senha: password,
      _csrf: decodeURIComponent(csrf),
    },
    maxRedirects: 0,
  });
  expect(signup.status()).toBe(302);
  const login = await page.request.post("/auth/login", {
    form: { email, senha: password, _csrf: decodeURIComponent(csrf) },
    maxRedirects: 0,
  });
  expect(login.status()).toBe(302);
  await page.evaluate(() => {
    localStorage.setItem("level-commerce-checkout", "legacy-draft");
    localStorage.setItem("level-commerce-checkout-request", "legacy-request");
  });
  await page.goto("/passe");
  await expect(
    page.getByRole("heading", { name: "Minha coleção" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Adquirir premium" }),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/pass-desktop.png",
    fullPage: true,
  });
  const catalog = await (await page.request.get("/api/store/catalog")).json();
  expect(catalog.produtos.length).toBeGreaterThan(0);
  const product =
    catalog.produtos.find((p) => p.nome === "Garrafa Thermal Elite") ||
    catalog.produtos[0];
  await page.goto("/produto/" + product.id);
  await page
    .locator(".lc-detail-actions")
    .getByRole("button", { name: "Comprar agora", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Endereco de entrega" }),
  ).toBeVisible();
  for (const [label, value] of [
    ["Nome completo", "Cliente navegador"],
    ["Telefone", "11999999999"],
    ["CEP", "01001000"],
    ["Endereco", "Rua teste"],
    ["Numero", "10"],
    ["Bairro", "Centro"],
    ["Cidade", "São Paulo"],
    ["Estado", "SP"],
  ]) {
    const input = page.getByLabel(label, { exact: true });
    await input.fill(value);
  }
  await page.screenshot({
    path: "test-results/checkout-desktop.png",
    fullPage: true,
  });
  const checkoutStorage = () =>
    page.evaluate(() => ({
      draft: localStorage.getItem("level-commerce-checkout"),
      request: localStorage.getItem("level-commerce-checkout-request"),
    }));
  await expect.poll(checkoutStorage).toEqual({ draft: null, request: null });

  const checkout = page.getByRole("button", {
    name: /Ir para pagamento|Criar pedido de demonstração/,
  });
  const coupon = page.getByLabel("Cupom da sua coleção");
  await coupon.fill("INVALIDO");
  await checkout.click();
  await expect(page.getByRole("alert")).toHaveText("Cupom não encontrado nesta conta.");
  const stock = async () => {
    const response = await page.request.get("/api/store/products/" + product.id);
    return (await response.json()).estoque;
  };
  expect(await stock()).toBe(product.estoque);
  await coupon.clear();

  // The server accepts the first request, but the browser loses the response.
  const endpoint = "**/api/checkout/orders";
  let originalAttempt;
  await page.route(endpoint, async (route) => {
    originalAttempt = route.request().postDataJSON().idempotencyKey;
    const response = await route.fetch();
    expect(response.ok()).toBeTruthy();
    await route.abort();
  }, { times: 1 });
  await checkout.click();
  await expect(page.getByRole("alert")).toHaveText("Não foi possível iniciar o pagamento agora.");
  expect(await checkoutStorage()).toEqual({ draft: null, request: null });

  const retry = page.waitForRequest(endpoint);
  await checkout.click();
  expect((await retry).postDataJSON().idempotencyKey).toBe(originalAttempt);
  await expect(page).toHaveURL(/\/pedidos\/\d+/);
  await expect(
    page.getByText(/Pagamentos desativados neste ambiente/),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Cancelar pedido", exact: true })
    .click();
  await expect(
    page.getByText("Pedido cancelado com sucesso.", { exact: true }),
  ).toBeVisible();
  expect(await stock()).toBe(product.estoque);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/passe");
  await expect(
    page.getByRole("heading", { name: "Minha coleção" }),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/pass-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  expect(errors).toEqual([]);
});
