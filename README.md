<div align="center">

# Ecommerce

**Aplicação de comércio eletrônico em Java e Spring Boot**

Projeto de desenvolvimento de software com foco em aplicações web, persistência de dados, integrações e organização de código.

[Visão geral](#visão-geral) · [Tecnologias](#tecnologias) · [Execução local](#execução-local) · [Evolução](#evolução-do-projeto)

</div>

---

## Visão geral

Este repositório acompanha o desenvolvimento de uma aplicação de e-commerce baseada em **Java e Spring Boot**. O objetivo é experimentar e consolidar práticas de desenvolvimento de sistemas comerciais, incluindo acesso a dados, segurança e integração com serviços externos.

**Estado:** em desenvolvimento. O uso de uma dependência não significa que a funcionalidade correspondente esteja concluída ou testada. A documentação abaixo distingue configurações existentes de funcionalidades a validar.

## Tecnologias

| Área | Tecnologias identificadas no repositório |
| --- | --- |
| Linguagem e framework | Java 17 no Maven, Spring Boot 3.5.13 |
| Aplicação web | Spring Web, Thymeleaf |
| Dados e persistência | Spring Data JPA, PostgreSQL, HikariCP |
| Segurança e validação | Spring Security, Bean Validation |
| Cache | Redis, Spring Cache (dependências declaradas) |
| Integrações | Mercado Pago SDK, Spring Mail (dependências declaradas) |
| Build e execução | Maven, Docker |
| Testes | Spring Boot Test, Spring Security Test |

As tecnologias foram verificadas no `pom.xml`, no `Dockerfile` e nos arquivos de configuração; não representam uma lista de recursos finalizados.

## Configuração

A classe principal da aplicação é `com.paulo.ecommerce.EcommerceApplication`.

O arquivo `src/main/resources/application.properties` usa parâmetros externos para conexão ao PostgreSQL e configuração do servidor:

| Variável | Descrição |
| --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL |
| `DB_USER` | Usuário de acesso ao banco |
| `DB_PASSWORD` | Senha do banco |
| `SPRING_PROFILES_ACTIVE` | Perfil de execução; padrão `local` |
| `PORT` | Porta da aplicação; padrão `8080` |
| `COOKIE_SECURE` | Configuração da flag Secure dos cookies |

A propriedade `spring.jpa.hibernate.ddl-auto=validate` exige que o esquema das tabelas já esteja compatível com as entidades mapeadas. As opções de Redis estão comentadas no arquivo de propriedades analisado.

## Execução local

### Requisitos

- Java 17, conforme configuração do Maven.
- Maven.
- PostgreSQL configurado com o esquema esperado pela aplicação.

Defina as variáveis de ambiente no terminal Linux:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/ecommerce'
export DB_USER='seu_usuario'
export DB_PASSWORD='sua_senha'
```

Em seguida, execute:

```bash
mvn spring-boot:run
```

A aplicação utiliza por padrão a porta `8080`, caso a inicialização seja concluída. A execução requer credenciais válidas e uma estrutura de banco compatível.

### Docker

O repositório disponibiliza um `Dockerfile` para compilar e executar um JAR Spring Boot:

```bash
docker build -t ecommerce-local .
```

O `Dockerfile` usa Java 21, enquanto o `pom.xml` configura Java 17. Recomenda-se alinhar as versões. A execução do contêiner também exige a configuração da conexão com o PostgreSQL; o `Dockerfile` isoladamente não provisiona o banco.

## Evolução do projeto

Prioridades recomendadas para tornar o sistema e a documentação mais consistentes:

1. Documentar entidades, rotas e funcionalidades já implementadas, diferenciando-as das planejadas.
2. Definir uma estratégia reproduzível para o esquema do banco, com migrations.
3. Padronizar o Java entre Maven, Docker e ambientes de desenvolvimento.
4. Criar uma configuração Docker Compose incluindo o PostgreSQL.
5. Cobrir regras de negócio e fluxos críticos com testes automatizados.
6. Documentar e validar separadamente os fluxos de autenticação, cache, pagamentos e notificações à medida que forem concluídos.

## Autor

Desenvolvimento e manutenção: [phzl0](https://github.com/phzl0).

> Este README será atualizado junto à evolução e validação das funcionalidades do projeto.
