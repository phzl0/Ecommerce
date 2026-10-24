package com.paulo.ecommerce.config;

import com.paulo.ecommerce.model.Categoria;
import com.paulo.ecommerce.model.Produto;
import com.paulo.ecommerce.model.Usuario;
import com.paulo.ecommerce.repository.CategoriaRepository;
import com.paulo.ecommerce.repository.ProdutoRepository;
import com.paulo.ecommerce.repository.UsuarioRepository;
import com.paulo.ecommerce.service.PasseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initAdmin(UsuarioRepository usuarioRepository,
                                       CategoriaRepository categoriaRepository,
                                       ProdutoRepository produtoRepository,
                                       PasswordEncoder passwordEncoder,
                                       PasseService passeService) {
        return args -> {
            passeService.garantirPasseAtual();

            // Cria o administrador inicial somente quando credenciais forem definidas
            // explicitamente no ambiente. Nunca use senhas fixas no código-fonte.
            String adminEmail = System.getenv("ADMIN_EMAIL");
            String adminPassword = System.getenv("ADMIN_PASSWORD");
            if (adminEmail != null && !adminEmail.isBlank()
                    && adminPassword != null && adminPassword.length() >= 12
                    && !usuarioRepository.existsByEmail(adminEmail)) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador");
                admin.setEmail(adminEmail.trim());
                admin.setSenha(passwordEncoder.encode(adminPassword));
                admin.setRole(Usuario.Role.ADMIN);
                usuarioRepository.save(admin);
                System.out.println("Administrador inicial criado a partir das variáveis de ambiente.");
            }

            List<Categoria> categorias = List.of(
                    criarCategoria(categoriaRepository, "Tecnologia Pro", "Setup, produtividade e performance."),
                    criarCategoria(categoriaRepository, "Casa Inteligente", "Automacao, conforto e design funcional."),
                    criarCategoria(categoriaRepository, "Audio Premium", "Som limpo para trabalho, treino e lifestyle."),
                    criarCategoria(categoriaRepository, "Mobilidade Urbana", "Itens para rotinas rapidas e conectadas."),
                    criarCategoria(categoriaRepository, "Bem-Estar", "Produtos para foco, recuperacao e energia.")
            );

            if (produtoRepository.count() < 10) {
                criarProduto(produtoRepository, categorias.get(0), "Notebook Level X14", "LEVEL-X14", "Level Tech",
                        "Notebook leve para rotina intensa.", "Tela 14 polegadas, SSD NVMe e design premium para operacoes de alta produtividade.",
                        "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("6499.90"), 18, true);
                criarProduto(produtoRepository, categorias.get(0), "Monitor Curve Vision 34", "VISION-34", "Vision",
                        "Monitor ultrawide para setups elegantes.", "Tela curva 34 polegadas, taxa elevada e contraste forte para criativos e gamers.",
                        "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("2899.90"), 12, true);
                criarProduto(produtoRepository, categorias.get(1), "Hub Casa Sense", "SENSE-HUB", "Sense Home",
                        "Central de automacao compacta.", "Controle luzes, rotinas e sensores em um unico painel de comando.",
                        "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("899.90"), 24, false);
                criarProduto(produtoRepository, categorias.get(1), "Lampada Aura Smart", "AURA-SMART", "Aura",
                        "Iluminacao inteligente com clima premium.", "Ajuste cenas, temperatura de cor e horarios para toda a casa.",
                        "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("199.90"), 40, false);
                criarProduto(produtoRepository, categorias.get(2), "Headphone Pulse ANC", "PULSE-ANC", "Pulse",
                        "Cancelamento de ruido para foco total.", "Audio premium com bateria duradoura para estudo, trabalho e viagem.",
                        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("1299.90"), 20, true);
                criarProduto(produtoRepository, categorias.get(2), "Speaker Flow Mini", "FLOW-MINI", "Flow",
                        "Caixa compacta com assinatura sonora forte.", "Bluetooth rapido, visual minimalista e grave equilibrado para ambientes modernos.",
                        "https://images.unsplash.com/photo-1545454675-3531b543be5d?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("459.90"), 28, false);
                criarProduto(produtoRepository, categorias.get(3), "Patinete Volt Street", "VOLT-STREET", "Volt",
                        "Mobilidade urbana com acabamento premium.", "Patinete eletrico para deslocamentos curtos com boa autonomia e estilo.",
                        "https://images.unsplash.com/photo-1571068316344-75bc76f77890?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("3899.90"), 6, true);
                criarProduto(produtoRepository, categorias.get(3), "Mochila Commute Pack", "COMMUTE-PACK", "Commute",
                        "Mochila urbana para laptop e acessorios.", "Compartimentos inteligentes, tecido resistente e desenho executivo.",
                        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("349.90"), 34, false);
                criarProduto(produtoRepository, categorias.get(4), "Smartwatch Core Fit", "CORE-FIT", "Core",
                        "Relogio para treino, rotina e notificacoes.", "Monitoramento de saude, tela vibrante e design clean para uso diario.",
                        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("999.90"), 22, true);
                criarProduto(produtoRepository, categorias.get(4), "Garrafa Thermal Elite", "THERMAL-ELITE", "Elite",
                        "Hidratacao premium para alta performance.", "Garrafa termica em acabamento fosco, pensada para treino e trabalho.",
                        "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=900&q=80",
                        new BigDecimal("159.90"), 50, false);
            }
        };
    }

    private Categoria criarCategoria(CategoriaRepository repository, String nome, String descricao) {
        return repository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> {
                    Categoria categoria = new Categoria();
                    categoria.setNome(nome);
                    categoria.setDescricao(descricao);
                    return repository.save(categoria);
                });
    }

    private void criarProduto(ProdutoRepository repository,
                              Categoria categoria,
                              String nome,
                              String sku,
                              String marca,
                              String descricaoCurta,
                              String descricao,
                              String imagemUrl,
                              BigDecimal preco,
                              int estoque,
                              boolean destaque) {
        if (repository.existsByNomeIgnoreCase(nome)) {
            return;
        }
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setSku(sku);
        produto.setMarca(marca);
        produto.setDescricaoCurta(descricaoCurta);
        produto.setDescricao(descricao);
        produto.setImagemUrl(imagemUrl);
        produto.setPreco(preco);
        produto.setEstoque(estoque);
        produto.setCategoria(categoria);
        produto.setDestaque(destaque);
        repository.save(produto);
    }
}
