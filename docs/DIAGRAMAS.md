# Diagramas da aplicação

Os blocos Mermaid podem ser visualizados em editores que suportem esse formato. A descrição textual também registra as relações.

## Classes de domínio e persistência

```mermaid
classDiagram
    class Entidade {
        <<abstract>>
        -long id
        +getId() long
    }
    class Fornecedor {
        -String nome
        -String contato
        -String telefone
        -String email
    }
    class Produto {
        -String codigo
        -String nome
        -String categoria
        -BigDecimal preco
        -int estoqueMinimo
        -int quantidade
        +isEstoqueBaixo() boolean
        +getValorEmEstoque() BigDecimal
    }
    class Movimentacao {
        <<abstract>>
        -int quantidade
        -LocalDateTime data
        -String observacao
        -int saldoApos
        +getTipo() String
        +calcularSaldo(int saldoAtual) int
    }
    class Entrada {
        +calcularSaldo(int saldoAtual) int
    }
    class Saida {
        +calcularSaldo(int saldoAtual) int
    }
    class CrudDAO~T~ {
        <<interface>>
        +salvar(T entidade) T
        +listar() List~T~
        +buscarPorId(long id) Optional~T~
        +excluir(long id) void
    }
    class FornecedorDAO
    class ProdutoDAO
    class MovimentacaoDAO
    class EstoqueService
    class BancoDados
    class EstoqueFrame
    Entidade <|-- Fornecedor
    Entidade <|-- Produto
    Entidade <|-- Movimentacao
    Movimentacao <|-- Entrada
    Movimentacao <|-- Saida
    Produto "*" --> "1" Fornecedor
    Movimentacao "*" --> "1" Produto
    CrudDAO <|.. FornecedorDAO
    CrudDAO <|.. ProdutoDAO
    EstoqueFrame --> EstoqueService
    EstoqueService --> FornecedorDAO
    EstoqueService --> ProdutoDAO
    EstoqueService --> MovimentacaoDAO
    FornecedorDAO --> BancoDados
    ProdutoDAO --> BancoDados
    MovimentacaoDAO --> BancoDados
```

`Entidade` é a base das entidades persistidas. `Movimentacao` acrescenta um contrato abstrato implementado por `Entrada` e `Saida`. Cada produto referencia um fornecedor e cada movimentação referencia um produto. A interface gráfica depende do serviço, que coordena a persistência por DAOs.

## Modelo relacional

```mermaid
erDiagram
    FORNECEDOR ||--o{ PRODUTO : fornece
    PRODUTO ||--o{ MOVIMENTACAO : possui
    FORNECEDOR {
        bigint id PK
        varchar nome
        varchar contato
        varchar telefone
        varchar email
    }
    PRODUTO {
        bigint id PK
        varchar codigo UK
        varchar nome
        varchar categoria
        decimal preco
        int estoque_minimo
        int quantidade
        bigint fornecedor_id FK
    }
    MOVIMENTACAO {
        bigint id PK
        bigint produto_id FK
        varchar tipo
        int quantidade
        timestamp data
        varchar observacao
        int saldo_apos
    }
```

## Transação de movimentação

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as EstoqueFrame
    participant Servico as EstoqueService
    participant Modelo as Entrada ou Saida
    participant DB as Banco H2 via JDBC
    Usuario->>UI: Produto, tipo e quantidade
    UI->>Servico: registrar(...), em SwingWorker
    Servico->>DB: setAutoCommit(false)
    Servico->>DB: SELECT produto ... FOR UPDATE
    DB-->>Servico: Produto com saldo atual
    Servico->>Modelo: calcularSaldo(saldoAtual)
    alt Valores válidos e saldo suficiente
        Modelo-->>Servico: Novo saldo
        Servico->>DB: UPDATE produto
        Servico->>DB: INSERT movimentacao
        Servico->>DB: commit()
        Servico-->>UI: Operação concluída
        UI-->>Usuario: Atualiza tabelas e indicadores
    else Validação ou falha SQL
        Servico->>DB: rollback()
        Servico-->>UI: Exceção
        UI-->>Usuario: Mensagem explicativa
    end
```
