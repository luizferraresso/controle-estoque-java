package br.edu.estoque;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.service.EstoqueService;
import br.edu.estoque.ui.EstoqueFrame;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/** Percorre as telas reais com componentes Swing. Usa um banco exclusivo de testes. */
public final class TesteInterface {
    private static EstoqueFrame frame;
    private static int verificacoes;
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("O teste Swing exige uma sessão gráfica.");
        Path csv = Path.of("build","relatorio-interface-"+UUID.randomUUID()+".csv").toAbsolutePath();
        try {
            BancoDados banco = new BancoDados("jdbc:h2:mem:interface_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
            banco.inicializar(); EstoqueService service = new EstoqueService(banco);
            edt(() -> {
                EstoqueFrame.configurarAparencia();
                frame = new EstoqueFrame(service,"Banco temporário de teste");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            });
            SwingUtilities.invokeLater(frame::atualizar);
            esperar(() -> componentes(frame,JLabel.class).stream().anyMatch(l -> l.getText()!=null && l.getText().startsWith("Atualizado às")));
            verificar(true,"Janela inicial carrega sem erro");

            selecionarAba(2); clicar(pagina(),"Novo fornecedor");
            JDialog fornecedor = esperarDialogo("Novo fornecedor");
            campo(fornecedor,"Nome *","Distribuidora Central"); campo(fornecedor,"Contato","Mariana");
            campo(fornecedor,"Telefone","34 3333-4000"); campo(fornecedor,"E-mail","contato@exemplo.com");
            clicar(fornecedor,"Salvar"); esperar(() -> !fornecedor.isShowing());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==1),"Cadastro de fornecedor pela tela");

            selecionarAba(1); clicar(pagina(),"Novo produto");
            JDialog produto = esperarDialogo("Novo produto");
            campo(produto,"Código único *","CAD-001"); campo(produto,"Nome *","Caderno universitário");
            campo(produto,"Categoria *","Papelaria"); campo(produto,"Preço unitário (R$) *","24,90"); campo(produto,"Estoque mínimo *","8");
            clicar(produto,"Salvar"); esperar(() -> !produto.isShowing());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==1 && tabela(pagina()).getValueAt(0,5).equals(0)),"Cadastro de produto com saldo inicial zero");

            selecionarAba(3); clicar(pagina(),"Registrar entrada");
            JDialog entrada = esperarDialogo("Registrar entrada");
            campo(entrada,"Quantidade *","10"); campo(entrada,"Observação","Compra para demonstração");
            clicar(entrada,"Salvar"); esperar(() -> !entrada.isShowing());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==1),"Entrada atualiza a tabela de histórico");

            clicar(pagina(),"Registrar saída"); JDialog saida = esperarDialogo("Registrar saída");
            campo(saida,"Quantidade *","11"); clicar(saida,"Salvar");
            JDialog erro = esperarDialogo("Verifique os dados");
            verificar(consultar(() -> componentes(erro,JOptionPane.class).stream().anyMatch(p -> String.valueOf(p.getMessage()).contains("Estoque insuficiente"))),"Saída acima do saldo mostra validação");
            clicar(erro,"OK"); esperar(() -> !erro.isShowing());
            campo(saida,"Quantidade *","3"); campo(saida,"Observação","Venda no balcão");
            clicar(saida,"Salvar"); esperar(() -> !saida.isShowing());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==2),"Saída válida preserva histórico de entrada");

            selecionarAba(1);
            edt(() -> tabela(pagina()).setRowSelectionInterval(0,0)); clicar(pagina(),"Editar");
            JDialog editar = esperarDialogo("Editar produto"); campo(editar,"Nome *","Caderno universitário 200 folhas");
            clicar(editar,"Salvar"); esperar(() -> !editar.isShowing());
            verificar(consultar(() -> tabela(pagina()).getValueAt(0,1).equals("Caderno universitário 200 folhas") && tabela(pagina()).getValueAt(0,5).equals(7)),"Edição de produto preserva saldo movimentado");
            edt(() -> componentes(pagina(),JTextField.class).get(0).setText("inexistente"));
            verificar(consultar(() -> tabela(pagina()).getRowCount()==0),"Busca de produtos filtra a tabela");
            edt(() -> componentes(pagina(),JTextField.class).get(0).setText(""));

            selecionarAba(4); clicar(pagina(),"Gerar relatório");
            esperar(() -> botao(pagina(),"Exportar CSV").isEnabled());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==1 && tabela(pagina()).getColumnCount()==9),"Relatório de inventário na interface");
            clicar(pagina(),"Exportar CSV");
            esperar(() -> Arrays.stream(Window.getWindows()).filter(Window::isShowing).anyMatch(w -> !componentes(w,JFileChooser.class).isEmpty()));
            edt(() -> {
                JFileChooser chooser = Arrays.stream(Window.getWindows()).filter(Window::isShowing).flatMap(w -> componentes(w,JFileChooser.class).stream()).findFirst().orElseThrow();
                chooser.setSelectedFile(csv.toFile()); chooser.approveSelection();
            });
            JDialog exportado = esperarDialogo("Exportação concluída"); clicar(exportado,"OK"); esperar(() -> !exportado.isShowing());
            verificar(Files.readString(csv).contains("Caderno universitário 200 folhas"),"Botão de exportação grava o relatório CSV");
            edt(() -> componentes(pagina(),JComboBox.class).get(0).setSelectedIndex(1));
            verificar(consultar(() -> !botao(pagina(),"Exportar CSV").isEnabled()),"Alterar filtros exige gerar um relatório atualizado");
            clicar(pagina(),"Gerar relatório"); esperar(() -> botao(pagina(),"Exportar CSV").isEnabled());
            verificar(consultar(() -> tabela(pagina()).getRowCount()==2),"Relatório de movimentações inclui entrada e saída");
            selecionarAba(0);
            verificar(consultar(() -> tabela(pagina()).getRowCount()==1),"Dashboard alerta saldo abaixo do mínimo");
            edt(() -> {
                try {
                    // Captura o desenho real da janela sem depender da posição do mouse.
                    var imagem = new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = imagem.createGraphics(); frame.printAll(g); g.dispose();
                    ImageIO.write(imagem,"png",new File("build/interface-validada.png"));
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("SUCESSO: "+verificacoes+" verificações Swing. Captura: build/interface-validada.png");
        } finally {
            edt(() -> { for (Window w : Window.getWindows()) w.dispose(); });
            Files.deleteIfExists(csv);
        }
    }

    private static Container pagina() { return (Container)componentes(frame,JTabbedPane.class).get(0).getSelectedComponent(); }
    private static void selecionarAba(int i) throws Exception { edt(() -> componentes(frame,JTabbedPane.class).get(0).setSelectedIndex(i)); }
    private static JTable tabela(Container raiz) { return componentes(raiz,JTable.class).get(0); }
    private static JButton botao(Container raiz, String texto) { return componentes(raiz,JButton.class).stream().filter(b -> texto.equals(b.getText())).findFirst().orElseThrow(); }
    private static void clicar(Container raiz, String texto) throws Exception {
        JButton botao = consultar(() -> botao(raiz,texto));
        SwingUtilities.invokeLater(botao::doClick);
    }
    private static void campo(Container raiz, String rotulo, String valor) throws Exception {
        edt(() -> {
            JLabel l = componentes(raiz,JLabel.class).stream().filter(x -> rotulo.equals(x.getText())).findFirst().orElseThrow();
            ((JTextField)l.getLabelFor()).setText(valor);
        });
    }
    private static JDialog esperarDialogo(String titulo) throws Exception {
        esperar(() -> dialogo(titulo)!=null);
        return consultar(() -> dialogo(titulo));
    }
    private static JDialog dialogo(String titulo) { return Arrays.stream(Window.getWindows()).filter(w -> w instanceof JDialog d && d.isShowing() && d.getTitle().equals(titulo)).map(w -> (JDialog)w).findFirst().orElse(null); }
    private static <T extends Component> List<T> componentes(Container raiz, Class<T> tipo) {
        List<T> lista = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            if (tipo.isInstance(c)) lista.add(tipo.cast(c));
            if (c instanceof Container filho) lista.addAll(componentes(filho,tipo));
        }
        return lista;
    }
    private static void edt(Runnable acao) throws Exception { SwingUtilities.invokeAndWait(acao); }
    private static <T> T consultar(Supplier<T> consulta) throws Exception {
        AtomicReference<T> valor = new AtomicReference<>(); edt(() -> valor.set(consulta.get())); return valor.get();
    }
    private static void esperar(BooleanSupplier condicao) throws Exception {
        long limite = System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime()<limite) {
            if (consultar(condicao::getAsBoolean)) return;
            Thread.sleep(40);
        }
        throw new AssertionError("Tempo esgotado ao aguardar a interface.");
    }
    private static void verificar(boolean condicao, String descricao) {
        if (!condicao) throw new AssertionError(descricao);
        verificacoes++; System.out.println("OK: "+descricao);
    }
}
