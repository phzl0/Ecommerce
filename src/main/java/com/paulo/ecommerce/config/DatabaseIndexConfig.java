package com.paulo.ecommerce.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseIndexConfig {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void criarIndices() {

        executarSeNaoExistir(
                "idx_produtos_ativo",
                "CREATE INDEX idx_produtos_ativo ON produtos(ativo)"
        );

        executarSeNaoExistir(
                "idx_produtos_categoria",
                "CREATE INDEX idx_produtos_categoria ON produtos(categoria_id)"
        );

        executarSeNaoExistir(
                "idx_produtos_nome",
                "CREATE INDEX idx_produtos_nome ON produtos(nome)"
        );

        executarSeNaoExistir(
                "idx_pedidos_usuario",
                "CREATE INDEX idx_pedidos_usuario ON pedidos(usuario_id)"
        );

        executarSeNaoExistir(
                "idx_pedidos_status",
                "CREATE INDEX idx_pedidos_status ON pedidos(status)"
        );

        executarSeNaoExistir(
                "idx_pedidos_data",
                "CREATE INDEX idx_pedidos_data ON pedidos(data_pedido)"
        );

        executarSeNaoExistir(
                "idx_itens_pedido",
                "CREATE INDEX idx_itens_pedido ON itens_pedido(pedido_id)"
        );

        executarSeNaoExistir(
                "idx_itens_produto",
                "CREATE INDEX idx_itens_produto ON itens_pedido(produto_id)"
        );

        executarSeNaoExistir(
                "idx_avaliacoes_produto",
                "CREATE INDEX idx_avaliacoes_produto ON avaliacoes(produto_id)"
        );

        executarSeNaoExistir(
                "idx_usuarios_email",
                "CREATE INDEX idx_usuarios_email ON usuarios(email)"
        );
    }

    private void executarSeNaoExistir(String nomeIndice, String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                    AND indexname = ?
                    """,
                    Integer.class,
                    nomeIndice
            );

            if (count != null && count == 0) {
                jdbcTemplate.execute(sql);
                System.out.println("Índice criado: " + nomeIndice);
            } else {
                System.out.println("Índice já existe: " + nomeIndice);
            }

        } catch (Exception e) {
            System.err.println("Erro ao criar índice " + nomeIndice + ": " + e.getMessage());
        }
    }
}