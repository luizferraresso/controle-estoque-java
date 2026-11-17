package br.edu.estoque;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.dao.ProdutoDAO;
import br.edu.estoque.model.*;
import br.edu.estoque.service.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/** Testes sem framework: usam o driver e o banco reais, incluindo concorrência e rollback. */
public final class TesteIntegracao {
    private static int verificacoes;
    public static void main(String[] args) throws Exception {
        BancoDados banco = new BancoDados("jdbc:h2:mem:teste_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        banco.inicializar(); banco.inicializar();
        EstoqueService s = new EstoqueService(banco);
        verificar(s.produtos().isEmpty(),"Banco novo vazio");
        rejeitar(() -> new Fornecedor(0," ","","",""), IllegalArgumentException.class);
        Fornecedor f = s.salvar(new Fornecedor(0,"Fornecedor inicial","Ana","34 99999-0000","ana@exemplo.com"));
        verificar(f.getId() > 0 && s.fornecedores().size() == 1,"Create e Read de fornecedor");
        f = s.salvar(new Fornecedor(f.getId(),"Fornecedor editado","Ana","34 3333-0000","ana@exemplo.com"));
        verificar(s.fornecedores().get(0).getNome().equals("Fornecedor editado"),"Update de fornecedor");
        final Fornecedor fornecedor = f;
        rejeitar(() -> produto("ERR","Produto",new BigDecimal("-1"),fornecedor),IllegalArgumentException.class);
        rejeitar(() -> produto("ERR","Produto",new BigDecimal("1.001"),fornecedor),IllegalArgumentException.class);
        rejeitar(() -> new Produto(0,"X","X","X",BigDecimal.ONE,0,0,null),IllegalArgumentException.class);
        Produto p = s.salvar(produto("abc-01","Caderno",new BigDecimal("12.50"),f));
        verificar(p.getCodigo().equals("ABC-01") && p.getQuantidade()==0,"Create de produto e normalização do código");
        verificar(p.isEstoqueBaixo(),"Alerta em saldo zero");
        rejeitar(() -> s.salvar(produto("abc-01","Duplicado",BigDecimal.ONE,fornecedor)),SQLException.class);
        rejeitar(() -> s.excluirFornecedor(fornecedor.getId()),SQLException.class);
        s.registrar(p.getId(),"ENTRADA",10,"Compra inicial");
        s.registrar(p.getId(),"SAIDA",3,"Venda");
        Produto atualizado = s.produtos().get(0);
        verificar(atualizado.getQuantidade()==7,"Entrada e saída atualizam o saldo");
        verificar(atualizado.getValorEmEstoque().compareTo(new BigDecimal("87.50"))==0,"Valor do inventário sem arredondamento binário");
        verificar(!atualizado.isEstoqueBaixo(),"Estoque regular acima do mínimo");
        var historico = s.movimentacoes(null,null,p.getId(),null);
        verificar(historico.size()==2 && historico.get(0) instanceof Saida && historico.get(1) instanceof Entrada,"Mapeamento polimórfico do histórico");
        verificar(historico.get(0).getSaldoApos()==7 && historico.get(1).getSaldoApos()==10,"Saldos históricos preservados");
        rejeitar(() -> s.registrar(p.getId(),"SAIDA",8,"Saldo insuficiente"),IllegalArgumentException.class);
        rejeitar(() -> s.registrar(p.getId(),"ENTRADA",0,"Zero"),IllegalArgumentException.class);
        rejeitar(() -> s.registrar(p.getId(),"ENTRADA",-1,"Negativa"),IllegalArgumentException.class);
        rejeitar(() -> s.registrar(p.getId(),"INVALIDO",1,""),IllegalArgumentException.class);
        rejeitar(() -> s.registrar(99999,"ENTRADA",1,""),IllegalArgumentException.class);
        rejeitar(() -> s.registrar(p.getId(),"ENTRADA",Integer.MAX_VALUE,"Overflow"),IllegalArgumentException.class);
        verificar(s.produtos().get(0).getQuantidade()==7 && s.movimentacoes(null,null,null,null).size()==2,"Operações recusadas não alteram saldo nem histórico");
        // Simula uma falha SQL somente no INSERT, depois do UPDATE de saldo.
        try (Connection c = banco.conectar(); Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE movimentacao ADD CONSTRAINT falha_teste CHECK (observacao <> 'FALHAR')");
        }
        rejeitar(() -> s.registrar(p.getId(),"ENTRADA",5,"FALHAR"),SQLException.class);
        verificar(s.produtos().get(0).getQuantidade()==7 && s.movimentacoes(null,null,null,null).size()==2,"Rollback desfaz UPDATE quando o INSERT falha");
        verificar(s.movimentacoes(LocalDate.now(),LocalDate.now(),p.getId(),"ENTRADA").size()==1,"Filtros de data, produto e tipo");
        verificar(s.movimentacoes(LocalDate.now().plusDays(1),null,null,null).isEmpty(),"Filtro fora do período");
        rejeitar(() -> s.movimentacoes(LocalDate.now(),LocalDate.now().minusDays(1),null,null),IllegalArgumentException.class);
        // Edição usa uma cópia antiga com saldo zero e não sobrescreve as movimentações.
        Produto editado = s.salvar(new Produto(p.getId(),p.getCodigo(),"Caderno editado","Papelaria",new BigDecimal("15.00"),7,0,f));
        verificar(editado.getQuantidade()==7 && editado.getNome().equals("Caderno editado"),"Update de produto preserva saldo atual");
        verificar(editado.isEstoqueBaixo(),"Alerta inclui saldo igual ao mínimo");
        rejeitar(() -> s.excluirProduto(p.getId()),IllegalArgumentException.class);
        s.registrar(p.getId(),"SAIDA",7,"Zerar");
        rejeitar(() -> s.excluirProduto(p.getId()),SQLException.class);
        verificar(s.movimentacoes(null,null,p.getId(),null).size()==3,"Exclusão bloqueada preserva histórico");
        Produto temporario = s.salvar(produto("TEMP","Temporário",BigDecimal.ZERO,f));
        s.excluirProduto(temporario.getId());
        verificar(s.produtos().size()==1,"Delete de produto sem histórico");
        Fornecedor descartavel = s.salvar(new Fornecedor(0,"Descartável","","",""));
        s.excluirFornecedor(descartavel.getId());
        verificar(s.fornecedores().size()==1,"Delete de fornecedor sem vínculo");
        verificar(new ProdutoDAO(banco).buscarPorId(99999).isEmpty(),"Busca por ID inexistente");
        concorrencia(s, f);
        exportacao();
        persistencia();
        System.out.println("SUCESSO: " + verificacoes + " verificações de integração (JDBC/H2, CRUD, transações, concorrência e CSV).");
    }

    private static Produto produto(String codigo, String nome, BigDecimal preco, Fornecedor f) {
        return new Produto(0,codigo,nome,"Papelaria",preco,2,0,f);
    }

    private static void concorrencia(EstoqueService s, Fornecedor f) throws Exception {
        Produto p = s.salvar(produto("CONCORRENTE","Concorrência",BigDecimal.ONE,f));
        s.registrar(p.getId(),"ENTRADA",10,"");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        Callable<Boolean> saida = () -> {
            inicio.await();
            try { s.registrar(p.getId(),"SAIDA",7,""); return true; }
            catch (IllegalArgumentException e) { return false; }
        };
        try {
            Future<Boolean> a = executor.submit(saida), b = executor.submit(saida);
            inicio.countDown();
            verificar(a.get(15,TimeUnit.SECONDS) != b.get(15,TimeUnit.SECONDS),"Apenas uma de duas saídas simultâneas é aceita");
            verificar(s.produtos().stream().filter(x -> x.getId()==p.getId()).findFirst().orElseThrow().getQuantidade()==3,"Bloqueio transacional impede venda acima do saldo");
            verificar(s.movimentacoes(null,null,p.getId(),null).size()==2,"Concorrência mantém histórico consistente");
        } finally { executor.shutdownNow(); }
    }

    private static void exportacao() throws Exception {
        Path arquivo = Files.createTempFile(Path.of("build"),"teste-csv-",".csv");
        try {
            CsvExporter.exportar(arquivo,List.of("Produto","Observação"),List.of(List.of("Ação; \"especial\"","Linha 1\nLinha 2"),List.of("=1+1"," @SOMA(A1)")));
            String csv = Files.readString(arquivo,StandardCharsets.UTF_8);
            verificar(csv.charAt(0)=='\uFEFF' && csv.contains("Ação; \"\"especial\"\""),"CSV com BOM, acentos, delimitador e aspas escapados");
            verificar(csv.contains("'=1+1") && csv.contains("' @SOMA(A1)"),"Campos CSV não executam fórmulas");
            verificar(csv.contains("Linha 1\nLinha 2"),"CSV preserva quebras de linha");
        } finally { Files.deleteIfExists(arquivo); }
    }

    private static void persistencia() throws Exception {
        Path pasta = Files.createTempDirectory(Path.of("build"),"banco-teste-").toAbsolutePath();
        String url = "jdbc:h2:file:" + pasta.resolve("estoque").toString().replace('\\','/');
        try {
            BancoDados primeiro = new BancoDados(url); primeiro.inicializar();
            EstoqueService s = new EstoqueService(primeiro);
            Fornecedor f = s.salvar(new Fornecedor(0,"Persistido","","",""));
            Produto p = s.salvar(produto("DISCO","Persistido",BigDecimal.TEN,f));
            s.registrar(p.getId(),"ENTRADA",4,"Persistência");
            BancoDados segundo = new BancoDados(url); segundo.inicializar();
            EstoqueService reaberto = new EstoqueService(segundo);
            verificar(reaberto.produtos().get(0).getQuantidade()==4 && reaberto.movimentacoes(null,null,null,null).size()==1,"Arquivo preserva dados ao fechar e reabrir conexões");
        } finally {
            try (var arquivos = Files.list(pasta)) { for (Path arquivo : arquivos.toList()) Files.deleteIfExists(arquivo); }
            Files.deleteIfExists(pasta);
        }
    }
    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) throw new AssertionError(descricao);
        verificacoes++; System.out.println("OK: " + descricao);
    }
    @FunctionalInterface private interface Acao { void executar() throws Exception; }
    private static void rejeitar(Acao acao, Class<? extends Exception> tipo) throws Exception {
        try { acao.executar(); } catch (Exception e) { if (tipo.isInstance(e)) { verificacoes++; return; } throw e; }
        throw new AssertionError("Esperava falha do tipo " + tipo.getSimpleName());
    }
}
