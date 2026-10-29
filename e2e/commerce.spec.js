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
    if (await input.count()) await input.fill(value);
  }
  await page.screenshot({
    path: "test-results/checkout-desktop.png",
    fullPage: true,
  });
  await page
    .getByRole("button", {
      name: /Ir para pagamento|Criar pedido de demonstração/,
    })
    .click();
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
