package com.paulo.ecommerce.dto.api;

public record PasseNivelResponse(
        long recompensaId,
        int nivel,
        int xpNecessario,
        String titulo,
        String descricao,
        String tipoDigital,
        String codigo,
        boolean desbloqueado,
        boolean resgatado,
        boolean disponivelParaResgate,
        int progressoPercentual
) {
}
