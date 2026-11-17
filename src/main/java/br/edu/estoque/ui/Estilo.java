package br.edu.estoque.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Estilo {
    static final Color FUNDO = new Color(244, 247, 249);
    static final Color TEXTO = new Color(24, 43, 61);
    static final Color VERDE = new Color(16, 117, 104);
    static final Color SUAVE = new Color(100, 116, 130);
    static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private Estilo() { }

    static void instalar() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { /* A aparência padrão do Swing continua disponível. */ }
        Font fonte = new Font("Segoe UI", Font.PLAIN, 14);
        for (Object chave : UIManager.getDefaults().keySet().toArray()) {
            if (UIManager.get(chave) instanceof javax.swing.plaf.FontUIResource) UIManager.put(chave, fonte);
        }
        UIManager.put("Panel.background", FUNDO);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("Table.gridColor", new Color(226, 232, 237));
        UIManager.put("Table.selectionBackground", new Color(213, 236, 231));
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("FileChooser.openButtonText", "Abrir");
        UIManager.put("FileChooser.saveButtonText", "Salvar");
    }

    static JButton botao(String texto, boolean principal) {
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(principal ? VERDE : new Color(205, 215, 222)), new EmptyBorder(9, 15, 9, 15)));
        b.setBackground(principal ? VERDE : Color.WHITE);
        b.setForeground(principal ? Color.WHITE : TEXTO);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        return b;
    }

    static JLabel titulo(String texto, int tamanho) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, tamanho));
        return l;
    }

    static DefaultTableModel modelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return getRowCount() == 0 || getValueAt(0, column) == null ? Object.class : getValueAt(0, column).getClass();
            }
        };
    }

    static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo);
        t.setRowHeight(35);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 39));
        t.getTableHeader().setFont(t.getFont().deriveFont(Font.BOLD));
        DefaultTableCellRenderer celula = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setHorizontalAlignment(value instanceof Number ? RIGHT : LEFT);
                setText(value instanceof BigDecimal ? MOEDA.format(value)
                        : value instanceof LocalDateTime data ? data.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss"))
                        : String.valueOf(value == null ? "" : value));
                if (!selected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    setForeground("BAIXO".equals(value) ? new Color(172, 63, 30) : TEXTO);
                }
                return this;
            }
        };
        t.setDefaultRenderer(Object.class, celula);
        t.setDefaultRenderer(Number.class, celula);
        return t;
    }

    static JPanel pagina(String titulo, String subtitulo, JComponent acoes, JComponent conteudo) {
        JPanel p = new JPanel(new BorderLayout(0, 17));
        p.setBorder(new EmptyBorder(22, 24, 22, 24));
        JPanel topo = new JPanel(new BorderLayout(12, 12));
        JPanel textos = new JPanel(new GridLayout(0, 1, 0, 5));
        textos.add(titulo(titulo, 23));
        JLabel ajuda = new JLabel(subtitulo); ajuda.setForeground(SUAVE); textos.add(ajuda);
        topo.add(textos, BorderLayout.NORTH);
        if (acoes != null) topo.add(acoes, BorderLayout.SOUTH);
        p.add(topo, BorderLayout.NORTH);
        p.add(conteudo, BorderLayout.CENTER);
        return p;
    }

    static JPanel linha(Component... itens) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        for (Component c : itens) p.add(c);
        return p;
    }
}
