# Roteiro de apresentação — Controle de Estoque

## 1. Objetivo (30 segundos)

“A aplicação controla o estoque de uma loja. Ela cadastra produtos e fornecedores, registra entradas e saídas, mostra produtos que precisam de reposição e gera relatórios. Foi desenvolvida em Java, com Swing e persistência JDBC no H2.”

## 2. Demonstrar o funcionamento (3 a 4 minutos)

Use `iniciar.bat`. Comece com o banco da sua apresentação, sem apagar arquivos com dados que queira guardar.

1. **Fornecedor:** cadastre `Distribuidora Central`, contato `Mariana`, telefone `34 3333-4000`, e-mail `contato@exemplo.com`.
2. **Produto:** cadastre código `CAD-001`, nome `Caderno universitário`, categoria `Papelaria`, preço `24,90`, estoque mínimo `8` e selecione o fornecedor.
3. Mostre que o saldo começa em **0**, aparecendo no alerta de estoque baixo.
4. **Entrada:** registre **10** unidades, observação `Compra inicial`. O alerta desaparece, pois 10 > 8.
5. **Saída inválida:** tente retirar **11** unidades. Mostre a mensagem de estoque insuficiente. O saldo continua 10.
6. Corrija a quantidade para **3** e salve, com observação `Venda no balcão`.
7. Na **Visão geral**, o saldo será **7**, o produto aparecerá no alerta, e o valor será **R$ 174,30** (7 × 24,90).
8. Em **Produtos**, edite o nome. Mostre que o saldo permanece 7. Demonstre a busca.
9. Em **Relatórios**, gere o inventário e exporte CSV. Depois gere movimentações no período de hoje: entrada de 10 e saída de 3.
10. Feche e reabra: os dados continuam no arquivo H2.

Para demonstrar exclusão sem afetar o histórico, crie e exclua um produto temporário sem movimentações. Depois crie e exclua um fornecedor temporário que não esteja vinculado a produtos.

## 3. Explicar os conceitos de POO (2 a 3 minutos)

### Classes, objetos e encapsulamento

Abra `model/Produto.java`. A classe é a definição de um produto; o caderno cadastrado é um objeto dessa classe. Nome, preço e saldo são atributos privados, acessados por métodos. O construtor valida os valores. Os objetos são imutáveis: a edição cria uma nova representação, e a persistência atualiza o registro pelo identificador.

### Herança e abstração

Abra `model/Entidade.java`: é uma classe abstrata com o identificador comum. `Produto`, `Fornecedor` e `Movimentacao` reaproveitam essa identidade.

Abra `model/Movimentacao.java`: os atributos comuns de uma entrada ou saída ficam na classe abstrata. Ela exige que os subtipos forneçam `getTipo()` e `calcularSaldo()`.

### Polimorfismo

Abra `model/Entrada.java`, `model/Saida.java` e `service/EstoqueService.java`.

```java
Movimentacao movimento = /* objeto Entrada ou Saida */;
int novoSaldo = movimento.calcularSaldo(produto.getQuantidade());
```

É a mesma chamada sobre o tipo abstrato. O objeto `Entrada` soma as unidades; o objeto `Saida` verifica a disponibilidade e subtrai. A escolha da implementação acontece conforme o objeto concreto. O cálculo não fica duplicado na interface gráfica.

### Interface e contrato

Abra `dao/CrudDAO.java`. A interface genérica define `salvar`, `listar`, `buscarPorId` e `excluir`. `FornecedorDAO` e `ProdutoDAO` implementam esse contrato para tipos diferentes de entidade. Em `EstoqueService`, o campo de fornecedores é declarado como `CrudDAO<Fornecedor>`.

### Composição e separação de responsabilidades

Um `Produto` possui um `Fornecedor`. A janela usa um `EstoqueService`, que coordena DAOs. As telas apresentam os dados; os DAOs concentram o SQL; os modelos e o serviço contêm as validações e regras do estoque.

## 4. Explicar JDBC e Swing (1 a 2 minutos)

- Em `db/BancoDados.java`, mostre `DriverManager.getConnection()` e as três tabelas.
- Em `dao/FornecedorDAO.java`, mostre INSERT, SELECT, UPDATE e DELETE usando `PreparedStatement`.
- Explique que os parâmetros são vinculados por `setString`, `setLong` etc.; o conteúdo digitado não é concatenado ao SQL.
- Mostre o `try-with-resources`: conexão, instrução e resultado são fechados ao terminar a operação.
- Em `EstoqueService.registrar()`, mostre a transação. A atualização do saldo e o lançamento histórico precisam ser confirmados juntos.
- Em `ui/EstoqueFrame.java`, mostre `JTabbedPane`, `JTable`, o formulário `JDialog` e `SwingWorker`. A interface é criada na thread de eventos e o acesso ao banco/arquivo roda em segundo plano.

## Perguntas que podem aparecer

**Por que usar H2?** É um banco relacional acessado por JDBC que pode acompanhar a aplicação em um JAR, sem servidor separado. Nesta aplicação ele grava em arquivo para manter os dados entre execuções.

**Onde está o CRUD?** Nos DAOs de produto e fornecedor. `salvar` faz INSERT para um objeto novo (`id == 0`) e UPDATE para um cadastro existente. `listar` e `buscarPorId` fazem SELECT; `excluir` faz DELETE.

**Por que o histórico não tem edição ou exclusão?** Para preservar os lançamentos e seus saldos. Correções são registradas por movimentos inversos com justificativa. O CRUD completo é demonstrado nos produtos e fornecedores.

**Por que o saldo não pode ser editado diretamente?** Uma alteração sem lançamento quebraria a relação entre saldo e histórico. O saldo é atualizado somente pela transação de movimentação.

**Por que `BigDecimal`?** Valores monetários precisam de representação decimal exata. Tipos binários como `double` podem introduzir diferenças de arredondamento.

**Como evita estoque negativo?** `Saida.calcularSaldo()` valida a quantidade contra o saldo lido com bloqueio na transação. O banco também tem restrição para impedir quantidade negativa.

**E se a gravação do histórico falhar?** O serviço executa `rollback`, desfazendo a atualização do saldo. Há um teste que força uma falha SQL no INSERT para verificar isso.

**O relatório calcula lucro?** Não. O inventário mostra o saldo valorizado pelo preço atual; as movimentações mostram quantidades, datas, observações e saldos após cada operação.

**Qual é o escopo?** Aplicação local, com uma instância usando o arquivo de banco. As quantidades são unidades inteiras e cada produto tem um fornecedor. Não inclui autenticação, emissão fiscal, vendas financeiras ou custo médio.

## Antes de apresentar

Execute os testes, revise as classes acima, pratique a demonstração e preencha nome e turma onde seu professor solicitar. Leve a pasta completa ou o ZIP, incluindo `lib/`, e confirme que o computador de apresentação tem JDK 17 ou superior. A entrega do código deve ser acompanhada da apresentação exigida no enunciado.
