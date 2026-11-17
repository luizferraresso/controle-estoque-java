package br.edu.estoque.dao;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.model.Fornecedor;
import java.sql.*;
import java.util.*;

public final class FornecedorDAO implements CrudDAO<Fornecedor> {
    private final BancoDados banco;
    public FornecedorDAO(BancoDados banco) { this.banco = banco; }

    @Override public Fornecedor salvar(Fornecedor f) throws SQLException {
        boolean novo = f.getId() == 0;
        String sql = novo ? "INSERT INTO fornecedor(nome,contato,telefone,email) VALUES(?,?,?,?)"
                : "UPDATE fornecedor SET nome=?,contato=?,telefone=?,email=? WHERE id=?";
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNome()); ps.setString(2, f.getContato());
            ps.setString(3, f.getTelefone()); ps.setString(4, f.getEmail());
            if (!novo) ps.setLong(5, f.getId());
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Fornecedor não encontrado. Atualize a lista.");
            long id = f.getId();
            if (novo) try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); id = keys.getLong(1); }
            return new Fornecedor(id, f.getNome(), f.getContato(), f.getTelefone(), f.getEmail());
        }
    }

    static Fornecedor mapear(ResultSet rs) throws SQLException {
        return new Fornecedor(rs.getLong("f_id"), rs.getString("f_nome"), rs.getString("contato"), rs.getString("telefone"), rs.getString("email"));
    }
    private static final String SELECT = "SELECT id AS f_id, nome AS f_nome, contato, telefone, email FROM fornecedor";

    @Override public List<Fornecedor> listar() throws SQLException {
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(SELECT + " ORDER BY nome,id"); ResultSet rs = ps.executeQuery()) {
            List<Fornecedor> lista = new ArrayList<>();
            while (rs.next()) lista.add(mapear(rs));
            return lista;
        }
    }

    @Override public Optional<Fornecedor> buscarPorId(long id) throws SQLException {
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(SELECT + " WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(mapear(rs)) : Optional.empty(); }
        }
    }

    @Override public void excluir(long id) throws SQLException {
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement("DELETE FROM fornecedor WHERE id=?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Fornecedor não encontrado.");
        }
    }
}
