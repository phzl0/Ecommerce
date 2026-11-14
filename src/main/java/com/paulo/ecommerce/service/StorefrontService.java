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

    @org.springframework.beans.factory.annotation.Autowired private com.paulo.ecommerce.repository.ProdutoRepository produtos;
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
                raizes("relevancia",8).stream().map(this::toCard).toList(),
                raizes("recentes",6).stream().map(this::toCard).toList()
        );
    }

    public StoreCatalogResponse carregarCatalogo(String busca,
                                                 Long categoriaId,
                                                 BigDecimal precoMin,
                                                 BigDecimal precoMax,
                                                 String ordenacao) {
        return carregarCatalogo(busca,categoriaId,precoMin,precoMax,ordenacao,"","","",0);
    }
    private List<Produto> raizes(String ordem,int limite){return produtos.catalogo("",null,null,null,"","","",ordem,org.springframework.data.domain.PageRequest.of(0,limite)).getContent();}
    public StoreCatalogResponse carregarCatalogo(String busca,Long categoriaId,BigDecimal min,BigDecimal max,String ordem,String cor,String tamanho,String tipo,int pagina){
        if(min!=null&&min.signum()<0||max!=null&&max.signum()<0||min!=null&&max!=null&&min.compareTo(max)>0)throw new IllegalArgumentException("Confira a faixa de preços.");
        if(!java.util.Set.of("","FISICO","DIGITAL").contains(tipo))throw new IllegalArgumentException("Tipo de produto inválido.");
        var page=produtos.catalogo(busca==null?"":busca.strip(),categoriaId,min,max,cor.strip(),tamanho.strip(),tipo,ordem,org.springframework.data.domain.PageRequest.of(Math.max(0,pagina),24));
        return new StoreCatalogResponse(categoriaService.listarTodas().stream().map(this::toCategory).toList(),page.getContent().stream().map(this::toCard).toList(),page.getTotalElements(),page.getNumber(),page.hasNext());
    }

    public StoreProductDetailResponse carregarProduto(Long id) {
        Produto produto = produtoService.buscarPorId(id);
        if (!produtoService.disponivelNaLoja(produto)) throw new com.paulo.ecommerce.exception.RecursoNaoEncontradoException("Produto", id);
        Produto raiz=produto.getProdutoPrincipalId()==null?produto:produtoService.buscarPorId(produto.getProdutoPrincipalId());
        var familia=produtoService.familia(produto).stream().filter(Produto::publicado).toList();
        var imagens=produto.imagens().isEmpty()?raiz.imagens():produto.imagens();
        List<StoreProductCardResponse> relacionados = produto.getCategoria() == null
                ? List.of()
                : produtoService.listarPorCategoria(produto.getCategoria().getId()).stream()
                .filter(rel -> rel.getProdutoPrincipalId()==null&&rel.publicado()&&!rel.getId().equals(raiz.getId()))
                .limit(4)
                .map(this::toCard)
                .toList();

        return new StoreProductDetailResponse(
                produto.getId(),
                raiz.getNome(),
                produto.getSku(),
                produto.getMarca(),
                produto.getDescricaoCurta(),
                produto.getDescricao()==null||produto.getDescricao().isBlank()?raiz.getDescricao():produto.getDescricao(),
                imagens.isEmpty()?null:imagens.get(0),
                produto.getPreco(),
                produto.getEstoque(),
                produto.isDestaque(),
                avaliacaoService.mediaPorProduto(produto.getId()),
                avaliacaoService.totalAvaliacoes(produto.getId()),
                toCategory(raiz.getCategoria()),
                relacionados,
                avaliacaoService.listarPorProduto(produto.getId()).stream()
                        .limit(5)
                        .map(avaliacao -> new StoreReviewResponse(
                                avaliacao.getUsuario().getNome(),
                                avaliacao.getNota(),
                                avaliacao.getComentario(),
                                avaliacao.getCriadoEm() == null ? "" : avaliacao.getCriadoEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        ))
                        .toList(),
                produto.getTipo().name(),raiz.getId(),produto.opcao(),
                familia.stream().map(v->new StoreProductDetailResponse.Variacao(v.getId(),v.getSku(),v.opcao().isBlank()?"Opção padrão":v.opcao(),v.getPreco(),v.getEstoque(),v.getImagemUrl())).toList(),imagens,
                produto.getPesoKg(),produto.getComprimentoCm(),produto.getLarguraCm(),produto.getAlturaCm(),produto.xpExtraAtual(),produto.getXpExtraFim()
        );
    }

    private StoreProductCardResponse toCard(Produto produto) {
        var familia=produtoService.familia(produto).stream().filter(Produto::publicado).toList();
        var preco=familia.stream().map(Produto::getPreco).min(BigDecimal::compareTo).orElse(produto.getPreco());
        int saldo=(int)Math.min(Integer.MAX_VALUE,familia.stream().mapToLong(Produto::getEstoque).sum());
        return new StoreProductCardResponse(
                produto.getId(),
                produto.getNome(),
                produto.getSku(),
                produto.getMarca(),
                produto.getDescricaoCurta(),
                produto.getImagemUrl(),
                preco,
                saldo,
                produto.isDestaque(),
                toCategory(produto.getCategoria()), familia.size()>1, familia.stream().mapToInt(Produto::xpExtraAtual).max().orElse(0)
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
