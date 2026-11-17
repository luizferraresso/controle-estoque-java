package br.edu.estoque.service;

import br.edu.estoque.dao.*;
import br.edu.estoque.db.BancoDados;
import br.edu.estoque.model.*;
import java.sql.*;
import java.time.*;
import java.util.List;

public final class EstoqueService {
    private final BancoDados banco;
    private final CrudDAO<Fornecedor> fornecedores;
    private final ProdutoDAO produtos;
    private final MovimentacaoDAO movimentacoes;

    public EstoqueService(BancoDados banco) {
        this.banco = banco;
        fornecedores = new FornecedorDAO(banco);
        produtos = new ProdutoDAO(banco);
        movimentacoes = new MovimentacaoDAO(banco);
    }
    public List<Fornecedor> fornecedores() throws SQLException { return fornecedores.listar(); }
    public List<Produto> produtos() throws SQLException { return produtos.listar(); }
    public Fornecedor salvar(Fornecedor f) throws SQLException { return fornecedores.salvar(f); }
    public Produto salvar(Produto p) throws SQLException { return produtos.salvar(p); }
    public void excluirFornecedor(long id) throws SQLException { fornecedores.excluir(id); }
    public void excluirProduto(long id) throws SQLException { produtos.excluir(id); }

    public List<Movimentacao> movimentacoes(LocalDate inicio, LocalDate fim, Long produto, String tipo) throws SQLException {
        if (inicio != null && fim != null && inicio.isAfter(fim)) throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à final.");
        if (tipo != null && !tipo.equals("ENTRADA") && !tipo.equals("SAIDA")) throw new IllegalArgumentException("Tipo de movimentação inválido.");
        return movimentacoes.listar(inicio, fim, produto, tipo);
    }

    public void registrar(long produtoId, String tipo, int quantidade, String observacao) throws SQLException {
        try (Connection con = banco.conectar()) {
            con.setAutoCommit(false);
            try {
                Produto produto = produtos.buscarPorId(con, produtoId, true)
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado. Atualize a lista."));
                LocalDateTime agora = LocalDateTime.now();
                Movimentacao movimento = switch (tipo) {
                    case "ENTRADA" -> new Entrada(0,produto,quantidade,observacao,agora,0);
                    case "SAIDA" -> new Saida(0,produto,quantidade,observacao,agora,0);
                    default -> throw new IllegalArgumentException("Tipo de movimentação inválido.");
                };
                int novoSaldo = movimento.calcularSaldo(produto.getQuantidade());
                try (PreparedStatement ps = con.prepareStatement("UPDATE produto SET quantidade=? WHERE id=?")) {
                    ps.setInt(1, novoSaldo); ps.setLong(2, produtoId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Produto não encontrado durante a movimentação.");
                }
                movimentacoes.inserir(con, movimento, novoSaldo);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
