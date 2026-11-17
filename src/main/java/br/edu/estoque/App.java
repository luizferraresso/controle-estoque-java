package br.edu.estoque;

import br.edu.estoque.db.BancoDados;
import br.edu.estoque.service.EstoqueService;
import br.edu.estoque.ui.EstoqueFrame;
import javax.swing.*;
import java.nio.file.*;

public final class App {
    private App() { }
    public static void main(String[] args) {
        try {
            Path pasta = Path.of(System.getProperty("estoque.dados", "dados")).toAbsolutePath().normalize();
            Files.createDirectories(pasta);
            BancoDados banco = new BancoDados("jdbc:h2:file:" + pasta.resolve("estoque").toString().replace('\\','/') + ";LOCK_TIMEOUT=10000");
            banco.inicializar();
            EstoqueService service = new EstoqueService(banco);
            SwingUtilities.invokeLater(() -> {
                EstoqueFrame.configurarAparencia();
                EstoqueFrame frame = new EstoqueFrame(service,pasta.resolve("estoque.mv.db").toString());
                frame.setVisible(true);
                frame.atualizar();
            });
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,"Não foi possível iniciar a aplicação.\nVerifique a instalação do Java, o driver H2, a permissão de gravação\ne se outra instância está usando o mesmo banco.\n\n" + e.getMessage(),"Falha ao iniciar",JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
