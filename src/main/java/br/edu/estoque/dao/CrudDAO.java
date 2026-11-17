package br.edu.estoque.dao;

import br.edu.estoque.model.Entidade;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Contrato compartilhado de persistência; a interface gráfica não escreve SQL. */
public interface CrudDAO<T extends Entidade> {
    T salvar(T entidade) throws SQLException;
    Optional<T> buscarPorId(long id) throws SQLException;
    List<T> listar() throws SQLException;
    void excluir(long id) throws SQLException;
}
