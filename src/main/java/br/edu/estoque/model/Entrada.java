package br.edu.estoque.model;

import java.time.LocalDateTime;

public final class Entrada extends Movimentacao {
    public Entrada(long id, Produto produto, int quantidade, String observacao, LocalDateTime data, int saldoApos) {
        super(id, produto, quantidade, observacao, data, saldoApos);
    }
    @Override public String getTipo() { return "ENTRADA"; }
    @Override public int calcularSaldo(int saldoAtual) {
        try { return Math.addExact(saldoAtual, getQuantidade()); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Quantidade acima do limite de estoque."); }
    }
}
