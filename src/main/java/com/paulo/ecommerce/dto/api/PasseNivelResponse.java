package com.paulo.ecommerce.dto.api;

public record PasseNivelResponse(
    long recompensaId,
    boolean premium,
    int nivel,
    int xpNecessario,
    String titulo,
    String descricao,
    String tipoDigital,
    String codigo,
    boolean desbloqueado,
    boolean resgatado,
    boolean disponivelParaResgate,
    int progressoPercentual,
    java.util.List<Opcao> opcoes) {
  public record Opcao(Long produtoId, String nome, boolean disponivel) {}
}
