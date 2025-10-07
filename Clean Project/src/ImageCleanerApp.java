import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageCleanerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            
            Path pastaImagens = Paths.get("C:\\Users\\pzane\\OneDrive\\Área de Trabalho\\Projeto Milionário\\Clean Project\\imagens");
            Path pastaQuarentena = Paths.get(System.getProperty("user.home"), "QuarentenaGaleria");
            new ViewerFrame(pastaImagens, pastaQuarentena).setVisible(true);
        });
    }
}