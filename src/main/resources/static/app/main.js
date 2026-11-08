import React, {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "https://esm.sh/react@18";
import { createRoot } from "https://esm.sh/react-dom@18/client";
import htm from "https://esm.sh/htm@3.1.1";

import { AuthPage, Brand, EventBadges, EventsPage, Icon, NotificationMenu } from "./experience.js";

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

function useRoute() {
  const getRoute = () =>
    `${window.location.pathname}${window.location.search || ""}`;
  const [route, setRoute] = useState(getRoute());

  useEffect(() => {
    const onChange = () => setRoute(getRoute());
    window.addEventListener("popstate", onChange);
    return () => window.removeEventListener("popstate", onChange);
  }, []);

  return route;
}

function navigate(path) {
  window.history.pushState({}, "", path);
  window.dispatchEvent(new PopStateEvent("popstate"));
  window.scrollTo(0, 0);
}

function parseRoute(route) {
  const [path, query = ""] = route.split("?");
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
  }).catch(() => null);
}

function claimPassReward(rewardId) {
  return postJson(`/api/pass/rewards/${rewardId}/claim`, {});
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

function NavBar({ totalItems, onSearch, auth, categories, notificationProps, path, dark, toggleTheme, address }) {
  const [search, setSearch] = useState(() => parseCatalogFilters(location.pathname + location.search).busca || "");
  return html`
    <header className="lc-nav-shell">
      <div className="lc-nav-top">
        <${Brand} />
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
          <${AppLink} href=${auth.authenticated ? "/conta" : "/login"} className="lc-nav-tool">
            <${Icon} name="user" /><span>${auth.authenticated ? "Minha conta" : "Entrar"}</span>
          <//>
          ${auth.authenticated ? html`<${NotificationMenu} ...${notificationProps} />` : null}
          <${AppLink} href="/sacola" className="lc-nav-tool lc-nav-bag">
            <${Icon} name="bag" /><span>Sacola</span><strong>${totalItems}</strong>
          <//>
        </nav>
      </div>
      <nav className="lc-nav-bottom" aria-label="Navegação da loja">
        <${AppLink} href=${auth.authenticated ? "/conta/endereco" : buildLoginUrl("/conta/endereco")} className="lc-nav-address">
          <${Icon} name="pin" />
          <span><small>Entregar em</small><strong>${address ? `${address.cidade} · ${formatCep(address.cep)}` : "Adicionar endereço"}</strong></span>
        <//>
        <${AppLink} href="/" current=${path === "/"}>Início<//>
        <details className="lc-nav-popover lc-categories-menu">
          <summary><${Icon} name="menu" /> Categorias</summary>
          <div className="lc-popover-panel">
            <a href="/catalogo">Ver todos os produtos</a>
            ${categories.map(category => html`<a key=${category.id} href=${`/catalogo?categoriaId=${category.id}`}>${category.nome}</a>`)}
          </div>
        </details>
        <${AppLink} href="/catalogo" current=${path === "/catalogo"}>Catálogo<//>
        <${AppLink} href=${auth.authenticated ? "/pedidos" : buildLoginUrl("/pedidos")}>Meus pedidos<//>
        <${AppLink} href="/passe" current=${path.startsWith("/passe")}>Passe LEVEL<//>
        <${AppLink} href="/eventos" current=${path === "/eventos"}>Eventos<//>
        ${auth.role === "ADMIN" || (!auth.authenticated && auth.ambienteLocal)
          ? html`<a className="lc-admin-link" href=${auth.authenticated ? "/admin" : buildLoginUrl("/admin")}>Administração</a>`
          : null}
      </nav>
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

function PassPreview({ auth }) {
  const [dashboard, setDashboard] = useState(null);

  useEffect(() => {
    if (!auth.authenticated) return;
    let active = true;
    trackPassAction("CHECKIN").then(() => getJson("/api/pass/current"))
      .then(data => { if (active) setDashboard(data); }).catch(() => {});
    return () => { active = false; };
  }, [auth.authenticated]);

  const pass = auth.authenticated ? dashboard : null;
  return html`<section className="lc-pass-spotlight" aria-labelledby="home-pass">
    <div>
      <span className="lc-eyebrow">LEVEL REWARDS</span>
      <h2 id="home-pass">Passe LEVEL</h2>
      <p>${pass ? pass.temporadaNome : "Ganhe XP e desbloqueie recompensas a cada nível."}</p>
    </div>
    ${pass ? html`<div className="lc-spotlight-progress">
      <strong>Nível ${pass.nivelAtual} · ${pass.xpTotal} XP</strong>
      <progress aria-label="Progresso da temporada" value=${Math.min(pass.xpTotal, pass.totalNiveis * pass.xpPorNivel)}
        max=${pass.totalNiveis * pass.xpPorNivel}></progress>
    </div>` : null}
    <${AppLink} href="/passe" className="lc-btn lc-neon-btn">Ver passe<//>
  </section>`;
}

function CategoryStrip({ categories }) {
  return html`
    <section className="lc-categories">
      ${categories.map(
        (category) => html`
          <${AppLink}
            key=${category.id}
            href=${`/catalogo?categoriaId=${category.id}`}
            className="lc-category-pill"
          >
            <strong>${category.nome}</strong>
            <span
              >${category.descricao || "Colecao premium para crescimento da loja"}</span
            >
          <//>
        `,
      )}
    </section>
  `;
}

function ProductCard({ product, onAddToCart }) {
  const href = `/produto/${product.id}`;
  return html`<article className="lc-product-card lc-shelf-card">
    <${AppLink} href=${href} className="lc-product-visual">
      <img src=${product.imagemUrl || "/images/sem-foto.png"} alt=${product.nome} loading="lazy"
        onError=${event => {
          if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
        }} />
    <//>
    <div className="lc-product-copy">
      <h3><${AppLink} href=${href}>${product.nome}<//></h3>
      <strong>${currency(product.preco)}</strong>
      <button type="button" className="lc-btn lc-btn-ghost" disabled=${product.estoque <= 0}
        onClick=${() => onAddToCart(product)}>${product.estoque > 0 ? "Adicionar" : "Esgotado"}</button>
    </div>
  </article>`;
}

function HomeProductShelf({ title, products, onAddToCart }) {
  if (!products.length) return null;
  return html`<section className="lc-section lc-home-shelf">
    <${SectionHeader} title=${title} actionLabel="Ver catálogo" />
    <div className="lc-home-products">
      ${products.slice(0, 6).map(product => html`
        <article className="lc-home-product" key=${product.id}>
          <${AppLink} href=${`/produto/${product.id}`} className="lc-home-product-photo">
            <img src=${product.imagemUrl || "/images/sem-foto.png"} alt=${product.nome} loading="lazy"
              onError=${event => {
                if (!event.currentTarget.src.endsWith("/images/sem-foto.png")) event.currentTarget.src = "/images/sem-foto.png";
              }} />
          <//>
          <div className="lc-home-product-info">
            <h3><${AppLink} href=${`/produto/${product.id}`}>${product.nome}<//></h3>
            <strong>${currency(product.preco)}</strong>
            <button type="button" className="lc-btn lc-btn-ghost" disabled=${product.estoque <= 0}
              onClick=${() => onAddToCart(product)}>${product.estoque > 0 ? "Adicionar" : "Esgotado"}</button>
          </div>
        </article>`)}
    </div>
  </section>`;
}

function HomePage({ data, onAddToCart, auth }) {
  return html`
    <main className="lc-home">
      <${BannerCarousel} banners=${data.banners} />
      <${PromoTicker} />
      <div className="lc-page lc-home-content">
        <${PassPreview} auth=${auth} />
        <${HomeProductShelf} title="Destaques da loja" products=${data.destaques} onAddToCart=${onAddToCart} />
        <section className="lc-home-campaigns" aria-label="Campanhas da LEVEL">
          <${AppLink} href="/eventos" className="lc-home-campaign rewards">
            <span>LEVEL REWARDS</span><strong>Eventos da semana</strong><small>Veja as campanhas e conquiste selos →</small>
          <//>
          <${AppLink} href="/catalogo" className="lc-home-campaign catalog">
            <span>EXPLORE A LOJA</span><strong>Encontre seu próximo upgrade</strong><small>Explorar catálogo →</small>
          <//>
        </section>
        <section className="lc-section lc-home-categories">
          <${SectionHeader} title="Compre por categoria" actionLabel="Todas as categorias" />
          <${CategoryStrip} categories=${data.categorias} />
        </section>
        <${HomeProductShelf} title="Novidades" products=${data.novidades} onAddToCart=${onAddToCart} />
      </div>
    </main>
  `;
}

function AccountPage({ auth }) {
  const options = auth.authenticated ? [
    ["/pedidos", "Meus pedidos", "Acompanhe pagamentos, envios e entregas."],
    ["/conta/endereco", "Endereço de entrega", "Gerencie o endereço usado nas suas compras."],
    ["/perfil", "Dados da conta", "Atualize seu nome e sua senha."],
    ["/passe", "Passe LEVEL", "Acompanhe sua assinatura, seus níveis e missões."],
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

function AddressPage({ address, onSaved, notify }) {
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
      <${AppLink} href="/conta">‹ Minha conta<//>
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

function CatalogFilters({ categories, filters, setFilters }) {
  return html`
    <aside className="lc-filter-card">
      <h3>Filtrar resultados</h3>
      <label>
        Busca
        <input
          value=${filters.busca}
          onChange=${(event) => setFilters({ ...filters, busca: event.target.value })}
          placeholder="Produto ou marca"
        />
      </label>
      <label>
        Categoria
        <select
          value=${filters.categoriaId}
          onChange=${(event) => setFilters({ ...filters, categoriaId: event.target.value })}
        >
          <option value="">Todas</option>
          ${categories.map(
            (category) => html`
              <option key=${category.id} value=${category.id}>
                ${category.nome}
              </option>
            `,
          )}
        </select>
      </label>
      <label>
        Preco minimo
        <input
          type="number"
          min="0"
          step="0.01"
          value=${filters.precoMin}
          onChange=${(event) => setFilters({ ...filters, precoMin: event.target.value })}
        />
      </label>
      <label>
        Preco maximo
        <input
          type="number"
          min="0"
          step="0.01"
          value=${filters.precoMax}
          onChange=${(event) => setFilters({ ...filters, precoMax: event.target.value })}
        />
      </label>
      <label>
        Ordenar por
        <select
          value=${filters.ordenacao}
          onChange=${(event) => setFilters({ ...filters, ordenacao: event.target.value })}
        >
          <option value="relevancia">Relevancia</option>
          <option value="preco-asc">Menor preco</option>
          <option value="preco-desc">Maior preco</option>
          <option value="recentes">Mais recentes</option>
          <option value="nome-desc">Nome Z-A</option>
        </select>
      </label>
    </aside>
  `;
}

function CatalogPage({ route, onAddToCart }) {
  const [data, setData] = useState({ categorias: [], produtos: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState(parseCatalogFilters(route));

  useEffect(() => {
    setFilters(parseCatalogFilters(route));
  }, [route]);

  useEffect(() => {
    const params = new URLSearchParams();
    if (filters.busca) params.set("busca", filters.busca);
    if (filters.categoriaId) params.set("categoriaId", filters.categoriaId);
    if (filters.precoMin) params.set("precoMin", filters.precoMin);
    if (filters.precoMax) params.set("precoMax", filters.precoMax);
    if (filters.ordenacao) params.set("ordenacao", filters.ordenacao);

    setLoading(true);
    getJson(`/api/store/catalog?${params.toString()}`)
      .then(setData)
      .finally(() => setLoading(false));
  }, [filters]);

  const updateFilters = (nextFilters) => {
    const params = new URLSearchParams();
    if (nextFilters.busca) params.set("busca", nextFilters.busca);
    if (nextFilters.categoriaId)
      params.set("categoriaId", nextFilters.categoriaId);
    if (nextFilters.precoMin) params.set("precoMin", nextFilters.precoMin);
    if (nextFilters.precoMax) params.set("precoMax", nextFilters.precoMax);
    if (nextFilters.ordenacao && nextFilters.ordenacao !== "relevancia")
      params.set("ordenacao", nextFilters.ordenacao);
    navigate(`/catalogo${params.toString() ? `?${params.toString()}` : ""}`);
  };

  return html`
    <main className="lc-page lc-catalog-page">
      <section className="lc-catalog-shell">
        <${CatalogFilters}
          categories=${data.categorias}
          filters=${filters}
          setFilters=${updateFilters}
        />
        <div className="lc-catalog-results">
          <div className="lc-section-header">
            <div>
              <h1>Catálogo</h1>
              <p>
                ${loading ? "Atualizando produtos..." : `${data.total} produtos encontrados.`}
              </p>
            </div>
          </div>
          <div className="lc-product-grid">
            ${data.produtos.map(
              (product) => html`
                <${ProductCard}
                  key=${product.id}
                  product=${product}
                  onAddToCart=${onAddToCart}
                />
              `,
            )}
          </div>
        </div>
      </section>
    </main>
  `;
}

function ProductDetailPage({ productId, onAddToCart, onBuyNow, auth, notify, address }) {
  const [product, setProduct] = useState(null);
  const [reviewForm, setReviewForm] = useState({ nota: "5", comentario: "" });
  const [reviewMessage, setReviewMessage] = useState("");

  useEffect(() => {
    getJson(`/api/store/products/${productId}`).then(setProduct);
  }, [productId]);

  useEffect(() => {
    trackPassAction("VIEW_PRODUCT", 1, `product-${productId}`);
  }, [productId]);

  const submitReview = async (event) => {
    event.preventDefault();
    setReviewMessage("");
    try {
      const response = await postJson(
        `/api/store/products/${productId}/reviews`,
        {
          nota: Number(reviewForm.nota),
          comentario: reviewForm.comentario,
        },
      );
      setProduct(response);
      setReviewForm({ nota: "5", comentario: "" });
      notify("Avaliação enviada");
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin(`/produto/${productId}`);
        return;
      }
      setReviewMessage("Nao foi possivel enviar a avaliacao.");
    }
  };

  if (!product)
    return html`<main className="lc-page"><p>Carregando produto...</p></main>`;

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
          <div className="lc-card-overlay detail">
            ${product.destaque ? html`<span className="lc-tag">Produto destaque</span>` : null}
            <span className="lc-outline-tag"
              >${product.sku || "LEVEL SKU"}</span
            >
          </div>
          <span className="lc-product-brand">${product.marca || BRAND}</span>
          <h1>${product.nome}</h1>
          <p className="lc-detail-price">${currency(product.preco)}</p>
          <div className="lc-review-summary">
            <strong
              >${product.mediaAvaliacoes?.toFixed ? product.mediaAvaliacoes.toFixed(1) : product.mediaAvaliacoes || "0.0"}</strong
            >
            <span>${product.totalAvaliacoes} avaliações</span>
          </div>
          <p className="lc-detail-short">
            ${product.descricaoCurta || product.descricao}
          </p>
          <p className="lc-detail-stock">
            ${product.estoque > 0 ? `${product.estoque} unidades disponiveis` : "Produto indisponivel"}
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

      <section className="lc-section lc-review-grid">
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
        <div className="lc-checkout-card">
          <h3>Avaliar produto</h3>
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
                    ${reviewMessage ? html`<p className="lc-checkout-note">${reviewMessage}</p>` : null}
                    <button type="submit" className="lc-btn lc-btn-primary">
                      Enviar avaliacao
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
                onAddToCart=${onAddToCart}
              />
            `,
          )}
        </div>
      </section>

    </main>
  `;
}

function CartPage({ items, updateQuantity, removeItem }) {
  const subtotal = useMemo(
    () => items.reduce((sum, item) => sum + item.preco * item.quantity, 0),
    [items],
  );
  const freight =
    subtotal >= 299 ? 0 : subtotal >= 149 ? 14.9 : items.length ? 24.9 : 0;
  const total = subtotal + freight;

  return html`
    <main className="lc-page lc-cart-page">
      <section className="lc-cart-shell">
        <div className="lc-cart-list">
          <div className="lc-section-header">
            <div>
              <h1>Minha sacola</h1>
              <p>Revise os itens antes de seguir para endereco e pagamento.</p>
            </div>
          </div>
          ${items.length === 0 ? html`<p className="lc-empty-state">Sua sacola ainda esta vazia.</p>` : null}
          ${items.map(
            (item) => html`
              <article key=${item.id} className="lc-cart-item">
                <img
                  src=${item.imagemUrl || "/images/sem-foto.png"}
                  alt=${item.nome}
                />
                <div>
                  <${AppLink} href=${`/produto/${item.id}`}><strong>${item.nome}</strong><//>
                  <span>${currency(item.preco)} por unidade</span>
                  <div className="lc-qty-row">
                    <button
                      type="button"
                      onClick=${() => updateQuantity(item.id, item.quantity - 1)}
                    >
                      -
                    </button>
                    <span>${item.quantity}</span>
                    <button
                      type="button"
                      onClick=${() => updateQuantity(item.id, item.quantity + 1)}
                    >
                      +
                    </button>
                    <button
                      type="button"
                      className="lc-link-button"
                      onClick=${() => removeItem(item.id)}
                    >
                      Remover
                    </button>
                  </div>
                </div>
                <strong>${currency(item.preco * item.quantity)}</strong>
              </article>
            `,
          )}
        </div>
        <aside className="lc-cart-summary">
          <span>Resumo da compra</span>
          <div className="lc-summary-line">
            <span>Subtotal</span><strong>${currency(subtotal)}</strong>
          </div>
          <div className="lc-summary-line">
            <span>Frete</span
            ><strong>${freight === 0 ? "Gratis" : currency(freight)}</strong>
          </div>
          <hr />
          <strong className="lc-summary-total">${currency(total)}</strong>
          <p className="lc-checkout-note">
            ${freight === 0 ? "Voce atingiu frete gratis." : "Frete gratis a partir de R$ 299,00."}
          </p>
          <${AppLink} href=${items.length ? "/checkout" : "/catalogo"} className="lc-btn lc-btn-primary">
            ${items.length ? "Continuar compra" : "Ver catálogo"}
          <//>
        </aside>
      </section>
    </main>
  `;
}

function CheckoutPage({ items, auth, clearCart, address }) {
  const [paymentConfig, setPaymentConfig] = useState(null);
  useEffect(() => {
    if (auth.authenticated)
      getJson("/api/checkout/config")
        .then(setPaymentConfig)
        .catch(() => {});
  }, [auth.authenticated]);
  const [form, setForm] = useState(() => ({ ...initialCheckoutForm(), ...address }));
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const subtotal = useMemo(
    () => items.reduce((sum, item) => sum + item.preco * item.quantity, 0),
    [items],
  );
  const freight =
    subtotal >= 299 ? 0 : subtotal >= 149 ? 14.9 : items.length ? 24.9 : 0;
  const total = subtotal + freight;

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
          <div className="lc-checkout-card">
            <h3>Endereco de entrega</h3>
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
                >Endereco<input
                  value=${form.endereco}
                  onChange=${(e) => setForm({ ...form, endereco: e.target.value })}
              /></label>
            </div>
            <div className="lc-form-grid three">
              <label
                >Numero<input
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
          </div>
          <div className="lc-checkout-card">
            <label
              >Cupom da sua coleção<input
                value=${form.cupom || ""}
                maxlength="60"
                onChange=${(e) => setForm({ ...form, cupom: e.target.value })}
                placeholder="LC-..."
            /></label>
            <p className="lc-checkout-note">
              O desconto é validado no servidor e aparece no total final do
              pedido.
            </p>
            <h3>Forma de pagamento</h3>
            ${paymentConfig && !paymentConfig.enabled ? html`<p role="status" className="lc-checkout-note">Modo demonstração: nenhuma cobrança será feita. O pedido será salvo como pendente para testar a jornada.</p>` : null}
            <div className="lc-payment-options">
              ${[
                ["pix", "Pix"],
                ["cartao", "Cartao de credito"],
                ["boleto", "Boleto bancario"],
              ].map(
                ([value, label]) => html`
                  <button
                    key=${value}
                    className=${`lc-payment-option ${form.pagamento === value ? "ativo" : ""}`}
                    onClick=${() => setForm({ ...form, pagamento: value })}
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
          </div>
        </div>
        <aside className="lc-cart-summary">
          <span>Resumo da compra</span>
          ${items.map(
            (item) => html`
              <div key=${item.id} className="lc-summary-line">
                <span>${item.nome} x ${item.quantity}</span>
                <strong>${currency(item.preco * item.quantity)}</strong>
              </div>
            `,
          )}
          <hr />
          <div className="lc-summary-line">
            <span>Subtotal</span><strong>${currency(subtotal)}</strong>
          </div>
          <div className="lc-summary-line">
            <span>Frete</span
            ><strong>${freight === 0 ? "Gratis" : currency(freight)}</strong>
          </div>
          <strong className="lc-summary-total">${currency(total)}</strong>
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
      <${AppLink} href="/perfil?aba=progresso" current=${active === "progresso"}>Progresso e XP<//>
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

function OrdersPage() {
  const [orders, setOrders] = useState(null);

  useEffect(() => {
    getJson("/api/account/orders")
      .then(setOrders)
      .catch((error) => {
        if (error.code === "AUTH_REQUIRED") {
          requireLogin("/pedidos");
        }
      });
  }, []);

  if (!orders) {
    return html`<main className="lc-page"><p>Carregando pedidos...</p></main>`;
  }

  return html`
    <main className="lc-page">
      <section className="lc-account-shell">
        <div className="lc-section-header">
          <div>
            <h2>Meus pedidos</h2>
            <p>Historico completo de compras, status e proximos passos.</p>
          </div>
          <${AppLink} href="/perfil">Meu perfil<//>
        </div>
        <div className="lc-orders-list">
          ${orders.length === 0 ? html`<p className="lc-empty-state">Voce ainda nao tem pedidos realizados.</p>` : null}
          ${orders.map(
            (order) => html`
              <${AppLink}
                key=${order.id}
                href=${`/pedidos/${order.id}`}
                className="lc-order-card"
              >
                <div className="lc-order-card-top">
                  <div>
                    <strong>${order.numero}</strong>
                    <span>${order.dataPedido}</span>
                  </div>
                  <span
                    className=${`lc-status-pill ${order.status.toLowerCase()}`}
                    >${orderStatusLabel(order.status)}</span
                  >
                </div>
                <div className="lc-order-card-grid">
                  <div>
                    <small>Total</small
                    ><strong>${currency(order.total)}</strong>
                  </div>
                  <div>
                    <small>Itens</small><strong>${order.totalItens}</strong>
                  </div>
                  <div>
                    <small>Pagamento</small
                    ><strong
                      >${paymentStatusLabel(order.pagamentoStatus)}</strong
                    >
                  </div>
                </div>
              <//>
            `,
          )}
        </div>
      </section>
    </main>
  `;
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

function PassPage({ auth, notify }) {
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(null);
  const track = useRef(null);
  const guided = useRef(false);

  useEffect(() => {
    if (!auth.authenticated) return;
    let active = true;
    trackPassAction("CHECKIN").then(() => getJson("/api/pass/current"))
      .then(data => { if (active) setDashboard(data); })
      .catch(() => { if (active) setError("Não foi possível carregar o passe."); });
    return () => { active = false; };
  }, [auth.authenticated]);

  const pass = auth.authenticated ? dashboard : null;
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
  const moveTrack = (direction) => {
    const rail = track.current;
    rail?.scrollBy({
      left: direction * rail.clientWidth * 0.8,
      behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth",
    });
  };

  async function claim(reward) {
    if (busy !== null || !reward.disponivelParaResgate) return;
    setBusy(reward.recompensaId);
    setError("");
    try {
      const result = await claimPassReward(reward.recompensaId);
      notify("Recompensa resgatada", result.titulo);
      setDashboard(await getJson("/api/pass/current"));
    } catch (failure) {
      if (failure.code === "AUTH_REQUIRED") requireLogin("/passe");
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
    if (reward.premium !== premium) return html`<div className=${`lc-rail-reward ${premium ? "premium" : "free"} empty`} aria-hidden="true">—</div>`;
    const available = reward.disponivelParaResgate && !reward.resgatado;
    return html`<div className=${`lc-rail-reward ${premium ? "premium" : "free"} ${available ? "available" : ""}`}>
      <span>${reward.tipoDigital}</span>
      <strong title=${reward.descricao}>${reward.titulo}</strong>
      ${available ? html`<button type="button" className="lc-btn lc-btn-primary"
        disabled=${busy !== null} aria-label=${`Resgatar nível ${reward.nivel}: ${reward.titulo}`}
        onClick=${() => claim(reward)}>${busy === reward.recompensaId ? "Resgatando…" : "Resgatar"}</button>`
        : html`<small>${reward.resgatado ? "Resgatado" : premium && !pass.premiumAtivo ? "Passe premium" : "Bloqueado"}</small>`}
    </div>`;
  };

  return html`<main className="lc-page lc-level-pass">
    <header className="lc-level-pass-header">
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
        <${AppLink} href="/passe/missoes" className="lc-btn lc-btn-ghost">Ver missões<//>
      </div>` : auth.ready && !auth.authenticated
        ? html`<${AppLink} href=${buildLoginUrl("/passe")} className="lc-btn lc-neon-btn">Entrar para ver meu passe<//>`
        : null}
    </header>
    ${error ? html`<p className="lc-form-notice error" role="alert">${error}</p>` : null}
    ${auth.authenticated && !pass && !error ? html`<p role="status">Carregando temporada…</p>` : null}
    ${pass ? html`<section className="lc-level-trail" aria-labelledby="level-trail-title">
      <div className="lc-level-trail-heading">
        <div><h2 id="level-trail-title">Trilha de recompensas</h2>
          <p>Os prêmios de cada trilha liberam conforme você alcança os níveis.</p></div>
      </div>
      <div className=${`lc-premium-access ${pass.premiumAtivo ? "active" : "locked"}`}>
        <span className="lc-premium-badge"><${Icon} name=${pass.premiumAtivo ? "shield" : "lock"} /></span>
        <div><strong>${pass.premiumAtivo ? "Premium ativo" : "Trilha Premium bloqueada"}</strong>
          <small>${pass.premiumAtivo ? `Acesso até ${new Date(pass.premiumValidoAte).toLocaleDateString("pt-BR")}`
            : `${currency(pass.precoPremium)} por mês${auth.ambienteLocal ? " · ativação de teste" : ""}`}</small></div>
        ${!pass.premiumAtivo && auth.ambienteLocal ? html`<button type="button" className="lc-btn lc-neon-btn"
          disabled=${busy !== null} onClick=${buyPremium}>Liberar Premium</button>` : null}
      </div>
      <div className="lc-rail-toolbar">
        <span>Deslize a trilha para explorar os níveis.</span>
        <div>
          <button type="button" className="lc-btn lc-btn-ghost" aria-label="Níveis anteriores" onClick=${() => moveTrack(-1)}>←</button>
          <button type="button" className="lc-btn lc-btn-ghost" aria-label="Próximos níveis" onClick=${() => moveTrack(1)}>→</button>
        </div>
      </div>
      <div className=${`lc-rail ${pass.premiumAtivo ? "" : "lc-rail-locked"}`}>
        <div className="lc-rail-labels" aria-hidden="true"><strong className="premium">Premium</strong><span>Nível</span><strong className="free">Gratuito</strong></div>
        <div className="lc-rail-track" ref=${track} tabIndex="0" role="region" aria-label="Níveis e recompensas do passe">
          ${pass.niveis.map(reward => html`
            <div className=${`lc-rail-level ${reward.desbloqueado ? "unlocked" : ""}`}
              key=${reward.nivel} aria-label=${`Nível ${reward.nivel}, trilha ${reward.premium ? "premium" : "gratuita"}`}>
              ${rewardCell(reward, true)}
              <span className="lc-rail-node" aria-current=${reward.nivel === pass.nivelAtual ? "step" : undefined}>${reward.nivel}</span>
              ${rewardCell(reward, false)}
            </div>`)}
        </div>
      </div>
      ${pass.historico.length ? html`<details className="lc-pass-xp-history">
        <summary>Últimos ganhos de XP</summary>
        ${pass.historico.map((item, index) => html`<div className="lc-summary-line" key=${index}>
          <span>${item.descricao}</span><strong>+${item.xpGanho} XP</strong>
        </div>`)}
      </details>` : null}
    </section>` : null}
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
      <h1>Cupons e recompensas</h1><${AppLink} href="/conta">Voltar à conta<//>
    </div>
    ${error ? html`<p className="lc-form-notice error" role="alert">${error}</p>` : null}
    ${auth.ready && !auth.authenticated ? html`<${AppLink} href=${buildLoginUrl("/conta/beneficios")}
      className="lc-btn lc-btn-primary">Entrar para ver meus benefícios<//>` : null}
    ${auth.authenticated && !collection && !error ? html`<p role="status">Carregando benefícios…</p>` : null}
    ${auth.authenticated && collection ? html`<section className="lc-checkout-card lc-benefit-list">
      ${collection.length === 0 ? html`<p>Você ainda não tem benefícios resgatados.</p><${AppLink} href="/passe">Ver passe →<//>` : null}
      ${collection.map(reward => html`<div key=${reward.id} className="lc-summary-line">
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

function PassMissionsPage() {
  const [dashboard, setDashboard] = useState(null);
  const [events, setEvents] = useState([]);
  const [selected, setSelected] = useState(0);
  const [error, setError] = useState("");
  const tabs = ["Diárias", "Semanais", "Mensais", "Especiais"];

  useEffect(() => {
    trackPassAction("CHECKIN").then(() => getJson("/api/pass/current"))
      .then(setDashboard)
      .catch(failure => {
        if (failure.code === "AUTH_REQUIRED") requireLogin("/passe/missoes");
        else setError("Não foi possível carregar as missões.");
      });
    getJson("/api/events")
      .then(data => setEvents(data.filter(event => event.estado !== "Encerrado").slice(0, 4)))
      .catch(() => {});
  }, []);

  const missions = dashboard ? [dashboard.missoesDiarias, dashboard.missoesSemanais, dashboard.missoesMensais][selected] : [];
  return html`<main className="lc-page lc-missions-page">
    <header className="lc-store-heading">
      <${AppLink} href="/passe">‹ Passe LEVEL<//>
      <h1>Missões do passe</h1>
      <p>Confira seus objetivos e acompanhe o XP que cada um rende.</p>
    </header>
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
    <section id="mission-panel" role="tabpanel" aria-labelledby=${`mission-tab-${selected}`}>
      ${selected === 3 ? html`
        <div className="lc-missions-grid">
          ${events.map(event => html`<article key=${event.id} className="lc-mission-row">
            <div><h2>${event.titulo}</h2><p>${event.descricao}</p></div>
            <div className="lc-mission-meta"><strong>${event.estado}</strong>
              <${AppLink} href="/eventos">Ver evento →<//></div>
          </article>`)}
        </div>
        ${events.length === 0 ? html`<p className="lc-muted">Nenhuma missão especial disponível no momento.</p>` : null}`
        : !dashboard && !error ? html`<p role="status">Carregando missões…</p>`
        : html`<div className="lc-missions-grid">
          ${(missions || []).slice(0, 4).map(mission => html`<article key=${mission.id} className="lc-mission-row">
            <div><h2>${mission.titulo}</h2><p>${mission.descricao}</p>
              ${mission.ctaHref && !mission.concluida ? html`<${AppLink} href=${mission.ctaHref}>
                ${mission.ctaLabel || "Ir para missão"} →
              <//>` : null}
            </div>
            <div className="lc-mission-meta">
              <strong>+${mission.xpRecompensa} XP</strong>
              <span>${mission.concluida ? "Concluída" : `${mission.progressoAtual}/${mission.metaValor}`}</span>
            </div>
          </article>`)}
        </div>`}
    </section>
  </main>`;
}

function Footer() {
  return html`
    <footer className="lc-footer">
      <div>
        <strong>${BRAND}</strong>
        <p>Compras, missões e benefícios em uma só experiência.</p>
      </div>
      <div className="lc-footer-links">
        <${AppLink} href="/">Início<//>
        <${AppLink} href="/catalogo">Catálogo<//>
        <${AppLink} href="/passe">Passe mensal<//>
        <${AppLink} href="/conta">Conta<//>
        <${AppLink} href="/eventos">Eventos<//>
      </div>
    </footer>
  `;
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

  const notify = (title, text = "", error = false) => {
    setNotice({ title, text, error });
    window.clearTimeout(toastTimer.current);
    toastTimer.current = window.setTimeout(() => setNotice(null), 4500);
    refreshNotifications();
  };

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
    const interval = window.setInterval(refreshNotifications, 45_000);
    return () => window.clearInterval(interval);
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
      return html`<${AddressPage} address=${address} onSaved=${setAddress} notify=${notify} />`;
    }
    if (path === "/conta/beneficios")
      return html`<${BenefitsPage} auth=${auth} onAuthRefresh=${refreshAuth} notify=${notify} />`;
    if (path === "/passe") return html`<${PassPage} auth=${auth} notify=${notify} />`;
    if (path === "/passe/missoes") return html`<${PassMissionsPage} />`;
    if (path === "/eventos")
      return html`<${EventsPage} auth=${auth} getJson=${getJson} postJson=${postJson} notify=${notify} />`;
    if (path === "/sacola") {
      return html`
        <${CartPage}
          items=${cart.items}
          updateQuantity=${cart.updateQuantity}
          removeItem=${(id) => {
            cart.removeItem(id);
            notify("Produto removido da sacola");
          }}
        />
      `;
    }
    if (path === "/checkout") {
      return html`<${CheckoutPage}
        items=${immediateCheckout ? immediateItem ? [immediateItem] : [] : cart.items}
        auth=${auth}
        address=${auth.authenticated ? address : null}
        clearCart=${immediateCheckout ? clearImmediateCheckout : cart.clearCart}
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
          onAddToCart=${addToCartWithTracking}
        />
      `;
    }
    if (path.startsWith("/produto/")) {
      const productId = path.split("/")[2];
      return html`
        <${ProductDetailPage}
          productId=${productId}
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
        onAddToCart=${addToCartWithTracking}
        auth=${auth}
      />
    `;
  }, [path, route, homeData, auth, cart, immediateItem, address]);

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
        path=${path}
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
      <div className="lc-toast-region" role="status">
        ${notice ? html`<div className=${`lc-toast ${notice.error ? "error" : ""}`}>
          ${notice.error ? null : html`<${Icon} name="check" />`}
          <div><strong>${notice.title}</strong>${notice.text ? html`<span>${notice.text}</span>` : null}</div>
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
