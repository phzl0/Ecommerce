package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.CarrinhoItem;
import com.paulo.ecommerce.exception.EstoqueInsuficienteException;
import com.paulo.ecommerce.model.Produto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CarrinhoService {

    private static final String SESSION_KEY = "carrinho";
    private static final BigDecimal FRETE_GRATIS = new BigDecimal("299.00");

    @SuppressWarnings("unchecked")
    public List<CarrinhoItem> getCarrinho(HttpSession session) {
        List<CarrinhoItem> carrinho =
                (List<CarrinhoItem>) session.getAttribute(SESSION_KEY);
        if (carrinho == null) {
            carrinho = new ArrayList<>();
            session.setAttribute(SESSION_KEY, carrinho);
        }
        return carrinho;
    }

    public void adicionar(HttpSession session, Produto produto, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade invalida.");
        }
        if (quantidade > produto.getEstoque()) {
            throw new EstoqueInsuficienteException(produto.getNome());
        }

        List<CarrinhoItem> carrinho = getCarrinho(session);
        CarrinhoItem existente = carrinho.stream()
                .filter(i -> i.getProdutoId().equals(produto.getId()))
                .findFirst().orElse(null);

        if (existente != null) {
            int novaQuantidade = existente.getQuantidade() + quantidade;
            if (novaQuantidade > produto.getEstoque()) {
                throw new EstoqueInsuficienteException(produto.getNome());
            }
            existente.setQuantidade(novaQuantidade);
        } else {
            carrinho.add(new CarrinhoItem(
                    produto.getId(), produto.getNome(),
                    produto.getImagemUrl(), produto.getPreco(), quantidade
            ));
        }
    }

    public void remover(HttpSession session, Long produtoId) {
        getCarrinho(session).removeIf(i -> i.getProdutoId().equals(produtoId));
    }

    public void atualizarQuantidade(HttpSession session, Long produtoId, int quantidade) {
        if (quantidade <= 0) {
            remover(session, produtoId);
            return;
        }
        getCarrinho(session).stream()
                .filter(i -> i.getProdutoId().equals(produtoId))
                .findFirst()
                .ifPresent(i -> i.setQuantidade(quantidade));
    }

    public void limpar(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    public BigDecimal getSubtotal(HttpSession session) {
        return getCarrinho(session).stream()
                .map(CarrinhoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularFrete(HttpSession session) {
        BigDecimal subtotal = getSubtotal(session);
        if (subtotal.compareTo(FRETE_GRATIS) >= 0) {
            return BigDecimal.ZERO;
        }
        if (subtotal.compareTo(new BigDecimal("149.00")) >= 0) {
            return new BigDecimal("14.90");
        }
        return new BigDecimal("24.90");
    }

    public BigDecimal getDesconto(HttpSession session) {
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotal(HttpSession session) {
        return getSubtotal(session)
                .add(calcularFrete(session))
                .subtract(getDesconto(session));
    }

    public BigDecimal getFaltaParaFreteGratis(HttpSession session) {
        BigDecimal falta = FRETE_GRATIS.subtract(getSubtotal(session));
        return falta.max(BigDecimal.ZERO);
    }

    public int getTotalItens(HttpSession session) {
        return getCarrinho(session).stream()
                .mapToInt(CarrinhoItem::getQuantidade).sum();
    }
}
