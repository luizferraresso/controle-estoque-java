package br.edu.estoque.model;

import java.time.LocalDateTime;

public final class Saida extends Movimentacao {
    public Saida(long id, Produto produto, int quantidade, String observacao, LocalDateTime data, int saldoApos) {
        super(id, produto, quantidade, observacao, data, saldoApos);
    }
    @Override public String getTipo() { return "SAIDA"; }
    @Override public int calcularSaldo(int saldoAtual) {
        if (getQuantidade() > saldoAtual) throw new IllegalArgumentException("Estoque insuficiente. Saldo disponível: " + saldoAtual + ".");
        return saldoAtual - getQuantidade();
    }
}
