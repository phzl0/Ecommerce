package com.paulo.ecommerce.dto.api;

import java.math.BigDecimal;
import java.util.List;

public record AccountOrderDetailResponse(
        Long id,
        String numero,
        String status,
        BigDecimal total,
        BigDecimal subtotal,
        BigDecimal frete,
        BigDecimal desconto,
        String dataPedido,
        String enderecoEntrega,
        String nomeDestinatario,
        String telefoneContato,
        String cepEntrega,
        String cidadeEntrega,
        String estadoEntrega,
        String complementoEntrega,
        String observacoes,
        String pagamentoStatus,
        String metodoPagamento,
        String pagoEm,
        List<AccountOrderItemResponse> itens
) {
}
