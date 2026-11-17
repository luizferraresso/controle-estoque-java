# Aplicação de Controle de Estoque

Trabalho final de Programação Orientada a Objetos em **Java**, com **Swing**, **JDBC** e banco relacional **H2 persistente em arquivo**.

## Executar no Windows

1. Mantenha todos os arquivos desta pasta juntos, especialmente `lib/`.
2. Dê dois cliques em **`iniciar.bat`**. Ele abre o executável pronto; se a aplicação já estiver aberta por esse iniciador, restaura sua janela.
3. Cadastre um **fornecedor**, depois um **produto** e registre uma **entrada** de estoque.

Para abrir, é necessário **Java 17 ou superior** no PATH. Para compilar ou testar, use um **JDK 17 ou superior**, com `java`, `javac` e `jar` no PATH. O driver H2 está incluído; esses scripts não precisam de Maven, internet ou instalação de um servidor de banco.

O executável compilado também está disponível:

```powershell
java -jar ControleEstoque.jar
```

Execute esse comando na pasta do projeto. O JAR depende de `lib/h2-2.5.252.jar`. Depois de alterar o código, feche a aplicação e execute `compilar.bat` para atualizar o executável; depois abra com `iniciar.bat`. Se o JAR estiver ausente, o iniciador tenta compilá-lo automaticamente. Os testes não substituem o JAR e podem ser executados com a aplicação aberta.

## Funcionalidades

| Tela | Recursos |
|---|---|
| Visão geral | Total de produtos, unidades disponíveis, valor do inventário e lista de produtos com estoque baixo. |
| Produtos | Incluir, listar, buscar, editar e excluir; código único, nome, categoria, preço, estoque mínimo e fornecedor. |
| Fornecedores | Incluir, listar, buscar, editar e excluir; nome, contato, telefone e e-mail. |
| Movimentações | Registrar entradas e saídas, com data/hora automática, quantidade, observação e saldo após o lançamento. |
| Relatórios | Inventário atual e movimentações por período, produto e tipo; exportação CSV compatível com planilhas. |

Use **F5** ou **Arquivo → Atualizar dados** para recarregar as informações. Um duplo clique em um produto ou fornecedor abre sua edição. As tabelas de cadastro e histórico permitem ordenar clicando no cabeçalho.

## Regras de estoque

- O produto começa com saldo zero. Cadastre o saldo inicial por uma entrada, que ficará registrada no histórico.
- Quantidades são inteiras e positivas nas movimentações. Saldo e estoque mínimo não podem ser negativos.
- Não é possível retirar mais unidades do que o saldo disponível.
- O alerta aparece quando **saldo ≤ estoque mínimo**, inclusive em saldo zero.
- O preço aceita `24,90` ou `24.90`, sem separador de milhares. Valores monetários usam `BigDecimal` e `DECIMAL`, com duas casas decimais.
- Uma edição do cadastro do produto não modifica seu saldo.
- Fornecedores vinculados a produtos e produtos com movimentações não podem ser excluídos. Um produto sem histórico e com saldo zero pode ser excluído.
- O histórico é preservado. Para corrigir um lançamento, registre uma movimentação inversa com a justificativa na observação. Uma saída de correção também precisa respeitar o saldo disponível.
- O valor do inventário é **saldo × preço unitário atual**. Não representa custo médio, lucro ou faturamento histórico.

## Relatórios

1. Na aba **Relatórios**, escolha **Inventário** ou **Movimentações**.
2. Selecione o produto, ou mantenha todos. Para inventário, há a opção de mostrar somente estoque baixo.
3. Para movimentações, informe as datas em `dd/mm/aaaa` e, se desejar, o tipo. As datas inicial e final são inclusivas; campos vazios deixam o período sem aquele limite. Por padrão, o período vai do início do mês até hoje.
4. Clique em **Gerar relatório** e, depois, em **Exportar CSV**.

O CSV usa UTF-8 com BOM, ponto e vírgula e vírgula decimal. Abra no Excel ou LibreOffice; se o programa pedir um delimitador, escolha ponto e vírgula. A exportação corresponde ao relatório gerado. Se os filtros ou dados mudarem, gere novamente antes de exportar.

## Banco de dados

O programa cria automaticamente as tabelas e o arquivo `dados/estoque.mv.db`. Ao fechar e reabrir a aplicação, os dados continuam disponíveis. A pasta é relativa ao diretório de execução; `iniciar.bat` fixa esse diretório na pasta do projeto.

Feche o programa antes de copiar a pasta `dados/` para fazer backup. Execute uma instância por vez para o mesmo arquivo de banco. Para usar outro diretório:

```powershell
java "-Destoque.dados=C:\MinhaLoja\dados" -jar ControleEstoque.jar
```

O esquema documentado está em [docs/banco.sql](docs/banco.sql). Não é necessário executar esse SQL manualmente: `BancoDados.inicializar()` cria as tabelas.

## Organização e requisitos de POO

```text
src/main/java/br/edu/estoque/
  App.java              Inicialização da aplicação
  model/                Entidades, validações e comportamento das movimentações
  dao/                  Contrato CRUD e implementação JDBC
  db/                   Conexão e criação das tabelas
  service/              Transações, regras de estoque e exportação CSV
  ui/                   Janelas, formulários e tabelas Swing
src/test/java/          Testes de integração e dos fluxos Swing
lib/                    Driver JDBC H2 e informações de licença
docs/                   Esquema, diagramas e roteiro de apresentação
```

| Requisito do enunciado | Implementação |
|---|---|
| Classes e objetos | `Produto`, `Fornecedor`, `Entrada`, `Saida` e objetos usados nos cadastros e lançamentos. |
| Encapsulamento | Atributos `private final`, acesso por métodos e validação nos construtores; uma edição cria um objeto com os novos valores. |
| Herança | `Produto`, `Fornecedor` e `Movimentacao` herdam de `Entidade`; `Entrada` e `Saida` herdam de `Movimentacao`. |
| Classes abstratas | `Entidade` centraliza identidade; `Movimentacao` define o contrato de cálculo de saldo. |
| Polimorfismo | `EstoqueService.registrar()` chama `calcularSaldo()` em uma referência `Movimentacao`. A implementação executada depende de ser `Entrada` ou `Saida`. |
| Interfaces | `CrudDAO<T extends Entidade>` define as operações de persistência; `ProdutoDAO` e `FornecedorDAO` implementam o contrato. |
| JDBC e CRUD | `Connection`, `PreparedStatement`, `ResultSet`, operações SQL reais e fechamento com `try-with-resources`. CRUD completo de produtos e fornecedores. |
| Interface gráfica Swing | `JFrame`, `JDialog`, `JTabbedPane`, `JTable`, filtros, formulários e `JFileChooser`. Operações de banco/arquivo em `SwingWorker`. |
| Controle de estoque | Entradas, saídas, saldo, fornecedores, alertas e relatórios de inventário e movimentação. |

Uma movimentação usa uma transação JDBC: bloqueia a linha para consulta do saldo, calcula a alteração, atualiza o produto, grava o histórico e confirma com `commit`. Se houver falha, executa `rollback`. Isso evita salvar uma alteração de saldo sem seu histórico e impede que saídas concorrentes usem o mesmo saldo.

O detalhamento e os exemplos para explicar ao professor estão em [docs/APRESENTACAO.md](docs/APRESENTACAO.md); os diagramas estão em [docs/DIAGRAMAS.md](docs/DIAGRAMAS.md).

## Testar

Execute `testar.bat` para as verificações de integração. Para incluir os fluxos reais da interface:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\executar.ps1 -Acao testar -Interface
```

Os testes usam bancos próprios, em memória e em uma pasta temporária dentro de `build/`. Eles não alteram `dados/`. O teste Swing abre janelas temporárias e as fecha ao terminar. Exige uma sessão gráfica.

Validação realizada: **43 verificações de integração** e **13 verificações Swing**, incluindo CRUD, validações, saldo, rollback após falha SQL, duas saídas simultâneas, filtros, CSV e reabertura do banco em arquivo. A captura da interface está em [docs/interface.png](docs/interface.png).

## IDE e Maven (opcionais)

No VS Code, abra a pasta **Trabalho_N2** inteira, que contém `src`, `lib` e `iniciar.bat`. A configuração em `.vscode/settings.json` define as duas pastas de código-fonte e o driver H2 local, dispensando importação Maven. Para executar pelo depurador, pressione **F5** e escolha **Controle de Estoque**. Feche a aplicação anterior antes de abrir outra instância.

Se o editor estava aberto antes da criação do projeto e continuar marcando erros de pacotes, pressione **Ctrl+Shift+P**, execute **Java: Clean Java Language Server Workspace** e confirme a reinicialização quando o VS Code solicitar. Isso recria o índice Java com as pastas corretas; os dados do estoque não são apagados.

Pelo terminal PowerShell do VS Code, estando na pasta do projeto, execute:

```powershell
.\iniciar.bat
```

Pelo Explorador de Arquivos do Windows, basta dar dois cliques em `iniciar.bat`. O arquivo precisa ser executado; abri-lo numa aba do editor apenas mostra seu conteúdo.

No IntelliJ ou Eclipse, a pasta também pode ser aberta como projeto Maven e a classe `br.edu.estoque.App` executada. A dependência está no `pom.xml`. Para usar a importação Maven no VS Code, altere `java.import.maven.enabled` para `true` nas configurações desta pasta. Quem já tiver Maven pode usar:

```powershell
mvn compile exec:java
```

Os testes deste projeto são executáveis Java sem framework; use o script acima para rodá-los. O ciclo `mvn test` não substitui essas verificações.

## Referências e dependência

- [Documentação oficial do H2](https://h2database.github.io/html/cheatSheet.html).
- [Código-fonte do H2 2.5.252](https://github.com/h2database/h2database/tree/version-2.5.252).
- A distribuição do driver é original e não foi modificada. Consulte [lib/3rdparty_license.txt](lib/3rdparty_license.txt).

O pacote local `ControleEstoque-entrega.zip`, quando gerado, reúne o código, executável, driver, scripts e documentação para levar a outro computador com Java instalado. O ZIP não é versionado no Git; para obter o projeto pelo GitHub, use **Code → Download ZIP** ou clone o repositório. O banco em `dados/` e os arquivos temporários de `build/` também ficam fora do repositório.
