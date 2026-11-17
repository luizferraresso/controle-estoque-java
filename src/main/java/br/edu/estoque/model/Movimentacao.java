package br.edu.estoque.model;

import java.time.LocalDateTime;

/** Cada subtipo decide como aplicar sua quantidade ao saldo: polimorfismo. */
public abstract class Movimentacao extends Entidade {
    private final Produto produto;
    private final int quantidade;
    private final String observacao;
    private final LocalDateTime data;
    private final int saldoApos;

    protected Movimentacao(long id, Produto produto, int quantidade, String observacao,
                          LocalDateTime data, int saldoApos) {
        super(id);
        if (produto == null || produto.getId() == 0) throw new IllegalArgumentException("Selecione um produto cadastrado.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        if (data == null || saldoApos < 0) throw new IllegalArgumentException("Movimentação inválida.");
        this.produto = produto;
        this.quantidade = quantidade;
        this.observacao = opcional(observacao, "Observação", 250);
        this.data = data;
        this.saldoApos = saldoApos;
    }

    public abstract String getTipo();
    public abstract int calcularSaldo(int saldoAtual);
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }
    public String getObservacao() { return observacao; }
    public LocalDateTime getData() { return data; }
    public int getSaldoApos() { return saldoApos; }
}
