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
    page.getByRole("heading", { name: "Passe LEVEL", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Liberar Premium" }),
  ).toBeVisible();
  await expect(page.getByRole("heading", { name: "Minha coleção" })).toHaveCount(0);
  await expect(page.getByRole("region", { name: "Níveis e recompensas do passe" })).toBeVisible();
  await expect(page.getByText("Trilha Premium bloqueada")).toBeVisible();
  await expect(page.locator(".lc-rail")).toHaveCSS("background-color", "rgb(0, 0, 0)");
  await expect(page.getByRole("button", { name: /^Bloqueado · nível 1 ·/ })).toBeDisabled();
  const intro = page.getByRole("complementary", { name: "Como subir de nível" });
  await expect(intro).toContainText("Suba de nível com compras, missões e eventos");
  const showcase = page.getByRole("region", { name: "Vitrine ilustrativa de prêmios" });
  await expect(showcase.getByRole("link")).toHaveCount(10);
  await expect(showcase.getByRole("button")).toHaveCount(0);
  await expect(showcase.locator("img").first()).toHaveCSS("height", "40px");
  const tracks = showcase.locator(".lc-reward-highlights-track");
  await expect(tracks).toHaveCount(3);
  for (const [index, direction] of ["reverse", "normal", "reverse"].entries()) {
    await expect(tracks.nth(index)).toHaveCSS("animation-name", "lc-reward-loop");
    await expect(tracks.nth(index)).toHaveCSS("animation-direction", direction);
  }
  expect(await showcase.evaluate(element => {
    const banner = element.closest("header");
    const frame = element.getBoundingClientRect();
    return banner && frame.left >= banner.querySelector("h1").getBoundingClientRect().right
      && frame.right <= banner.getBoundingClientRect().right;
  })).toBeTruthy();
  const overlay = page.locator(".lc-rail > .lc-premium-overlay");
  await expect(overlay.getByRole("button", { name: "Liberar Premium" })).toBeVisible();
  expect(await overlay.evaluate(element => {
    const layer = element.getBoundingClientRect();
    const premium = element.parentElement.querySelector(".lc-rail-reward.premium").getBoundingClientRect();
    const free = element.parentElement.querySelector(".lc-rail-reward.free").getBoundingClientRect();
    return layer.top === premium.top && layer.bottom === premium.bottom && layer.bottom < free.top;
  })).toBeTruthy();
  await page.emulateMedia({ reducedMotion: "reduce" });
  await expect(tracks.first()).toHaveCSS("animation-name", "none");
  await showcase.getByRole("link").first().click();
  await expect(page).toHaveURL(/#trilha-do-passe$/);
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await expect(page.getByRole("navigation", { name: "Navegação da loja" })
    .getByRole("link", { name: "Missões", exact: true })).toBeVisible();
  await expect(page.getByText(/Faltam \d+ XP para o próximo nível/)).toBeVisible();
  await expect(page.getByText(/Recompensa final ·/)).toHaveCount(0);
  await expect(page.locator(".lc-rail-labels .premium")).toHaveCSS("background-image", /level-premium\.webp/);
  await expect(page.locator(".lc-rail-reward").first()).toHaveCSS("background-image", "none");
  await page.reload();
  await expect(page.getByRole("button", { name: "Liberar Premium" })).toBeVisible();
  await expect(page.locator(".lc-toast-region .lc-toast")).toHaveCount(0);
  await expect(intro).toHaveCount(0);
  const account = await (await page.request.get("/api/auth/me")).json();
  await page.evaluate(id => {
    localStorage.setItem(`level-commerce-pass-guide-${id}`, String(Date.now() - 301_000));
  }, account.id);
  await page.reload();
  await expect(page.getByText(/Faltam \d+ XP para o próximo nível/)).toBeVisible();
  await page.screenshot({
    path: "test-results/pass-desktop.png",
    fullPage: true,
  });
  const navigation = page.getByRole("navigation", { name: "Navegação da loja" });
  await navigation.getByRole("link", { name: "Início", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  const home = page.getByRole("main");
  await expect(home.getByRole("heading", { name: "Passe LEVEL" })).toBeVisible();
  await expect(home.getByRole("heading", { name: "Destaques da loja" })).toBeVisible();
  const sections = await home.locator("section").evaluateAll(sections =>
    sections.map(section => section.getAttribute("aria-label") || section.querySelector("h2")?.textContent));
  expect(sections).toEqual([
    "Campanhas em destaque", "Chamadas promocionais demonstrativas", "Passe LEVEL", "Trilha de recompensas", "Destaques da loja",
    "Campanhas da LEVEL", "Compre por categoria", "Netflix", "Novidades",
  ]);
  await expect(home.getByRole("region", { name: "Níveis e recompensas do passe" })).toBeVisible();
  await expect(navigation.getByRole("link", { name: "Meus pedidos", exact: true })).toHaveCount(0);
  await expect(navigation.getByRole("link", { name: "Eventos", exact: true })).toHaveCount(0);
  await expect(home.getByRole("region", { name: "Destaque do Passe LEVEL" }))
    .toContainText(/Faltam? \d+ XP para desbloquear este prêmio/);
  await expect(home.locator(".lc-product-card").first().locator(".lc-product-copy p")).toBeVisible();
  await expect(home.locator(".lc-product-card").getByRole("button")).toHaveCount(0);
  await expect(home.getByRole("region", { name: "Netflix" })).toContainText("CAMPANHA FICTÍCIA");
  const footer = page.getByRole("contentinfo");
  await expect(footer).toContainText("Endereço e cadastro fictícios");
  await expect(footer.getByRole("link", { name: "Acompanhar pedido" })).toHaveAttribute("href", "/pedidos");
  await expect(home.getByRole("button", { name: "Pausar faixa" })).toHaveCount(0);
  const themeToggle = page.getByRole("button", { name: "Modo escuro" });
  await expect(themeToggle).toHaveAttribute("aria-pressed", "false");
  await page.screenshot({ path: "test-results/home-light-desktop.png" });
  await themeToggle.click();
  await page.reload();
  await expect(themeToggle).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator("html")).toHaveAttribute("data-color-mode", "dark");
  await expect(home.getByRole("heading", { name: "Destaques da loja" })).toBeVisible();
  await page.screenshot({ path: "test-results/home-dark-desktop.png" });
  await themeToggle.click();

  await page.goto("/conta");
  await page.getByRole("link", { name: /Cupons e recompensas/ }).click();
  await expect(page.getByRole("heading", { name: "Cupons e recompensas" })).toBeVisible();
  await expect(page.getByText("Você ainda não tem benefícios resgatados.")).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Cupons e recompensas" })).toBeVisible();

  const catalog = await (await page.request.get("/api/store/catalog")).json();
  expect(catalog.produtos.length).toBeGreaterThan(0);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/catalogo");
  const filterToggle = page.getByRole("button", { name: /^Filtros e ordenação/ });
  const filters = page.getByRole("form", { name: "Filtrar catálogo" });
  await expect(filters).toBeHidden();
  await filterToggle.click();
  await filters.getByLabel("Busca").fill("produto-inexistente-para-filtro");
  await expect(page).toHaveURL(/\/catalogo$/);
  await filters.getByRole("button", { name: "Aplicar filtros" }).click();
  await expect(page.getByText(/Nenhum produto encontrado/)).toBeVisible();
  await expect(filters).toBeHidden();
  await filterToggle.click();
  await filters.getByRole("button", { name: "Limpar", exact: true }).click();
  await expect(page.locator(".lc-product-visual").first()).toHaveCSS("height", "96px");

  const longName = "Produto com título longo " + "SuperModelo".repeat(14);
  await page.route("**/api/store/catalog?*", async route => {
    const data = await (await route.fetch()).json();
    data.produtos[0].nome = longName;
    await route.fulfill({ json: data });
  });
  await page.goto("/catalogo");
  const longTitle = page.getByRole("heading", { name: longName, exact: true });
  await expect(longTitle).toBeVisible();
  expect(await longTitle.evaluate(link => {
    const card = link.closest("article");
    return card.scrollWidth <= card.clientWidth
      && link.getBoundingClientRect().height <= parseFloat(getComputedStyle(link).lineHeight) * 2 + 1;
  })).toBeTruthy();
  await page.unroute("**/api/store/catalog?*");
  await page.setViewportSize({ width: 1440, height: 1000 });
  const product =
    catalog.produtos.find((p) => p.nome === "Garrafa Thermal Elite") ||
    catalog.produtos[0];
  await page.goto("/catalogo");
  const card = page.locator(".lc-product-card").filter({ hasText: product.nome });
  await expect(card.getByRole("button")).toHaveCount(0);
  await card.getByRole("link").click();
  await expect(page).toHaveURL(new RegExp(`/produto/${product.id}$`));
  await page.locator(".lc-detail-actions").getByRole("button", { name: "Comprar agora", exact: true }).click();
  await expect(page).toHaveURL(/\/checkout\?modo=imediato$/);
  const summary = page.getByRole("complementary", { name: "Resumo da compra" });
  await expect(summary).toContainText(product.nome);
  await expect(summary).toContainText("Quantidade 1");
  await expect(summary.locator("img")).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Endereço de entrega" }),
  ).toBeVisible();
  for (const [label, value] of [
    ["Nome completo", "Cliente navegador"],
    ["Telefone", "11999999999"],
    ["CEP", "01001000"],
    ["Endereço", "Rua teste"],
    ["Número", "10"],
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
    page.getByRole("heading", { name: "Passe LEVEL", exact: true }),
  ).toBeVisible();
  await expect(page.getByRole("region", { name: "Níveis e recompensas do passe" })).toBeVisible();
  await page.getByRole("button", { name: "Modo escuro" }).click();
  await page.screenshot({
    path: "test-results/pass-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  const passProducts = page.locator(".lc-pass-products");
  await expect(passProducts.getByRole("heading", { name: "Produtos da loja" })).toBeVisible();
  const shelfCard = passProducts.locator("article").first();
  await expect(shelfCard.getByRole("button")).toHaveCount(0);
  const itemName = await shelfCard.getByRole("heading").innerText();
  await shelfCard.getByRole("link").click();
  await expect(page.getByRole("heading", { name: itemName, level: 1 })).toBeVisible();
  await expect(page.locator(".lc-detail-actions").getByRole("button", { name: "Comprar agora" })).toBeVisible();
  expect(errors).toEqual([]);
});
