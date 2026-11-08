package com.paulo.ecommerce.service;

import com.paulo.ecommerce.dto.store.StoreCatalogResponse;
import com.paulo.ecommerce.dto.store.StoreBannerResponse;
import com.paulo.ecommerce.dto.store.StoreCategoryResponse;
import com.paulo.ecommerce.dto.store.StoreHomeResponse;
import com.paulo.ecommerce.dto.store.StoreProductCardResponse;
import com.paulo.ecommerce.dto.store.StoreProductDetailResponse;
import com.paulo.ecommerce.dto.store.StoreReviewResponse;
import com.paulo.ecommerce.model.Categoria;
import com.paulo.ecommerce.model.Produto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StorefrontService {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final AvaliacaoService avaliacaoService;

    public StorefrontService(ProdutoService produtoService,
                             CategoriaService categoriaService,
                             AvaliacaoService avaliacaoService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.avaliacaoService = avaliacaoService;
    }

    public StoreHomeResponse carregarHome() {
        return new StoreHomeResponse(
                "LEVEL COMMERCE",
                "Tecnologia, lifestyle e performance em uma vitrine premium.",
                "Uma loja pensada para conversao, com curadoria forte e visual de marca.",
                List.of(
                        new StoreBannerResponse(
                                "Tecnologia para o seu dia",
                                "Encontre acessórios, áudio e equipamentos para trabalho e lazer.",
                                "DESTAQUES DA LOJA",
                                "Explorar catálogo",
                                "/catalogo",
                                "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1400&q=80"
                        ),
                        new StoreBannerResponse(
                                "Descubra os eventos da LEVEL",
                                "Acompanhe as campanhas da semana e desbloqueie selos na sua conta.",
                                "LEVEL REWARDS",
                                "Ver eventos",
                                "/eventos",
                                "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80"
                        )
                ),
                categoriaService.listarTodas().stream().map(this::toCategory).toList(),
                produtoService.listarDestaques(8).stream().map(this::toCard).toList(),
                produtoService.listarRecentes(6).stream().map(this::toCard).toList()
        );
    }

    public StoreCatalogResponse carregarCatalogo(String busca,
                                                 Long categoriaId,
                                                 BigDecimal precoMin,
                                                 BigDecimal precoMax,
                                                 String ordenacao) {
        List<StoreProductCardResponse> produtos = produtoService
                .filtrarCatalogo(busca, categoriaId, precoMin, precoMax, ordenacao)
                .stream()
                .map(this::toCard)
                .toList();

        return new StoreCatalogResponse(
                categoriaService.listarTodas().stream().map(this::toCategory).toList(),
                produtos,
                produtos.size()
        );
    }

    public StoreProductDetailResponse carregarProduto(Long id) {
        Produto produto = produtoService.buscarPorId(id);
        List<StoreProductCardResponse> relacionados = produto.getCategoria() == null
                ? List.of()
                : produtoService.listarPorCategoria(produto.getCategoria().getId()).stream()
                .filter(rel -> !rel.getId().equals(produto.getId()))
                .limit(4)
                .map(this::toCard)
                .toList();

        return new StoreProductDetailResponse(
                produto.getId(),
                produto.getNome(),
                produto.getSku(),
                produto.getMarca(),
                produto.getDescricaoCurta(),
                produto.getDescricao(),
                produto.getImagemUrl(),
                produto.getPreco(),
                produto.getEstoque(),
                produto.isDestaque(),
                avaliacaoService.mediaPorProduto(produto.getId()),
                avaliacaoService.totalAvaliacoes(produto.getId()),
                toCategory(produto.getCategoria()),
                relacionados,
                avaliacaoService.listarPorProduto(produto.getId()).stream()
                        .limit(5)
                        .map(avaliacao -> new StoreReviewResponse(
                                avaliacao.getUsuario().getNome(),
                                avaliacao.getNota(),
                                avaliacao.getComentario(),
                                avaliacao.getCriadoEm() == null ? "" : avaliacao.getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        ))
                        .toList()
        );
    }

    private StoreProductCardResponse toCard(Produto produto) {
        return new StoreProductCardResponse(
                produto.getId(),
                produto.getNome(),
                produto.getSku(),
                produto.getMarca(),
                produto.getDescricaoCurta(),
                produto.getImagemUrl(),
                produto.getPreco(),
                produto.getEstoque(),
                produto.isDestaque(),
                toCategory(produto.getCategoria())
        );
    }

    private StoreCategoryResponse toCategory(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new StoreCategoryResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao()
        );
    }
}
