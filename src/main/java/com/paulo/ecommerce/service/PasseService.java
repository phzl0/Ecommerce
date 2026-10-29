package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.api.PasseClaimResponse;
import com.paulo.ecommerce.dto.api.PasseDashboardResponse;
import com.paulo.ecommerce.dto.api.PasseHistoricoXpResponse;
import com.paulo.ecommerce.dto.api.PasseMissaoResponse;
import com.paulo.ecommerce.dto.api.PasseNivelResponse;
import com.paulo.ecommerce.exception.RecursoNaoEncontradoException;
import com.paulo.ecommerce.model.PasseAcaoTipo;
import com.paulo.ecommerce.model.PasseMissaoProgresso;
import com.paulo.ecommerce.model.PasseMissaoTemplate;
import com.paulo.ecommerce.model.PasseOrigemXpTipo;
import com.paulo.ecommerce.model.PassePeriodicidade;
import com.paulo.ecommerce.model.PasseProgressoUsuario;
import com.paulo.ecommerce.model.PasseRecompensaNivel;
import com.paulo.ecommerce.model.PasseRecompensaResgate;
import com.paulo.ecommerce.model.PasseTemporada;
import com.paulo.ecommerce.model.PasseXpHistorico;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.PasseMissaoProgressoRepository;
import com.paulo.ecommerce.repository.PasseMissaoTemplateRepository;
import com.paulo.ecommerce.repository.PasseProgressoUsuarioRepository;
import com.paulo.ecommerce.repository.PasseRecompensaNivelRepository;
import com.paulo.ecommerce.repository.PasseRecompensaResgateRepository;
import com.paulo.ecommerce.repository.PasseTemporadaRepository;
import com.paulo.ecommerce.repository.PasseXpHistoricoRepository;
import com.paulo.ecommerce.repository.ProdutoRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PasseService {

    private static final Locale LOCALE_PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @org.springframework.beans.factory.annotation.Autowired private com.paulo.ecommerce.repository.PasseEventoRepository eventoRepository;
    @org.springframework.beans.factory.annotation.Autowired private com.paulo.ecommerce.repository.PedidoRepository pedidoRepository;
    @org.springframework.beans.factory.annotation.Value("${app.pass.premium-price:197.00}") private BigDecimal precoPremium;
    private final PasseTemporadaRepository temporadaRepository;
    private final PasseRecompensaNivelRepository recompensaRepository;
    private final PasseMissaoTemplateRepository missaoTemplateRepository;
    private final PasseMissaoProgressoRepository missaoProgressoRepository;
    private final PasseProgressoUsuarioRepository progressoUsuarioRepository;
    private final PasseXpHistoricoRepository xpHistoricoRepository;
    private final PasseRecompensaResgateRepository recompensaResgateRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;

    public PasseService(PasseTemporadaRepository temporadaRepository,
                        PasseRecompensaNivelRepository recompensaRepository,
                        PasseMissaoTemplateRepository missaoTemplateRepository,
                        PasseMissaoProgressoRepository missaoProgressoRepository,
                        PasseProgressoUsuarioRepository progressoUsuarioRepository,
                        PasseXpHistoricoRepository xpHistoricoRepository,
                        PasseRecompensaResgateRepository recompensaResgateRepository,
                        UsuarioRepository usuarioRepository,
                        ProdutoRepository produtoRepository) {
        this.temporadaRepository = temporadaRepository;
        this.recompensaRepository = recompensaRepository;
        this.missaoTemplateRepository = missaoTemplateRepository;
        this.missaoProgressoRepository = missaoProgressoRepository;
        this.progressoUsuarioRepository = progressoUsuarioRepository;
        this.xpHistoricoRepository = xpHistoricoRepository;
        this.recompensaResgateRepository = recompensaResgateRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public void garantirPasseAtual() {
        YearMonth yearMonth = YearMonth.now();
        PasseTemporada temporada = temporadaRepository.findByAnoAndMes(yearMonth.getYear(), yearMonth.getMonthValue())
                .orElseGet(() -> criarTemporada(yearMonth));

        if (recompensaRepository.findByTemporadaIdOrderByNivelAsc(temporada.getId()).size() < temporada.getTotalNiveis()) {
            criarRecompensas(temporada);
        }

        if (missaoTemplateRepository.findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(temporada.getId()).isEmpty()) {
            criarMissoes(temporada);
        }
    }

    @Transactional
    public PasseDashboardResponse carregarDashboard(String emailUsuario) {
        PasseTemporada temporada = obterPasseAtual();
        Usuario usuario = buscarUsuario(emailUsuario);
        PasseProgressoUsuario progressoUsuario = obterOuCriarProgresso(usuario, temporada);
        List<PasseRecompensaNivel> recompensas = recompensaRepository.findByTemporadaIdOrderByNivelAsc(temporada.getId());
        List<PasseMissaoTemplate> templates = missaoTemplateRepository
                .findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(temporada.getId());
        Map<Long, PasseMissaoProgresso> progressoMap = carregarProgressoMissao(usuario, temporada, templates);

        int xpPorNivel = temporada.getXpPorNivel();
        int xpNoNivelAtual = progressoUsuario.getXpTotal() % xpPorNivel;
        int xpParaProximoNivel = progressoUsuario.getNivelAtual() >= temporada.getTotalNiveis()
                ? 0
                : xpPorNivel - xpNoNivelAtual;
        java.util.Set<Long> resgatadas = recompensaResgateRepository.findByUsuarioIdAndTemporadaId(usuario.getId(), temporada.getId())
                .stream()
                .map(resgate -> resgate.getRecompensa().getId())
                .collect(Collectors.toSet());

        List<PasseNivelResponse> niveis = recompensas.stream()
                .map(recompensa -> {
                    int xpNecessario = recompensa.getNivel() * xpPorNivel;
                    int xpBaseNivel = Math.max(0, xpNecessario - xpPorNivel);
                    int progressoBruto = progressoUsuario.getXpTotal() - xpBaseNivel;
                    int progressoPercentual = Math.max(0, Math.min(100,
                            (int) Math.round((progressoBruto / (double) xpPorNivel) * 100)));
                    boolean desbloqueado = progressoUsuario.getXpTotal() >= xpNecessario;
                    boolean resgatado = resgatadas.contains(recompensa.getId());
                    return new PasseNivelResponse(
                            recompensa.getId(),
                            !gratuita(recompensa.getNivel()),
                            recompensa.getNivel(),
                            xpNecessario,
                            recompensa.getTitulo(),
                            recompensa.getDescricao(),
                            recompensa.getTipoDigital(),
                            resgatado ? recompensa.getCodigo() : null,
                            desbloqueado,
                            resgatado,
                            desbloqueado && !resgatado && (gratuita(recompensa.getNivel()) || premiumAtivo(usuario,temporada)),
                            progressoPercentual
                    );
                })
                .toList();

        List<PasseMissaoResponse> missoesDiarias = buildMissaoResponses(templates, progressoMap, PassePeriodicidade.DIARIA);
        List<PasseMissaoResponse> missoesSemanais = buildMissaoResponses(templates, progressoMap, PassePeriodicidade.SEMANAL);
        List<PasseMissaoResponse> missoesMensais = buildMissaoResponses(templates, progressoMap, PassePeriodicidade.MENSAL);
        List<PasseHistoricoXpResponse> historico = xpHistoricoRepository
                .findTop10ByUsuarioIdAndTemporadaIdOrderByCriadoEmDesc(usuario.getId(), temporada.getId())
                .stream()
                .map(item -> new PasseHistoricoXpResponse(
                        item.isEstornado() ? "Estornado: " + item.getDescricao() : item.getDescricao(),
                        item.isEstornado() ? 0 : item.getXpGanho(),
                        item.getCriadoEm() == null ? "" : item.getCriadoEm().format(DATE_TIME_FORMATTER)
                ))
                .toList();

        return new PasseDashboardResponse(
                premiumAtivo(usuario,temporada), precoPremium,
                temporada.getNome(),
                temporada.getInicioEm().format(DateTimeFormatter.ofPattern("dd/MM")) + " a " +
                        temporada.getFimEm().format(DateTimeFormatter.ofPattern("dd/MM")),
                progressoUsuario.getXpTotal(),
                progressoUsuario.getNivelAtual(),
                temporada.getTotalNiveis(),
                xpPorNivel,
                xpNoNivelAtual,
                xpParaProximoNivel,
                niveis,
                missoesDiarias,
                missoesSemanais,
                missoesMensais,
                historico
        );
    }

    @Transactional
    public void registrarAcaoNavegador(String email, PasseAcaoTipo acao, String referencia) {
        Usuario usuario = buscarUsuario(email);
        if (acao != PasseAcaoTipo.CHECKIN && acao != PasseAcaoTipo.VIEW_PRODUCT && acao != PasseAcaoTipo.ADD_TO_CART) throw new IllegalArgumentException("Ação reservada ao servidor.");
        String ref = "checkin";
        if (acao != PasseAcaoTipo.CHECKIN) {
            if (referencia == null || !referencia.matches("(?:product|cart|buy-now)-[0-9]{1,18}")) throw new IllegalArgumentException("Produto inválido.");
            Long id = Long.parseLong(referencia.substring(referencia.lastIndexOf('-')+1));
            Produto produto = produtoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
            if (!produto.isAtivo()) throw new IllegalArgumentException("Produto indisponível.");
            ref = "produto-" + id;
        }
        LocalDate dia = LocalDate.now();
        if (eventoRepository.existsByUsuarioIdAndDiaAndAcaoAndReferencia(usuario.getId(),dia,acao,ref)) return;
        var event = new com.paulo.ecommerce.model.PasseEvento(); event.usuarioId=usuario.getId(); event.dia=dia; event.acao=acao; event.referencia=ref;
        eventoRepository.save(event);
        registrarAcao(email, acao, 1, ref);
    }

    @Transactional
    public void registrarAcao(String emailUsuario, PasseAcaoTipo acaoTipo, int valor, String referencia) {
        PasseTemporada temporada = obterPasseAtual();
        Usuario usuario = buscarUsuario(emailUsuario);
        List<PasseMissaoTemplate> templates = missaoTemplateRepository
                .findByTemporadaIdAndAtivaTrueOrderByPeriodicidadeAscOrdemExibicaoAsc(temporada.getId())
                .stream()
                .filter(template -> template.getAcaoTipo() == acaoTipo)
                .toList();

        for (PasseMissaoTemplate template : templates) {
            PeriodoPeriodo periodo = resolverPeriodo(template.getPeriodicidade());
            PasseMissaoProgresso progresso = missaoProgressoRepository
                    .findByUsuarioIdAndTemplateIdAndPeriodoInicio(usuario.getId(), template.getId(), periodo.inicio())
                    .orElseGet(() -> criarProgressoMissao(usuario, temporada, template, periodo));

            if (Boolean.TRUE.equals(progresso.getConcluida())) {
                continue;
            }

            int incremento = Math.max(valor, 0);
            int novoProgresso = Math.min(template.getMetaValor(), progresso.getProgressoAtual() + incremento);
            progresso.setProgressoAtual(novoProgresso);

            if (novoProgresso >= template.getMetaValor()) {
                progresso.setConcluida(Boolean.TRUE);
                progresso.setConcluidaEm(LocalDateTime.now());
                if (!Boolean.TRUE.equals(progresso.getRecompensaEntregue())) {
                    entregarXpMissao(usuario, temporada, template, periodo);
                    progresso.setRecompensaEntregue(Boolean.TRUE);
                    progresso.setRecompensaEntregueEm(LocalDateTime.now());
                }
            }

            missaoProgressoRepository.save(progresso);
        }
    }

    @Transactional
    public void registrarXpCompraAprovada(String emailUsuario, Long pedidoId, BigDecimal totalPedido) {
        PasseTemporada temporada = obterPasseAtual();
        Usuario usuario = buscarUsuario(emailUsuario);
        String referencia = "PURCHASE-" + pedidoId;
        if (!xpHistoricoRepository.findByUsuarioIdAndOrigemTipoAndReferencia(usuario.getId(), PasseOrigemXpTipo.PURCHASE, referencia).isEmpty()) {
            return;
        }

        int xpCompra = Math.max(5, Math.min(120, totalPedido.divideToIntegralValue(BigDecimal.TEN).intValue()));
        adicionarXp(usuario, temporada, xpCompra, PasseOrigemXpTipo.PURCHASE, referencia,
                "Compra aprovada do pedido #" + pedidoId);

        registrarAcao(emailUsuario, PasseAcaoTipo.PURCHASE_APPROVED, 1, referencia);
        registrarAcao(emailUsuario, PasseAcaoTipo.SPEND_APPROVED_CENTS, totalPedido.multiply(new BigDecimal("100")).intValue(), referencia);
    }

    @Transactional
    public void estornarCompra(String email, Long pedidoId) {
        Usuario usuario = buscarUsuario(email);
        var registros = xpHistoricoRepository.findByUsuarioIdAndOrigemTipoAndReferencia(usuario.getId(), PasseOrigemXpTipo.PURCHASE, "PURCHASE-"+pedidoId);
        for (PasseXpHistorico registro : registros) {
            registro.setEstornado(true); xpHistoricoRepository.save(registro);
            PasseTemporada temporada = registro.getTemporada();
            var pedidos = pedidoRepository.findByUsuarioIdOrderByDataPedidoDesc(usuario.getId()).stream()
                .filter(p -> p.getStatus() != com.paulo.ecommerce.model.Pedido.Status.CANCELADO && p.getPagamento() != null && p.getPagamento().getStatus() == com.paulo.ecommerce.model.Pagamento.StatusPagamento.APROVADO).toList();
            for (PasseMissaoProgresso progresso : missaoProgressoRepository.findByUsuarioIdAndTemporadaId(usuario.getId(),temporada.getId())) {
                PasseMissaoTemplate template = progresso.getTemplate();
                if (template.getAcaoTipo() != PasseAcaoTipo.PURCHASE_APPROVED && template.getAcaoTipo() != PasseAcaoTipo.SPEND_APPROVED_CENTS) continue;
                var elegiveis = pedidos.stream().filter(p -> p.getPagamento().getPagoEm()!=null && !p.getPagamento().getPagoEm().isBefore(progresso.getPeriodoInicio()) && p.getPagamento().getPagoEm().isBefore(progresso.getPeriodoFim())).toList();
                int valor = template.getAcaoTipo() == PasseAcaoTipo.PURCHASE_APPROVED ? elegiveis.size() : elegiveis.stream().map(p -> p.getSubtotal().multiply(new BigDecimal("100"))).reduce(BigDecimal.ZERO,BigDecimal::add).intValueExact();
                progresso.setProgressoAtual(Math.min(template.getMetaValor(),valor));
                boolean completa = valor >= template.getMetaValor(); progresso.setConcluida(completa); progresso.setRecompensaEntregue(completa);
                String ref = "MISSION-"+template.getCodigo()+"-"+progresso.getPeriodoInicio();
                xpHistoricoRepository.findByUsuarioIdAndTemporadaIdAndOrigemTipoAndReferencia(usuario.getId(),temporada.getId(),PasseOrigemXpTipo.MISSION,ref).ifPresent(h -> { h.setEstornado(!completa); xpHistoricoRepository.save(h); });
                missaoProgressoRepository.save(progresso);
            }
            recalcularXp(usuario,temporada);
        }
    }

    private void recalcularXp(Usuario usuario,PasseTemporada temporada) {
        int total = xpHistoricoRepository.findByUsuarioIdAndTemporadaId(usuario.getId(),temporada.getId()).stream().filter(h -> !h.isEstornado()).mapToInt(PasseXpHistorico::getXpGanho).sum();
        PasseProgressoUsuario progresso = obterOuCriarProgresso(usuario,temporada);
        progresso.setXpTotal(Math.max(0,Math.min(total,temporada.getTotalNiveis()*temporada.getXpPorNivel())));
        progresso.setNivelAtual(Math.max(1,Math.min(temporada.getTotalNiveis(),progresso.getXpTotal()/temporada.getXpPorNivel())));
        progressoUsuarioRepository.save(progresso);
    }

    @Transactional
    public PasseClaimResponse reclamarRecompensa(String emailUsuario, Long recompensaId) {
        PasseTemporada temporada = obterPasseAtual();
        Usuario usuario = buscarUsuario(emailUsuario);
        PasseRecompensaNivel recompensa = recompensaRepository.findById(recompensaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recompensa", recompensaId));

        if (!recompensa.getTemporada().getId().equals(temporada.getId())) throw new IllegalArgumentException("Esta recompensa não pertence à temporada atual.");
        if (!gratuita(recompensa.getNivel()) && !premiumAtivo(usuario,temporada)) throw new IllegalStateException("Esta recompensa exige passe premium ativo.");
        int xpNecessario = recompensa.getNivel() * temporada.getXpPorNivel();
        PasseProgressoUsuario progresso = obterOuCriarProgresso(usuario, temporada);
        if (progresso.getXpTotal() < xpNecessario) {
            throw new IllegalStateException("Recompensa bloqueada.");
        }

        recompensaResgateRepository.findByUsuarioIdAndRecompensaId(usuario.getId(), recompensaId)
                .orElseGet(() -> {
                    PasseRecompensaResgate resgate = new PasseRecompensaResgate();
                    resgate.setUsuario(usuario);
                    resgate.setTemporada(temporada);
                    resgate.setRecompensa(recompensa);
                    return recompensaResgateRepository.save(resgate);
                });

        return new PasseClaimResponse(
                recompensa.getCodigo(),
                recompensa.getTitulo(),
                recompensa.getDescricao()
        );
    }

    private boolean gratuita(int nivel) { return nivel % 3 == 0 || nivel == 1 || nivel % 5 == 0; }

    private boolean premiumAtivo(Usuario usuario,PasseTemporada temporada) {
        return pedidoRepository.findByUsuarioIdAndTemporadaPasseId(usuario.getId(),temporada.getId()).stream().anyMatch(p -> p.getStatus()!=com.paulo.ecommerce.model.Pedido.Status.CANCELADO && p.getPagamento()!=null && p.getPagamento().getStatus()==com.paulo.ecommerce.model.Pagamento.StatusPagamento.APROVADO);
    }

    @Transactional
    public Long criarPedidoPremium(String email) {
        Usuario usuario = buscarUsuario(email); PasseTemporada temporada = obterPasseAtual();
        if (premiumAtivo(usuario,temporada)) throw new IllegalStateException("Você já possui o passe premium desta temporada.");
        var anteriores = pedidoRepository.findByUsuarioIdAndTemporadaPasseId(usuario.getId(),temporada.getId());
        var aberto = anteriores.stream().filter(p -> p.getStatus()==com.paulo.ecommerce.model.Pedido.Status.PENDENTE).findFirst();
        if(aberto.isPresent()) return aberto.get().getId();
        if (precoPremium==null || precoPremium.signum()<=0) throw new IllegalStateException("Configure o preço do passe.");
        Produto produto = produtoRepository.findBySku("PASSE-PREMIUM-"+temporada.getSlug()).orElseGet(() -> {
            Produto virtual=new Produto();virtual.setNome("Passe premium — "+temporada.getNome());virtual.setSku("PASSE-PREMIUM-"+temporada.getSlug());virtual.setPreco(precoPremium);virtual.setAtivo(false);virtual.setEstoque(0);return produtoRepository.save(virtual);
        });
        var pedido=new com.paulo.ecommerce.model.Pedido();pedido.setUsuario(usuario);pedido.setTemporadaPasseId(temporada.getId());pedido.setNumero("PP-"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,24));pedido.setSubtotal(precoPremium);pedido.setFrete(BigDecimal.ZERO);pedido.setTotal(precoPremium);pedido.setEnderecoEntrega("Entrega digital na conta");
        var item=new com.paulo.ecommerce.model.ItemPedido();item.setPedido(pedido);item.setProduto(produto);item.setQuantidade(1);item.setPrecoUnitario(precoPremium);pedido.setItens(List.of(item));
        return pedidoRepository.save(pedido).getId();
    }

    private List<PasseMissaoResponse> buildMissaoResponses(List<PasseMissaoTemplate> templates,
                                                           Map<Long, PasseMissaoProgresso> progressoMap,
                                                           PassePeriodicidade periodicidade) {
        return templates.stream()
                .filter(template -> template.getPeriodicidade() == periodicidade)
                .map(template -> {
                    PeriodoPeriodo periodo = resolverPeriodo(template.getPeriodicidade());
                    PasseMissaoProgresso progresso = progressoMap.get(template.getId());
                    int progressoAtual = progresso == null ? 0 : progresso.getProgressoAtual();
                    boolean concluida = progresso != null && Boolean.TRUE.equals(progresso.getConcluida());
                    CtaMissao cta = resolverCta(template);
                    return new PasseMissaoResponse(
                            template.getId(),
                            template.getPeriodicidade().name(),
                            template.getTitulo(),
                            template.getDescricao(),
                            progressoAtual,
                            template.getMetaValor(),
                            template.getXpRecompensa(),
                            concluida,
                            periodo.fim().format(DATE_TIME_FORMATTER),
                            cta.label(),
                            cta.href()
                    );
                })
                .toList();
    }

    private Map<Long, PasseMissaoProgresso> carregarProgressoMissao(Usuario usuario,
                                                                    PasseTemporada temporada,
                                                                    List<PasseMissaoTemplate> templates) {
        return templates.stream()
                .map(template -> {
                    PeriodoPeriodo periodo = resolverPeriodo(template.getPeriodicidade());
                    return missaoProgressoRepository
                            .findByUsuarioIdAndTemplateIdAndPeriodoInicio(usuario.getId(), template.getId(), periodo.inicio())
                            .orElseGet(() -> missaoProgressoRepository.save(criarProgressoMissao(usuario, temporada, template, periodo)));
                })
                .collect(Collectors.toMap(progresso -> progresso.getTemplate().getId(), progresso -> progresso));
    }

    private PasseTemporada obterPasseAtual() {
        garantirPasseAtual();
        YearMonth yearMonth = YearMonth.now();
        return temporadaRepository.findByAnoAndMes(yearMonth.getYear(), yearMonth.getMonthValue())
                .orElseThrow(() -> new IllegalStateException("Passe mensal atual nao encontrado."));
    }

    private Usuario buscarUsuario(String emailUsuario) {
        return usuarioRepository.findLockedByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", 0L));
    }

    private PasseProgressoUsuario obterOuCriarProgresso(Usuario usuario, PasseTemporada temporada) {
        return progressoUsuarioRepository.findByUsuarioIdAndTemporadaId(usuario.getId(), temporada.getId())
                .orElseGet(() -> {
                    PasseProgressoUsuario progresso = new PasseProgressoUsuario();
                    progresso.setUsuario(usuario);
                    progresso.setTemporada(temporada);
                    return progressoUsuarioRepository.save(progresso);
                });
    }

    private PasseMissaoProgresso criarProgressoMissao(Usuario usuario,
                                                      PasseTemporada temporada,
                                                      PasseMissaoTemplate template,
                                                      PeriodoPeriodo periodo) {
        PasseMissaoProgresso progresso = new PasseMissaoProgresso();
        progresso.setUsuario(usuario);
        progresso.setTemporada(temporada);
        progresso.setTemplate(template);
        progresso.setPeriodoInicio(periodo.inicio());
        progresso.setPeriodoFim(periodo.fim());
        return progresso;
    }

    private void entregarXpMissao(Usuario usuario,
                                  PasseTemporada temporada,
                                  PasseMissaoTemplate template,
                                  PeriodoPeriodo periodo) {
        String referencia = "MISSION-" + template.getCodigo() + "-" + periodo.inicio();
        adicionarXp(usuario, temporada, template.getXpRecompensa(), PasseOrigemXpTipo.MISSION, referencia,
                template.getTitulo());
    }

    private void adicionarXp(Usuario usuario,
                             PasseTemporada temporada,
                             int xp,
                             PasseOrigemXpTipo origemTipo,
                             String referencia,
                             String descricao) {
        var existente = xpHistoricoRepository.findByUsuarioIdAndTemporadaIdAndOrigemTipoAndReferencia(usuario.getId(), temporada.getId(), origemTipo, referencia);
        if (existente.isPresent()) {
            if (existente.get().isEstornado()) { existente.get().setEstornado(false); xpHistoricoRepository.save(existente.get()); recalcularXp(usuario,temporada); }
            return;
        }

        PasseProgressoUsuario progressoUsuario = obterOuCriarProgresso(usuario, temporada);
        int xpTotal = Math.min(temporada.getTotalNiveis() * temporada.getXpPorNivel(), progressoUsuario.getXpTotal() + xp);
        progressoUsuario.setXpTotal(xpTotal);
        progressoUsuario.setNivelAtual(Math.max(1, Math.min(
                temporada.getTotalNiveis(),
                xpTotal / temporada.getXpPorNivel()
        )));
        if (xpTotal >= temporada.getTotalNiveis() * temporada.getXpPorNivel()) {
            progressoUsuario.setNivelAtual(temporada.getTotalNiveis());
        }
        progressoUsuario.setUltimoGanhoXpEm(LocalDateTime.now());
        progressoUsuarioRepository.save(progressoUsuario);

        PasseXpHistorico historico = new PasseXpHistorico();
        historico.setUsuario(usuario);
        historico.setTemporada(temporada);
        historico.setOrigemTipo(origemTipo);
        historico.setReferencia(referencia);
        historico.setXpGanho(xp);
        historico.setDescricao(descricao);
        xpHistoricoRepository.save(historico);
    }

    private PasseTemporada criarTemporada(YearMonth yearMonth) {
        PasseTemporada temporada = new PasseTemporada();
        temporada.setAno(yearMonth.getYear());
        temporada.setMes(yearMonth.getMonthValue());
        temporada.setSlug("passe-" + yearMonth.getYear() + "-" + String.format("%02d", yearMonth.getMonthValue()));
        String mesNome = yearMonth.getMonth().getDisplayName(TextStyle.FULL, LOCALE_PT_BR);
        temporada.setNome("Passe " + capitalize(mesNome) + " " + yearMonth.getYear());
        temporada.setTotalNiveis(30);
        temporada.setXpPorNivel(100);
        temporada.setAtiva(Boolean.TRUE);
        temporada.setInicioEm(yearMonth.atDay(1));
        temporada.setFimEm(yearMonth.atEndOfMonth());
        return temporadaRepository.save(temporada);
    }

    private void criarRecompensas(PasseTemporada temporada) {
        String[] tipos = {"Cupom", "Avatar", "Wallpaper", "Badge", "Tema"};
        java.util.Set<Integer> niveisExistentes = recompensaRepository.findByTemporadaIdOrderByNivelAsc(temporada.getId())
                .stream()
                .map(PasseRecompensaNivel::getNivel)
                .collect(Collectors.toSet());
        for (int nivel = 1; nivel <= temporada.getTotalNiveis(); nivel++) {
            if (niveisExistentes.contains(nivel)) {
                continue;
            }
            PasseRecompensaNivel recompensa = new PasseRecompensaNivel();
            recompensa.setTemporada(temporada);
            recompensa.setNivel(nivel);
            recompensa.setTitulo("Recompensa digital nivel " + nivel);
            recompensa.setDescricao(descricaoRecompensa(nivel));
            recompensa.setTipoDigital(tipos[(nivel - 1) % tipos.length]);
            recompensa.setCodigo("LEVEL-" + temporada.getAno() + temporada.getMes() + "-" + String.format("%02d", nivel));
            recompensaRepository.save(recompensa);
        }
    }

    private void criarMissoes(PasseTemporada temporada) {
        criarMissao(temporada, "daily-checkin", "Check-in diario", "Entre na loja e marque sua presenca do dia.", PassePeriodicidade.DIARIA, PasseAcaoTipo.CHECKIN, 1, 20, 1);
        criarMissao(temporada, "daily-view-products", "Ver 3 produtos", "Explore 3 paginas de produto no mesmo dia.", PassePeriodicidade.DIARIA, PasseAcaoTipo.VIEW_PRODUCT, 3, 25, 2);
        criarMissao(temporada, "daily-add-cart", "Adicionar item a sacola", "Coloque 1 item na sacola para testar a jornada.", PassePeriodicidade.DIARIA, PasseAcaoTipo.ADD_TO_CART, 1, 15, 3);

        criarMissao(temporada, "weekly-approved-order", "1 compra aprovada", "Conclua 1 compra com pagamento aprovado na semana.", PassePeriodicidade.SEMANAL, PasseAcaoTipo.PURCHASE_APPROVED, 1, 120, 4);
        criarMissao(temporada, "weekly-spend-500", "Gastar R$ 500 na semana", "Acumule R$ 500 em compras aprovadas na semana.", PassePeriodicidade.SEMANAL, PasseAcaoTipo.SPEND_APPROVED_CENTS, 50000, 180, 5);
        criarMissao(temporada, "weekly-review", "Fazer 1 avaliacao", "Avalie um produto comprado para gerar confianca na loja.", PassePeriodicidade.SEMANAL, PasseAcaoTipo.REVIEW_SUBMITTED, 1, 90, 6);

        criarMissao(temporada, "monthly-approved-orders", "3 compras aprovadas", "Feche 3 pedidos aprovados durante a temporada.", PassePeriodicidade.MENSAL, PasseAcaoTipo.PURCHASE_APPROVED, 3, 320, 7);
        criarMissao(temporada, "monthly-spend-1200", "Gastar R$ 1.200 no mes", "Acumule R$ 1.200 em compras aprovadas no passe atual.", PassePeriodicidade.MENSAL, PasseAcaoTipo.SPEND_APPROVED_CENTS, 120000, 420, 8);
    }

    private void criarMissao(PasseTemporada temporada,
                             String codigo,
                             String titulo,
                             String descricao,
                             PassePeriodicidade periodicidade,
                             PasseAcaoTipo acaoTipo,
                             int metaValor,
                             int xpRecompensa,
                             int ordem) {
        PasseMissaoTemplate missao = new PasseMissaoTemplate();
        missao.setTemporada(temporada);
        missao.setCodigo(codigo);
        missao.setTitulo(titulo);
        missao.setDescricao(descricao);
        missao.setPeriodicidade(periodicidade);
        missao.setAcaoTipo(acaoTipo);
        missao.setMetaValor(metaValor);
        missao.setXpRecompensa(xpRecompensa);
        missao.setOrdemExibicao(ordem);
        missaoTemplateRepository.save(missao);
    }

    private String descricaoRecompensa(int nivel) {
        return switch ((nivel - 1) % 5) {
            case 0 -> "Cupom digital de vitrine para a proxima compra.";
            case 1 -> "Avatar exclusivo da temporada para a conta.";
            case 2 -> "Wallpaper premium da LEVEL COMMERCE.";
            case 3 -> "Badge de perfil para exibir progresso.";
            default -> "Tema digital para personalizar sua area do cliente.";
        };
    }

    private PeriodoPeriodo resolverPeriodo(PassePeriodicidade periodicidade) {
        LocalDateTime now = LocalDateTime.now();
        return switch (periodicidade) {
            case DIARIA -> {
                LocalDateTime inicio = now.toLocalDate().atStartOfDay();
                yield new PeriodoPeriodo(inicio, inicio.plusDays(1));
            }
            case SEMANAL -> {
                LocalDate inicioData = now.toLocalDate().with(DayOfWeek.MONDAY);
                LocalDateTime inicio = LocalDateTime.of(inicioData, LocalTime.MIN);
                yield new PeriodoPeriodo(inicio, inicio.plusWeeks(1));
            }
            case MENSAL -> {
                LocalDate inicioData = now.toLocalDate().withDayOfMonth(1);
                LocalDateTime inicio = LocalDateTime.of(inicioData, LocalTime.MIN);
                yield new PeriodoPeriodo(inicio, inicio.plusMonths(1));
            }
        };
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(LOCALE_PT_BR) + text.substring(1);
    }

    private CtaMissao resolverCta(PasseMissaoTemplate template) {
        Produto produtoSugerido = produtoRepository.findAll().stream().findFirst().orElse(null);
        return switch (template.getAcaoTipo()) {
            case REVIEW_SUBMITTED -> produtoSugerido != null
                    ? new CtaMissao("Avaliar produto", "/produto/" + produtoSugerido.getId())
                    : new CtaMissao("Ver catalogo", "/catalogo");
            case VIEW_PRODUCT -> produtoSugerido != null
                    ? new CtaMissao("Ver produto", "/produto/" + produtoSugerido.getId())
                    : new CtaMissao("Explorar catalogo", "/catalogo");
            case ADD_TO_CART -> produtoSugerido != null
                    ? new CtaMissao("Adicionar item", "/produto/" + produtoSugerido.getId())
                    : new CtaMissao("Explorar catalogo", "/catalogo");
            case PURCHASE_APPROVED, SPEND_APPROVED_CENTS -> new CtaMissao("Comprar agora", "/catalogo");
            case CHECKIN -> new CtaMissao("Ir para inicio", "/");
        };
    }

    private record PeriodoPeriodo(LocalDateTime inicio, LocalDateTime fim) {
    }

    private record CtaMissao(String label, String href) {
    }
}
