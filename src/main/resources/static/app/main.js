import React, {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "https://esm.sh/react@18";
import { createRoot } from "https://esm.sh/react-dom@18/client";
import htm from "https://esm.sh/htm@3.1.1";

import { AuthPage, Brand, EventBadges, PassEvents, Icon, NotificationMenu } from "./experience.js";

const html = htm.bind(React.createElement);
const BRAND = "LEVEL COMMERCE";
const CART_KEY = "level-commerce-cart";
const BUY_NOW_KEY = "level-commerce-buy-now";
const PASS_GUIDE_INTERVAL = 5 * 60 * 1000;
const passGuideSeenAt = new Map();

function shouldShowPassGuide(accountId) {
  if (accountId == null) return false;
  const now = Date.now();
  const key = `level-commerce-pass-guide-${accountId}`;
  let previous = passGuideSeenAt.get(accountId) || 0;
  try { previous = Math.max(previous, Number(localStorage.getItem(key)) || 0); } catch {}
  if (previous <= now && now - previous < PASS_GUIDE_INTERVAL) return false;
  passGuideSeenAt.set(accountId, now);
  try { localStorage.setItem(key, String(now)); } catch {}
  return true;
}

const reviewReminderIntervals = [3, 6, 12].map(hours => hours * 60 * 60 * 1000);
const reviewReminderMemory = new Map();

function takeReviewReminder(accountId, missions, now = Date.now()) {
  let due = false;
  for (const mission of missions) {
    const key = `level-commerce-review-reminder-${accountId}-${mission.id}`;
    const previous = readStorage(key, reviewReminderMemory.get(key), localStorage);
    const current = previous?.period === mission.resetaEm && previous.nextAt > now;
    if (current) continue;

    const step = previous?.period === mission.resetaEm
      ? ((previous.step + 1) % reviewReminderIntervals.length) : 0;
    const state = { period: mission.resetaEm, step, nextAt: now + reviewReminderIntervals[step] };
    reviewReminderMemory.set(key, state);
    try { localStorage.setItem(key, JSON.stringify(state)); } catch {}
    due = true;
  }
  return due;
}

function pendingReviewMissions(dashboard) {
  if (!dashboard) return [];
  return [...dashboard.missoesDiarias, ...dashboard.missoesSemanais, ...dashboard.missoesMensais]
    .filter(mission => mission.acao === "REVIEW_SUBMITTED" && !mission.concluida);
}

function reviewMissionLabel(missions) {
  const xp = missions.filter(mission => mission.progressoAtual + 1 >= mission.metaValor)
    .reduce((total, mission) => total + mission.xpRecompensa, 0);
  return xp ? `Avalie e ganhe ${xp} XP` : "Avalie e avance na missão";
}

function useRoute() {
  const getRoute = () =>
    `${window.location.pathname}${window.location.search}${window.location.hash}`;
  const [route, setRoute] = useState(getRoute());

  useEffect(() => {
    const onChange = () => setRoute(getRoute());
    window.addEventListener("popstate", onChange);
    return () => window.removeEventListener("popstate", onChange);
  }, []);

  return route;
}

function navigate(path) {
  const target = path === "/eventos" ? "/passe#eventos" : path;
  window.history.pushState({}, "", target);
  window.dispatchEvent(new PopStateEvent("popstate"));
  const section = document.getElementById(target.split("#")[1] || "");
  if (section) section.scrollIntoView();
  else window.scrollTo(0, 0);
}

function parseRoute(route) {
  const [path, query = ""] = route.split("#", 1)[0].split("?");
  return { path, params: new URLSearchParams(query) };
}

function parseCatalogFilters(route) {
  const { params } = parseRoute(route);
  return {
    busca: params.get("busca") || "",
    categoriaId: params.get("categoriaId") || "",
    precoMin: params.get("precoMin") || "",
    precoMax: params.get("precoMax") || "",
    ordenacao: params.get("ordenacao") || "relevancia",
  };
}

function readStorage(key, fallback, storage = localStorage) {
  try {
    return JSON.parse(storage.getItem(key) || JSON.stringify(fallback));
  } catch {
    return fallback;
  }
}

function getCookie(name) {
  return document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${name}=`))
    ?.split("=")[1];
}

function getCsrfToken() {
  return decodeURIComponent(getCookie("XSRF-TOKEN") || "");
}

function initialCheckoutForm() {
  return {
    nome: "",
    telefone: "",
    cep: "",
    endereco: "",
    numero: "",
    complemento: "",
    bairro: "",
    cidade: "",
    estado: "",
    pagamento: "pix",
    cupom: "",
  };
}

function cartItem(product, quantity) {
  return {
    id: product.id,
    nome: product.nome,
    preco: product.preco,
    imagemUrl: product.imagemUrl,
    quantity,
  };
}

function useLocalCart() {
  const [items, setItems] = useState(() => readStorage(CART_KEY, []));

  useEffect(() => {
    localStorage.setItem(CART_KEY, JSON.stringify(items));
  }, [items]);

  const addItem = (product, quantity = 1) => {
    setItems((current) => {
      const existing = current.find((item) => item.id === product.id);
      if (existing) {
        return current.map((item) =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + quantity }
            : item,
        );
      }
      return [...current, cartItem(product, quantity)];
    });
  };

  const updateQuantity = (productId, nextQuantity) => {
    setItems((current) => {
      if (nextQuantity <= 0) {
        return current.filter((item) => item.id !== productId);
      }
      return current.map((item) =>
        item.id === productId ? { ...item, quantity: nextQuantity } : item,
      );
    });
  };

  const removeItem = (productId) => {
    setItems((current) => current.filter((item) => item.id !== productId));
  };

  const restoreItem = (item, index) => {
    setItems(current => current.some(entry => entry.id === item.id)
      ? current : [...current.slice(0, index), item, ...current.slice(index)]);
  };

  const clearCart = () => {
    localStorage.removeItem(CART_KEY);
    setItems([]);
  };

  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
  return {
    items,
    totalItems,
    addItem,
    updateQuantity,
    removeItem,
    restoreItem,
    clearCart,
  };
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });

  if (response.status === 401) {
    const error = new Error("AUTH_REQUIRED");
    error.code = "AUTH_REQUIRED";
    throw error;
  }

  if (!response.ok) {
    const body = await response.text();
    let message = "Não foi possível concluir a solicitação.";
    try {
      message = JSON.parse(body).message || message;
    } catch {}
    const error = new Error(message || "Falha ao processar a solicitacao.");
    error.code = "REQUEST_FAILED";
    error.status = response.status;
    throw error;
  }

  if (response.status === 204 || response.headers.get("content-length") === "0")
    return null;
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

function getJson(url) {
  return requestJson(url, { method: "GET" });
}

function postJson(url, body) {
  return requestJson(url, {
    method: "POST",
    headers: { "X-XSRF-TOKEN": getCsrfToken() },
    body: JSON.stringify(body),
  });
}

function putJson(url, body) {
  return requestJson(url, {
    method: "PUT",
    headers: { "X-XSRF-TOKEN": getCsrfToken() },
    body: JSON.stringify(body),
  });
}

function trackPassAction(acao, valor = 1, referencia = "") {
  return postJson("/api/pass/actions", {
    acao,
    valor,
    referencia,
  }).then(() => window.dispatchEvent(new Event("level-pass-action"))).catch(() => null);
}

function claimPassReward(rewardId, produtoId) {
  return postJson(`/api/pass/rewards/${rewardId}/claim`, { produtoId });
}

function currency(value) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(Number(value || 0));
}

function orderStatusLabel(status) {
  const labels = {
    PENDENTE: "Pendente",
    CONFIRMADO: "Confirmado",
    ENVIADO: "Enviado",
    ENTREGUE: "Entregue",
    CANCELADO: "Cancelado",
  };
  return labels[status] || status;
}

function paymentStatusLabel(status) {
  const labels = {
    PENDENTE: "Pagamento pendente",
    APROVADO: "Pagamento aprovado",
    RECUSADO: "Pagamento recusado",
    CANCELADO: "Pagamento cancelado",
    REEMBOLSADO: "Pagamento reembolsado",
    SEM_PAGAMENTO: "Pagamento nao iniciado",
  };
  return labels[status] || status;
}

function paymentMethodLabel(method) {
  const labels = {
    PIX: "Pix",
    BOLETO: "Boleto",
    CARTAO_CREDITO: "Cartao de credito",
    CARTAO_DEBITO: "Cartao de debito",
  };
  return labels[method] || "Nao definido";
}

function buildLoginUrl(
  returnUrl = window.location.pathname + window.location.search,
) {
  return `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
}

function requireLogin(
  navigateTo = window.location.pathname + window.location.search,
) {
  navigate(buildLoginUrl(navigateTo));
}

function AppLink({ href, className = "", current, children }) {
  return html`
    <a
      href=${href}
      className=${className}
      aria-current=${current ? "page" : undefined}
      onClick=${(event) => {
        if (href.startsWith("/") && !event.metaKey && !event.ctrlKey
            && !event.shiftKey && !event.altKey && event.button === 0) {
          event.preventDefault();
          navigate(href);
        }
      }}
    >
      ${children}
    </a>
  `;
}

function NavBar({ totalItems, onSearch, auth, categories, notificationProps, route, dark, toggleTheme, address }) {
  const { path } = parseRoute(route);
  const [search, setSearch] = useState(() => parseCatalogFilters(location.pathname + location.search).busca || "");
  const account = useRef(null);
  const drawer = useRef(null);
  const closeAccount = () => { if (account.current) account.current.open = false; };
  const closeDrawer = () => { if (drawer.current?.open) drawer.current.close(); };
  useEffect(() => { closeAccount(); closeDrawer(); }, [route]);
  useEffect(() => {
    const mobile = matchMedia("(max-width: 800px)");
    const onResize = () => { if (!mobile.matches) closeDrawer(); };
    const onOutside = event => { if (!account.current?.contains(event.target)) closeAccount(); };
    mobile.addEventListener("change", onResize);
    document.addEventListener("pointerdown", onOutside);
    return () => {
      mobile.removeEventListener("change", onResize);
      document.removeEventListener("pointerdown", onOutside);
    };
  }, []);
  const addressLink = html`<${AppLink} href=${auth.authenticated ? "/conta/endereco" : buildLoginUrl("/conta/endereco")} className="lc-nav-address">
    <${Icon} name="pin" />
    <span><small>Entregar em</small><strong>${address ? `${address.cidade} · ${formatCep(address.cep)}` : "Adicionar endereço"}</strong></span>
  <//>`;
  const links = [["/", "Início"], ["/catalogo", "Catálogo"], ["/passe", "Passe mensal"], ["/passe/missoes", "Missões"]];
  const adminLink = auth.role === "ADMIN" || (!auth.authenticated && auth.ambienteLocal)
    ? html`<a className="lc-admin-link" href=${auth.authenticated ? "/admin" : buildLoginUrl("/admin")}>Administração</a>` : null;

  return html`
    <header className="lc-nav-shell">
      <div className="lc-nav-top">
        <div className="lc-nav-brand-area">
          <button type="button" className="lc-menu-toggle" aria-label="Abrir menu da loja" aria-haspopup="dialog"
            onClick=${() => { if (!drawer.current.open) drawer.current.showModal(); }}><${Icon} name="menu" /></button>
          <${Brand} />
        </div>
        <form className="lc-search" role="search" onSubmit=${(event) => {
          event.preventDefault();
          onSearch(search.trim());
        }}>
          <input type="search" aria-label="Buscar produtos" value=${search}
            onChange=${(event) => setSearch(event.target.value)}
            placeholder="Buscar produtos, marcas e categorias" />
          <button type="submit" aria-label="Buscar"><${Icon} name="search" /><span>Buscar</span></button>
        </form>
        <nav className="lc-nav-tools" aria-label="Sua conta e compras">
          <button className="lc-theme-toggle" type="button" aria-label="Modo escuro"
            aria-pressed=${dark} title=${dark ? "Ativar tema claro" : "Ativar tema escuro"} onClick=${toggleTheme}>
            <${Icon} name=${dark ? "sun" : "moon"} />
          </button>
          <details className="lc-nav-popover lc-account-popover" ref=${account} onKeyDown=${event => {
            if (event.key !== "Escape" || !account.current.open) return;
            closeAccount();
            account.current.querySelector("summary").focus();
          }}>
            <summary aria-label="Minha conta"><${Icon} name="user" /><span>${auth.authenticated ? "Minha conta" : "Entrar"}</span></summary>
            <nav className="lc-popover-panel" aria-label="Menu da conta" onClick=${event => {
              if (event.target.closest("a")) closeAccount();
            }}>
              ${auth.authenticated ? html`
                <${AppLink} href="/perfil">Meu perfil<//><${AppLink} href="/conta/vip">Meu VIP<//>
                <${AppLink} href="/pedidos">Meus pedidos<//>
                <${AppLink} href="/conta/beneficios">Meu Baú<//>
                <${AppLink} href="/conta">Gerenciar conta<//>
                <form action="/auth/logout" method="post">
                  <input type="hidden" name="_csrf" value=${getCsrfToken()} />
                  <button type="submit">Sair</button>
                </form>` : html`<${AppLink} href="/login">Entrar<//><${AppLink} href="/cadastro">Criar conta<//>`}
            </nav>
          </details>
          ${auth.authenticated ? html`<${NotificationMenu} ...${notificationProps} />` : null}
          <${AppLink} href="/sacola" className="lc-nav-tool lc-nav-bag">
            <${Icon} name="bag" /><span>Sacola</span><strong>${totalItems}</strong>
          <//>
        </nav>
      </div>
      <nav className="lc-nav-bottom" aria-label="Navegação da loja">
        ${addressLink}
        <${AppLink} href="/" current=${path === "/"}>Início<//>
        <details className="lc-nav-popover lc-categories-menu">
          <summary><${Icon} name="menu" /> Categorias</summary>
          <div className="lc-popover-panel">
            <a href="/catalogo">Ver todos os produtos</a>
            ${categories.map(category => html`<a key=${category.id} href=${`/catalogo?categoriaId=${category.id}`}>${category.nome}</a>`)}
          </div>
        </details>
        ${links.slice(1).map(([href, label]) => html`<${AppLink} key=${href} href=${href} current=${path === href}>${label}<//>`)}
        ${adminLink}
      </nav>
      <nav className="lc-mobile-address" aria-label="Navegação da loja">${addressLink}</nav>
      <dialog className="lc-mobile-drawer" ref=${drawer} aria-labelledby="drawer-title"
        onClick=${event => { if (event.target === drawer.current) closeDrawer(); }}>
        <div className="lc-mobile-drawer-inner">
          <header><strong id="drawer-title">Menu da loja</strong>
            <button type="button" autoFocus aria-label="Fechar menu" onClick=${closeDrawer}>×</button></header>
          <nav aria-label="Menu lateral" onClick=${event => { if (event.target.closest("a")) closeDrawer(); }}>
            ${links.map(([href, label]) => html`<${AppLink} key=${href} href=${href} current=${path === href}>${label}<//>`)}
            ${adminLink}
            ${categories.length ? html`<details><summary>Categorias</summary>
              ${categories.map(category => html`<${AppLink} key=${category.id}
                href=${`/catalogo?categoriaId=${category.id}`}>${category.nome}<//>`)}
            </details>` : null}
          </nav>
        </div>
      </dialog>
    </header>
  `;
}

function SectionHeader({
  title,
  description,
  actionHref = "/catalogo",
  actionLabel = "Ver mais",
}) {
  return html`
    <div className="lc-section-header">
      <div>
        <h2>${title}</h2>
        ${description ? html`<p>${description}</p>` : null}
      </div>
      <${AppLink} href=${actionHref}>${actionLabel}<//>
    </div>
  `;
}

function BannerCarousel({ banners }) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [focused, setFocused] = useState(false);

  useEffect(() => {
    if (banners.length < 2 || focused || matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const interval = window.setInterval(() => setActiveIndex(index => (index + 1) % banners.length), 5500);
    return () => window.clearInterval(interval);
  }, [banners.length, focused]);

  const banner = banners[activeIndex];
  if (!banner) return null;

  return html`
    <section className="lc-hero-full" aria-label="Campanhas em destaque"
      onFocus=${() => setFocused(true)}
      onBlur=${(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false); }}>
      <img key=${banner.imagemUrl} src=${banner.imagemUrl} alt="" fetchPriority="high"
        onError=${event => { event.currentTarget.hidden = true; }} />
      <div className="lc-hero-inner">
        <span className="lc-eyebrow">${banner.destaque}</span>
        <h1>${banner.titulo}</h1>
        <p>${banner.subtitulo}</p>
        <${AppLink} href=${banner.ctaHref} className="lc-btn lc-neon-btn">${banner.ctaLabel}<//>
        <div className="lc-hero-dots">
          ${banners.map((item, index) => html`
            <button key=${item.titulo} type="button"
              aria-label=${`Mostrar destaque ${index + 1}: ${item.titulo}`}
              aria-pressed=${index === activeIndex}
              onClick=${() => setActiveIndex(index)}></button>
          `)}
        </div>
      </div>
    </section>`;
}

function PromoTicker() {
  const headlines = ["Compre agora!", "Desconto de até 50%!", "Ganhe XP nas suas compras", "Confira os eventos da semana"];
  return html`<section className="lc-ticker" aria-label="Chamadas promocionais demonstrativas">
    <span className="lc-ticker-label">DEMO</span>
    <div className="lc-ticker-window">
      <div className="lc-ticker-track">
        ${[false, true].map(copy => html`<ul key=${String(copy)} aria-hidden=${copy}>
          ${headlines.map(text => html`<li key=${text}>${text}</li>`)}
        </ul>`)}
      </div>
    </div>
  </section>`;
}

function usePassOverview(auth) {
  const [state, setState] = useState(null);
  const account = auth.authenticated ? auth.id : "visitor";

  useEffect(() => {
    if (!auth.ready) return;
    let active = true;
    const request = auth.authenticated
      ? trackPassAction("CHECKIN").then(() => getJson("/api/pass/current"))
      : getJson("/api/pass/preview");
    request.then(data => {
      if (active) setState({ account, data, error: "" });
    }).catch(() => {
      if (active) setState({ account, data: null, error: "Não foi possível carregar o passe." });
    });
    return () => { active = false; };
  }, [auth.ready, auth.authenticated, account]);

  const data = state?.account === account ? state.data : null;
  return {
    pass: auth.authenticated ? data : null,
    levels: auth.authenticated ? data?.niveis : data,
    error: state?.account === account ? state.error : "",
    updatePass: update => setState(current => ({ ...current, data: update(current.data) })),
  };
}

function CategoryStrip({ categories }) {
  return html`<nav className="lc-categories" aria-label="Categorias de produtos">
    ${categories.map(category => html`<${AppLink} key=${category.id}
      href=${`/catalogo?categoriaId=${category.id}`} className="lc-category-pill">
      <strong>${category.nome}</strong><span aria-hidden="true">→</span>
    <//>`)}
  </nav>`;
}

function ProductCard({ product, href = `/produto/${product.id}`, badge }) {
  return html`<article className="lc-product-card lc-shelf-card">
    <${AppLink} href=${href} className="lc-product-card-link">
      <div className="lc-product-visual">
        <img src=${product.imagemUrl || "/images/sem-foto.png"} alt="" loading="lazy" decoding="async"
          onError=${event => {
            if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
          }} />
      </div>
      <div className="lc-product-copy">
        ${badge ? html`<span className="lc-mission-product-tag">${badge}</span>` : null}
        <h3>${product.nome}</h3>
        <p>${product.descricaoCurta?.trim() || "Veja os detalhes do produto."}</p>
        <strong className="lc-card-price">${currency(product.preco)}</strong>
      </div>
    <//>
  </article>`;
}

function HomeProductShelf({ title, products }) {
  if (!products.length) return null;
  return html`<section className="lc-section lc-home-shelf">
    <${SectionHeader} title=${title} actionLabel="Ver catálogo" />
    <div className="lc-home-products">
      ${products.slice(0, 6).map(product => html`<${ProductCard} key=${product.id} product=${product} />`)}
    </div>
  </section>`;
}

function HomePage({ data, auth, notifyGain }) {
  const overview = usePassOverview(auth);
  const featured = [...data.destaques, ...data.novidades].find(product => product.estoque > 0);
  const audio = data.categorias.find(category => /[aá]udio/i.test(category.nome));
  const entertainmentHref = audio ? `/catalogo?categoriaId=${audio.id}` : "/catalogo";
  return html`
    <main className="lc-home">
      <${BannerCarousel} banners=${data.banners} />
      <${PromoTicker} />
      <div className="lc-page lc-home-content">
        <${RewardTrail} auth=${auth} overview=${overview} notifyGain=${notifyGain} />
        <${HomeProductShelf} title="Destaques da loja" products=${data.destaques} />
        <section className="lc-home-campaigns" aria-label="Campanhas da LEVEL">
          <${AppLink} href=${featured ? `/produto/${featured.id}` : "/catalogo"} className="lc-home-campaign product">
            <div><span>DESTAQUE DA LOJA</span>
              <strong>${featured?.nome || "Seu próximo upgrade"}</strong>
              <small>${featured ? `${currency(featured.preco)} · Ver produto →` : "Explorar catálogo →"}</small>
            </div>
            ${featured?.imagemUrl ? html`<img src=${featured.imagemUrl} alt="" loading="lazy"
              onError=${event => { event.currentTarget.hidden = true; }} />` : null}
          <//>
          <a href="#campanha-netflix" className="lc-home-campaign entertainment">
            <div><span>ENTRETENIMENTO</span><strong>Noite de cinema</strong>
              <small>Conheça a campanha demonstrativa →</small></div>
            <span className="lc-campaign-play" aria-hidden="true">▶</span>
          </a>
        </section>
        <section className="lc-section lc-home-categories">
          <${SectionHeader} title="Compre por categoria" actionLabel="Todas as categorias" />
          <${CategoryStrip} categories=${data.categorias} />
        </section>
      </div>
      <section id="campanha-netflix" className="lc-hero-full lc-netflix-banner" aria-labelledby="netflix-heading">
        <div className="lc-hero-inner">
          <span className="lc-eyebrow">CAMPANHA FICTÍCIA</span>
          <h2 id="netflix-heading">Netflix</h2>
          <p>Prepare o som e escolha o filme. Explore produtos para a sua próxima sessão em casa.</p>
          <${AppLink} href=${entertainmentHref} className="lc-btn lc-netflix-action">Ver produtos para a sessão<//>
        </div>
        <div className="lc-netflix-art" aria-hidden="true"><span>▶</span></div>
      </section>
      <div className="lc-page lc-home-content">
        <${HomeProductShelf} title="Novidades" products=${data.novidades} />
      </div>
    </main>
  `;
}

function AccountPage({ auth }) {
  const options = auth.authenticated ? [
    ["/pedidos", "Meus pedidos", "Acompanhe pagamentos, envios e entregas."],
    ["/conta/endereco", "Endereço de entrega", "Gerencie o endereço usado nas suas compras."],
    ["/perfil", "Dados da conta", "Atualize seu nome e sua senha."],
    ["/passe", "Passe LEVEL", "Acompanhe sua temporada, seus níveis e missões."],
    ["/conta/beneficios", "Cupons e recompensas", "Veja e use os benefícios que você resgatou."],
  ] : [
    ["/login", "Entrar", "Acesse seus pedidos e dados da conta."],
    ["/cadastro", "Criar conta", "Cadastre-se para acompanhar compras e benefícios."],
    ["/catalogo", "Explorar catálogo", "Continue navegando pela loja."],
  ];

  return html`<main className="lc-page lc-account-page">
    <header className="lc-store-heading">
      <h1>Minha conta</h1>
      ${auth.authenticated ? html`<p>${auth.nome} · ${auth.email}</p>` : null}
    </header>
    <nav className="lc-account-menu" aria-label="Opções da conta">
      ${options.map(([href, title, description]) => html`
        <${AppLink} key=${href} href=${href} className="lc-account-row">
          <span><strong>${title}</strong><small>${description}</small></span><span aria-hidden="true">›</span>
        <//>`)}
    </nav>
    ${auth.authenticated ? html`<form action="/auth/logout" method="post" className="lc-account-exit">
      <input type="hidden" name="_csrf" value=${getCsrfToken()} />
      <button type="submit" className="lc-btn lc-btn-ghost">Sair da conta</button>
    </form>` : null}
  </main>`;
}

const addressFields = [
  ["cep", "CEP", 9, "postal-code"], ["endereco", "Rua ou avenida", 120, "address-line1"],
  ["numero", "Número", 20], ["complemento", "Complemento (opcional)", 80, "address-line2"],
  ["bairro", "Bairro", 80], ["cidade", "Cidade", 80, "address-level2"], ["estado", "UF", 2, "address-level1"],
];

function AddressPage({ address, onSaved, notify, backHref = "/conta" }) {
  const [form, setForm] = useState(() => ({ ...address }));
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const values = editing ? form : (address || {});

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const saved = await putJson("/api/account/address", values);
      onSaved(saved);
      setEditing(false);
      notify("Endereço salvo", `${saved.cidade} · ${formatCep(saved.cep)}`);
    } catch (failure) {
      if (failure.code === "AUTH_REQUIRED") requireLogin("/conta/endereco");
      else setError(failure.message || "Não foi possível salvar o endereço.");
    } finally {
      setSaving(false);
    }
  }

  return html`<main className="lc-page lc-account-page">
    <header className="lc-store-heading">
      <${AppLink} href=${backHref}>${backHref === "/sacola" ? "‹ Minha sacola" : "‹ Minha conta"}<//>
      <h1>Endereço de entrega</h1>
      <p>Salve um endereço padrão para usar nas suas compras.</p>
    </header>
    <form className="lc-checkout-card lc-address-form" onSubmit=${save}>
      <div className="lc-form-grid two">
        ${addressFields.map(([field, label, max, autocomplete]) => html`
          <label key=${field}>${label}<input value=${values[field] || ""} maxLength=${max}
            autoComplete=${autocomplete} required=${field !== "complemento"}
            inputMode=${field === "cep" ? "numeric" : undefined}
            pattern=${field === "cep" ? "[0-9]{5}-?[0-9]{3}" : undefined}
            onChange=${event => {
              setEditing(true);
              setForm({ ...values, [field]: event.target.value });
            }} /></label>`)}
      </div>
      ${error ? html`<p role="alert" className="lc-form-notice error">${error}</p>` : null}
      <button type="submit" className="lc-btn lc-btn-primary" disabled=${saving}>
        ${saving ? "Salvando…" : "Salvar endereço"}
      </button>
    </form>
  </main>`;
}

function formatCep(cep) {
  return cep.replace(/^(\d{5})(\d{3})$/, "$1-$2");
}

function CatalogFilters({ categories, initialFilters, onApply }) {
  const [filters, setFilters] = useState(initialFilters);
  const [expanded, setExpanded] = useState(false);
  const change = (field, value) => setFilters(current => ({ ...current, [field]: value }));
  const activeCount = [initialFilters.busca, initialFilters.categoriaId, initialFilters.precoMin,
    initialFilters.precoMax, initialFilters.ordenacao !== "relevancia"].filter(Boolean).length;

  return html`<aside className="lc-catalog-filters" data-expanded=${expanded}>
    <button type="button" className="lc-btn lc-btn-ghost lc-filter-toggle" aria-expanded=${expanded}
      aria-controls="catalog-filters" onClick=${() => setExpanded(!expanded)}>
      Filtros e ordenação${activeCount ? ` (${activeCount})` : ""}
    </button>
    <form id="catalog-filters" className="lc-filter-card" aria-label="Filtrar catálogo" onSubmit=${event => {
      event.preventDefault();
      onApply(filters);
      setExpanded(false);
    }}>
      <h2>Filtros</h2>
      <label>Busca
        <input value=${filters.busca} onChange=${event => change("busca", event.target.value)}
          placeholder="Nome do produto" />
      </label>
      <label>Categoria
        <select value=${filters.categoriaId} onChange=${event => change("categoriaId", event.target.value)}>
          <option value="">Todas</option>
          ${categories.map(category => html`<option key=${category.id} value=${category.id}>${category.nome}</option>`)}
        </select>
      </label>
      <div className="lc-filter-prices">
        <label>Preço mínimo
          <input type="number" min="0" step="0.01" value=${filters.precoMin}
            onChange=${event => change("precoMin", event.target.value)} />
        </label>
        <label>Preço máximo
          <input type="number" min="0" step="0.01" value=${filters.precoMax}
            onChange=${event => change("precoMax", event.target.value)} />
        </label>
      </div>
      <label>Ordenar por
        <select value=${filters.ordenacao} onChange=${event => change("ordenacao", event.target.value)}>
          <option value="relevancia">Relevância</option>
          <option value="preco-asc">Menor preço</option>
          <option value="preco-desc">Maior preço</option>
          <option value="recentes">Mais recentes</option>
          <option value="nome-desc">Nome Z–A</option>
        </select>
      </label>
      <div className="lc-filter-actions">
        <button type="submit" className="lc-btn lc-btn-primary">Aplicar filtros</button>
        <button type="button" className="lc-btn lc-btn-ghost" onClick=${() => {
          const empty = parseCatalogFilters("/catalogo");
          setFilters(empty);
          onApply(empty);
          setExpanded(false);
        }}>Limpar</button>
      </div>
    </form>
  </aside>`;
}

function CatalogPage({ route }) {
  const [data, setData] = useState({ categorias: [], produtos: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const filters = useMemo(() => parseCatalogFilters(route), [route]);

  useEffect(() => {
    let active = true;
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([field, value]) => { if (value) params.set(field, value); });
    setLoading(true);
    setError(false);
    getJson(`/api/store/catalog?${params.toString()}`)
      .then(result => { if (active) setData(result); })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filters]);

  const updateFilters = (nextFilters) => {
    const params = new URLSearchParams();
    Object.entries(nextFilters).forEach(([field, value]) => {
      if (value && !(field === "ordenacao" && value === "relevancia")) params.set(field, value);
    });
    navigate(`/catalogo${params.size ? `?${params}` : ""}`);
  };

  return html`<main className="lc-page lc-catalog-page">
    <header className="lc-store-heading lc-catalog-heading">
      <h1>Catálogo</h1>
      <p role="status">${loading ? "Atualizando produtos…" : !error && `${data.total} ${data.total === 1 ? "produto encontrado" : "produtos encontrados"}`}</p>
    </header>
    <section className="lc-catalog-shell">
      <${CatalogFilters} key=${route} categories=${data.categorias} initialFilters=${filters} onApply=${updateFilters} />
      <div className="lc-catalog-results" aria-busy=${loading}>
        ${error ? html`<p role="alert" className="lc-form-notice error">Não foi possível carregar os produtos.</p>`
          : !loading && !data.produtos.length ? html`<p className="lc-catalog-empty">Nenhum produto encontrado. Ajuste ou limpe os filtros para continuar.</p>`
          : html`<div className="lc-product-grid">${data.produtos.map(product => html`
              <${ProductCard} key=${product.id} product=${product} />`)}
            </div>`}
      </div>
    </section>
  </main>`;
}

function ProductDetailPage({ productId, onAddToCart, onBuyNow, auth, notify, reviewRequested, address }) {
  const [product, setProduct] = useState(null);
  const [reviewForm, setReviewForm] = useState({ nota: "5", comentario: "" });
  const [reviewMessage, setReviewMessage] = useState("");
  const [reviewMissions, setReviewMissions] = useState([]);
  const [submittingReview, setSubmittingReview] = useState(false);
  const reviewSection = useRef(null);
  const reviewSubmitted = useRef(false);

  useEffect(() => {
    let active = true;
    getJson(`/api/store/products/${productId}`).then(data => { if (active) setProduct(data); })
      .catch(() => { if (active) setProduct(false); });
    return () => { active = false; };
  }, [productId]);

  useEffect(() => {
    if (!auth.authenticated || !product) return;
    trackPassAction("VIEW_PRODUCT", 1, `product-${productId}`);
  }, [auth.authenticated, product?.id, productId]);

  useEffect(() => {
    if (!auth.authenticated || !product) return;
    let active = true;
    getJson(`/api/pass/missions/review/${productId}`).then(missions => {
      if (!active || reviewSubmitted.current) return;
      setReviewMissions(missions);
      if (!reviewRequested && takeReviewReminder(auth.id, missions)) {
        notify("Missão de avaliação", reviewMissionLabel(missions),
          false, "mission", `/produto/${productId}?avaliar=1`);
      }
    }).catch(() => {});
    return () => { active = false; };
  }, [auth.authenticated, auth.id, product?.id, productId, reviewRequested]);

  useEffect(() => {
    if (reviewRequested && reviewSection.current) {
      reviewSection.current.focus({ preventScroll: true });
      reviewSection.current.scrollIntoView({ block: "start" });
    }
  }, [reviewRequested, product?.id]);

  const submitReview = async (event) => {
    event.preventDefault();
    setReviewMessage("");
    setSubmittingReview(true);
    try {
      const response = await postJson(
        `/api/store/products/${productId}/reviews`,
        {
          nota: Number(reviewForm.nota),
          comentario: reviewForm.comentario,
        },
      );
      reviewSubmitted.current = true;
      setProduct(response);
      setReviewMissions([]);
      setReviewForm({ nota: "5", comentario: "" });
      notify("Avaliação enviada");
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin(`/produto/${productId}`);
        return;
      }
      setReviewMessage(error.message || "Não foi possível enviar a avaliação.");
    } finally {
      setSubmittingReview(false);
    }
  };

  if (!product)
    return html`<main className="lc-page"><p role=${product === false ? "alert" : "status"}>
      ${product === false ? "Não foi possível carregar o produto." : "Carregando produto…"}
    </p></main>`;

  return html`
    <main className="lc-page lc-product-page">
      <nav className="lc-breadcrumb" aria-label="Onde você está">
        <${AppLink} href="/">Início<//><span>›</span><${AppLink} href="/catalogo">Catálogo<//>
      </nav>
      <section className="lc-detail-shell">
        <div className="lc-detail-visual">
          <img
            src=${product.imagemUrl || "/images/sem-foto.png"}
            alt=${product.nome}
            onError=${event => {
              if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
            }}
          />
        </div>
        <div className="lc-detail-copy">
          <div className="lc-detail-meta">
            <span>${product.marca || BRAND}</span>
            ${product.sku ? html`<span>SKU ${product.sku}</span>` : null}
            ${product.destaque ? html`<span className="lc-detail-featured">Destaque</span>` : null}
          </div>
          <h1>${product.nome}</h1><p>${product.tipo === "DIGITAL" ? "Produto digital" : "Produto físico"}</p>
          <div className="lc-review-summary">
            ${product.totalAvaliacoes
              ? html`<strong>★ ${Number(product.mediaAvaliacoes || 0).toLocaleString("pt-BR", { minimumFractionDigits: 1 })}</strong>`
              : null}
            <a href="#avaliacoes-do-produto">${product.totalAvaliacoes
              ? `${product.totalAvaliacoes} ${product.totalAvaliacoes === 1 ? "avaliação" : "avaliações"}`
              : "Ainda sem avaliações"}</a>
          </div>
          <p className="lc-detail-price">${currency(product.preco)}</p>
          ${product.descricaoCurta ? html`<p className="lc-detail-short">${product.descricaoCurta}</p>` : null}
          <p className="lc-detail-stock" data-available=${product.estoque > 0}>
            ${product.estoque > 0 ? "Em estoque" : "Produto indisponível"}
          </p>
          <div className="lc-detail-actions">
            <button
              className="lc-btn lc-btn-primary"
              disabled=${product.estoque <= 0}
              onClick=${() => onBuyNow(product)}
            >
              Comprar agora
            </button>
            <button
              className="lc-btn lc-btn-ghost"
              disabled=${product.estoque <= 0}
              onClick=${() => onAddToCart(product)}
            >
              Adicionar à sacola
            </button>
          </div>
          <p className="lc-detail-delivery"><${Icon} name="pin" /> Entregar em
            <${AppLink} href=${auth.authenticated ? "/conta/endereco" : buildLoginUrl("/conta/endereco")}>
              ${address ? `${address.cidade} · ${formatCep(address.cep)}` : "Adicionar endereço"}
            <//>
          </p>
        </div>
      </section>

      <section className="lc-section lc-product-description">
        <h2>Descrição do produto</h2>
        <p>${product.descricao || "Descrição detalhada em breve."}</p>
      </section>

      <section id="avaliacoes-do-produto" className="lc-section lc-review-grid">
        <div className="lc-checkout-card">
          <h3>Avaliações de clientes</h3>
          <div className="lc-pass-history">
            ${
              product.avaliacoes?.length
                ? product.avaliacoes.map(
                    (review, index) => html`
                      <div key=${index} className="lc-review-card">
                        <div className="lc-review-card-top">
                          <strong>${review.autor}</strong>
                          <span
                            >${"★".repeat(review.nota)}${"☆".repeat(5 - review.nota)}</span
                          >
                        </div>
                        <p>
                          ${review.comentario || "Cliente avaliou sem comentario adicional."}
                        </p>
                        <small>${review.criadoEm}</small>
                      </div>
                    `,
                  )
                : html`<p className="lc-checkout-note">
                    Ainda nao ha avaliacoes para este produto.
                  </p>`
            }
          </div>
        </div>
        <div className="lc-checkout-card lc-review-form" ref=${reviewSection} tabIndex="-1">
          <h3>Avaliar produto</h3>
          ${reviewMissions.length ? html`<p className="lc-mission-product-tag">${reviewMissionLabel(reviewMissions)}</p>` : null}
          ${
            auth.authenticated
              ? html`
                  <form onSubmit=${submitReview}>
                    <div className="lc-form-grid two">
                      <label>
                        Nota
                        <select
                          value=${reviewForm.nota}
                          onChange=${(e) => setReviewForm({ ...reviewForm, nota: e.target.value })}
                        >
                          <option value="5">5 estrelas</option>
                          <option value="4">4 estrelas</option>
                          <option value="3">3 estrelas</option>
                          <option value="2">2 estrelas</option>
                          <option value="1">1 estrela</option>
                        </select>
                      </label>
                      <label>
                        Comentario
                        <input
                          value=${reviewForm.comentario}
                          onChange=${(e) => setReviewForm({ ...reviewForm, comentario: e.target.value })}
                          placeholder="O que achou do produto?"
                        />
                      </label>
                    </div>
                    ${reviewMessage ? html`<p className="lc-checkout-note" role="alert">${reviewMessage}</p>` : null}
                    <button type="submit" className="lc-btn lc-btn-primary" disabled=${submittingReview}>
                      Enviar avaliação
                    </button>
                  </form>
                `
              : html`
                  <p className="lc-checkout-note">
                    Entre na sua conta para avaliar este produto e ganhar
                    progresso no passe.
                  </p>
                  <${AppLink}
                    href=${buildLoginUrl(`/produto/${productId}`)}
                    className="lc-btn lc-btn-primary"
                    >Entrar para avaliar<//
                  >
                `
          }
        </div>
      </section>
      <section className="lc-section">
        <${SectionHeader}
          title="Produtos relacionados"

        />
        <div className="lc-product-grid compact">
          ${product.relacionados.map(
            (related) => html`
              <${ProductCard}
                key=${related.id}
                product=${related}
              />
            `,
          )}
        </div>
      </section>

    </main>
  `;
}

function useCheckoutQuote(items, coupon) {
  const body = JSON.stringify({
    itens: items.map(item => ({ produtoId: item.id, quantidade: item.quantity })),
    cupom: coupon.trim(),
  });
  const [state, setState] = useState(null);
  const hasItems = items.length > 0;

  useEffect(() => {
    if (!hasItems) return;
    let active = true;
    const timer = window.setTimeout(() => {
      postJson("/api/checkout/quote", JSON.parse(body))
        .then(quote => { if (active) setState({ body, quote }); })
        .catch(error => { if (active) setState({ body, error: error.message || "Não foi possível calcular o total." }); });
    }, 200);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [body, hasItems]);

  return state?.body === body ? state : { loading: hasItems };
}

function PurchaseTotals({ quote, loading, error }) {
  if (!quote) return html`<p className="lc-checkout-note" role=${error ? "alert" : "status"}>
    ${error || (loading ? "Calculando os valores da compra…" : "Nenhum produto nesta compra.")}
  </p>`;
  return html`
    <div className="lc-summary-line"><span>Subtotal</span><strong>${currency(quote.subtotal)}</strong></div>
    <div className="lc-summary-line"><span>Frete</span><strong>${Number(quote.frete) === 0 ? "Grátis" : currency(quote.frete)}</strong></div>
    ${Number(quote.desconto) > 0 ? html`<div className="lc-summary-line lc-summary-discount">
      <span>Desconto</span><strong>− ${currency(quote.desconto)}</strong>
    </div>` : null}
    <div className="lc-summary-line lc-summary-total"><span>Total</span><strong>${currency(quote.total)}</strong></div>`;
}

function CartPage({ items, updateQuantity, removeItem, auth, address, coupon, setCoupon }) {
  const [draft, setDraft] = useState(coupon);
  const { quote, loading, error } = useCheckoutQuote(items, coupon);
  const addressHref = "/conta/endereco?voltar=sacola";

  return html`<main className="lc-page lc-cart-page">
    <header className="lc-section-header"><div>
      <h1>Minha sacola</h1>
      <p>Revise os itens, a entrega e o total antes de seguir para o pagamento.</p>
    </div></header>
    ${!items.length ? html`<div className="lc-empty-state">
      <p>Sua sacola ainda está vazia.</p>
      <${AppLink} href="/catalogo" className="lc-btn lc-btn-primary">Ver catálogo<//>
    </div>` : html`
      <section className="lc-cart-list" aria-label="Produtos da sacola">
        ${items.map(item => {
          const price = quote?.precos[item.id] ?? item.preco;
          return html`<article key=${item.id} className="lc-cart-item">
            <img src=${item.imagemUrl || "/images/sem-foto.png"} alt="" loading="lazy" onError=${event => {
              if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
            }} />
            <div className="lc-cart-item-info">
              <${AppLink} href=${`/produto/${item.id}`}><strong>${item.nome}</strong><//>
              <span>${currency(price)} por unidade</span>
            </div>
            <div className="lc-cart-item-actions">
              <div className="lc-qty-row">
                <button type="button" aria-label=${`Diminuir quantidade de ${item.nome}`} disabled=${item.quantity <= 1}
                  onClick=${() => updateQuantity(item.id, item.quantity - 1)}>−</button>
                <span aria-label="Quantidade">${item.quantity}</span>
                <button type="button" aria-label=${`Aumentar quantidade de ${item.nome}`} disabled=${item.quantity >= 999}
                  onClick=${() => updateQuantity(item.id, item.quantity + 1)}>+</button>
              </div>
              <button type="button" className="lc-link-button" onClick=${() => removeItem(item.id)}>Remover</button>
              <strong className="lc-cart-item-total">${currency(price * item.quantity)}</strong>
            </div>
          </article>`;
        })}
      </section>
      <div className="lc-cart-review">
        <div className="lc-cart-options">
          <section className="lc-checkout-card" aria-labelledby="cart-address">
            <h2 id="cart-address">Endereço de entrega</h2>
            ${address ? html`<address>
              <strong>${address.endereco}, ${address.numero}${address.complemento ? ` · ${address.complemento}` : ""}</strong>
              <span>${address.bairro} · ${address.cidade} / ${address.estado} · ${formatCep(address.cep)}</span>
            </address>` : html`<p className="lc-checkout-note">Escolha onde quer receber a compra.</p>`}
            <${AppLink} href=${auth.authenticated ? addressHref : buildLoginUrl(addressHref)}
              className="lc-btn lc-btn-ghost">${address ? "Alterar endereço" : "Adicionar endereço"}<//>
          </section>
          <section className="lc-checkout-card" aria-labelledby="cart-coupon">
            <h2 id="cart-coupon">Cupom</h2>
            ${auth.authenticated ? html`<form className="lc-cart-coupon-form" onSubmit=${event => {
              event.preventDefault();
              const code = draft.trim().toUpperCase();
              setDraft(code);
              setCoupon(code);
            }}>
              <label>Cupom de desconto<input value=${draft} maxLength="60" placeholder="LC-..."
                onChange=${event => setDraft(event.target.value)} /></label>
              <button type="submit" className="lc-btn lc-btn-ghost" disabled=${!draft.trim()}>Aplicar cupom</button>
            </form>` : html`<${AppLink} href=${buildLoginUrl("/sacola")}>Entrar para aplicar cupom<//>`}
            ${coupon ? html`<div className="lc-cart-coupon-status">
              ${quote?.cupomErro ? html`<span role="alert">${quote.cupomErro}</span>` : quote
                ? html`<span role="status">Cupom aplicado: ${coupon}</span>` : null}
              <button type="button" className="lc-link-button" onClick=${() => { setCoupon(""); setDraft(""); }}>Limpar cupom</button>
            </div>` : null}
          </section>
        </div>
        <aside className="lc-cart-summary" aria-label="Resumo da compra">
          <h2>Resumo da compra</h2>
          <${PurchaseTotals} quote=${quote} loading=${loading} error=${error} />
          <button type="button" className="lc-btn lc-btn-primary" disabled=${!quote}
            onClick=${() => {
              if (quote.cupomErro) setCoupon("");
              navigate("/checkout");
            }}>Continuar para pagamento</button>
        </aside>
      </div>`}
  </main>`;
}

function CheckoutPage({ items, auth, clearCart, address, initialCoupon }) {
  const [paymentConfig, setPaymentConfig] = useState(null);
  useEffect(() => {
    if (auth.authenticated)
      getJson("/api/checkout/config")
        .then(setPaymentConfig)
        .catch(() => {});
  }, [auth.authenticated]);
  const [form, setForm] = useState(() => ({ ...initialCheckoutForm(), ...address, cupom: initialCoupon }));
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const { quote, loading, error: quoteError } = useCheckoutQuote(items, form.cupom);

  const checkoutAttempt = useRef(null);
  const requestInProgress = useRef(false);
  const submitCheckout = async () => {
    if (requestInProgress.current) return;
    requestInProgress.current = true;
    setSubmitting(true);
    setMessage("");
    try {
      const fingerprint = JSON.stringify({
        form,
        items: items.map((i) => ({ id: i.id, quantity: i.quantity })),
      });
      if (checkoutAttempt.current?.fingerprint !== fingerprint) {
        checkoutAttempt.current = { fingerprint, id: window.crypto.randomUUID() };
      }
      const response = await postJson("/api/checkout/orders", {
        idempotencyKey: checkoutAttempt.current.id,
        nome: form.nome,
        telefone: form.telefone,
        cep: form.cep,
        endereco: form.endereco,
        numero: form.numero,
        complemento: form.complemento,
        bairro: form.bairro,
        cidade: form.cidade,
        estado: form.estado,
        pagamento: form.pagamento,
        cupom: form.cupom || "",
        itens: items.map((item) => ({
          produtoId: item.id,
          quantidade: item.quantity,
        })),
      });
      clearCart();
      window.location.href = response.redirectUrl;
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin();
        return;
      }
      setMessage(
        error.status === 400 ? error.message : "Não foi possível iniciar o pagamento agora.",
      );
    } finally {
      requestInProgress.current = false;
      setSubmitting(false);
    }
  };

  return html`
    <main className="lc-page lc-checkout-page">
      <section className="lc-checkout-shell">
        <div className="lc-checkout-main">
          <div className="lc-section-header">
            <div>
              <h1>Finalizar compra</h1>
              <p>Informe onde receber e confira as condições da compra.</p>
            </div>
          </div>
          <section className="lc-checkout-card" aria-labelledby="checkout-delivery">
            <h2 id="checkout-delivery"><span className="lc-checkout-step" aria-hidden="true">1</span> Endereço de entrega</h2>
            ${address ? html`<button type="button" className="lc-btn lc-btn-ghost lc-use-address"
              onClick=${() => setForm(current => ({ ...current, ...address }))}>Usar endereço salvo</button>` : null}
            <div className="lc-form-grid two">
              <label
                >Nome completo<input
                  value=${form.nome}
                  onChange=${(e) => setForm({ ...form, nome: e.target.value })}
              /></label>
              <label
                >Telefone<input
                  value=${form.telefone}
                  onChange=${(e) => setForm({ ...form, telefone: e.target.value })}
              /></label>
            </div>
            <div className="lc-form-grid two">
              <label
                >CEP<input
                  value=${form.cep}
                  onChange=${(e) => setForm({ ...form, cep: e.target.value })}
              /></label>
              <label
                >Endereço<input
                  value=${form.endereco}
                  onChange=${(e) => setForm({ ...form, endereco: e.target.value })}
              /></label>
            </div>
            <div className="lc-form-grid three">
              <label
                >Número<input
                  value=${form.numero}
                  onChange=${(e) => setForm({ ...form, numero: e.target.value })}
              /></label>
              <label
                >Complemento<input
                  value=${form.complemento}
                  onChange=${(e) => setForm({ ...form, complemento: e.target.value })}
              /></label>
              <label
                >Bairro<input
                  value=${form.bairro}
                  onChange=${(e) => setForm({ ...form, bairro: e.target.value })}
              /></label>
            </div>
            <div className="lc-form-grid two">
              <label
                >Cidade<input
                  value=${form.cidade}
                  onChange=${(e) => setForm({ ...form, cidade: e.target.value })}
              /></label>
              <label
                >Estado<input
                  value=${form.estado}
                  onChange=${(e) => setForm({ ...form, estado: e.target.value })}
              /></label>
            </div>
          </section>
          <section className="lc-checkout-card" aria-labelledby="checkout-payment">
            <h2 id="checkout-payment"><span className="lc-checkout-step" aria-hidden="true">2</span> Pagamento</h2>
            <label
              >Cupom da sua coleção<input
                value=${form.cupom || ""}
                maxlength="60"
                onChange=${(e) => setForm({ ...form, cupom: e.target.value })}
                placeholder="LC-..."
            /></label>
            <p className="lc-checkout-note">
              ${quote?.cupomErro || (Number(quote?.desconto) > 0
                ? "Cupom aplicado." : "O desconto aparece no resumo depois da validação.")}
            </p>
            <h3>Forma de pagamento</h3>
            ${paymentConfig && !paymentConfig.enabled ? html`<p role="status" className="lc-checkout-note">Modo demonstração: nenhuma cobrança será feita. O pedido será salvo como pendente para testar a jornada.</p>` : null}
            <div className="lc-payment-options">
              ${[
                ["pix", "Pix"],
                ["cartao", "Cartão de crédito"],
                ["boleto", "Boleto bancario"],
              ].map(
                ([value, label]) => html`
                  <button
                    key=${value}
                    className=${`lc-payment-option ${form.pagamento === value ? "ativo" : ""}`}
                    onClick=${() => setForm({ ...form, pagamento: value })}
                    aria-pressed=${form.pagamento === value}
                    type="button"
                  >
                    ${label}
                  </button>
                `,
              )}
            </div>
            <p className="lc-checkout-note">
              ${
                auth.authenticated
                  ? "Confira o total antes de continuar. O pedido ficará disponível na sua conta."
                  : "Entre na sua conta para concluir a compra e iniciar o pagamento."
              }
            </p>
          </section>
        </div>
        <aside className="lc-cart-summary" aria-label="Resumo da compra">
          <h2>Resumo da compra</h2>
          ${items.map(
            (item) => html`
              <div key=${item.id} className="lc-checkout-item">
                <img src=${item.imagemUrl || "/images/sem-foto.png"} alt=""
                  onError=${event => {
                    if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
                  }} />
                <div><span>${item.nome}</span><small>Quantidade ${item.quantity}</small></div>
                <strong>${currency((quote?.precos[item.id] ?? item.preco) * item.quantity)}</strong>
              </div>
            `,
          )}
          ${quoteError && message ? null : html`<${PurchaseTotals} quote=${quote} loading=${loading} error=${quoteError} />`}
          ${message ? html`<p className="lc-checkout-note" role="alert">${message}</p>` : null}
          ${
            auth.authenticated
              ? html`<button
                  className="lc-btn lc-btn-primary"
                  onClick=${submitCheckout}
                  disabled=${submitting || items.length === 0}
                >
                  ${submitting ? "Enviando..." : paymentConfig && !paymentConfig.enabled ? "Criar pedido de demonstração" : "Ir para pagamento"}
                </button>`
              : html`<${AppLink}
                  href=${buildLoginUrl()}
                  className="lc-btn lc-btn-primary"
                  >Entrar para pagar<//
                >`
          }
        </aside>
      </section>
    </main>
  `;
}

function ProfileProgress() {
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    getJson("/api/pass/current")
      .then(data => { if (active) setDashboard(data); })
      .catch(() => { if (active) setError("Não foi possível carregar seu progresso."); });
    return () => { active = false; };
  }, []);

  if (error) return html`<p role="alert" className="lc-form-notice error">${error}</p>`;
  if (!dashboard) return html`<p role="status">Carregando progresso…</p>`;

  const completed = [
    ...dashboard.missoesDiarias, ...dashboard.missoesSemanais, ...dashboard.missoesMensais,
  ].filter(mission => mission.concluida).length;
  const rewards = dashboard.niveis.filter(reward => reward.disponivelParaResgate && !reward.resgatado).length;
  const history = dashboard.historico.slice(0, 6);
  const maxXp = Math.max(1, ...history.map(entry => entry.xpGanho));

  return html`<div className="lc-profile-progress">
    <div className="lc-profile-progress-heading">
      <div><h2>Seu progresso</h2><p>${dashboard.temporadaNome} · ${dashboard.temporadaPeriodo}</p></div>
      <${AppLink} href="/passe" className="lc-btn lc-btn-primary">Abrir passe<//>
    </div>
    <dl className="lc-profile-metrics">
      <div><dt>XP na temporada</dt><dd>${dashboard.xpTotal}</dd></div>
      <div><dt>Nível atual</dt><dd>${dashboard.nivelAtual}<small> / ${dashboard.totalNiveis}</small></dd></div>
      <div><dt>Missões concluídas</dt><dd>${completed}</dd></div>
      <div><dt>Prêmios disponíveis</dt><dd>${rewards}</dd></div>
    </dl>
    <div className="lc-profile-level-progress">
      <strong>${dashboard.nivelAtual >= dashboard.totalNiveis ? "Último nível alcançado" : `Faltam ${dashboard.xpParaProximoNivel} XP para o próximo nível`}</strong>
      <progress aria-label="Progresso para o próximo nível" max=${dashboard.xpPorNivel}
        value=${Math.min(dashboard.xpNoNivelAtual, dashboard.xpPorNivel)}></progress>
      <${AppLink} href="/passe/missoes">Ver missões →<//>
    </div>
    <section className="lc-profile-history" aria-labelledby="profile-xp-title">
      <h2 id="profile-xp-title">Últimos ganhos de XP</h2>
      ${history.length === 0 ? html`<p>Seu histórico aparece quando você ganha XP.</p>`
        : html`<ul>${history.map((entry, index) => html`<li key=${index}>
            <div><strong>${entry.descricao}</strong><small>${entry.criadoEm}</small></div>
            <div className="lc-profile-xp-track" aria-hidden="true"><span style=${{width: `${entry.xpGanho / maxXp * 100}%`}}></span></div>
            <strong>${entry.xpGanho > 0 ? "+" : ""}${entry.xpGanho} XP</strong>
          </li>`)}</ul>`}
    </section>
    <${EventBadges} getJson=${getJson} />
  </div>`;
}

function ProfilePage({ auth, route, onAuthRefresh, notify }) {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ nome: "", senha: "" });
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);
  const active = parseRoute(route).params.get("aba") === "progresso" ? "progresso" : "conta";

  useEffect(() => {
    let active = true;
    getJson("/api/account/profile")
      .then(response => {
        if (!active) return;
        setProfile(response);
        setForm({ nome: response.nome || "", senha: "" });
      })
      .catch(error => {
        if (!active) return;
        if (error.code === "AUTH_REQUIRED") requireLogin();
        else setMessage("Não foi possível carregar o perfil.");
      });
    return () => { active = false; };
  }, []);

  async function submitProfile(event) {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      const response = await putJson("/api/account/profile", form);
      setProfile(response);
      setForm({ nome: response.nome, senha: "" });
      notify("Perfil atualizado");
      onAuthRefresh();
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") requireLogin();
      else setMessage(error.message || "Não foi possível atualizar o perfil.");
    } finally {
      setSaving(false);
    }
  }

  return html`<main className="lc-page lc-profile-page">
    <header className="lc-profile-header">
      <${AppLink} href="/conta">‹ Minha conta<//>
      <div className="lc-profile-identity">
        ${auth.avatarPasse ? html`<img className="lc-profile-avatar"
          src=${`/api/pass/rewards/${auth.avatarPasse}/download?inline=true`} alt="Avatar resgatado" />`
          : html`<span className="lc-profile-avatar" aria-hidden="true"><${Icon} name="user" /></span>`}
        <div><h1>Meu perfil</h1>${profile ? html`<p>${profile.nome} · ${profile.email}</p>` : null}</div>
        ${auth.badgePasse ? html`<img className="lc-profile-badge"
          src=${`/api/pass/rewards/${auth.badgePasse}/download?inline=true`} alt="Conquista equipada" />` : null}
      </div>
    </header>
    <nav className="lc-profile-tabs" aria-label="Seções do perfil">
      <${AppLink} href="/perfil" current=${active === "conta"}>Dados da conta<//>
      <${AppLink} href="/perfil?aba=progresso" current=${active === "progresso"}>Progresso e XP<//><${AppLink} href="/conta/vip">Meu VIP<//>
    </nav>
    ${message && (!profile || active === "conta") ? html`<p className="lc-form-notice error" role="alert">${message}</p>` : null}
    ${!profile ? !message ? html`<p role="status">Carregando perfil…</p>` : null
      : active === "progresso" ? html`<${ProfileProgress} />`
      : html`<div className="lc-profile-account">
        <form className="lc-profile-form" onSubmit=${submitProfile}>
          <fieldset><legend>Informações pessoais</legend>
            <div className="lc-form-grid two">
              <label>Nome completo<input required minLength="2" maxLength="100" autoComplete="name"
                value=${form.nome} onChange=${event => setForm({...form, nome: event.target.value})} /></label>
              <label>E-mail<input type="email" value=${profile.email} readOnly /></label>
            </div>
          </fieldset>
          <fieldset><legend>Segurança</legend>
            <label>Nova senha<input type="password" autoComplete="new-password" minLength="12"
              value=${form.senha} onChange=${event => setForm({...form, senha: event.target.value})}
              placeholder="Deixe em branco para manter" /></label>
          </fieldset>
          <button type="submit" className="lc-btn lc-btn-primary" disabled=${saving}>
            ${saving ? "Salvando…" : "Salvar alterações"}
          </button>
        </form>
        <nav className="lc-profile-shortcuts" aria-label="Atalhos da conta">
          <h2>Gerenciar conta</h2>
          <${AppLink} href="/conta/endereco">Endereço de entrega →<//>
          <${AppLink} href="/pedidos">Meus pedidos →<//>
          <${AppLink} href="/conta/beneficios">Cupons e recompensas →<//>
        </nav>
      </div>`}
  </main>`;
}

function OrderProduct({ item }) {
  return html`<li className="lc-order-product">
    <img src=${item.imagemUrl || "/images/sem-foto.png"} alt="" loading="lazy" onError=${event => {
      if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
    }} />
    <div>
      <strong>${item.nome}</strong>
      ${item.descricaoCurta ? html`<p>${item.descricaoCurta}</p>` : null}
      <small>Quantidade ${item.quantidade} · ${currency(item.subtotal)}</small>
    </div>
    ${item.produtoDisponivel ? html`<${AppLink} href=${`/produto/${item.produtoId}`} className="lc-btn lc-btn-ghost">Ver produto<//>` : null}
  </li>`;
}

function OrdersPage() {
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    getJson("/api/account/orders").then(data => { if (active) setOrders(data); })
      .catch(failure => {
        if (!active) return;
        if (failure.code === "AUTH_REQUIRED") requireLogin("/pedidos");
        else setError(true);
      });
    return () => { active = false; };
  }, []);

  return html`<main className="lc-page lc-orders-page">
    <section className="lc-account-shell">
      <header className="lc-section-header">
        <div><h1>Meus pedidos</h1><p>Acompanhe o pagamento e a entrega das suas compras.</p></div>
        <${AppLink} href="/perfil">Meu perfil<//><${AppLink} href="/conta/vip">Meu VIP<//>
      </header>
      ${error ? html`<p role="alert">Não foi possível carregar os pedidos.</p>`
        : !orders ? html`<p role="status">Carregando pedidos…</p>`
        : !orders.length ? html`<p className="lc-empty-state">Você ainda não tem pedidos.</p>`
        : html`<div className="lc-orders-list">
          ${orders.map(order => html`<article key=${order.id} className="lc-order-card">
            <header className="lc-order-card-top">
              <div className="lc-order-identity"><h2>Pedido ${order.numero}</h2><span>${order.dataPedido}</span></div>
              <span className=${`lc-status-pill ${order.status.toLowerCase()}`}>${orderStatusLabel(order.status)}</span>
            </header>
            <div className="lc-order-card-grid">
              <div><small>Total</small><strong>${currency(order.total)}</strong></div>
              <div><small>Itens</small><strong>${order.totalItens}</strong></div>
              <div><small>Pagamento</small><strong>${paymentStatusLabel(order.pagamentoStatus)}</strong></div>
            </div>
            ${order.itens?.length ? html`<ul className="lc-order-products">
              ${order.itens.slice(0, 3).map((item, index) => html`<${OrderProduct} key=${index} item=${item} />`)}
            </ul>` : null}
            ${order.itens?.length > 3 ? html`<details className="lc-order-more">
              <summary>Ver todos os itens (${order.itens.length})</summary>
              <ul className="lc-order-products">${order.itens.slice(3).map((item, index) => html`<${OrderProduct} key=${index} item=${item} />`)}</ul>
            </details>` : null}
            <${AppLink} href=${`/pedidos/${order.id}`} className="lc-btn lc-btn-primary">Ver detalhes do pedido<//>
          </article>`)}
        </div>`}
    </section>
  </main>`;
}

function OrderDetailPage({ orderId, route, notify }) {
  const [order, setOrder] = useState(null);
  const { params } = parseRoute(route);
  const [message, setMessage] = useState(
    params.get("paymentsDisabled")
      ? "Pagamentos desativados neste ambiente de demonstração. Nenhuma cobrança foi feita."
      : params.get("paymentError")
        ? "Nao foi possivel abrir o pagamento automaticamente. Tente novamente."
        : params.get("openPayment")
          ? "O pedido esta pronto para retomar o pagamento."
          : "",
  );
  const [busyAction, setBusyAction] = useState("");

  const loadOrder = () => {
    getJson(`/api/account/orders/${orderId}`)
      .then(setOrder)
      .catch((error) => {
        if (error.code === "AUTH_REQUIRED") {
          requireLogin(`/pedidos/${orderId}`);
        }
      });
  };

  useEffect(() => {
    loadOrder();
  }, [orderId]);

  const cancelOrder = async () => {
    setBusyAction("cancel");
    setMessage("");
    try {
      const response = await postJson(
        `/api/account/orders/${orderId}/cancel`,
        {},
      );
      setOrder(response);
      notify("Pedido cancelado com sucesso.");
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin(`/pedidos/${orderId}`);
        return;
      }
      setMessage("Nao foi possivel cancelar o pedido.");
    } finally {
      setBusyAction("");
    }
  };

  const retryPayment = async () => {
    setBusyAction("payment");
    setMessage("");
    try {
      const response = await postJson(
        `/api/account/orders/${orderId}/payment`,
        {
          pagamento:
            order.metodoPagamento === "BOLETO"
              ? "boleto"
              : order.metodoPagamento === "CARTAO_CREDITO"
                ? "cartao"
                : "pix",
        },
      );
      window.location.href = response.redirectUrl;
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin(`/pedidos/${orderId}`);
        return;
      }
      setMessage("Nao foi possivel retomar o pagamento.");
    } finally {
      setBusyAction("");
    }
  };

  if (!order) {
    return html`<main className="lc-page"><p>Carregando pedido...</p></main>`;
  }

  return html`
    <main className="lc-page">
      <section className="lc-order-detail-shell">
        <div className="lc-order-detail-main">
          <div className="lc-section-header">
            <div>
              <h2>Pedido ${order.numero}</h2>
              <p>Resumo completo de itens, entrega e pagamento.</p>
            </div>
            <${AppLink} href="/pedidos">Voltar para pedidos<//>
          </div>
          <div className="lc-order-card">
            <div className="lc-order-card-top">
              <div>
                <strong>Status do pedido</strong>
                <span>${order.dataPedido}</span>
              </div>
              <span className=${`lc-status-pill ${order.status.toLowerCase()}`}
                >${orderStatusLabel(order.status)}</span
              >
            </div>
            <div className="lc-order-card-grid">
              <div>
                <small>Subtotal</small
                ><strong>${currency(order.subtotal)}</strong>
              </div>
              <div>
                <small>Frete</small><strong>${currency(order.frete)}</strong>
              </div>
              ${order.desconto > 0 ? html`<div><small>Desconto</small><strong>${currency(order.desconto)}</strong></div>` : null}
              <div>
                <small>Total</small><strong>${currency(order.total)}</strong>
              </div>
            </div>
          </div>

          <div className="lc-checkout-card">
            <h3>Itens do pedido</h3>
            <div className="lc-order-items">
              ${order.itens.map(
                (item) => html`
                  <div key=${item.produtoId} className="lc-order-item-row">
                    <img
                      src=${item.imagemUrl || "/images/sem-foto.png"}
                      alt=${item.nome}
                    />
                    <div>
                      <strong>${item.nome}</strong>
                      <span>${item.quantidade} unidade(s)</span>
                    </div>
                    <strong>${currency(item.subtotal)}</strong>
                  </div>
                `,
              )}
            </div>
          </div>

          <div className="lc-checkout-card">
            <h3>Entrega</h3>
            <div className="lc-order-card-grid">
              <div>
                <small>Destinatario</small
                ><strong>${order.nomeDestinatario || "-"}</strong>
              </div>
              <div>
                <small>Telefone</small
                ><strong>${order.telefoneContato || "-"}</strong>
              </div>
              <div>
                <small>CEP</small><strong>${order.cepEntrega || "-"}</strong>
              </div>
            </div>
            <p className="lc-checkout-note">
              ${order.enderecoEntrega || "Endereco indisponivel."}
            </p>
          </div>
        </div>
        <aside className="lc-cart-summary">
          <span>Pagamento</span>
          <strong>${paymentStatusLabel(order.pagamentoStatus)}</strong>
          <div className="lc-summary-line">
            <span>Metodo</span
            ><strong>${paymentMethodLabel(order.metodoPagamento)}</strong>
          </div>
          ${order.pagoEm ? html`<div className="lc-summary-line"><span>Pago em</span><strong>${order.pagoEm}</strong></div>` : null}
          ${message ? html`<p className="lc-checkout-note">${message}</p>` : null}
          ${
            order.status === "PENDENTE"
              ? html`
                  <button
                    className="lc-btn lc-btn-primary"
                    onClick=${retryPayment}
                    disabled=${busyAction === "payment"}
                  >
                    ${busyAction === "payment" ? "Abrindo pagamento..." : "Retomar pagamento"}
                  </button>
                  <button
                    className="lc-btn lc-btn-ghost"
                    onClick=${cancelOrder}
                    disabled=${busyAction === "cancel"}
                  >
                    ${busyAction === "cancel" ? "Cancelando..." : "Cancelar pedido"}
                  </button>
                `
              : null
          }
          <button className="lc-btn lc-btn-ghost" onClick=${loadOrder}>
            Atualizar status
          </button>
        </aside>
      </section>
    </main>
  `;
}

function PaymentResultPage({ route }) {
  const { params } = parseRoute(route);
  const status = params.get("status") || "pendente";
  const pedidoId = params.get("pedidoId");

  const map = {
    sucesso: {
      title: "Retorno do pagamento recebido",
      copy: "Confira o status confirmado pelo provedor nos detalhes do pedido. O retorno do navegador não comprova aprovação.",
    },
    falha: {
      title: "Pagamento nao aprovado",
      copy: "Voce pode revisar o pedido e tentar outra forma de pagamento.",
    },
    pendente: {
      title: "Pagamento pendente",
      copy: "Assim que a aprovacao chegar, o pedido sera atualizado automaticamente.",
    },
  };

  const content = map[status] || map.pendente;

  return html`
    <main className="lc-page">
      <section className="lc-payment-result">
        <div className="lc-checkout-card">
          <span className="lc-tag">${status.toUpperCase()}</span>
          <h2>${content.title}</h2>
          <p className="lc-checkout-note">${content.copy}</p>
          <div className="lc-hero-actions">
            ${
              pedidoId
                ? html`<${AppLink}
                    href=${`/pedidos/${pedidoId}`}
                    className="lc-btn lc-btn-primary"
                    >Ver pedido<//
                  >`
                : html`<${AppLink}
                    href="/pedidos"
                    className="lc-btn lc-btn-primary"
                    >Ir para pedidos<//
                  >`
            }
            <${AppLink} href="/catalogo" className="lc-btn lc-btn-ghost"
              >Continuar comprando<//
            >
          </div>
        </div>
      </section>
    </main>
  `;
}

function moveCarousel(track, direction) {
  track?.scrollBy({
    left: direction * track.clientWidth * 0.8,
    behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth",
  });
}

const rewardExamples = [
  "Notebook", "Monitor", "Fone sem fio", "Caixa de som", "Tênis", "Relógio",
  "Garrafa térmica", "Kit para setup", "Mochila", "Acessório para jogos",
];

function RewardHighlights({ products, target }) {
  const highlights = rewardExamples.map((label, index) => ({
    label: products[index]?.nome || label,
    image: products[index]?.imagemUrl || (index % 2 ? "/app/level-free.webp" : "/app/level-premium.webp"),
  }));
  const rows = [highlights.slice(0, 4), highlights.slice(4, 7), highlights.slice(7)];
  return html`<section className="lc-reward-highlights" aria-label="Vitrine ilustrativa de prêmios">
    ${rows.map((row, index) => html`<div key=${index} className="lc-reward-highlights-row">
      <div className="lc-reward-highlights-track" data-direction=${index === 1 ? "left" : "right"}>
        ${[false, true].map(copy => html`<div className="lc-reward-highlight-set" key=${String(copy)}
          aria-hidden=${copy ? "true" : undefined}>
          ${row.map(({ image, label }, itemIndex) => html`<a href=${target} className="lc-reward-highlight"
            key=${itemIndex} tabIndex=${copy ? -1 : undefined} aria-label=${`${label} · Ver passe`}>
            <img src=${image} alt="" decoding="async" onError=${event => {
              if (!event.currentTarget.src.endsWith("/app/level-free.webp")) event.currentTarget.src = "/app/level-free.webp";
            }} />
            <strong>${label}</strong>
          </a>`)}
        </div>`)}
      </div>
    </div>`)}
  </section>`;
}

function PassPage({ auth, route, notify, notifyGain, products }) {
  const overview = usePassOverview(auth);
  const { pass, levels } = overview;
  const showEvents = route.endsWith("#eventos");
  const trailReady = Boolean(levels?.length);
  useEffect(() => {
    if (showEvents) document.getElementById("eventos")?.scrollIntoView();
  }, [showEvents, trailReady]);
  const guided = useRef(false);
  const [showIntro, setShowIntro] = useState(false);

  useEffect(() => {
    if (!auth.ready) return;
    const key = `level-commerce-pass-intro-${auth.id ?? "visitor"}`;
    try {
      if (localStorage.getItem(key)) return;
      localStorage.setItem(key, "seen");
    } catch {}
    setShowIntro(true);
  }, [auth.ready, auth.id]);

  useEffect(() => {
    if (!pass || guided.current) return;
    guided.current = true;
    if (!shouldShowPassGuide(auth.id)) return;
    if (pass.niveis.some(reward => reward.disponivelParaResgate && !reward.resgatado)) {
      notify("Você tem prêmios para resgatar", "Veja os níveis liberados na trilha.");
    } else if (pass.xpParaProximoNivel > 0) {
      const xp = pass.xpParaProximoNivel;
      notify(`${xp === 1 ? "Falta" : "Faltam"} ${xp} XP para o próximo nível`, "Veja as missões disponíveis.");
    } else {
      notify("Você alcançou o último nível", "Confira as recompensas da temporada.");
    }
  }, [pass, notify, auth.id]);
  return html`<main className="lc-level-pass">
    <header className="lc-rewards-banner lc-level-pass-header">
      <div className="lc-page lc-rewards-banner-content">
        <div className="lc-pass-banner-copy">
          <span className="lc-eyebrow">LEVEL REWARDS</span>
          <h1>Passe LEVEL</h1>
          ${pass ? html`<p className="lc-pass-season">${pass.temporadaNome} <span>· ${pass.temporadaPeriodo}</span></p>`
            : html`<p>Suba de nível com suas compras e missões e desbloqueie recompensas na temporada.</p>`}
          ${pass ? html`<div className="lc-level-pass-progress">
            <div>
              <strong>Nível ${pass.nivelAtual} de ${pass.totalNiveis} <span>· ${pass.xpTotal} XP</span></strong>
              <progress aria-label="Progresso para o próximo nível" value=${Math.min(pass.xpNoNivelAtual, pass.xpPorNivel)}
                max=${pass.xpPorNivel}></progress>
              <small>${pass.nivelAtual >= pass.totalNiveis ? "Último nível alcançado" : `${pass.xpParaProximoNivel} XP para o próximo nível`}</small>
            </div>
            <${AppLink} href="/passe/missoes" className="lc-btn lc-neon-btn">Ver missões<//>
          </div>` : auth.ready && !auth.authenticated
            ? html`<${AppLink} href=${buildLoginUrl("/passe")} className="lc-btn lc-neon-btn">Entrar para ver meu passe<//>`
            : null}
        </div>
        <${RewardHighlights} products=${products} target="#trilha-do-passe" />
      </div>
    </header>
    ${showIntro ? html`<div className="lc-page lc-rewards-content">
    ${showIntro ? html`<aside className="lc-pass-intro" aria-label="Como subir de nível">
      <p>Suba de nível com compras, missões e eventos para liberar recompensas.</p>
      <${AppLink} href="/passe/missoes">Ver missões<//>
      <button type="button" className="lc-btn lc-btn-ghost" onClick=${() => setShowIntro(false)}>Entendi</button>
    </aside>` : null}
    </div>` : null}
    <${RewardTrail} auth=${auth} overview=${overview} notifyGain=${notifyGain} standalone />
    <${PassEvents} auth=${auth} getJson=${getJson} postJson=${postJson}
      notify=${(title, text) => notifyGain(title, text, "prize")} />
    ${products.length ? html`<div className="lc-page lc-pass-products">
      <${HomeProductShelf} title="Produtos da loja" products=${products} />
    </div>` : null}
  </main>`;
}

function RewardTrail({ auth, overview, notifyGain, standalone = false }) {
  const [choices, setChoices] = useState({});
  const [claimedItem, setClaimedItem] = useState(null);
  const { pass, levels } = overview;
  const [busy, setBusy] = useState(null);
  const [error, setError] = useState("");
  const track = useRef(null);
  async function claim(reward) {
    if (busy !== null || !reward.disponivelParaResgate) return;
    setBusy(reward.recompensaId);
    setError("");
    try {
      const result = await claimPassReward(reward.recompensaId, choices[reward.recompensaId]
        || (reward.opcoes?.length === 1 ? reward.opcoes[0].produtoId : null));
      setClaimedItem(result.itemBauId);
      notifyGain("Recompensa recebida", result.titulo, "prize");
      overview.updatePass(current => ({ ...current, niveis: current.niveis.map(item =>
        item.recompensaId === reward.recompensaId
          ? { ...item, resgatado: true, disponivelParaResgate: false }
          : item
      ) }));
    } catch (failure) {
      if (failure.code === "AUTH_REQUIRED") requireLogin();
      else setError(failure.message || "Não foi possível atualizar o passe.");
    } finally {
      setBusy(null);
    }
  }

  async function buyPremium() {
    if (busy !== null) return;
    setBusy("premium");
    setError("");
    try {
      const response = await postJson("/api/pass/premium", {});
      window.location.assign(response.redirectUrl);
    } catch (failure) {
      setError(failure.message || "Não foi possível iniciar a compra do passe.");
      setBusy(null);
    }
  }

  const rewardCell = (reward, premium) => {
    if (!reward || reward.premium !== premium) return html`<div className=${`lc-rail-reward ${premium ? "premium" : "free"} empty`} aria-hidden="true">—</div>`;
    const available = reward.disponivelParaResgate && !reward.resgatado;
    const blockedReason = premium && !pass?.premiumAtivo ? "Requer Premium" : `Requer ${reward.xpNecessario} XP`;
    return html`<div className=${`lc-rail-reward ${premium ? "premium" : "free"} ${available ? "available" : ""}`}>
      <span>${reward.tipoDigital}</span>
      <strong title=${reward.descricao}>${reward.titulo}</strong>
      ${reward.opcoes?.length && !reward.resgatado ? html`<select aria-label=${`Escolher prêmio do nível ${reward.nivel}`}
        value=${choices[reward.recompensaId] || (reward.opcoes.length === 1 ? reward.opcoes[0].produtoId : "")}
        onChange=${e => setChoices(current => ({ ...current, [reward.recompensaId]: Number(e.target.value) }))}>
        ${reward.opcoes.length > 1 ? html`<option value="">Escolha seu prêmio</option>` : null}
        ${reward.opcoes.map(option => html`<option key=${option.produtoId} value=${option.produtoId}
          disabled=${!option.disponivel}>${option.nome}${option.disponivel ? "" : " · indisponível"}</option>`)}
      </select>` : null}
      <button type="button" className=${`lc-reward-state ${available ? "available" : reward.resgatado ? "received" : "locked"}`}
        disabled=${!available || busy !== null} aria-label=${`${available ? "Receber" : reward.resgatado ? "Recebido" : "Bloqueado"} · nível ${reward.nivel} · ${reward.titulo}${!available && !reward.resgatado ? ` · ${blockedReason}` : ""}`}
        onClick=${() => claim(reward)}>
        ${busy === reward.recompensaId ? "Recebendo…" : available ? "Receber" : reward.resgatado ? "Recebido" : "Bloqueado"}
      </button>
      ${!available && !reward.resgatado ? html`<small>${blockedReason}</small>` : null}
    </div>`;
  };

  const premiumAccess = html`<div className=${`lc-premium-access ${pass?.premiumAtivo ? "active" : "locked"}`}>
    <span className="lc-premium-badge"><${Icon} name=${pass?.premiumAtivo ? "shield" : "lock"} /></span>
    <div><strong>${pass?.premiumAtivo ? "Premium ativo" : "Trilha Premium bloqueada"}</strong>
      <small>${pass?.premiumAtivo ? `Acesso até ${new Date(pass.premiumValidoAte).toLocaleDateString("pt-BR")}`
        : pass ? `${currency(pass.precoPremium)} nesta temporada · ${pass.temporadaPeriodo}${auth.ambienteLocal ? " · ativação de teste" : ""}`
          : "Entre para acompanhar a trilha e resgatar prêmios."}</small></div>
    ${!pass ? html`<${AppLink} href=${buildLoginUrl()} className="lc-btn lc-neon-btn">Entrar<//>`
      : !pass.premiumAtivo && auth.ambienteLocal ? html`<button type="button" className="lc-btn lc-neon-btn"
        disabled=${busy !== null} onClick=${buyPremium}>Liberar Premium</button>` : null}
  </div>`;

  return html`<section id="trilha-do-passe" className=${`lc-level-trail ${standalone ? "lc-page" : ""}`} aria-label=${standalone ? undefined : "Trilha de recompensas"}
    aria-labelledby=${standalone ? "level-trail-title" : undefined}>
      ${standalone ? html`<div className="lc-level-trail-heading">
        <div><h2 id="level-trail-title">Trilha de recompensas</h2>
          <p>Os prêmios de cada trilha liberam conforme você alcança os níveis.</p></div>
      </div>` : null}
      ${claimedItem ? html`<div className="lc-form-notice" role="status">Prêmio guardado no Baú.
        <${AppLink} href=${`/conta/bau/${claimedItem}/entrega`}>Receber agora →<//>
        <${AppLink} href="/conta/beneficios">Ver Baú<//></div>` : null}
      ${standalone && pass ? html`<p>Compra única válida na temporada ${pass.temporadaPeriodo}. O progresso encerra com a temporada.</p>` : null}
      ${error || overview.error ? html`<p className="lc-form-notice error" role="alert">${error || overview.error}</p>` : null}
      ${!levels ? !overview.error ? html`<p role="status">Carregando temporada…</p>` : null : html`
      ${standalone && pass?.premiumAtivo ? premiumAccess : null}
      <div className="lc-rail-toolbar">
        ${standalone ? html`<span>Deslize a trilha para explorar os níveis.</span>` : null}
        <div>
          <button type="button" className="lc-btn lc-btn-ghost" aria-label="Níveis anteriores" onClick=${() => moveCarousel(track.current, -1)}>←</button>
          <button type="button" className="lc-btn lc-btn-ghost" aria-label="Próximos níveis" onClick=${() => moveCarousel(track.current, 1)}>→</button>
        </div>
      </div>
      <div className=${`lc-rail ${pass?.premiumAtivo ? "" : "lc-rail-locked"}`}>
        ${!pass?.premiumAtivo ? html`<div className="lc-premium-overlay">${premiumAccess}</div>` : null}
        <div className="lc-rail-labels" aria-hidden="true"><strong className="premium">Premium</strong><span>Nível</span><strong className="free">Gratuito</strong></div>
        <div className="lc-rail-track" ref=${track} tabIndex="0" role="region" aria-label="Níveis e recompensas do passe">
          ${[...new Set(levels.map(r => r.nivel))].map(nivel => html`
            <div className="lc-rail-level" key=${nivel} aria-label=${`Nível ${nivel}`}>
              ${rewardCell(levels.find(r => r.nivel === nivel && r.premium), true)}
              <span className="lc-rail-node" aria-current=${nivel === pass?.nivelAtual ? "step" : undefined}>${nivel}</span>
              ${rewardCell(levels.find(r => r.nivel === nivel && !r.premium), false)}
            </div>`)}
        </div>
      </div>
      `}
      ${standalone && pass?.historico.length ? html`<details className="lc-pass-xp-history">
        <summary>Últimos ganhos de XP</summary>
        ${pass.historico.map((item, index) => html`<div className="lc-summary-line" key=${index}>
          <span>${item.descricao}</span><strong>+${item.xpGanho} XP</strong>
        </div>`)}
      </details>` : null}
  </section>`;
}

function BauItems() {
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    getJson("/api/account/bau").then(setItems).catch(() => setError("Não foi possível carregar o Baú."));
  }, []);
  return html`<section className="lc-bau-list" aria-label="Itens guardados no Baú">
    ${error ? html`<p role="alert">${error}</p>` : null}
    ${items.map(item => html`<article key=${item.id} className="lc-bau-item">
      ${item.imagem ? html`<img src=${item.imagem} alt="" loading="lazy" />` : html`<span aria-hidden="true">✦</span>`}
      <div><h2>${item.titulo}</h2><p>${item.fisico ? "Produto e entrega gratuitos" : "Selo da sua coleção VIP"}</p>
      ${item.podeSolicitar ? html`<${AppLink} className="lc-btn lc-btn-primary" href=${`/conta/bau/${item.id}/entrega`}>Solicitar entrega<//>`
        : item.pedidoId ? html`<${AppLink} href=${`/pedidos/${item.pedidoId}`}>Acompanhar pedido · ${item.estado}<//>`
        : html`<span>Na coleção</span>`}
      </div></article>`)}
  </section>`;
}

function RewardDeliveryPage({ id, auth }) {
  const [item, setItem] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [address, setAddress] = useState({});
  useEffect(() => {
    if (!auth.authenticated) return;
    getJson("/api/account/bau").then(items => {
      const found = items.find(i => String(i.id) === id);
      if (!found) throw new Error("Recompensa não encontrada.");
      setItem(found);
    }).catch(e => setError(e.message));
  }, [id, auth.authenticated]);
  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      const result = await postJson(`/api/account/bau/${id}/entrega`, address);
      window.location.assign(`/pedidos/${result.pedidoId}`);
    } catch (e) {
      setError(e.message || "Não foi possível solicitar a entrega.");
      setBusy(false);
    }
  }
  return html`<main className="lc-page"><div className="lc-section-header"><h1>Receber recompensa</h1>
    <${AppLink} href="/conta/beneficios">Voltar ao Baú<//></div>
    ${error ? html`<p role="alert" className="lc-form-notice error">${error}</p>` : null}
    ${!auth.authenticated ? html`<${AppLink} href=${buildLoginUrl()}>Entrar<//>`
      : item ? html`<section className="lc-checkout-card"><h2>${item.titulo}</h2>
      <p>Produto R$ 0,00 · Frete R$ 0,00 · Total R$ 0,00</p>
      ${!item.podeSolicitar ? html`<${AppLink} href=${`/pedidos/${item.pedidoId}`}>Ver pedido de entrega<//>`
      : html`<form onSubmit=${submit} className="lc-reward-address">
        ${[["nomeDestinatario", "Destinatário", 120], ["telefoneContato", "Telefone", 20],
          ["cep", "CEP", 9], ["logradouro", "Endereço", 160], ["numero", "Número", 20],
          ["complemento", "Complemento", 80], ["bairro", "Bairro", 80],
          ["cidade", "Cidade", 80], ["uf", "Estado (UF)", 2]].map(([name, label, max]) => html`
          <label key=${name}>${label}<input name=${name} required=${name !== "complemento"} maxLength=${max}
            value=${address[name] || ""} onChange=${e => setAddress(a => ({ ...a, [name]: e.target.value }))} /></label>`)}
        <button className="lc-btn lc-btn-primary" disabled=${busy}>${busy ? "Confirmando…" : "Confirmar entrega gratuita"}</button>
      </form>`}</section>` : html`<p role="status">Carregando recompensa…</p>`}
  </main>`;
}

function VipPage({ auth }) {
  const [vip, setVip] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(null);
  const refresh = () => getJson("/api/account/vip").then(setVip);
  useEffect(() => {
    if (auth.authenticated) refresh().catch(() => setError("Não foi possível carregar seu VIP."));
  }, [auth.authenticated]);
  async function claim(id) {
    if (busy !== null) return;
    setBusy(id);
    setError("");
    try {
      await postJson(`/api/account/vip/${id}/claim`, {});
      await refresh();
    } catch (e) { setError(e.message || "Não foi possível resgatar."); }
    finally { setBusy(null); }
  }
  return html`<main className="lc-page"><div className="lc-section-header"><h1>Meu VIP</h1>
    <${AppLink} href="/conta/beneficios">Meu Baú →<//></div>
    ${error ? html`<p role="alert" className="lc-form-notice error">${error}</p>` : null}
    ${!auth.authenticated ? html`<${AppLink} href=${buildLoginUrl()}>Entrar para ver meu VIP<//>`
      : vip ? html`<section className="lc-vip-overview"><h2>${vip.nivel}</h2><strong>${vip.pontos} pontos VIP</strong>
        <p>Compras, passe, missões e eventos ajudam você a progredir.</p>
        <p>${vip.expirado ? "Seu progresso expirou por inatividade. Participe novamente para recomeçar."
          : vip.validade ? `Mantenha sua atividade até ${new Date(vip.validade).toLocaleDateString("pt-BR")}.`
          : `O progresso expira após ${vip.diasInatividade} dias sem atividade elegível.`}</p>
        </section>
        <section className="lc-vip-levels" aria-label="Níveis VIP">
          ${vip.niveis.map(n => html`<article key=${n.id}><div><h2>${n.nome}</h2>
            <p>${n.pontos} pontos · ${n.premio}</p></div>
            <button className="lc-btn lc-btn-primary" disabled=${!n.disponivel || busy !== null}
              onClick=${() => claim(n.id)}>${n.resgatado ? "Recebido" : n.disponivel ? "Receber" : "Bloqueado"}</button></article>`)}
        </section>
        <h2>Histórico de pontos</h2>
        ${vip.historico.map((h, index) => html`<div key=${index} className="lc-summary-line">
          <span>${h.origem.startsWith("COMPRA") ? "Compra aprovada" : h.origem.startsWith("MISSAO") ? "Missão concluída"
            : h.origem.startsWith("EVENTO") ? "Evento" : "Bônus do passe"}</span><strong>+${h.pontos} pontos</strong></div>`)}
      ` : html`<p role="status">Carregando VIP…</p>`}
  </main>`;
}

function BenefitsPage({ auth, onAuthRefresh, notify }) {
  const [collection, setCollection] = useState(null);
  const [error, setError] = useState("");
  const [applying, setApplying] = useState(null);
  useEffect(() => {
    if (!auth.authenticated) return;
    let active = true;
    getJson("/api/pass/rewards").then(data => { if (active) setCollection(data); })
      .catch(() => { if (active) setError("Não foi possível carregar seus benefícios."); });
    return () => { active = false; };
  }, [auth.authenticated]);

  async function apply(reward) {
    if (applying !== null) return;
    setApplying(reward.id);
    setError("");
    try {
      await postJson(`/api/pass/rewards/${reward.id}/apply`, {});
      onAuthRefresh();
      notify("Benefício aplicado", reward.titulo);
    } catch (failure) {
      setError(failure.message || "Não foi possível aplicar o benefício.");
    } finally {
      setApplying(null);
    }
  }

  return html`<main className="lc-page">
    <div className="lc-section-header">
      <h1>Meu Baú</h1><${AppLink} href="/conta">Voltar à conta<//>
    </div>
    ${error ? html`<p className="lc-form-notice error" role="alert">${error}</p>` : null}
    ${auth.ready && !auth.authenticated ? html`<${AppLink} href=${buildLoginUrl("/conta/beneficios")}
      className="lc-btn lc-btn-primary">Entrar para ver meus benefícios<//>` : null}
    ${auth.authenticated && !collection && !error ? html`<p role="status">Carregando benefícios…</p>` : null}
    ${auth.authenticated ? html`<${BauItems} />` : null}
    ${auth.authenticated && collection ? html`<section className="lc-checkout-card lc-benefit-list">
      ${collection.length === 0 ? html`<p>Você ainda não tem benefícios resgatados.</p><${AppLink} href="/passe">Ver passe →<//>` : null}
      ${collection.filter(reward => reward.tipo !== "Fisico").map(reward => html`<div key=${reward.id} className="lc-summary-line">
        <strong>${reward.titulo} · ${reward.tipo}</strong>
        ${!reward.ativo ? html`<span>Indisponível</span>`
          : reward.tipo === "Cupom" ? html`<span>
            <code>${reward.usado ? "Utilizado / reservado" : reward.codigo}</code>
            · ${currency(reward.valor)} em produtos a partir de ${currency(reward.minimo)} · até ${reward.validade}
          </span>`
          : ["Tema", "Avatar", "Badge"].includes(reward.tipo)
            ? html`<button type="button" className="lc-btn lc-btn-ghost" disabled=${applying !== null}
                onClick=${() => apply(reward)}>${applying === reward.id ? "Aplicando…" : "Aplicar benefício"}</button>`
            : html`<a href=${`/api/pass/rewards/${reward.id}/download`}>Baixar benefício</a>`}
      </div>`)}
    </section>` : null}
  </main>`;
}

function MissionProductShelf({ dashboard, demoProducts }) {
  const reviewMissions = pendingReviewMissions(dashboard);
  const reviews = dashboard?.produtosParaAvaliar || [];
  const demos = demoProducts.filter(product => !reviews.some(review => review.id === product.id))
    .slice(0, 2);
  if (!reviews.length && !demos.length) return null;

  const items = [
    ...reviews.map(product => ({ product, label: reviewMissionLabel(reviewMissions), review: true })),
    ...demos.map(product => ({ product, label: "XP extra · exemplo", review: false })),
  ];
  return html`<section className="lc-mission-products" aria-labelledby="mission-products">
    <h2 id="mission-products">Produtos e missões</h2>
    <div className="lc-mission-product-grid">
      ${items.map(({ product, label, review }) => html`<${ProductCard} key=${product.id}
        product=${product} badge=${label} href=${`/produto/${product.id}${review ? "?avaliar=1" : ""}`} />`)}
    </div>
  </section>`;
}

function PassMissionsPage({ auth, demoProducts }) {
  const [dashboard, setDashboard] = useState(null);
  const [events, setEvents] = useState([]);
  const [selected, setSelected] = useState(0);
  const [error, setError] = useState("");
  const tabs = ["Diárias", "Semanais", "Mensais", "Especiais", "Premium"];

  useEffect(() => {
    if (!auth.ready) return;
    let active = true;
    const request = auth.authenticated
      ? trackPassAction("CHECKIN").then(() => getJson("/api/pass/current"))
      : getJson("/api/pass/missions/preview");
    request
      .then(data => { if (active) setDashboard(data); })
      .catch(failure => {
        if (!active) return;
        if (failure.code === "AUTH_REQUIRED") requireLogin("/passe/missoes");
        else setError("Não foi possível carregar as missões.");
      });
    return () => { active = false; };
  }, [auth.authenticated, auth.ready]);

  useEffect(() => {
    getJson("/api/events")
      .then(data => setEvents(data.filter(event => event.estado !== "Encerrado").slice(0, 4)))
      .catch(() => {});
  }, []);

  const missionGroups = dashboard ? [dashboard.missoesDiarias, dashboard.missoesSemanais, dashboard.missoesMensais] : [];
  const missions = selected === 4 ? missionGroups.flat().filter(m => m.premium)
    : (missionGroups[selected] || []).filter(m => !m.premium);
  return html`<main className="lc-missions-page">
    <header className="lc-rewards-banner lc-missions-banner">
      <div className="lc-page lc-rewards-banner-content">
        <span className="lc-eyebrow">LEVEL REWARDS</span>
        <h1>Missões do passe</h1>
        <p>Escolha uma missão, acompanhe o progresso e ganhe XP.</p>
        <${AppLink} href="/passe" className="lc-btn lc-neon-btn">Ver passe mensal<//>
      </div>
    </header>
    <div className="lc-page lc-rewards-content">
    ${auth.ready && !auth.authenticated ? html`<p><${AppLink} href=${buildLoginUrl("/passe/missoes")}
      className="lc-btn lc-btn-primary">Entrar para ver minhas missões<//></p>` : null}
    ${error ? html`<p className="lc-form-notice error" role="alert">${error}</p>` : null}
    <div className="lc-mission-tabs" role="tablist" aria-label="Tipos de missão">
      ${tabs.map((label, index) => html`<button key=${label} type="button" role="tab"
        id=${`mission-tab-${index}`} aria-controls="mission-panel" aria-selected=${selected === index}
        tabIndex=${selected === index ? 0 : -1}
        onClick=${() => setSelected(index)}
        onKeyDown=${event => {
          const delta = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;
          if (!delta) return;
          event.preventDefault();
          const next = (index + delta + tabs.length) % tabs.length;
          setSelected(next);
          event.currentTarget.parentElement.querySelectorAll("button")[next].focus();
        }}>${label}</button>`)}
    </div>
    <section id="mission-panel" className="lc-mission-panel" role="tabpanel" aria-labelledby=${`mission-tab-${selected}`}>
      ${selected === 3 ? html`
        <div className="lc-missions-grid">
          ${events.map(event => html`<article key=${event.id} className="lc-mission-row">
            <div><h2>${event.titulo}</h2><p>${event.descricao}</p></div>
            <div className="lc-mission-meta"><strong>${event.estado}</strong>
              <${AppLink} href="/passe#eventos" className="lc-btn lc-btn-ghost lc-mission-cta">Ver evento<//></div>
          </article>`)}
        </div>
        ${events.length === 0 ? html`<p className="lc-muted">Nenhuma missão especial disponível no momento.</p>` : null}`

        : !dashboard ? !error ? html`<p role="status">Carregando missões…</p>` : null
        : html`<div className="lc-missions-grid">
          ${(missions || []).map(mission => html`<article key=${mission.id} className="lc-mission-row">
            <div><h2>${mission.titulo}${mission.premium ? " · Premium" : ""}</h2><p>${mission.descricao}</p>
              <progress aria-label=${`Progresso de ${mission.titulo}`} value=${mission.progressoAtual}
                max=${mission.metaValor}></progress>
              <small>Renova em ${mission.resetaEm}</small>
            </div>
            <div className="lc-mission-meta">
              <strong>+${mission.xpRecompensa} XP</strong>
              <span>${mission.concluida ? "Concluída" : `${mission.progressoAtual}/${mission.metaValor}`}</span>
              ${mission.premium && !dashboard.premiumAtivo ? html`<span>🔒 Requer Premium</span>`
                : !mission.concluida && mission.ctaHref ? html`<${AppLink} href=${mission.ctaHref}
                className="lc-btn lc-btn-ghost lc-mission-cta">${mission.ctaLabel}<//>`
                : !mission.concluida && mission.ctaLabel ? html`<small>${mission.ctaLabel}</small>` : null}
            </div>
          </article>`)}
        </div>`}
    </section>
    <${MissionProductShelf} dashboard=${dashboard} demoProducts=${demoProducts} />
    </div>
  </main>`;
}

function Footer() {
  return html`<footer className="lc-footer">
    <div className="lc-footer-inner">
      <div className="lc-footer-columns">
        <section aria-labelledby="footer-about">
          <h2 id="footer-about">Institucional</h2>
          <p><strong>${BRAND}</strong></p>
          <p>Loja com missões, XP e recompensas.</p>
          <details><summary>Privacidade</summary>
            <p>O endereço salvo preenche a entrega. A sacola e a preferência de tema são guardadas no navegador.</p>
          </details>
        </section>
        <nav aria-labelledby="footer-support">
          <h2 id="footer-support">Suporte</h2>
          <${AppLink} href="/pedidos">Acompanhar pedido<//>
          <${AppLink} href="/conta">Minha conta<//>
          <${AppLink} href="/conta/beneficios">Meu Baú<//>
        </nav>
        <section aria-labelledby="footer-contact">
          <h2 id="footer-contact">Contato</h2>
          <p>suporte@levelcommerce.example</p>
          <p>(00) 0000-0000</p>
          <small>Canais fictícios para demonstração.</small>
        </section>
        <section aria-labelledby="footer-company">
          <h2 id="footer-company">Dados da empresa</h2>
          <address>LEVEL COMMERCE LTDA<br />Rua Exemplo, 100 · Centro<br />São Paulo / SP · CEP 00000-000</address>
          <p>CNPJ 00.000.000/0000-00</p>
          <small>Endereço e cadastro fictícios.</small>
        </section>
      </div>
      <div className="lc-footer-bottom">
        <span>${BRAND}</span>
        <span>Ambiente demonstrativo</span>
      </div>
    </div>
  </footer>`;
}

function GainToast({ item, dismiss }) {
  useEffect(() => {
    const timer = window.setTimeout(() => dismiss(item.id), 5000);
    return () => window.clearTimeout(timer);
  }, [item.id, dismiss]);
  const [value, ...details] = item.kind === "xp" ? item.text.split(" · ") : ["", item.text];
  return html`<div className="lc-toast lc-gain-toast">
    <span className="lc-gain-mark" aria-hidden="true">
      ${item.kind === "xp" ? "XP" : html`<${Icon} name="check" />`}
    </span>
    <div><strong>${item.title}</strong>
      ${value ? html`<strong className="lc-gain-value">${value}</strong>` : null}
      <span>${details.join(" · ")}</span>
      ${item.kind === "xp" ? html`<${AppLink} href="/passe" className="lc-toast-link">Ver passe<//>` : null}
    </div>
    <button type="button" aria-label="Fechar aviso" onClick=${() => dismiss(item.id)}>×</button>
  </div>`;
}

function App() {
  const route = useRoute();
  const { path } = parseRoute(route);
  const [dark, setDark] = useState(() => document.documentElement.dataset.colorMode === "dark");
  const toggleTheme = () => {
    const mode = dark ? "light" : "dark";
    document.documentElement.dataset.colorMode = mode;
    try { localStorage.setItem("level-commerce-color-mode", mode); } catch {}
    setDark(!dark);
  };
  const cart = useLocalCart();
  const [cartCoupon, setCartCoupon] = useState("");
  const [immediateItem, setImmediateItem] = useState(() => readStorage(BUY_NOW_KEY, null, sessionStorage));
  const immediateCheckout = parseRoute(route).params.get("modo") === "imediato";
  const clearImmediateCheckout = () => {
    sessionStorage.removeItem(BUY_NOW_KEY);
    setImmediateItem(null);
  };
  const [homeData, setHomeData] = useState(null);
  const [auth, setAuth] = useState({ authenticated: false, ready: false });
  const [address, setAddress] = useState(null);
  const [notice, setNotice] = useState(null);
  const [gains, setGains] = useState([]);
  const gainSerial = useRef(0);
  const gainCursor = useRef(new Map());
  const dismissGain = useMemo(() => id => setGains(items => items.filter(item => item.id !== id)), []);
  const [notificationBox, setNotificationBox] = useState(null);
  const toastTimer = useRef(null);
  const authPageTypes = {
    "/login": "login", "/cadastro": "register",
    "/recuperar-senha": "forgot", "/redefinir-senha": "reset",
  };
  const authPageType = authPageTypes[path];

  const refreshAuth = () => {
    getJson("/api/auth/me").then((data) => {
      setAuth({ ...data, ready: true });
      document.body.dataset.tema = data.tema || "padrao";
    }).catch(() => setAuth({ authenticated: false, ready: true }));
  };

  const refreshNotifications = () => {
    if (auth.authenticated)
      getJson("/api/account/notifications").then(setNotificationBox).catch(() => {});
  };

  const notify = (title, text = "", error = false, kind, href, undo) => {
    setNotice({ title, text, error, kind, href, undo });
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setNotice(null), href || undo ? 8000 : 4500);
    if (kind !== "mission") refreshNotifications();
  };

  const notifyGain = (title, text, kind) => {
    const item = { id: `gain-${++gainSerial.current}`, title, text, kind };
    setGains(items => [...items, item]);
    refreshNotifications();
  };

  useEffect(() => {
    if (!auth.authenticated || !notificationBox?.itens?.length) return;
    const key = `level-commerce-xp-cursor-${auth.id}`;
    const previous = Math.max(gainCursor.current.get(key) || 0, readStorage(key, 0));
    const latest = Math.max(previous, ...notificationBox.itens.map(item => item.id));
    if (latest === previous) return;
    gainCursor.current.set(key, latest);
    try { localStorage.setItem(key, String(latest)); } catch {}
    const updates = notificationBox.itens
      .filter(item => item.id > previous && !item.lida && item.titulo === "XP recebido")
      .reverse()
      .map(item => ({ id: `xp-${item.id}`, title: item.titulo, text: item.mensagem, kind: "xp" }));
    if (updates.length) setGains(items => [...items, ...updates]);
  }, [auth.id, auth.authenticated, notificationBox]);

  useEffect(() => {
    refreshAuth();
    return () => window.clearTimeout(toastTimer.current);
  }, []);
  useEffect(() => {
    if (!authPageType && !homeData) getJson("/api/store/home").then(setHomeData).catch(() => {});
  }, [authPageType, homeData]);
  useEffect(() => {
    if (!auth.authenticated) return;
    refreshNotifications();
    const interval = window.setInterval(refreshNotifications, path.startsWith("/pedidos/") ? 10_000 : 45_000);
    window.addEventListener("level-pass-action", refreshNotifications);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener("level-pass-action", refreshNotifications);
    };
  }, [auth.authenticated, path]);

  useEffect(() => {
    if (!auth.authenticated) return;
    let active = true;
    getJson("/api/account/address").then(data => { if (active) setAddress(data); }).catch(() => {});
    return () => { active = false; };
  }, [auth.authenticated]);

  const addToCartWithTracking = (product, quantity = 1) => {
    const existing = cart.items.find(item => item.id === product.id)?.quantity || 0;
    if (existing + quantity > product.estoque) {
      notify("Limite de estoque atingido", product.nome, true);
      return;
    }
    cart.addItem(product, quantity);
    trackPassAction("ADD_TO_CART", 1, `cart-${product.id}`);
    notify("Adicionado à sacola", product.nome);
  };

  const buyNow = (product) => {
    if (product.estoque <= 0) return;
    const item = cartItem(product, 1);
    sessionStorage.setItem(BUY_NOW_KEY, JSON.stringify(item));
    setImmediateItem(item);
    trackPassAction("ADD_TO_CART", 1, `buy-now-${product.id}`);
    navigate("/checkout?modo=imediato");
  };

  const currentScreen = useMemo(() => {
    if (path === "/conta") return html`<${AccountPage} auth=${auth} />`;
    if (path === "/conta/endereco") {
      if (auth.ready && !auth.authenticated) return html`<main className="lc-page">
        <${AppLink} href=${buildLoginUrl(path)} className="lc-btn lc-btn-primary">Entrar para salvar endereço<//>
      </main>`;
      if (!auth.ready) return null;
      return html`<${AddressPage} address=${address} onSaved=${setAddress} notify=${notify}
        backHref=${parseRoute(route).params.get("voltar") === "sacola" ? "/sacola" : "/conta"} />`;
    }
    if (path === "/conta/vip") return html`<${VipPage} auth=${auth} />`;
    const rewardDelivery = path.match(/^\/conta\/bau\/(\d+)\/entrega$/);
    if (rewardDelivery) return html`<${RewardDeliveryPage} id=${rewardDelivery[1]} auth=${auth} />`;
    if (path === "/conta/beneficios")
      return html`<${BenefitsPage} auth=${auth} onAuthRefresh=${refreshAuth} notify=${notify} />`;
    if (path === "/passe") {
      const products = [...(homeData?.destaques || []), ...(homeData?.novidades || [])];
      const uniqueProducts = [...new Map(products.map(product => [product.id, product])).values()];
      return html`<${PassPage} auth=${auth} route=${route} notify=${notify} notifyGain=${notifyGain} products=${uniqueProducts} />`;
    }
    if (path === "/passe/missoes") return html`<${PassMissionsPage} auth=${auth} demoProducts=${homeData?.destaques || []} />`;
    if (path === "/sacola") {
      return html`
        <${CartPage}
          items=${cart.items}
          updateQuantity=${cart.updateQuantity}
          auth=${auth}
          address=${auth.authenticated ? address : null}
          coupon=${cartCoupon}
          setCoupon=${setCartCoupon}
          removeItem=${id => {
            const index = cart.items.findIndex(item => item.id === id);
            if (index < 0) return;
            const item = cart.items[index];
            cart.removeItem(id);
            notify("Produto removido da sacola", item.nome, false, "cart", null, () => cart.restoreItem(item, index));
          }}
        />
      `;
    }
    if (path === "/checkout") {
      return html`<${CheckoutPage}
        items=${immediateCheckout ? immediateItem ? [immediateItem] : [] : cart.items}
        auth=${auth}
        address=${auth.authenticated ? address : null}
        initialCoupon=${immediateCheckout ? "" : cartCoupon}
        clearCart=${immediateCheckout ? clearImmediateCheckout : () => { cart.clearCart(); setCartCoupon(""); }}
      />`;
    }
    if (path === "/perfil") {
      return html`<${ProfilePage} auth=${auth} route=${route} onAuthRefresh=${refreshAuth} notify=${notify} />`;
    }
    if (path === "/pedidos") {
      return html`<${OrdersPage} />`;
    }
    if (path.startsWith("/pedidos/")) {
      const orderId = path.split("/")[2];
      return html`<${OrderDetailPage} orderId=${orderId} route=${route} notify=${notify} />`;
    }
    if (path === "/pagamento/retorno") {
      return html`<${PaymentResultPage} route=${route} />`;
    }
    if (path.startsWith("/catalogo") || path.startsWith("/produtos")) {
      return html`
        <${CatalogPage}
          route=${route}
        />
      `;
    }
    if (path.startsWith("/produto/")) {
      const productId = path.split("/")[2];
      return html`
        <${ProductDetailPage}
          key=${productId}
          productId=${productId}
          reviewRequested=${parseRoute(route).params.get("avaliar") === "1"}
          onAddToCart=${addToCartWithTracking}
          onBuyNow=${buyNow}
          auth=${auth}
          notify=${notify}
          address=${auth.authenticated ? address : null}
        />
      `;
    }
    if (!homeData)
      return html`<main className="lc-page">
        <p role="status">Carregando a loja…</p>
      </main>`;
    return html`
      <${HomePage}
        data=${homeData}
        auth=${auth}
        notifyGain=${notifyGain}
      />
    `;
  }, [path, route, homeData, auth, cart, cartCoupon, immediateItem, address]);

  const searchAndNavigate = (searchTerm) => {
    const params = new URLSearchParams();
    if (searchTerm) params.set("busca", searchTerm);
    navigate(`/catalogo${params.toString() ? `?${params.toString()}` : ""}`);
  };

  if (authPageType) return html`<${AuthPage} key=${authPageType} type=${authPageType}
    auth=${auth} route=${route} postJson=${postJson} csrf=${getCsrfToken()} />`;

  return html`
    <div className="lc-app-shell">
      <${NavBar}
        totalItems=${cart.totalItems}
        onSearch=${searchAndNavigate}
        categories=${homeData?.categorias || []}
        route=${route}
        dark=${dark}
        toggleTheme=${toggleTheme}
        address=${auth.authenticated ? address : null}
        notificationProps=${{
          box: notificationBox, refresh: refreshNotifications, openLink: navigate,
          readAll: (id) => postJson(`/api/account/notifications/read/${id}`, {})
            .then(refreshNotifications).catch(() => notify("Não foi possível marcar as notificações", "", true)),
        }}
        auth=${auth}
      />
      ${currentScreen}
      <${Footer} />
      <div className="lc-gain-region" role="status" aria-label="Ganhos de XP e prêmios">
        ${gains.length ? html`<${GainToast} key=${gains[0].id} item=${gains[0]} dismiss=${dismissGain} />` : null}
      </div>
      <div className="lc-toast-region" role="status">
        ${notice ? html`<div className=${`lc-toast ${notice.error ? "error" : ""}`}>
          ${notice.error ? null : html`<${Icon} name="check" />`}
          <div><strong>${notice.title}</strong>${notice.text ? html`<span>${notice.text}</span>` : null}
            ${notice.href ? html`<a href=${notice.href} className="lc-toast-link" onClick=${event => {
              if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey || event.button !== 0) return;
              event.preventDefault();
              setNotice(null);
              navigate(notice.href);
            }}>Ir para avaliação →</a>` : null}
            ${notice.undo ? html`<button type="button" className="lc-toast-undo" onClick=${() => {
              notice.undo();
              setNotice(null);
            }}>Desfazer</button>` : null}</div>
          <button type="button" aria-label="Fechar aviso" onClick=${() => setNotice(null)}>×</button>
        </div>` : null}
      </div>
    </div>
  `;
}

// Clear personal data left in the browser by earlier versions.
localStorage.removeItem("level-commerce-checkout");
localStorage.removeItem("level-commerce-checkout-request");

createRoot(document.getElementById("root")).render(html`<${App} />`);
