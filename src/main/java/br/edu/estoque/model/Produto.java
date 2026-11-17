package br.edu.estoque.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Produto extends Entidade {
    private final String codigo;
    private final String nome;
    private final String categoria;
    private final BigDecimal preco;
    private final int estoqueMinimo;
    private final int quantidade;
    private final Fornecedor fornecedor;

    public Produto(long id, String codigo, String nome, String categoria, BigDecimal preco,
                   int estoqueMinimo, int quantidade, Fornecedor fornecedor) {
        super(id);
        this.codigo = obrigatorio(codigo, "Código", 40).toUpperCase(Locale.ROOT);
        this.nome = obrigatorio(nome, "Nome do produto", 120);
        this.categoria = obrigatorio(categoria, "Categoria", 80);
        if (preco == null || preco.signum() < 0 || preco.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("O preço deve estar entre 0 e 9.999.999.999,99.");
        }
        try { this.preco = preco.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("O preço deve ter no máximo duas casas decimais."); }
        if (estoqueMinimo < 0 || quantidade < 0) throw new IllegalArgumentException("O estoque não pode ser negativo.");
        if (fornecedor == null || fornecedor.getId() == 0) throw new IllegalArgumentException("Selecione um fornecedor cadastrado.");
        this.estoqueMinimo = estoqueMinimo;
        this.quantidade = quantidade;
        this.fornecedor = fornecedor;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPreco() { return preco; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public int getQuantidade() { return quantidade; }
    public Fornecedor getFornecedor() { return fornecedor; }
    public boolean isEstoqueBaixo() { return quantidade <= estoqueMinimo; }
    public BigDecimal getValorEmEstoque() { return preco.multiply(BigDecimal.valueOf(quantidade)); }
    @Override public String toString() { return codigo + " • " + nome; }
}
