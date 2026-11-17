package br.edu.estoque.dao;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class MovimentacaoDAO {
    private final BancoDados banco;
    public MovimentacaoDAO(BancoDados banco) { this.banco = banco; }

    /** Recebe a conexão da transação que também atualiza o saldo. */
    public void inserir(Connection con, Movimentacao mov, int saldo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO movimentacao(produto_id,tipo,quantidade,data,observacao,saldo_apos) VALUES(?,?,?,?,?,?)")) {
            ps.setLong(1, mov.getProduto().getId()); ps.setString(2, mov.getTipo()); ps.setInt(3, mov.getQuantidade());
            ps.setTimestamp(4, Timestamp.valueOf(mov.getData())); ps.setString(5, mov.getObservacao()); ps.setInt(6, saldo);
            ps.executeUpdate();
        }
    }

    public List<Movimentacao> listar(LocalDate inicio, LocalDate fim, Long produtoId, String tipo) throws SQLException {
        String sql = """
            SELECT p.id,p.codigo,p.nome,p.categoria,p.preco,p.estoque_minimo,p.quantidade,
            f.id AS f_id,f.nome AS f_nome,f.contato,f.telefone,f.email,
            m.id AS m_id,m.tipo,m.quantidade AS m_quantidade,m.data,m.observacao,m.saldo_apos
            FROM movimentacao m JOIN produto p ON p.id=m.produto_id JOIN fornecedor f ON f.id=p.fornecedor_id
            WHERE 1=1
            """;
        if (inicio != null) sql += " AND m.data >= ?";
        if (fim != null) sql += " AND m.data < ?";
        if (produtoId != null) sql += " AND m.produto_id = ?";
        if (tipo != null) sql += " AND m.tipo = ?";
        sql += " ORDER BY m.data DESC,m.id DESC";
        try (Connection con = banco.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (inicio != null) ps.setTimestamp(i++, Timestamp.valueOf(inicio.atStartOfDay()));
            if (fim != null) ps.setTimestamp(i++, Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
            if (produtoId != null) ps.setLong(i++, produtoId);
            if (tipo != null) ps.setString(i, tipo);
            List<Movimentacao> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Produto p = ProdutoDAO.mapear(rs);
                    long id = rs.getLong("m_id"); int qtd = rs.getInt("m_quantidade");
                    var data = rs.getTimestamp("data").toLocalDateTime();
                    String obs = rs.getString("observacao"); int saldo = rs.getInt("saldo_apos");
                    Movimentacao mov = rs.getString("tipo").equals("ENTRADA")
                            ? new Entrada(id,p,qtd,obs,data,saldo) : new Saida(id,p,qtd,obs,data,saldo);
                    lista.add(mov);
                }
            }
            return lista;
        }
    }
}
