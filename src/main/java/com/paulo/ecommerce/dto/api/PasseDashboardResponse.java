package com.paulo.ecommerce.dto.api;

import java.util.List;

public record PasseDashboardResponse(
    boolean premiumAtivo,
    java.math.BigDecimal precoPremium,
    java.time.LocalDateTime premiumValidoAte,
    String temporadaNome,
    String temporadaPeriodo,
    int xpTotal,
    int nivelAtual,
    int totalNiveis,
    int xpPorNivel,
    int xpNoNivelAtual,
    int xpParaProximoNivel,
    List<PasseNivelResponse> niveis,
    List<PasseMissaoResponse> missoesDiarias,
    List<PasseMissaoResponse> missoesSemanais,
    List<PasseMissaoResponse> missoesMensais,
    List<PasseHistoricoXpResponse> historico,
    List<ProdutoAvaliavel> produtosParaAvaliar) {

  public record ProdutoAvaliavel(Long id, String nome, String imagemUrl, java.math.BigDecimal preco, int estoque, String descricaoCurta) {}
}
