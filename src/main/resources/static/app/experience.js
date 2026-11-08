import React, { useEffect, useState } from "https://esm.sh/react@18";
import htm from "https://esm.sh/htm@3.1.1";

const html = htm.bind(React.createElement);
const dates = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const reais = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function Icon({ name }) {
  const paths = {
    search: "M21 21l-4.4-4.4M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z",
    user: "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2m12-14a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
    bag: "M4 8h16l1 13H3L4 8Zm4 1V6a4 4 0 0 1 8 0v3",
    bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-8 12a2 2 0 0 0 4 0",
    menu: "M4 6h16M4 12h16M4 18h16",
    check: "m5 12 4 4L19 6",
    shield: "M12 22s7-3 7-10V5l-7-3-7 3v7c0 7 7 10 7 10Z",
  };
  return html`<svg width="20" height="20" viewBox="0 0 24 24" fill="none"
    stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true"><path d=${paths[name]} /></svg>`;
}

export function Brand() {
  return html`<a href="/" className="lc-brand"><span className="lc-brand-badge" aria-hidden="true">L<span>.</span></span>
    <strong>LEVEL<span> COMMERCE</span></strong></a>`;
}

const authTitles = {
  login: "Entrar",
  register: "Criar conta",
  forgot: "Recuperar senha",
  reset: "Redefinir senha",
};

function safeDestination(value) {
  return value && value.startsWith("/") && !value.startsWith("//") && !/[\\\s]/.test(value)
    ? value : "/conta";
}

export function AuthPage({ type, auth, route, postJson, csrf }) {
  const query = new URLSearchParams(route.split("?")[1]);
  const destination = safeDestination(query.get("returnUrl"));
  const context = `?returnUrl=${encodeURIComponent(destination)}`;
  const [form, setForm] = useState({ nome: "", email: "", senha: "", confirmar: "" });
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [token] = useState(() => new URLSearchParams(window.location.hash.slice(1)).get("token"));
  useEffect(() => {
    if (type === "reset" && window.location.hash) {
      window.history.replaceState(window.history.state, "", window.location.pathname + window.location.search);
    }
  }, [type]);
  const createsPassword = type === "register" || type === "reset";
  const update = (field) => (event) => {
    setForm({ ...form, [field]: event.target.value });
    setFeedback(null);
  };
  const validLength = form.senha.trim().length > 0 && form.senha.length >= 12
    && new TextEncoder().encode(form.senha).length <= 72;
  const notice = feedback || (query.has("erro") ? { error: true, text: "E-mail ou senha incorretos." }
    : query.has("reset") ? { text: "Senha atualizada. Entre com sua nova senha." }
    : query.has("sucesso") ? { text: "Conta criada. Entre para continuar." }
    : query.has("logout") ? { text: "Você saiu da sua conta." } : null);

  async function submit(event) {
    event.preventDefault();
    if (createsPassword && (!validLength || form.senha !== form.confirmar)) {
      setFeedback({ error: true, text: !validLength
        ? "A senha precisa atender aos requisitos abaixo."
        : "A confirmação deve ser igual à senha." });
      return;
    }
    setBusy(true);
    try {
      if (type === "register") {
        await postJson("/api/auth/register", { nome: form.nome, email: form.email, senha: form.senha });
        window.location.assign(`/login${context}&sucesso=true`);
      } else if (type === "forgot") {
        await postJson("/api/auth/password/forgot", { email: form.email });
        setFeedback({ text: "Se o e-mail estiver cadastrado, você receberá um link para redefinir a senha." });
      } else {
        await postJson("/api/auth/password/reset", { token, senha: form.senha });
        window.location.assign("/login?reset=true");
      }
    } catch (error) {
      setFeedback({ error: true, text: error.message || "Não foi possível concluir. Tente novamente." });
    } finally {
      setBusy(false);
    }
  }

  const unavailable = (type === "forgot" || type === "reset") && auth.ready && !auth.ambienteLocal;
  return html`
    <main className="lc-auth-page">
      <${Brand} />
      <section className="lc-auth-card" aria-labelledby="auth-title">
        <h1 id="auth-title">${authTitles[type]}</h1>
        ${notice ? html`<p className=${`lc-form-notice ${notice.error ? "error" : "success"}`}
          role=${notice.error ? "alert" : "status"}>${notice.text}</p>` : null}
        ${auth.authenticated && (type === "login" || type === "register")
          ? html`<p>Você já está conectado.</p><a className="lc-btn lc-btn-primary" href=${destination}>Continuar</a>`
          : unavailable ? html`<p>A recuperação de senha está disponível apenas no ambiente de teste.</p>`
          : type === "reset" && !token
            ? html`<p role="alert">Abra o link completo recebido por e-mail.</p><a href="/recuperar-senha">Solicitar outro link</a>`
          : html`<form action=${type === "login" ? "/auth/login" : undefined} method="post"
              onSubmit=${type === "login" ? undefined : submit}>
              ${type === "login" ? html`
                <input type="hidden" name="_csrf" value=${csrf} />
                <input type="hidden" name="returnUrl" value=${destination} />` : null}
              ${type === "register" ? html`<label>Nome completo
                <input name="nome" autoComplete="name" required minLength="2" maxLength="100"
                  value=${form.nome} onChange=${update("nome")} />
              </label>` : null}
              ${type !== "reset" ? html`<label>E-mail
                <input name="email" type="email" autoComplete=${type === "login" ? "username" : "email"}
                  maxLength="254" required value=${form.email} onChange=${update("email")} />
              </label>` : null}
              ${type !== "forgot" ? html`<label>${type === "reset" ? "Nova senha" : "Senha"}
                <span className="lc-password-field">
                  <input name="senha" aria-label=${type === "reset" ? "Nova senha" : "Senha"} type=${visible ? "text" : "password"} required
                    minLength=${createsPassword ? 12 : undefined}
                    maxLength=${createsPassword ? 72 : undefined}
                    autoComplete=${createsPassword ? "new-password" : "current-password"}
                    aria-describedby=${createsPassword ? "password-hint" : undefined}
                    value=${form.senha} onChange=${update("senha")} />
                  <button type="button" aria-pressed=${visible} onClick=${() => setVisible(!visible)}>
                    ${visible ? "Ocultar" : "Mostrar"}
                  </button>
                </span>
              </label>` : null}
              ${createsPassword ? html`
                <p id="password-hint" className=${validLength ? "lc-password-ok" : "lc-auth-hint"}>
                  ${validLength ? "✓ Comprimento válido." : "Use pelo menos 12 caracteres e até 72 bytes. Prefira uma frase longa que só você conheça."}
                </p>
                <label>Confirme a senha
                  <input type=${visible ? "text" : "password"} aria-label="Confirme a senha" required autoComplete="new-password"
                    value=${form.confirmar} onChange=${update("confirmar")} />
                </label>` : null}
              ${type === "login" && auth.ambienteLocal ? html`<a className="lc-auth-help" href="/recuperar-senha">Esqueci minha senha</a>` : null}
              <button type="submit" className="lc-btn lc-btn-primary" disabled=${busy || !auth.ready || !csrf}>
                ${busy ? "Aguarde…" : type === "forgot" ? "Enviar link" : authTitles[type]}
              </button>
            </form>`}
        <div className="lc-auth-switch">
          ${type === "login"
            ? html`<span>Primeira vez na LEVEL?</span><a href=${"/cadastro" + context}>Criar conta</a>`
            : html`<a href=${"/login" + context}>${type === "register" ? "Já tenho conta" : "Voltar para o login"}</a>`}
        </div>
      </section>
      <a href="/" className="lc-auth-return">Voltar para a loja</a>
    </main>`;
}

export function NotificationMenu({ box, readAll, refresh, openLink }) {
  const last = box?.itens[0]?.id;
  return html`<details className="lc-nav-popover lc-notification-menu"
    onToggle=${(event) => { if (event.currentTarget.open) refresh(); }}>
    <summary aria-label=${`Notificações${box?.naoLidas ? `, ${box.naoLidas} não lidas` : ""}`}>
      <${Icon} name="bell" />${box?.naoLidas ? html`<span className="lc-count">${box.naoLidas > 99 ? "99+" : box.naoLidas}</span>` : null}
    </summary>
    <div className="lc-popover-panel">
      <div className="lc-notification-heading"><strong>Notificações</strong>
        ${box?.naoLidas && last ? html`<button type="button" onClick=${() => readAll(last)}>Marcar como lidas</button>` : null}
      </div>
      ${box ? box.itens.length ? html`<ul>
        ${box.itens.map((item) => html`<li key=${item.id} className=${item.lida ? "" : "unread"}>
          <a href=${item.destino} onClick=${(event) => {
            if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            event.currentTarget.closest("details").open = false;
            openLink(item.destino);
          }}><strong>${item.titulo}</strong><span>${item.mensagem}</span>
            <time dateTime=${item.criadoEm}>${dates.format(new Date(item.criadoEm))}</time></a>
        </li>`)}
      </ul>` : html`<p>Você ainda não tem notificações.</p>` : html`<p>Buscando notificações…</p>`}
    </div>
  </details>`;
}

export function EventsPage({ auth, getJson, postJson, notify }) {
  const [events, setEvents] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(null);
  const load = () => getJson("/api/events").then(setEvents).catch(() => setError("Não foi possível carregar os eventos."));
  useEffect(() => { load(); }, [auth.authenticated]);
  async function claim(event) {
    setBusy(event.id);
    setError("");
    try {
      await postJson(`/api/account/events/${event.id}/claim`, {});
      await load();
      notify("Selo resgatado", event.selo);
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(null);
    }
  }
  return html`<main className="lc-page lc-events-page">
    <section className="lc-events-hero">
      <span className="lc-eyebrow">LEVEL REWARDS</span>
      <h1>Eventos da loja</h1>
      <p>Veja as campanhas da semana, acompanhe seu progresso e desbloqueie selos para a sua coleção.</p>
    </section>
    ${error ? html`<p className="lc-form-notice error" role="alert">${error}</p>` : null}
    ${events === null ? html`<p role="status">Carregando eventos…</p>`
      : events.length === 0 ? html`<div className="lc-event-card"><h2>Nenhum evento publicado</h2>
        <p>As próximas campanhas da loja aparecem aqui.</p><a href="/passe">Explorar o passe →</a></div>`
      : html`<div className="lc-events-grid">${events.map((event) => html`
        <article className="lc-event-card" key=${event.id}>
          <div className="lc-event-top"><span className="lc-event-state">${event.estado}</span>
            <strong>${event.multiplicador}× XP</strong></div>
          <h2>${event.titulo}</h2><p>${event.descricao}</p>
          <dl><div><dt>Período</dt><dd>${dates.format(new Date(event.inicio))} – ${dates.format(new Date(event.fim))}</dd></div>
            <div><dt>Compra mínima</dt><dd>${reais.format(event.minimoCompra)} em produtos</dd></div>
            <div><dt>Limite por cliente</dt><dd>${event.limiteXp} XP</dd></div></dl>
          <div className="lc-event-reward"><span>Selo digital</span><strong>${event.selo}</strong></div>
          ${auth.authenticated ? html`
            <label className="lc-event-progress">${event.xp} de ${event.metaXp} XP para o selo
              <progress value=${Math.min(event.xp, event.metaXp)} max=${event.metaXp}></progress>
            </label>
            ${event.resgatado
              ? html`<strong>${event.xp >= event.metaXp ? "Selo na sua coleção" : "Selo indisponível após ajuste de XP"}</strong>`
              : html`<button className="lc-btn lc-btn-primary" type="button"
                  disabled=${!event.podeResgatar || busy !== null} onClick=${() => claim(event)}>
                ${busy === event.id ? "Resgatando…" : "Resgatar selo"}
              </button>`}`
            : html`<a href="/login?returnUrl=%2Feventos">Entre para acompanhar seu XP →</a>`}
          <small>XP da campanha separado do passe. Resgates até sete dias após o fim.</small>
        </article>`)}</div>`}
  </main>`;
}

export function EventBadges({ getJson }) {
  const [badges, setBadges] = useState([]);
  useEffect(() => { getJson("/api/account/events/badges").then(setBadges).catch(() => {}); }, []);
  if (!badges.length) return null;
  return html`<section className="lc-checkout-card lc-event-badges"><h3>Selos de eventos</h3>
    <ul>${badges.map((badge, index) => html`<li key=${index}><${Icon} name="shield" />${badge}</li>`)}</ul>
  </section>`;
}
