import React, {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "https://esm.sh/react@18";
import { createRoot } from "https://esm.sh/react-dom@18/client";
import htm from "https://esm.sh/htm@3.1.1";

const html = htm.bind(React.createElement);
const BRAND = "LEVEL COMMERCE";
const CART_KEY = "level-commerce-cart";
const CHECKOUT_KEY = "level-commerce-checkout";

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

function readStorage(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key) || JSON.stringify(fallback));
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

function saveCheckoutDraft(data) {
  localStorage.setItem(CHECKOUT_KEY, JSON.stringify(data));
}

function loadCheckoutDraft() {
  return readStorage(CHECKOUT_KEY, {
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
  });
}

function clearCheckoutDraft() {
  localStorage.removeItem(CHECKOUT_KEY);
  localStorage.removeItem("level-commerce-checkout-request");
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
      return [
        ...current,
        {
          id: product.id,
          nome: product.nome,
          preco: product.preco,
          imagemUrl: product.imagemUrl,
          quantity,
        },
      ];
    });
  };

  const replaceWithSingleItem = (product) => {
    setItems([
      {
        id: product.id,
        nome: product.nome,
        preco: product.preco,
        imagemUrl: product.imagemUrl,
        quantity: 1,
      },
    ]);
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
    setItems([]);
  };

  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
  return {
    items,
    totalItems,
    addItem,
    replaceWithSingleItem,
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

function AppLink({ href, className = "", children }) {
  return html`
    <a
      href=${href}
      className=${className}
      onClick=${(event) => {
        if (href.startsWith("/")) {
          event.preventDefault();
          navigate(href);
        }
      }}
    >
      ${children}
    </a>
  `;
}

function benefitItems() {
  return [
    {
      title: "Compra protegida",
      copy: "Sua conta, seus pedidos e seu histórico em um lugar.",
    },
    {
      title: "Navegacao marketplace",
      copy: "Cards compactos, busca direta e leitura visual mais proxima de lojas grandes.",
    },
    {
      title: "Fluxo continuo",
      copy: "Retome a compra e acompanhe pedidos e benefícios pela sua conta.",
    },
  ];
}

function NavBar({ totalItems, onSearch, auth }) {
  const [search, setSearch] = useState("");

  return html`
    <header className="lc-nav-shell">
      <div className="lc-nav-top">
        <${AppLink} className="lc-brand" href="/">
          <span className="lc-brand-badge">LC</span>
          <div>
            <strong>${BRAND}</strong>
            <small>marketplace performance store</small>
          </div>
        <//>
        <form
          className="lc-search"
          onSubmit=${(event) => {
            event.preventDefault();
            onSearch(search);
          }}
        >
          <input
            value=${search}
            onChange=${(event) => setSearch(event.target.value)}
            placeholder="Buscar produtos, marcas e categorias"
          />
          <button type="submit">Buscar</button>
        </form>
      </div>
      <div className="lc-nav-bottom">
        <nav className="lc-nav-links lc-nav-links-main">
          <${AppLink} href="/">Inicio<//>
          <${AppLink} href="/catalogo">Catalogos<//>
          <${AppLink} href="/passe">Passe<//>
          <${AppLink} href="/passe/missoes">Missoes<//>
          <${AppLink} href="/conta">Contato<//>
        </nav>
        <nav className="lc-nav-links lc-nav-links-side">
          <${AppLink} href="/conta"
            >${auth.authenticated ? auth.nome?.split(" ")[0] || "Conta" : "Conta"}<//
          >
          <${AppLink} href="/sacola" className="lc-cart-pill"
            >Sacola <span>${totalItems}</span><//
          >
        </nav>
      </div>
      <div className="lc-benefits-bar">
        ${benefitItems().map(
          (item) => html`
            <div key=${item.title} className="lc-benefit-item">
              <strong>${item.title}</strong>
              <span>${item.copy}</span>
            </div>
          `,
        )}
      </div>
    </header>
  `;
}

function Hero({ headline, subheadline }) {
  return html`
    <section className="lc-hero">
      <div className="lc-hero-copy">
        <span className="lc-kicker">LEVEL COMMERCE EXPERIENCE</span>
        <h1>${headline}</h1>
        <p>${subheadline}</p>
        <div className="lc-hero-actions">
          <${AppLink} href="/catalogo" className="lc-btn lc-btn-primary"
            >Explorar catalogo<//
          >
          <${AppLink} href="/conta" className="lc-btn lc-btn-secondary"
            >Entrar ou criar conta<//
          >
        </div>
      </div>
      <div className="lc-hero-panel">
        <div className="lc-stat-card">
          <strong>Visual mais profissional</strong>
          <span>Home mais enxuta, leitura mais rapida e foco em produto.</span>
        </div>
        <div className="lc-stat-card">
          <strong>Compra sem quebra</strong>
          <span
            >O login guarda o retorno e o checkout segue para o pagamento
            correto.</span
          >
        </div>
        <div className="lc-stat-card">
          <strong>Conta unificada</strong>
          <span
            >Perfil, pedidos e pagamento agora fazem parte da mesma SPA.</span
          >
        </div>
      </div>
    </section>
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
        <p>${description}</p>
      </div>
      <${AppLink} href=${actionHref}>${actionLabel}<//>
    </div>
  `;
}

function BannerCarousel({ banners }) {
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    if (!banners?.length) {
      return undefined;
    }
    const interval = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % banners.length);
    }, 4500);
    return () => window.clearInterval(interval);
  }, [banners]);

  if (!banners?.length) {
    return null;
  }

  return html`
    <section className="lc-banner-shell">
      <div
        className="lc-banner-track"
        style=${{ transform: `translateX(-${activeIndex * 100}%)` }}
      >
        ${banners.map(
          (banner) => html`
            <article key=${banner.titulo} className="lc-banner-slide">
              <img src=${banner.imagemUrl} alt=${banner.titulo} />
              <div className="lc-banner-overlay">
                <span className="lc-kicker">${banner.destaque}</span>
                <h2>${banner.titulo}</h2>
                <p>${banner.subtitulo}</p>
                <${AppLink}
                  href=${banner.ctaHref}
                  className="lc-btn lc-btn-secondary"
                  >${banner.ctaLabel}<//
                >
              </div>
            </article>
          `,
        )}
      </div>
      <div className="lc-banner-dots">
        ${banners.map(
          (banner, index) => html`
            <button
              key=${banner.titulo}
              type="button"
              className=${index === activeIndex ? "active" : ""}
              onClick=${() => setActiveIndex(index)}
            ></button>
          `,
        )}
      </div>
    </section>
  `;
}

function PassPreview({ auth }) {
  const [dashboard, setDashboard] = useState(null);

  useEffect(() => {
    if (!auth.authenticated) {
      return;
    }
    trackPassAction("CHECKIN")
      .then(() => getJson("/api/pass/current"))
      .then(setDashboard)
      .catch(() => null);
  }, [auth.authenticated]);

  if (!auth.authenticated || !dashboard) {
    return null;
  }

  const previewLevels = dashboard.niveis.slice(0, 6);
  const progressPercent = Math.min(
    100,
    (dashboard.xpNoNivelAtual / dashboard.xpPorNivel) * 100,
  );

  return html`
    <section className="lc-section">
      <div className="lc-pass-preview">
        <div className="lc-pass-preview-head">
          <div>
            <span className="lc-kicker">PASSE EM ANDAMENTO</span>
            <h2>${dashboard.temporadaNome}</h2>
            <p>
              Nivel ${dashboard.nivelAtual}, ${dashboard.xpTotal} XP e missoes
              sempre ativas para aumentar recorrencia.
            </p>
          </div>
          <div className="lc-pass-progress compact">
            <strong
              >${dashboard.xpParaProximoNivel} XP para o proximo nivel</strong
            >
            <div className="lc-pass-bar">
              <div style=${{ width: `${progressPercent}%` }}></div>
            </div>
            <${AppLink} href="/passe" className="lc-btn lc-btn-primary"
              >Abrir passe<//
            >
          </div>
        </div>
        <div className="lc-pass-preview-levels">
          ${previewLevels.map(
            (reward) => html`
              <div
                key=${reward.nivel}
                className=${`lc-pass-chip ${reward.desbloqueado ? "unlocked" : ""}`}
              >
                <strong>Nivel ${reward.nivel}</strong>
                <span>${reward.tipoDigital}</span>
              </div>
            `,
          )}
        </div>
      </div>
    </section>
  `;
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

function ProductCard({ product, onAddToCart, onBuyNow }) {
  return html`
    <article className="lc-product-card">
      <${AppLink}
        href=${`/produto/${product.id}`}
        className="lc-product-visual"
      >
        <img
          src=${product.imagemUrl || "/images/sem-foto.png"}
          alt=${product.nome}
        />
        <div className="lc-card-overlay">
          ${product.destaque ? html`<span className="lc-tag">Destaque</span>` : null}
          <span className="lc-outline-tag">${product.sku || "LEVEL"}</span>
        </div>
      <//>
      <div className="lc-product-copy">
        <span className="lc-product-brand">${product.marca || BRAND}</span>
        <h3>${product.nome}</h3>
        <p>
          ${product.descricaoCurta || "Curadoria pensada para performance e conversao."}
        </p>
        <div className="lc-product-meta">
          <span>${product.categoria?.nome || "Colecao especial"}</span>
          <strong>${currency(product.preco)}</strong>
        </div>
      </div>
      <div className="lc-card-actions">
        <button
          className="lc-btn lc-btn-primary"
          onClick=${() => onBuyNow(product)}
        >
          Comprar agora
        </button>
        <button
          className="lc-btn lc-btn-ghost"
          onClick=${() => onAddToCart(product)}
        >
          Adicionar
        </button>
      </div>
    </article>
  `;
}

function HomePage({ data, onAddToCart, onBuyNow, auth }) {
  return html`
    <main className="lc-page">
      <${BannerCarousel} banners=${data.banners} />
      <${CategoryStrip} categories=${data.categorias} />
      <${PassPreview} auth=${auth} />
      <section className="lc-section">
        <${SectionHeader}
          title="Destaques LEVEL"
          description="Selecao principal da loja com maior apelo comercial e visual."
        />
        <div className="lc-product-grid">
          ${data.destaques.map(
            (product) => html`
              <${ProductCard}
                key=${product.id}
                product=${product}
                onAddToCart=${onAddToCart}
                onBuyNow=${onBuyNow}
              />
            `,
          )}
        </div>
      </section>
      <section className="lc-section lc-dark-panel">
        <${SectionHeader}
          title="Novidades selecionadas"
          description="Rotacao de catalogo para manter descoberta e recorrencia."
        />
        <div className="lc-product-grid compact">
          ${data.novidades.map(
            (product) => html`
              <${ProductCard}
                key=${product.id}
                product=${product}
                onAddToCart=${onAddToCart}
                onBuyNow=${onBuyNow}
              />
            `,
          )}
        </div>
      </section>
    </main>
  `;
}

function AccountPage({ auth }) {
  return html`
    <main className="lc-page">
      <section className="lc-account-shell">
        <div className="lc-section-header">
          <div>
            <h2>Conta LEVEL COMMERCE</h2>
            <p>
              ${
                auth.authenticated
                  ? `Voce esta logado como ${auth.nome}.`
                  : "Escolha o proximo passo da sua jornada de compra."
              }
            </p>
          </div>
        </div>
        <div className="lc-account-grid">
          ${
            auth.authenticated
              ? html`
                  <${AppLink} href="/pedidos" className="lc-account-card">
                    <strong>Meus pedidos</strong>
                    <span>Acompanhe entrega, pagamento e historico.</span>
                  <//>
                  <${AppLink} href="/passe" className="lc-account-card">
                    <strong>Passe mensal</strong>
                    <span>Suba de nivel com XP de compras e missoes.</span>
                  <//>
                  <${AppLink} href="/perfil" className="lc-account-card">
                    <strong>Meu perfil</strong>
                    <span>Atualize nome, senha e dados da conta.</span>
                  <//>
                  <${AppLink} href="/checkout" className="lc-account-card">
                    <strong>Retomar checkout</strong>
                    <span
                      >Volte direto para endereco e forma de pagamento.</span
                    >
                  <//>
                  <form
                    action="/auth/logout"
                    method="post"
                    className="lc-account-card lc-account-form"
                  >
                    <input type="hidden" name="_csrf" value=${getCsrfToken()} />
                    <strong>Encerrar sessao</strong>
                    <span>Sair da conta atual com seguranca.</span>
                    <button type="submit" className="lc-btn lc-btn-ghost">
                      Sair
                    </button>
                  </form>
                `
              : html`
                  <${AppLink} href="/login" className="lc-account-card">
                    <strong>Entrar</strong>
                    <span
                      >Acesse sua area para compras, pedidos e pagamentos.</span
                    >
                  <//>
                  <${AppLink} href="/cadastro" className="lc-account-card">
                    <strong>Criar conta</strong>
                    <span
                      >Cadastre-se para continuar a compra com dados
                      salvos.</span
                    >
                  <//>
                  <${AppLink} href="/catalogo" className="lc-account-card">
                    <strong>Explorar catalogo</strong>
                    <span>Descubra as colecoes da LEVEL COMMERCE.</span>
                  <//>
                  <${AppLink}
                    href=${buildLoginUrl("/checkout")}
                    className="lc-account-card"
                  >
                    <strong>Continuar compra</strong>
                    <span>Entre e volte direto para o checkout.</span>
                  <//>
                `
          }
        </div>
      </section>
    </main>
  `;
}

function AuthPage({ type, auth, route }) {
  const { params } = parseRoute(route);
  const returnUrl = params.get("returnUrl") || "/conta";
  const isLogin = type === "login";

  if (auth.authenticated) {
    return html`
      <main className="lc-page">
        <section className="lc-account-shell">
          <div className="lc-section-header">
            <div>
              <h2>Voce ja esta autenticado</h2>
              <p>Siga para o proximo passo sem repetir o login.</p>
            </div>
          </div>
          <div className="lc-account-grid">
            <${AppLink} href=${returnUrl} className="lc-account-card">
              <strong>Continuar</strong>
              <span>Voltar exatamente para onde voce estava.</span>
            <//>
            <${AppLink} href="/conta" className="lc-account-card">
              <strong>Ir para conta</strong>
              <span>Gerencie pedidos e dados pessoais.</span>
            <//>
          </div>
        </section>
      </main>
    `;
  }

  return html`
    <main className="lc-page">
      <section className="lc-checkout-shell">
        <div className="lc-checkout-main">
          <div className="lc-section-header">
            <div>
              <h2>${isLogin ? "Entrar" : "Criar conta"}</h2>
              <p>
                ${
                  isLogin
                    ? "Acesse para concluir sua compra sem perder o ponto da navegacao."
                    : "Cadastre-se para compras mais rapidas e acompanhamento de pedidos."
                }
              </p>
            </div>
          </div>
          <form
            className="lc-checkout-card"
            action=${isLogin ? "/auth/login" : "/auth/cadastro"}
            method="post"
          >
            <input type="hidden" name="_csrf" value=${getCsrfToken()} />
            ${isLogin ? html`<input type="hidden" name="returnUrl" value=${returnUrl} />` : null}
            ${
              !isLogin
                ? html`
                    <div className="lc-form-grid two">
                      <label>Nome completo<input name="nome" required /></label>
                      <label
                        >E-mail<input name="email" type="email" required
                      /></label>
                    </div>
                    <div className="lc-form-grid two">
                      <label
                        >Senha<input name="senha" type="password" required
                      /></label>
                      <label
                        >Fluxo recomendado<input
                          value="Conta nova LEVEL"
                          disabled
                      /></label>
                    </div>
                  `
                : html`
                    <div className="lc-form-grid two">
                      <label
                        >E-mail<input name="email" type="email" required
                      /></label>
                      <label
                        >Senha<input name="senha" type="password" required
                      /></label>
                    </div>
                  `
            }
            <button type="submit" className="lc-btn lc-btn-primary">
              ${isLogin ? "Entrar e continuar" : "Criar conta"}
            </button>
          </form>
        </div>
        <aside className="lc-cart-summary">
          <span>${isLogin ? "Compra mais rapida" : "Conta LEVEL"}</span>
          <strong>${isLogin ? "Retome o checkout" : "Cadastro agil"}</strong>
          <p className="lc-checkout-note">
            ${
              isLogin
                ? "Depois de entrar, você continuará de onde parou."
                : "Depois de criar sua conta, entre para continuar a compra."
            }
          </p>
          <${AppLink}
            href=${isLogin ? "/cadastro" : buildLoginUrl(returnUrl)}
            className="lc-btn lc-btn-ghost"
          >
            ${isLogin ? "Criar conta" : "Ja tenho conta"}
          <//>
        </aside>
      </section>
    </main>
  `;
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

function CatalogPage({ route, onAddToCart, onBuyNow }) {
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
              <h2>Catalogo LEVEL COMMERCE</h2>
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
                  onBuyNow=${onBuyNow}
                />
              `,
            )}
          </div>
        </div>
      </section>
    </main>
  `;
}

function ProductDetailPage({ productId, onAddToCart, onBuyNow, auth }) {
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
      setReviewMessage("Avaliacao enviada com sucesso.");
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
    <main className="lc-page">
      <section className="lc-detail-shell">
        <div className="lc-detail-visual">
          <img
            src=${product.imagemUrl || "/images/sem-foto.png"}
            alt=${product.nome}
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
            <span>${product.totalAvaliacoes} avaliacao(oes)</span>
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
              onClick=${() => onBuyNow(product)}
            >
              Comprar agora
            </button>
            <button
              className="lc-btn lc-btn-ghost"
              onClick=${() => onAddToCart(product)}
            >
              Adicionar a sacola
            </button>
          </div>
          <div className="lc-rich-copy">
            <h3>Descricao completa</h3>
            <p>${product.descricao || "Descricao detalhada em breve."}</p>
          </div>
        </div>
      </section>

      <section className="lc-section">
        <${SectionHeader}
          title="Produtos relacionados"
          description="Mais itens para ampliar ticket medio e reforcar a jornada de compra."
        />
        <div className="lc-product-grid compact">
          ${product.relacionados.map(
            (related) => html`
              <${ProductCard}
                key=${related.id}
                product=${related}
                onAddToCart=${onAddToCart}
                onBuyNow=${onBuyNow}
              />
            `,
          )}
        </div>
      </section>

      <section className="lc-section lc-review-grid">
        <div className="lc-checkout-card">
          <h3>Avaliacoes de clientes</h3>
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
    <main className="lc-page">
      <section className="lc-cart-shell">
        <div className="lc-cart-list">
          <div className="lc-section-header">
            <div>
              <h2>Sacola LEVEL</h2>
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
                  <strong>${item.nome}</strong>
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
          <${AppLink} href="/checkout" className="lc-btn lc-btn-primary"
            >Continuar compra<//
          >
        </aside>
      </section>
    </main>
  `;
}

function CheckoutPage({ items, auth, clearCart }) {
  const [paymentConfig, setPaymentConfig] = useState(null);
  useEffect(() => {
    if (auth.authenticated)
      getJson("/api/checkout/config")
        .then(setPaymentConfig)
        .catch(() => {});
  }, [auth.authenticated]);
  const [form, setForm] = useState(loadCheckoutDraft());
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const subtotal = useMemo(
    () => items.reduce((sum, item) => sum + item.preco * item.quantity, 0),
    [items],
  );
  const freight =
    subtotal >= 299 ? 0 : subtotal >= 149 ? 14.9 : items.length ? 24.9 : 0;
  const total = subtotal + freight;

  useEffect(() => {
    saveCheckoutDraft(form);
  }, [form]);

  const checkoutRequestId = useRef(window.crypto.randomUUID());
  const submitCheckout = async () => {
    setSubmitting(true);
    setMessage("");
    try {
      const fingerprint = JSON.stringify({
        form,
        items: items.map((i) => ({ id: i.id, quantity: i.quantity })),
      });
      const previous = readStorage("level-commerce-checkout-request", {});
      if (previous.fingerprint !== fingerprint)
        checkoutRequestId.current = window.crypto.randomUUID();
      const requestId =
        previous.fingerprint === fingerprint
          ? previous.id
          : checkoutRequestId.current;
      localStorage.setItem(
        "level-commerce-checkout-request",
        JSON.stringify({ fingerprint, id: requestId }),
      );
      const response = await postJson("/api/checkout/orders", {
        idempotencyKey: requestId,
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
      clearCheckoutDraft();
      window.location.href = response.redirectUrl;
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin("/checkout");
        return;
      }
      setMessage("Nao foi possivel iniciar o pagamento agora.");
    } finally {
      setSubmitting(false);
    }
  };

  return html`
    <main className="lc-page">
      <section className="lc-checkout-shell">
        <div className="lc-checkout-main">
          <div className="lc-section-header">
            <div>
              <h2>Endereco e pagamento</h2>
              <p>Informe onde receber e confira as condições da compra.</p>
            </div>
          </div>
          <div className="lc-checkout-card">
            <h3>Endereco de entrega</h3>
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
          ${message ? html`<p className="lc-checkout-note">${message}</p>` : null}
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
                  href=${buildLoginUrl("/checkout")}
                  className="lc-btn lc-btn-primary"
                  >Entrar para pagar<//
                >`
          }
        </aside>
      </section>
    </main>
  `;
}

function ProfilePage({ auth, onAuthRefresh }) {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ nome: "", senha: "" });
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getJson("/api/account/profile")
      .then((response) => {
        setProfile(response);
        setForm({ nome: response.nome || "", senha: "" });
      })
      .catch((error) => {
        if (error.code === "AUTH_REQUIRED") {
          requireLogin("/perfil");
        }
      });
  }, []);

  const submitProfile = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      const response = await putJson("/api/account/profile", form);
      setProfile(response);
      setForm((current) => ({ ...current, senha: "" }));
      setMessage("Perfil atualizado com sucesso.");
      onAuthRefresh();
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin("/perfil");
        return;
      }
      setMessage("Nao foi possivel atualizar o perfil agora.");
    } finally {
      setSaving(false);
    }
  };

  if (!profile) {
    return html`<main className="lc-page"><p>Carregando perfil...</p></main>`;
  }

  return html`
    <main className="lc-page">
      <section className="lc-account-shell">
        <div className="lc-section-header">
          <div>
            <h2>Meu perfil</h2>
            ${auth.avatarPasse ? html`<img src=${`/api/pass/rewards/${auth.avatarPasse}/download?inline=true`} alt="Avatar resgatado" style=${{ width: "96px", height: "96px", objectFit: "cover", borderRadius: "50%" }} />` : null}
            ${auth.badgePasse ? html`<img src=${`/api/pass/rewards/${auth.badgePasse}/download?inline=true`} alt="Conquista resgatada" style=${{ width: "96px", height: "60px", objectFit: "cover" }} />` : null}
            <p>
              Gerencie os dados da sua conta e mantenha a jornada de compra
              pronta para o proximo pedido.
            </p>
          </div>
          <${AppLink} href="/pedidos">Ver pedidos<//>
        </div>
        <div className="lc-profile-grid">
          <form className="lc-checkout-card" onSubmit=${submitProfile}>
            <h3>Dados da conta</h3>
            <div className="lc-form-grid two">
              <label
                >Nome completo<input
                  value=${form.nome}
                  onChange=${(e) => setForm({ ...form, nome: e.target.value })}
              /></label>
              <label>E-mail<input value=${profile.email} disabled /></label>
            </div>
            <div className="lc-form-grid two">
              <label
                >Nova senha<input
                  type="password"
                  value=${form.senha}
                  onChange=${(e) => setForm({ ...form, senha: e.target.value })}
                  placeholder="Deixe em branco para manter"
              /></label>
              <label
                >Tipo de conta<input value=${profile.role} disabled
              /></label>
            </div>
            ${message ? html`<p className="lc-checkout-note">${message}</p>` : null}
            <button
              className="lc-btn lc-btn-primary"
              type="submit"
              disabled=${saving}
            >
              ${saving ? "Salvando..." : "Salvar alteracoes"}
            </button>
          </form>
          <aside className="lc-cart-summary">
            <span>Resumo da conta</span>
            <div className="lc-summary-line">
              <span>Nome</span><strong>${auth.nome || profile.nome}</strong>
            </div>
            <div className="lc-summary-line">
              <span>E-mail</span><strong>${profile.email}</strong>
            </div>
            <div className="lc-summary-line">
              <span>Total de pedidos</span
              ><strong>${profile.totalPedidos}</strong>
            </div>
            <${AppLink} href="/checkout" className="lc-btn lc-btn-ghost"
              >Ir para checkout<//
            >
          </aside>
        </div>
      </section>
    </main>
  `;
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

function OrderDetailPage({ orderId, route }) {
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
      setMessage("Pedido cancelado com sucesso.");
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

function PassPage() {
  const [collection, setCollection] = useState([]);
  useEffect(() => {
    getJson("/api/pass/rewards")
      .then(setCollection)
      .catch(() => {});
  }, []);
  const [dashboard, setDashboard] = useState(null);
  const [claimState, setClaimState] = useState({
    loadingId: null,
    message: "",
    error: "",
  });
  const trackRef = useRef(null);

  const loadDashboard = () => {
    trackPassAction("CHECKIN")
      .then(() => getJson("/api/pass/current"))
      .then(setDashboard)
      .catch((error) => {
        if (error.code === "AUTH_REQUIRED") {
          requireLogin("/passe");
        }
      });
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  if (!dashboard) {
    return html`<main className="lc-page">
      <p>Carregando passe mensal...</p>
    </main>`;
  }

  const scrollTrack = (direction) => {
    if (!trackRef.current) {
      return;
    }
    trackRef.current.scrollBy({
      left: direction * 420,
      behavior: "smooth",
    });
  };

  const premiumLevels = dashboard.niveis;
  const freeLevels = dashboard.niveis.map((reward) => {
    const hasReward = !reward.premium;
    return {
      ...reward,
      titulo: hasReward ? reward.titulo : "Sem recompensa neste nivel",
      tipoDigital: hasReward ? reward.tipoDigital : "Nivel vazio",
      descricao: hasReward
        ? reward.descricao
        : "A trilha gratuita libera recompensas em niveis selecionados.",
      hasReward,
    };
  });
  const progressPercent = Math.min(
    100,
    (dashboard.xpTotal / (dashboard.totalNiveis * dashboard.xpPorNivel)) * 100,
  );

  const handleClaimReward = async (reward) => {
    if (!reward.disponivelParaResgate || claimState.loadingId) {
      return;
    }
    setClaimState({ loadingId: reward.recompensaId, message: "", error: "" });
    try {
      const result = await claimPassReward(reward.recompensaId);
      setClaimState({
        loadingId: null,
        message: `${result.titulo} liberada com o codigo ${result.codigo}.`,
        error: "",
      });
      loadDashboard();
      getJson("/api/pass/rewards")
        .then(setCollection)
        .catch(() => {});
    } catch (error) {
      if (error.code === "AUTH_REQUIRED") {
        requireLogin("/passe");
        return;
      }
      setClaimState({
        loadingId: null,
        message: "",
        error: error.message || "Nao foi possivel reclamar a recompensa.",
      });
    }
  };

  return html`
    <main className="lc-page">
      <section className="lc-pass-shell">
        <div className="lc-checkout-card">
          <h3>Minha coleção</h3>
          <p>
            Cupons pessoais de uso único. Confira o desconto e o mínimo de
            compra em cada benefício.
          </p>
          ${
            collection.length
              ? collection.map(
                  (r) =>
                    html`<div key=${r.id} className="lc-summary-line">
                      <span>${r.titulo} · ${r.tipo}</span>${
                        !r.ativo
                          ? html`<span>Indisponível</span>`
                          : r.tipo === "Cupom"
                            ? html`<span
                                ><code
                                  >${r.usado ? "Utilizado / reservado" : r.codigo}</code
                                >
                                · ${currency(r.valor)} em produtos a partir de
                                ${currency(r.minimo)} · até ${r.validade}</span
                              >`
                            : ["Tema", "Avatar", "Badge"].includes(r.tipo)
                              ? html`<button
                                  onClick=${async () => {
                                    await postJson(
                                      `/api/pass/rewards/${r.id}/apply`,
                                      {},
                                    );
                                    window.location.reload();
                                  }}
                                >
                                  Aplicar benefício
                                </button>`
                              : html`<a
                                  href=${`/api/pass/rewards/${r.id}/download`}
                                  >Baixar benefício</a
                                >`
                      }
                    </div>`,
                )
              : html`<p>Resgate uma recompensa para vê-la aqui.</p>`
          }
        </div>
        <div className="lc-pass-topbar">
          <div>
            <span className="lc-kicker">PASSE MENSAL</span>
            <h2>${dashboard.temporadaNome}</h2>
            <p>
              ${dashboard.temporadaPeriodo} · acompanhe niveis, XP e recompensas
              como em um passe de jogo.
            </p>
            <p>
              Recompensa final: ${currency(dashboard.gastoAprovado)} de
              ${currency(dashboard.gastoMinimoFinal)} em produtos aprovados.
              Frete e premium não entram.
            </p>
          </div>
          <${AppLink} href="/passe/missoes" className="lc-btn lc-btn-primary"
            >Ver missoes<//
          >
        </div>

        <div className="lc-pass-hero">
          <div>
            <strong
              >Nivel ${dashboard.nivelAtual}/${dashboard.totalNiveis}</strong
            >
            <p>
              Ganhe XP com compras aprovadas, exploracao da loja, avaliacoes e
              missoes recorrentes.
            </p>
          </div>
          <div className="lc-pass-side">
            <div className="lc-pass-progress">
              <span>${dashboard.xpTotal} XP acumulado</span>
              <div className="lc-pass-bar">
                <div
                  style=${{ width: `${Math.min(100, (dashboard.xpNoNivelAtual / dashboard.xpPorNivel) * 100)}%` }}
                ></div>
              </div>
              <div className="lc-pass-progress-actions">
                <small
                  >${dashboard.xpParaProximoNivel} XP para o proximo
                  nivel</small
                >
                <${AppLink}
                  href="/passe/missoes"
                  className="lc-btn lc-btn-primary"
                  >Ver missoes<//
                >
              </div>
              ${claimState.message ? html`<p className="lc-inline-feedback success">${claimState.message}</p>` : null}
              ${claimState.error ? html`<p className="lc-inline-feedback error">${claimState.error}</p>` : null}
            </div>
            <div className="lc-pass-history-card">
              <h3>Ultimos ganhos de XP</h3>
              <div className="lc-pass-history">
                ${dashboard.historico.map(
                  (item, index) => html`
                    <div key=${index} className="lc-summary-line">
                      <span>${item.descricao}</span>
                      <strong>+${item.xpGanho} XP</strong>
                    </div>
                  `,
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="lc-checkout-card">
          <div className="lc-section-header">
            <div>
              <h3>Trilha do passe</h3>
              <p>
                ${dashboard.premiumAtivo ? "Premium ativo nesta temporada" : `Premium: ${currency(dashboard.precoPremium)} por temporada, sem renovação automática`}
              </p>
              ${
                !dashboard.premiumAtivo
                  ? html`<button
                      className="lc-btn lc-btn-primary"
                      onClick=${async () => {
                        try {
                          const r = await postJson("/api/pass/premium", {});
                          window.location.href = r.redirectUrl;
                        } catch (e) {
                          setClaimState({
                            loadingId: null,
                            message: "",
                            error: e.message,
                          });
                        }
                      }}
                    >
                      Adquirir premium
                    </button>`
                  : null
              }
              <p>
                Premium em cima, progresso no meio e gratuito embaixo, em uma
                unica trilha horizontal.
              </p>
            </div>
            <div className="lc-pass-actions">
              <button
                type="button"
                className="lc-btn lc-btn-ghost"
                onClick=${() => scrollTrack(-1)}
              >
                Anterior
              </button>
              <button
                type="button"
                className="lc-btn lc-btn-ghost"
                onClick=${() => scrollTrack(1)}
              >
                Proximo
              </button>
            </div>
          </div>
          <div className="lc-pass-track-shell">
            <div className="lc-pass-track" ref=${trackRef}>
              ${premiumLevels.map((premiumReward, index) => {
                const freeReward = freeLevels[index];
                const premiumButtonLabel = premiumReward.resgatado
                  ? "Resgatada"
                  : premiumReward.disponivelParaResgate
                    ? claimState.loadingId === premiumReward.recompensaId
                      ? "Reclamando..."
                      : "Reclamar"
                    : "Bloqueado";
                const freeButtonLabel = freeReward.hasReward
                  ? freeReward.desbloqueado
                    ? "Liberado"
                    : "Bloqueado"
                  : "Sem premio";
                return html`
                  <div
                    key=${premiumReward.nivel}
                    className="lc-pass-column-card"
                  >
                    <div
                      className=${`lc-pass-lane-card premium ${premiumReward.desbloqueado ? "unlocked" : ""}`}
                    >
                      <small>Premium</small>
                      <strong>Nivel ${premiumReward.nivel}</strong>
                      <span
                        >${premiumReward.premium ? premiumReward.tipoDigital : "Sem recompensa premium"}</span
                      >
                      <div className="lc-pass-lane-progress">
                        <div
                          style=${{ width: `${premiumReward.progressoPercentual}%` }}
                        ></div>
                      </div>
                      <button
                        type="button"
                        className=${`lc-pass-claim-btn ${premiumReward.disponivelParaResgate ? "available" : ""}`}
                        disabled=${!premiumReward.premium || premiumReward.resgatado || !premiumReward.disponivelParaResgate || claimState.loadingId === premiumReward.recompensaId}
                        onClick=${() => handleClaimReward(premiumReward)}
                      >
                        ${premiumButtonLabel}
                      </button>
                    </div>
                    <div className="lc-pass-progress-node">
                      <span>${premiumReward.nivel}</span>
                      <div className="lc-pass-thin-bar">
                        <div
                          style=${{ width: `${premiumReward.progressoPercentual}%` }}
                        ></div>
                      </div>
                    </div>
                    <div
                      className=${`lc-pass-lane-card free ${freeReward.desbloqueado ? "unlocked" : ""} ${freeReward.hasReward ? "" : "empty"}`}
                    >
                      <small>Gratuito</small>
                      <strong>Nivel ${freeReward.nivel}</strong>
                      <span
                        >${freeReward.hasReward ? freeReward.tipoDigital : "Sem recompensa"}</span
                      >
                      <div className="lc-pass-lane-progress">
                        <div
                          style=${{ width: `${freeReward.progressoPercentual}%` }}
                        ></div>
                      </div>
                      <button
                        type="button"
                        className="lc-pass-claim-btn"
                        disabled=${!freeReward.hasReward || freeReward.resgatado || !freeReward.disponivelParaResgate || !!claimState.loadingId}
                        onClick=${() => handleClaimReward(freeReward)}
                      >
                        ${freeReward.resgatado ? "Resgatada" : freeReward.hasReward && freeReward.disponivelParaResgate ? "Resgatar" : freeButtonLabel}
                      </button>
                    </div>
                  </div>
                `;
              })}
            </div>
          </div>
        </div>
      </section>
    </main>
  `;
}

function PassMissionsPage() {
  const [dashboard, setDashboard] = useState(null);

  useEffect(() => {
    trackPassAction("CHECKIN")
      .then(() => getJson("/api/pass/current"))
      .then(setDashboard)
      .catch((error) => {
        if (error.code === "AUTH_REQUIRED") {
          requireLogin("/passe/missoes");
        }
      });
  }, []);

  if (!dashboard) {
    return html`<main className="lc-page"><p>Carregando missoes...</p></main>`;
  }

  const missionSections = [
    ["Missoes diarias", dashboard.missoesDiarias],
    ["Missoes semanais", dashboard.missoesSemanais],
    ["Missoes mensais", dashboard.missoesMensais],
  ];

  return html`
    <main className="lc-page">
      <section className="lc-pass-shell">
        <div className="lc-checkout-card">
          <h3>Minha coleção</h3>
          <p>
            Cupons pessoais de uso único. Confira o desconto e o mínimo de
            compra em cada benefício.
          </p>
          ${
            collection.length
              ? collection.map(
                  (r) =>
                    html`<div key=${r.id} className="lc-summary-line">
                      <span>${r.titulo} · ${r.tipo}</span>${
                        !r.ativo
                          ? html`<span>Indisponível</span>`
                          : r.tipo === "Cupom"
                            ? html`<span
                                ><code
                                  >${r.usado ? "Utilizado / reservado" : r.codigo}</code
                                >
                                · ${currency(r.valor)} em produtos a partir de
                                ${currency(r.minimo)} · até ${r.validade}</span
                              >`
                            : ["Tema", "Avatar", "Badge"].includes(r.tipo)
                              ? html`<button
                                  onClick=${async () => {
                                    await postJson(
                                      `/api/pass/rewards/${r.id}/apply`,
                                      {},
                                    );
                                    window.location.reload();
                                  }}
                                >
                                  Aplicar benefício
                                </button>`
                              : html`<a
                                  href=${`/api/pass/rewards/${r.id}/download`}
                                  >Baixar benefício</a
                                >`
                      }
                    </div>`,
                )
              : html`<p>Resgate uma recompensa para vê-la aqui.</p>`
          }
        </div>
        <div className="lc-section-header">
          <div>
            <h2>Missoes do passe</h2>
            <p>
              Acompanhe os objetivos do ciclo atual e volte para a trilha quando
              quiser.
            </p>
          </div>
          <${AppLink} href="/passe" className="lc-btn lc-btn-primary"
            >Voltar para niveis<//
          >
        </div>
        ${missionSections.map(
          ([title, missions]) => html`
            <div key=${title} className="lc-checkout-card">
              <h3>${title}</h3>
              <div className="lc-pass-missions">
                ${missions.map(
                  (mission) => html`
                    <div key=${mission.id} className="lc-pass-mission">
                      <div>
                        <strong>${mission.titulo}</strong>
                        <span>${mission.descricao}</span>
                        ${
                          mission.ctaHref
                            ? html`
                                <div className="lc-pass-mission-action">
                                  <${AppLink}
                                    href=${mission.ctaHref}
                                    className="lc-pass-mini-btn"
                                  >
                                    ${mission.ctaLabel || "Ir para missao"}
                                  <//>
                                </div>
                              `
                            : null
                        }
                      </div>
                      <div className="lc-pass-mission-meta">
                        <span
                          >${mission.progressoAtual}/${mission.metaValor}</span
                        >
                        <strong>+${mission.xpRecompensa} XP</strong>
                        <small>Reseta em ${mission.resetaEm}</small>
                      </div>
                    </div>
                  `,
                )}
              </div>
            </div>
          `,
        )}
      </section>
    </main>
  `;
}

function Footer() {
  return html`
    <footer className="lc-footer">
      <div>
        <strong>${BRAND}</strong>
        <p>Compras, missões e benefícios em uma só experiência.</p>
      </div>
      <div className="lc-footer-links">
        <${AppLink} href="/">Inicio<//>
        <${AppLink} href="/catalogo">Catalogo<//>
        <${AppLink} href="/passe">Passe mensal<//>
        <${AppLink} href="/conta">Conta<//>
        <a href="/admin">Admin</a>
      </div>
    </footer>
  `;
}

function App() {
  const route = useRoute();
  const { path } = parseRoute(route);
  const cart = useLocalCart();
  const [homeData, setHomeData] = useState(null);
  const [auth, setAuth] = useState({ authenticated: false });

  const refreshAuth = () => {
    getJson("/api/auth/me").then((data) => {
      setAuth(data);
      document.body.dataset.tema = data.tema || "padrao";
    });
  };

  useEffect(() => {
    getJson("/api/store/home").then(setHomeData);
    refreshAuth();
  }, []);

  const addToCartWithTracking = (product, quantity = 1) => {
    cart.addItem(product, quantity);
    trackPassAction("ADD_TO_CART", 1, `cart-${product.id}`);
  };

  const buyNow = (product) => {
    cart.replaceWithSingleItem(product);
    trackPassAction("ADD_TO_CART", 1, `buy-now-${product.id}`);
    navigate("/checkout");
  };

  const currentScreen = useMemo(() => {
    if (path === "/conta") return html`<${AccountPage} auth=${auth} />`;
    if (path === "/passe") return html`<${PassPage} />`;
    if (path === "/passe/missoes") return html`<${PassMissionsPage} />`;
    if (path === "/login")
      return html`<${AuthPage} type="login" auth=${auth} route=${route} />`;
    if (path === "/cadastro")
      return html`<${AuthPage} type="register" auth=${auth} route=${route} />`;
    if (path === "/sacola") {
      return html`
        <${CartPage}
          items=${cart.items}
          updateQuantity=${cart.updateQuantity}
          removeItem=${cart.removeItem}
        />
      `;
    }
    if (path === "/checkout") {
      return html`<${CheckoutPage}
        items=${cart.items}
        auth=${auth}
        clearCart=${cart.clearCart}
      />`;
    }
    if (path === "/perfil") {
      return html`<${ProfilePage} auth=${auth} onAuthRefresh=${refreshAuth} />`;
    }
    if (path === "/pedidos") {
      return html`<${OrdersPage} />`;
    }
    if (path.startsWith("/pedidos/")) {
      const orderId = path.split("/")[2];
      return html`<${OrderDetailPage} orderId=${orderId} route=${route} />`;
    }
    if (path === "/pagamento/retorno") {
      return html`<${PaymentResultPage} route=${route} />`;
    }
    if (path.startsWith("/catalogo") || path.startsWith("/produtos")) {
      return html`
        <${CatalogPage}
          route=${route}
          onAddToCart=${addToCartWithTracking}
          onBuyNow=${buyNow}
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
        />
      `;
    }
    if (!homeData)
      return html`<main className="lc-page">
        <p>Carregando storefront...</p>
      </main>`;
    return html`
      <${HomePage}
        data=${homeData}
        onAddToCart=${addToCartWithTracking}
        onBuyNow=${buyNow}
        auth=${auth}
      />
    `;
  }, [path, route, homeData, auth, cart]);

  const searchAndNavigate = (searchTerm) => {
    const params = new URLSearchParams();
    if (searchTerm) params.set("busca", searchTerm);
    navigate(`/catalogo${params.toString() ? `?${params.toString()}` : ""}`);
  };

  return html`
    <div className="lc-app-shell">
      <${NavBar}
        totalItems=${cart.totalItems}
        onSearch=${searchAndNavigate}
        auth=${auth}
      />
      ${currentScreen}
      <${Footer} />
    </div>
  `;
}

createRoot(document.getElementById("root")).render(html`<${App} />`);
