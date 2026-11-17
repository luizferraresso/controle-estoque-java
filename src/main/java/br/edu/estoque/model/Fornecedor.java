package br.edu.estoque.model;

public final class Fornecedor extends Entidade {
    private final String nome;
    private final String contato;
    private final String telefone;
    private final String email;

    public Fornecedor(long id, String nome, String contato, String telefone, String email) {
        super(id);
        this.nome = obrigatorio(nome, "Nome do fornecedor", 120);
        this.contato = opcional(contato, "Contato", 100);
        this.telefone = opcional(telefone, "Telefone", 30);
        this.email = opcional(email, "E-mail", 120);
        if (!this.email.isEmpty() && !this.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
    }

    public String getNome() { return nome; }
    public String getContato() { return contato; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    @Override public String toString() { return nome; }
}
