package br.edu.estoque.dao;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.model.Produto;
import java.sql.*;
import java.util.*;

public final class ProdutoDAO implements CrudDAO<Produto> {
    private final BancoDados banco;
    public ProdutoDAO(BancoDados banco) { this.banco = banco; }
    static final String SELECT = """
        SELECT p.id,p.codigo,p.nome,p.categoria,p.preco,p.estoque_minimo,p.quantidade,
        f.id AS f_id,f.nome AS f_nome,f.contato,f.telefone,f.email
        FROM produto p JOIN fornecedor f ON f.id=p.fornecedor_id
        """;

    static Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(rs.getLong("id"), rs.getString("codigo"), rs.getString("nome"), rs.getString("categoria"),
                rs.getBigDecimal("preco"), rs.getInt("estoque_minimo"), rs.getInt("quantidade"), FornecedorDAO.mapear(rs));
    }

    @Override public Produto salvar(Produto p) throws SQLException {
        boolean novo = p.getId() == 0;
        if (novo && p.getQuantidade() != 0) throw new IllegalArgumentException("Cadastre o saldo inicial pela tela de movimentações.");
        String sql = novo ? "INSERT INTO produto(codigo,nome,categoria,preco,estoque_minimo,fornecedor_id) VALUES(?,?,?,?,?,?)"
                : "UPDATE produto SET codigo=?,nome=?,categoria=?,preco=?,estoque_minimo=?,fornecedor_id=? WHERE id=?";
        // Quantidade nunca é alterada por uma edição de cadastro, evitando sobrescrever entradas e saídas.
        long id = p.getId();
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCodigo()); ps.setString(2, p.getNome()); ps.setString(3, p.getCategoria());
            ps.setBigDecimal(4, p.getPreco()); ps.setInt(5, p.getEstoqueMinimo()); ps.setLong(6, p.getFornecedor().getId());
            if (!novo) ps.setLong(7, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Produto não encontrado. Atualize a lista.");
            if (novo) try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
        }
        return buscarPorId(id).orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));
    }

    @Override public List<Produto> listar() throws SQLException {
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY p.nome,p.id"); ResultSet rs = ps.executeQuery()) {
            List<Produto> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        }
    }

    @Override public Optional<Produto> buscarPorId(long id) throws SQLException {
        try (Connection con = banco.conectar()) { return buscarPorId(con, id, false); }
    }

    public Optional<Produto> buscarPorId(Connection con, long id, boolean bloquear) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT + " WHERE p.id=?" + (bloquear ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    @Override public void excluir(long id) throws SQLException {
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement("DELETE FROM produto WHERE id=? AND quantidade=0")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Só é possível excluir um produto com saldo zero e sem movimentações.");
        }
    }
}
