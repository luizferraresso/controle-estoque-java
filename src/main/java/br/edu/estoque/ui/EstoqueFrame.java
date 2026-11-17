package br.edu.estoque.ui;

import br.edu.estoque.model.*;
import br.edu.estoque.service.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

// A janela não é serializada: os dados são persistidos pelos DAOs JDBC.
@SuppressWarnings("serial")
public final class EstoqueFrame extends JFrame {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");
    private final EstoqueService service;
    private final String localBanco;
    private List<Produto> produtos = List.of();
    private List<Fornecedor> fornecedores = List.of();
    private List<Movimentacao> movimentos = List.of();
    private List<Produto> produtosVisiveis = List.of();
    private List<Fornecedor> fornecedoresVisiveis = List.of();
    private final JTabbedPane abas = new JTabbedPane();
    private final JLabel status = new JLabel("Pronto");
    private final JLabel totalProdutos = Estilo.titulo("0", 29);
    private final JLabel totalUnidades = Estilo.titulo("0", 29);
    private final JLabel totalBaixo = Estilo.titulo("0", 29);
    private final JLabel valorEstoque = Estilo.titulo("R$ 0,00", 29);
    private final JLabel orientacao = new JLabel();
    private final JTextField buscaProdutos = new JTextField(22);
    private final JTextField buscaFornecedores = new JTextField(22);
    private final JCheckBox apenasBaixos = new JCheckBox("Somente estoque baixo");
    private final DefaultTableModel modProdutos = Estilo.modelo("Código", "Produto", "Categoria", "Fornecedor", "Preço", "Saldo", "Mínimo", "Situação");
    private final DefaultTableModel modFornecedores = Estilo.modelo("Nome", "Contato", "Telefone", "E-mail");
    private final DefaultTableModel modMovimentos = Estilo.modelo("Data / hora", "Tipo", "Produto", "Quantidade", "Saldo após", "Observação");
    private final DefaultTableModel modBaixos = Estilo.modelo("Código", "Produto", "Saldo", "Mínimo", "Fornecedor");
    private final JTable tabProdutos = Estilo.tabela(modProdutos);
    private final JTable tabFornecedores = Estilo.tabela(modFornecedores);
    private final JTable tabMovimentos = Estilo.tabela(modMovimentos);
    private final JComboBox<String> relatorioTipo = new JComboBox<>(new String[]{"Inventário", "Movimentações"});
    private final JTextField dataInicio = new JTextField(LocalDate.now().withDayOfMonth(1).format(DATA), 10);
    private final JTextField dataFim = new JTextField(LocalDate.now().format(DATA), 10);
    private final JComboBox<Object> relatorioProduto = new JComboBox<>();
    private final JComboBox<String> relatorioMovimento = new JComboBox<>(new String[]{"Todos", "Entrada", "Saída"});
    private final JCheckBox relatorioBaixos = new JCheckBox("Somente estoque baixo");
    private final JPanel areaRelatorio = new JPanel(new BorderLayout());
    private final JLabel resumoRelatorio = new JLabel("Escolha os filtros e clique em Gerar relatório.");
    private final JButton exportar = Estilo.botao("Exportar CSV", false);
    private List<String> cabecalhoCsv = List.of();
    private List<List<String>> linhasCsv = List.of();
    private boolean atualizandoFiltros;

    public EstoqueFrame(EstoqueService service, String localBanco) {
        super("Controle de Estoque | Trabalho de POO");
        this.service = service;
        this.localBanco = localBanco;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 710));
        setSize(1250, 820);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout());
        JPanel topo = new JPanel(new BorderLayout(20, 0));
        topo.setBackground(Estilo.TEXTO);
        topo.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel marca = Estilo.titulo("ESTOQUE / Controle de produtos", 24); marca.setForeground(Color.WHITE);
        JLabel detalhe = new JLabel("Java • POO • JDBC • Swing"); detalhe.setForeground(new Color(181, 210, 220));
        topo.add(marca, BorderLayout.WEST); topo.add(detalhe, BorderLayout.EAST);
        raiz.add(topo, BorderLayout.NORTH);
        abas.addTab("  Visão geral  ", criarDashboard());
        abas.addTab("  Produtos  ", criarProdutos());
        abas.addTab("  Fornecedores  ", criarFornecedores());
        abas.addTab("  Movimentações  ", criarMovimentos());
        abas.addTab("  Relatórios  ", criarRelatorios());
        raiz.add(abas, BorderLayout.CENTER);
        status.setBorder(new EmptyBorder(10, 24, 10, 24));
        status.setToolTipText("Banco de dados: " + localBanco);
        raiz.add(status, BorderLayout.SOUTH);
        setContentPane(raiz);
        JMenuBar menu = new JMenuBar();
        JMenu arquivo = new JMenu("Arquivo");
        JMenuItem atualizar = new JMenuItem("Atualizar dados (F5)"); atualizar.addActionListener(e -> atualizar());
        JMenuItem sair = new JMenuItem("Sair"); sair.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        arquivo.add(atualizar); arquivo.addSeparator(); arquivo.add(sair);
        JMenu ajuda = new JMenu("Ajuda");
        JMenuItem sobre = new JMenuItem("Como usar / Sobre"); sobre.addActionListener(e -> mostrarAjuda()); ajuda.add(sobre);
        menu.add(arquivo); menu.add(ajuda); setJMenuBar(menu);
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "atualizar");
        raiz.getActionMap().put("atualizar", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { atualizar(); } });
    }

    private JPanel criarDashboard() {
        JPanel conteudo = new JPanel(new BorderLayout(0, 22));
        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.add(card("Produtos cadastrados", totalProdutos, "Itens diferentes no catálogo"));
        cards.add(card("Unidades disponíveis", totalUnidades, "Saldo total de produtos"));
        cards.add(card("Estoque baixo", totalBaixo, "Saldo menor ou igual ao mínimo"));
        cards.add(card("Valor do inventário", valorEstoque, "Saldo × preço atual"));
        conteudo.add(cards, BorderLayout.NORTH);
        JPanel alertas = new JPanel(new BorderLayout(0, 12));
        alertas.add(Estilo.titulo("Produtos que precisam de reposição", 18), BorderLayout.NORTH);
        alertas.add(new JScrollPane(Estilo.tabela(modBaixos)), BorderLayout.CENTER);
        orientacao.setForeground(Estilo.SUAVE); alertas.add(orientacao, BorderLayout.SOUTH);
        conteudo.add(alertas, BorderLayout.CENTER);
        JButton entrada = Estilo.botao("Registrar entrada", true); entrada.addActionListener(e -> abrirMovimento("ENTRADA"));
        JButton saida = Estilo.botao("Registrar saída", false); saida.addActionListener(e -> abrirMovimento("SAIDA"));
        JButton atualizar = Estilo.botao("Atualizar", false); atualizar.addActionListener(e -> atualizar());
        return Estilo.pagina("Seu estoque em um só lugar", "Acompanhe saldos, reposição e o valor atual do inventário.", Estilo.linha(entrada, saida, atualizar), conteudo);
    }

    private JPanel card(String titulo, JLabel valor, String legenda) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 9)); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(222, 229, 234)), new EmptyBorder(18, 18, 18, 18)));
        JLabel t = new JLabel(titulo); t.setForeground(Estilo.SUAVE); p.add(t); p.add(valor);
        JLabel l = new JLabel(legenda); l.setFont(l.getFont().deriveFont(12f)); l.setForeground(Estilo.SUAVE); p.add(l);
        return p;
    }

    private JPanel criarProdutos() {
        JButton novo = Estilo.botao("Novo produto", true); novo.addActionListener(e -> abrirProduto(null));
        JButton editar = Estilo.botao("Editar", false); editar.addActionListener(e -> { Produto p = produtoSelecionado(); if (p != null) abrirProduto(p); });
        JButton excluir = Estilo.botao("Excluir", false); excluir.addActionListener(e -> excluirProduto());
        observar(buscaProdutos, this::filtrarProdutos); apenasBaixos.addActionListener(e -> filtrarProdutos());
        duploClique(tabProdutos, () -> editar.doClick());
        JPanel acoes = new JPanel(new BorderLayout());
        acoes.add(Estilo.linha(novo, editar, excluir), BorderLayout.NORTH);
        acoes.add(Estilo.linha(new JLabel("Buscar:"), buscaProdutos, apenasBaixos), BorderLayout.SOUTH);
        return Estilo.pagina("Produtos", "Cadastre o produto e registre o saldo inicial em Movimentações. Campos com * são obrigatórios.", acoes, new JScrollPane(tabProdutos));
    }

    private JPanel criarFornecedores() {
        JButton novo = Estilo.botao("Novo fornecedor", true); novo.addActionListener(e -> abrirFornecedor(null));
        JButton editar = Estilo.botao("Editar", false); editar.addActionListener(e -> { Fornecedor f = fornecedorSelecionado(); if (f != null) abrirFornecedor(f); });
        JButton excluir = Estilo.botao("Excluir", false); excluir.addActionListener(e -> excluirFornecedor());
        observar(buscaFornecedores, this::filtrarFornecedores); duploClique(tabFornecedores, () -> editar.doClick());
        return Estilo.pagina("Fornecedores", "Mantenha os contatos de quem abastece sua loja.",
                Estilo.linha(novo, editar, excluir, new JLabel("Buscar:"), buscaFornecedores), new JScrollPane(tabFornecedores));
    }

    private JPanel criarMovimentos() {
        JButton entrada = Estilo.botao("Registrar entrada", true); entrada.addActionListener(e -> abrirMovimento("ENTRADA"));
        JButton saida = Estilo.botao("Registrar saída", false); saida.addActionListener(e -> abrirMovimento("SAIDA"));
        JButton atualizar = Estilo.botao("Atualizar", false); atualizar.addActionListener(e -> atualizar());
        return Estilo.pagina("Movimentações de estoque", "Histórico em ordem da mais recente. Para corrigir um lançamento, registre uma movimentação inversa.",
                Estilo.linha(entrada, saida, atualizar), new JScrollPane(tabMovimentos));
    }

    private JPanel criarRelatorios() {
        relatorioProduto.setPrototypeDisplayValue("Todos os produtos                                  ");
        JPanel filtros = new JPanel(new GridLayout(0, 1, 0, 6));
        JButton gerar = Estilo.botao("Gerar relatório", true); gerar.addActionListener(e -> gerarRelatorio());
        exportar.setEnabled(false); exportar.addActionListener(e -> exportarCsv());
        filtros.add(Estilo.linha(new JLabel("Relatório:"), relatorioTipo, new JLabel("Produto:"), relatorioProduto, relatorioBaixos));
        filtros.add(Estilo.linha(new JLabel("De (dd/mm/aaaa):"), dataInicio, new JLabel("Até:"), dataFim, new JLabel("Tipo:"), relatorioMovimento, gerar, exportar));
        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        areaRelatorio.add(new JLabel("Gere um relatório para visualizar os resultados.", SwingConstants.CENTER));
        corpo.add(areaRelatorio, BorderLayout.CENTER); corpo.add(resumoRelatorio, BorderLayout.SOUTH);
        relatorioTipo.addActionListener(e -> { habilitarFiltros(); invalidarRelatorio(); });
        relatorioProduto.addActionListener(e -> invalidarRelatorio());
        relatorioMovimento.addActionListener(e -> invalidarRelatorio());
        relatorioBaixos.addActionListener(e -> invalidarRelatorio());
        observar(dataInicio, this::invalidarRelatorio); observar(dataFim, this::invalidarRelatorio);
        habilitarFiltros();
        return Estilo.pagina("Relatórios", "Consulte o inventário atual ou o histórico por período. Exporte os resultados para abrir em uma planilha.", filtros, corpo);
    }

    private void habilitarFiltros() {
        boolean movimento = relatorioTipo.getSelectedIndex() == 1;
        dataInicio.setEnabled(movimento); dataFim.setEnabled(movimento); relatorioMovimento.setEnabled(movimento);
        relatorioBaixos.setEnabled(!movimento);
    }

    private void invalidarRelatorio() {
        if (atualizandoFiltros) return;
        exportar.setEnabled(false);
        resumoRelatorio.setText("Filtros ou dados alterados. Clique em Gerar relatório para atualizar.");
    }

    private record Dados(List<Produto> produtos, List<Fornecedor> fornecedores, List<Movimentacao> movimentos) { }

    private Dados carregarDados() throws SQLException {
        return new Dados(service.produtos(), service.fornecedores(), service.movimentacoes(null,null,null,null));
    }

    public void atualizar() { trabalhar(this::carregarDados, this::aplicarDados); }

    private void aplicarDados(Dados dados) {
        produtos = dados.produtos(); fornecedores = dados.fornecedores(); movimentos = dados.movimentos();
        filtrarProdutos(); filtrarFornecedores();
        totalProdutos.setText(String.valueOf(produtos.size()));
        totalUnidades.setText(String.valueOf(produtos.stream().mapToLong(Produto::getQuantidade).sum()));
        totalBaixo.setText(String.valueOf(produtos.stream().filter(Produto::isEstoqueBaixo).count()));
        valorEstoque.setText(Estilo.MOEDA.format(produtos.stream().map(Produto::getValorEmEstoque).reduce(BigDecimal.ZERO, BigDecimal::add)));
        modBaixos.setRowCount(0);
        produtos.stream().filter(Produto::isEstoqueBaixo).forEach(p -> modBaixos.addRow(new Object[]{p.getCodigo(),p.getNome(),p.getQuantidade(),p.getEstoqueMinimo(),p.getFornecedor().getNome()}));
        orientacao.setText(fornecedores.isEmpty() ? "Comece cadastrando um fornecedor, depois um produto e uma entrada de estoque."
                : produtos.isEmpty() ? "Cadastre um produto e registre uma entrada para começar."
                : modBaixos.getRowCount() == 0 ? "Tudo em dia: nenhum produto com estoque baixo." : "Reposição recomendada para os produtos listados acima.");
        modMovimentos.setRowCount(0);
        for (Movimentacao m : movimentos) modMovimentos.addRow(linhaMovimento(m));
        // O tipo LocalDateTime permite ordenar cronologicamente mesmo no formato brasileiro.
        tabMovimentos.setDefaultRenderer(LocalDateTime.class, new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) { setText(value == null ? "" : ((LocalDateTime)value).format(DATA_HORA)); }
        });
        Long produtoId = relatorioProduto.getSelectedItem() instanceof Produto p ? p.getId() : null;
        atualizandoFiltros = true;
        relatorioProduto.removeAllItems(); relatorioProduto.addItem("Todos os produtos");
        for (Produto p : produtos) { relatorioProduto.addItem(p); if (Objects.equals(produtoId, p.getId())) relatorioProduto.setSelectedItem(p); }
        atualizandoFiltros = false;
        invalidarRelatorio();
        status.setText("Atualizado às " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "   •   " + produtos.size() + " produtos   •   Banco local H2");
    }

    private void filtrarProdutos() {
        String busca = buscaProdutos.getText().strip().toLowerCase(Locale.ROOT);
        produtosVisiveis = produtos.stream().filter(p -> !apenasBaixos.isSelected() || p.isEstoqueBaixo())
                .filter(p -> (p.getCodigo()+" "+p.getNome()+" "+p.getCategoria()+" "+p.getFornecedor().getNome()).toLowerCase(Locale.ROOT).contains(busca)).toList();
        modProdutos.setRowCount(0);
        for (Produto p : produtosVisiveis) modProdutos.addRow(new Object[]{p.getCodigo(),p.getNome(),p.getCategoria(),p.getFornecedor().getNome(),p.getPreco(),p.getQuantidade(),p.getEstoqueMinimo(),p.isEstoqueBaixo()?"BAIXO":"Regular"});
    }

    private void filtrarFornecedores() {
        String busca = buscaFornecedores.getText().strip().toLowerCase(Locale.ROOT);
        fornecedoresVisiveis = fornecedores.stream().filter(f -> (f.getNome()+" "+f.getContato()+" "+f.getTelefone()+" "+f.getEmail()).toLowerCase(Locale.ROOT).contains(busca)).toList();
        modFornecedores.setRowCount(0);
        for (Fornecedor f : fornecedoresVisiveis) modFornecedores.addRow(new Object[]{f.getNome(),f.getContato(),f.getTelefone(),f.getEmail()});
    }

    private Produto produtoSelecionado() {
        int i = tabProdutos.getSelectedRow();
        if (i < 0) { aviso("Selecione um produto na tabela."); return null; }
        return produtosVisiveis.get(tabProdutos.convertRowIndexToModel(i));
    }
    private Fornecedor fornecedorSelecionado() {
        int i = tabFornecedores.getSelectedRow();
        if (i < 0) { aviso("Selecione um fornecedor na tabela."); return null; }
        return fornecedoresVisiveis.get(tabFornecedores.convertRowIndexToModel(i));
    }

    private void abrirFornecedor(Fornecedor atual) {
        JTextField nome = new JTextField(atual == null ? "" : atual.getNome(), 28);
        JTextField contato = new JTextField(atual == null ? "" : atual.getContato());
        JTextField telefone = new JTextField(atual == null ? "" : atual.getTelefone());
        JTextField email = new JTextField(atual == null ? "" : atual.getEmail());
        Formulario form = new Formulario(atual == null ? "Novo fornecedor" : "Editar fornecedor");
        form.campo("Nome *", nome); form.campo("Contato", contato); form.campo("Telefone", telefone); form.campo("E-mail", email);
        form.aoSalvar(() -> {
            Fornecedor f = new Fornecedor(atual == null ? 0 : atual.getId(),nome.getText(),contato.getText(),telefone.getText(),email.getText());
            salvar(form, () -> service.salvar(f), "Fornecedor salvo.");
        });
        form.mostrar();
    }

    private void abrirProduto(Produto atual) {
        if (fornecedores.isEmpty()) { aviso("Cadastre um fornecedor antes de cadastrar produtos."); abas.setSelectedIndex(2); return; }
        JTextField codigo = new JTextField(atual == null ? "" : atual.getCodigo(), 28);
        JTextField nome = new JTextField(atual == null ? "" : atual.getNome());
        JTextField categoria = new JTextField(atual == null ? "" : atual.getCategoria());
        JTextField preco = new JTextField(atual == null ? "0,00" : atual.getPreco().toPlainString().replace('.', ','));
        JTextField minimo = new JTextField(atual == null ? "0" : String.valueOf(atual.getEstoqueMinimo()));
        JComboBox<Fornecedor> fornecedor = new JComboBox<>(fornecedores.toArray(Fornecedor[]::new));
        if (atual != null) for (Fornecedor f : fornecedores) if (f.getId() == atual.getFornecedor().getId()) fornecedor.setSelectedItem(f);
        Formulario form = new Formulario(atual == null ? "Novo produto" : "Editar produto");
        form.campo("Código único *", codigo); form.campo("Nome *", nome); form.campo("Categoria *", categoria);
        form.campo("Preço unitário (R$) *", preco); form.campo("Estoque mínimo *", minimo); form.campo("Fornecedor *", fornecedor);
        form.campo("Saldo disponível", new JLabel(atual == null ? "0 • registre o saldo inicial em Movimentações" : atual.getQuantidade() + " unidades • alterado por movimentações"));
        form.aoSalvar(() -> {
            Produto p = new Produto(atual == null ? 0 : atual.getId(),codigo.getText(),nome.getText(),categoria.getText(),lerPreco(preco.getText()),
                    inteiro(minimo.getText(),"Estoque mínimo",false),atual == null ? 0 : atual.getQuantidade(),(Fornecedor)fornecedor.getSelectedItem());
            salvar(form, () -> service.salvar(p), "Produto salvo.");
        });
        form.mostrar();
    }

    private void abrirMovimento(String tipo) {
        if (produtos.isEmpty()) { aviso("Cadastre um produto antes de registrar movimentações."); abas.setSelectedIndex(1); return; }
        JComboBox<Produto> produto = new JComboBox<>(produtos.toArray(Produto[]::new));
        if (abas.getSelectedIndex() == 1 && tabProdutos.getSelectedRow() >= 0) produto.setSelectedItem(produtosVisiveis.get(tabProdutos.convertRowIndexToModel(tabProdutos.getSelectedRow())));
        JLabel saldo = new JLabel();
        Runnable mostrarSaldo = () -> { Produto p = (Produto)produto.getSelectedItem(); saldo.setText(p == null ? "" : p.getQuantidade()+" unidades disponíveis"); };
        produto.addActionListener(e -> mostrarSaldo.run()); mostrarSaldo.run();
        JTextField quantidade = new JTextField("1", 28);
        JTextField observacao = new JTextField();
        Formulario form = new Formulario(tipo.equals("ENTRADA") ? "Registrar entrada" : "Registrar saída");
        form.campo("Produto *", produto); form.campo("Saldo atual", saldo); form.campo("Quantidade *", quantidade); form.campo("Observação", observacao);
        form.aoSalvar(() -> {
            Produto p = (Produto)produto.getSelectedItem();
            if (p == null) throw new IllegalArgumentException("Selecione um produto.");
            int qtd = inteiro(quantidade.getText(),"Quantidade",true);
            String obs = observacao.getText();
            salvar(form, () -> { service.registrar(p.getId(),tipo,qtd,obs); return null; }, "Movimentação registrada.");
        });
        form.mostrar();
    }

    private <T> void salvar(Formulario form, Tarefa<T> tarefa, String mensagem) {
        trabalhar(() -> { tarefa.executar(); return carregarDados(); }, dados -> {
            form.dispose(); aplicarDados(dados); status.setText(mensagem + "   •   Dados atualizados.");
        });
    }

    private void excluirProduto() {
        Produto p = produtoSelecionado(); if (p == null) return;
        if (confirmar("Excluir o produto “" + p.getNome() + "”?\nSó é permitida a exclusão de produtos sem movimentações e com saldo zero."))
            trabalhar(() -> { service.excluirProduto(p.getId()); return carregarDados(); }, this::aplicarDados);
    }
    private void excluirFornecedor() {
        Fornecedor f = fornecedorSelecionado(); if (f == null) return;
        if (confirmar("Excluir o fornecedor “" + f.getNome() + "”?\nFornecedores vinculados a produtos não podem ser excluídos."))
            trabalhar(() -> { service.excluirFornecedor(f.getId()); return carregarDados(); }, this::aplicarDados);
    }

    private void gerarRelatorio() {
        try {
            Produto selecionado = relatorioProduto.getSelectedItem() instanceof Produto p ? p : null;
            Long id = selecionado == null ? null : selecionado.getId();
            boolean baixo = relatorioBaixos.isSelected();
            boolean inventario = relatorioTipo.getSelectedIndex() == 0;
            if (inventario) {
                trabalhar(service::produtos, lista -> {
                    List<Produto> filtrados = lista.stream().filter(p -> id == null || p.getId() == id).filter(p -> !baixo || p.isEstoqueBaixo()).toList();
                    cabecalhoCsv = List.of("Código","Produto","Categoria","Fornecedor","Preço unitário (R$)","Saldo","Mínimo","Valor total (R$)","Situação");
                    linhasCsv = filtrados.stream().map(p -> List.of(p.getCodigo(),p.getNome(),p.getCategoria(),p.getFornecedor().getNome(),decimal(p.getPreco()),String.valueOf(p.getQuantidade()),String.valueOf(p.getEstoqueMinimo()),decimal(p.getValorEmEstoque()),p.isEstoqueBaixo()?"BAIXO":"Regular")).toList();
                    BigDecimal total = filtrados.stream().map(Produto::getValorEmEstoque).reduce(BigDecimal.ZERO,BigDecimal::add);
                    exibirRelatorio("Inventário atual • " + filtrados.size() + " produtos • Valor: " + Estilo.MOEDA.format(total));
                });
            } else {
                LocalDate inicio = lerData(dataInicio.getText()); LocalDate fim = lerData(dataFim.getText());
                String tipo = switch (relatorioMovimento.getSelectedIndex()) { case 1 -> "ENTRADA"; case 2 -> "SAIDA"; default -> null; };
                trabalhar(() -> service.movimentacoes(inicio,fim,id,tipo), lista -> {
                    cabecalhoCsv = List.of("Data / hora","Tipo","Código","Produto","Quantidade","Saldo após","Observação");
                    linhasCsv = lista.stream().map(m -> List.of(m.getData().format(DATA_HORA),tipoAmigavel(m.getTipo()),m.getProduto().getCodigo(),m.getProduto().getNome(),String.valueOf(m.getQuantidade()),String.valueOf(m.getSaldoApos()),m.getObservacao())).toList();
                    long entradas = lista.stream().filter(m -> m instanceof Entrada).mapToLong(Movimentacao::getQuantidade).sum();
                    long saidas = lista.stream().filter(m -> m instanceof Saida).mapToLong(Movimentacao::getQuantidade).sum();
                    exibirRelatorio(lista.size() + " movimentações • Entradas: " + entradas + " un. • Saídas: " + saidas + " un.");
                });
            }
        } catch (IllegalArgumentException e) { aviso(e.getMessage()); }
    }

    private void exibirRelatorio(String resumo) {
        DefaultTableModel modelo = Estilo.modelo(cabecalhoCsv.toArray(String[]::new));
        for (List<String> linha : linhasCsv) modelo.addRow(linha.toArray());
        JTable tabela = Estilo.tabela(modelo);
        // O CSV conserva a ordem do relatório, sem reordenação local divergente.
        tabela.setAutoCreateRowSorter(false); tabela.setRowSorter(null);
        areaRelatorio.removeAll(); areaRelatorio.add(new JScrollPane(tabela)); areaRelatorio.revalidate(); areaRelatorio.repaint();
        resumoRelatorio.setText(resumo + " • Gerado às " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        exportar.setEnabled(true);
    }

    private void exportarCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Salvar relatório CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("Planilha CSV (*.csv)","csv"));
        chooser.setSelectedFile(new java.io.File("relatorio-estoque-" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = chooser.getSelectedFile().toPath().toAbsolutePath();
        if (!destino.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) destino = Path.of(destino + ".csv");
        if (java.nio.file.Files.exists(destino) && !confirmar("O arquivo já existe. Deseja substituí-lo?")) return;
        Path arquivo = destino; List<String> cabecalho = cabecalhoCsv; List<List<String>> linhas = linhasCsv;
        trabalhar(() -> { CsvExporter.exportar(arquivo,cabecalho,linhas); return arquivo; }, salvo -> JOptionPane.showMessageDialog(this,"Relatório salvo em:\n"+salvo,"Exportação concluída",JOptionPane.INFORMATION_MESSAGE));
    }

    private static Object[] linhaMovimento(Movimentacao m) {
        return new Object[]{m.getData(),tipoAmigavel(m.getTipo()),m.getProduto().getNome(),m.getQuantidade(),m.getSaldoApos(),m.getObservacao()};
    }
    private static String tipoAmigavel(String tipo) { return tipo.equals("ENTRADA") ? "Entrada" : "Saída"; }
    private static String decimal(BigDecimal valor) { return valor.toPlainString().replace('.', ','); }
    private static int inteiro(String texto, String campo, boolean positivo) {
        try {
            int valor = Integer.parseInt(texto.strip());
            if (valor < (positivo ? 1 : 0)) throw new NumberFormatException();
            return valor;
        } catch (NumberFormatException e) { throw new IllegalArgumentException(campo + " deve ser um inteiro " + (positivo ? "maior que zero." : "maior ou igual a zero.")); }
    }
    private static BigDecimal lerPreco(String texto) {
        String valor = texto.strip();
        if (!valor.matches("\\d+([,.]\\d{1,2})?")) throw new IllegalArgumentException("Informe o preço como 29,90 ou 29.90, sem separador de milhares.");
        return new BigDecimal(valor.replace(',','.'));
    }
    private static LocalDate lerData(String texto) {
        if (texto.isBlank()) return null;
        try {
            LocalDate data = LocalDate.parse(texto.strip(),DATA);
            if (data.getYear() < 1 || data.getYear() > 9999) throw new DateTimeParseException("Ano fora do intervalo", texto, 0);
            return data;
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Data inválida. Use dd/mm/aaaa ou deixe em branco para consultar todo o período."); }
    }

    private static void observar(JTextField campo, Runnable acao) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { acao.run(); }
            @Override public void removeUpdate(DocumentEvent e) { acao.run(); }
            @Override public void changedUpdate(DocumentEvent e) { acao.run(); }
        });
    }
    private static void duploClique(JTable tabela, Runnable acao) {
        tabela.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && tabela.rowAtPoint(e.getPoint()) >= 0) acao.run(); } });
    }
    private void aviso(String texto) { JOptionPane.showMessageDialog(this,texto,"Verifique os dados",JOptionPane.WARNING_MESSAGE); }
    private boolean confirmar(String texto) { return JOptionPane.showConfirmDialog(this,texto,"Confirmar exclusão / substituição",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION; }

    private void mostrarAjuda() {
        JOptionPane.showMessageDialog(this,"""
            CONTROLE DE ESTOQUE

            1. Cadastre um fornecedor.
            2. Cadastre um produto com preço e estoque mínimo.
            3. Registre uma entrada para informar o saldo inicial.
            4. Registre saídas; o sistema impede saldo negativo.
            5. Acompanhe os alertas e exporte relatórios em CSV.

            Os lançamentos preservam o histórico. Para corrigir um erro,
            registre uma movimentação inversa e explique na observação.
            Produtos com histórico e fornecedores vinculados são preservados.

            Estoque baixo: saldo menor ou igual ao mínimo.
            Valor do inventário: saldo multiplicado pelo preço atual.
            F5 atualiza os dados. Datas em branco consultam todo o período.
            """ + "\nBanco de dados: " + localBanco + "\n\nTecnologias: Java 17+, Swing, JDBC e H2.","Como usar / Sobre",JOptionPane.INFORMATION_MESSAGE);
    }

    @FunctionalInterface private interface Tarefa<T> { T executar() throws Exception; }

    /** Uma janela modal mantém os comandos bloqueados, enquanto o JDBC roda fora da EDT. */
    private <T> void trabalhar(Tarefa<T> tarefa, Consumer<T> sucesso) {
        Window dono = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        JDialog progresso = new JDialog(dono == null ? this : dono,"Aguarde",Dialog.ModalityType.APPLICATION_MODAL);
        progresso.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel painel = new JPanel(new BorderLayout(12,12)); painel.setBorder(new EmptyBorder(20,30,20,30));
        painel.add(new JLabel("Processando dados…"),BorderLayout.NORTH);
        JProgressBar barra = new JProgressBar(); barra.setIndeterminate(true); painel.add(barra);
        progresso.setContentPane(painel); progresso.pack(); progresso.setLocationRelativeTo(dono == null ? this : dono);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return tarefa.executar(); }
            @Override protected void done() {
                progresso.dispose();
                try { sucesso.accept(get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); aviso("Operação interrompida."); }
                catch (ExecutionException e) { mostrarErro(e.getCause()); }
            }
        }.execute();
        progresso.setVisible(true);
    }

    private void mostrarErro(Throwable erro) {
        if (erro instanceof IllegalArgumentException) { aviso(erro.getMessage()); return; }
        String mensagem = "Não foi possível concluir a operação.";
        if (erro instanceof SQLException sql) {
            String estado = sql.getSQLState();
            if ("23505".equals(estado)) mensagem = "Este código de produto já está cadastrado. Informe outro código.";
            else if ("23503".equals(estado) || "23506".equals(estado)) mensagem = "Cadastro vinculado a outros registros.\nPreserve produtos com movimentações e fornecedores usados por produtos.\nSe o cadastro vinculado foi removido, atualize os dados com F5.";
            else mensagem = "Falha ao acessar o banco. Verifique se outra instância está aberta e se a pasta permite gravação.\nDetalhes: " + sql.getMessage();
        } else if (erro instanceof java.io.IOException) mensagem = "Não foi possível gravar o arquivo. Verifique o local e as permissões.\n" + erro.getMessage();
        erro.printStackTrace();
        JOptionPane.showMessageDialog(this,mensagem,"Operação não concluída",JOptionPane.ERROR_MESSAGE);
    }

    private final class Formulario extends JDialog {
        private final JPanel campos = new JPanel(new GridBagLayout());
        private final JButton salvar = Estilo.botao("Salvar",true);
        private int linha;
        Formulario(String titulo) {
            super(EstoqueFrame.this,titulo,true);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JPanel conteudo = new JPanel(new BorderLayout(0,18)); conteudo.setBorder(new EmptyBorder(22,24,18,24));
            conteudo.add(campos,BorderLayout.CENTER);
            JButton cancelar = Estilo.botao("Cancelar",false); cancelar.addActionListener(e -> dispose());
            conteudo.add(Estilo.linha(salvar,cancelar),BorderLayout.SOUTH);
            setContentPane(conteudo); getRootPane().setDefaultButton(salvar);
            getRootPane().registerKeyboardAction(e -> dispose(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        void campo(String rotulo, JComponent componente) {
            GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=linha++; c.anchor=GridBagConstraints.WEST; c.insets=new Insets(6,0,6,18);
            JLabel label = new JLabel(rotulo); label.setLabelFor(componente); campos.add(label,c);
            c.gridx=1; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.insets=new Insets(6,0,6,0); campos.add(componente,c);
        }
        void aoSalvar(Runnable acao) { salvar.addActionListener(e -> { try { acao.run(); } catch (IllegalArgumentException ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Verifique os dados",JOptionPane.WARNING_MESSAGE); } }); }
        void mostrar() { pack(); setMinimumSize(new Dimension(580,getHeight())); setLocationRelativeTo(EstoqueFrame.this); setVisible(true); }
    }

    public static void configurarAparencia() { Estilo.instalar(); }
}
