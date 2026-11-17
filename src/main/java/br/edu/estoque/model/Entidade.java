package br.edu.estoque.model;

/** Identidade comum aos objetos persistidos. O identificador zero representa um novo cadastro. */
public abstract class Entidade {
    private final long id;

    protected Entidade(long id) {
        if (id < 0) throw new IllegalArgumentException("Identificador inválido.");
        this.id = id;
    }

    public final long getId() { return id; }

    protected static String obrigatorio(String valor, String campo, int limite) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty()) throw new IllegalArgumentException(campo + " é obrigatório.");
        if (texto.length() > limite) throw new IllegalArgumentException(campo + ": máximo de " + limite + " caracteres.");
        return texto;
    }

    protected static String opcional(String valor, String campo, int limite) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.length() > limite) throw new IllegalArgumentException(campo + ": máximo de " + limite + " caracteres.");
        return texto;
    }
}
