import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;

public class App {

    static int indiceAtual = 0;
    static File[] todosOsArquivos;
    static JLabel labelImagem;

    public static void main(String[] args) {
        String caminhoDaPasta = "C:\\Users\\pzane\\OneDrive\\Área de Trabalho\\Projeto Milionário\\Projeto\\imagens";
        File objetoPasta = new File(caminhoDaPasta);
        todosOsArquivos = objetoPasta.listFiles();

        SwingUtilities.invokeLater(() -> criarEmostrarGUI());
    }

    public static void criarEmostrarGUI() {
        JFrame frame = new JFrame("Visualizador de Imagens (em Loop)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        labelImagem = new JLabel();
        labelImagem.setHorizontalAlignment(JLabel.CENTER);

        JButton botaoProximo = new JButton("Próximo");
        JButton botaoAnterior = new JButton("Anterior");

        JPanel painelBotoes = new JPanel();
        painelBotoes.add(botaoAnterior);
        painelBotoes.add(botaoProximo);

        frame.getContentPane().add(labelImagem, BorderLayout.CENTER);
        frame.getContentPane().add(painelBotoes, BorderLayout.SOUTH);

        // --- ACTION LISTENERS COM LÓGICA DE LOOP ---
        botaoProximo.addActionListener(e -> {
            indiceAtual++;
            // Se o índice passar do último item, ele volta para o primeiro (índice 0).
            if (indiceAtual >= todosOsArquivos.length) {
                indiceAtual = 0;
            }
            atualizarImagem();
        });

        botaoAnterior.addActionListener(e -> {
            indiceAtual--;
            // Se o índice ficar menor que o primeiro (0), ele vai para o último.
            if (indiceAtual < 0) {
                indiceAtual = todosOsArquivos.length - 1;
            }
            atualizarImagem();
        });
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    public static void atualizarImagem() {
        String caminhoDoArquivo = todosOsArquivos[indiceAtual].getPath();
        ImageIcon icone = new ImageIcon(caminhoDoArquivo);
        labelImagem.setIcon(icone);
    }
}