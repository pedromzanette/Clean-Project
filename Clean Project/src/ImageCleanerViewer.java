import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImageCleanerViewer extends JFrame {

    // ---------- Configuração ----------
    // Ajuste este caminho para a pasta que você quer visualizar:
    private final Path pastaImagens = Paths.get("C:\\Users\\pzane\\OneDrive\\Área de Trabalho\\Projeto Milionário\\Clean Project\\imagens");
    // Pasta de "Quarentena" (lixeira do app):
    private final Path pastaQuarentena = Paths.get(System.getProperty("user.home"), "QuarentenaGaleria");

    // ---------- Estado ----------
    private List<Path> arquivosImagem = new ArrayList<>();
    private int indiceAtual = 0;

    // ---------- UI ----------
    private final JLabel labelImagem = new JLabel("Carregando…", SwingConstants.CENTER);
    private final JLabel labelInfo = new JLabel(" ");
    private final JButton btnAnterior = new JButton("Anterior");
    private final JButton btnProximo = new JButton("Próximo");
    private final JButton btnMoverLixeira = new JButton("Mover p/ Lixeira");
    private final JButton btnRemoverDaLista = new JButton("Remover da Lista");
    private final JProgressBar progress = new JProgressBar();

    public ImageCleanerViewer() {
        super("Visualizador de Imagens (com Quarentena)");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);

        // Centro
        labelImagem.setOpaque(true);
        labelImagem.setBackground(Color.DARK_GRAY);
        labelImagem.setForeground(Color.LIGHT_GRAY);

        // Barra inferior (controles)
        JPanel barra = new JPanel(new GridBagLayout());
        barra.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.gridy = 0;

        c.gridx = 0; barra.add(btnAnterior, c);
        c.gridx = 1; barra.add(btnProximo, c);
        c.gridx = 2; barra.add(btnMoverLixeira, c);
        c.gridx = 3; barra.add(btnRemoverDaLista, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 4; c.fill = GridBagConstraints.HORIZONTAL;
        barra.add(progress, c);

        // Barra superior (info)
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(new EmptyBorder(8, 8, 8, 8));
        topo.add(labelInfo, BorderLayout.WEST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topo, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(labelImagem,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        getContentPane().add(barra, BorderLayout.SOUTH);

        // Ações
        btnAnterior.addActionListener(e -> navegar(-1));
        btnProximo.addActionListener(e -> navegar(+1));
        btnMoverLixeira.addActionListener(this::moverAtualParaQuarentena);
        btnRemoverDaLista.addActionListener(this::removerAtualDaLista);

        // Atalhos de teclado (setas esquerda/direita)
        InputMap im = labelImagem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = labelImagem.getActionMap();
        im.put(KeyStroke.getKeyStroke("RIGHT"), "next");
        am.put("next", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navegar(+1); }
        });
        im.put(KeyStroke.getKeyStroke("LEFT"), "prev");
        am.put("prev", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navegar(-1); }
        });

        // Scroll do mouse para navegar
        labelImagem.addMouseWheelListener(e -> navegar(e.getWheelRotation() > 0 ? +1 : -1));

        // Carrega a lista e mostra a primeira imagem
        carregarListaInicial();
    }

    private void carregarListaInicial() {
        progress.setIndeterminate(true);
        labelImagem.setText("Indexando imagens…");
        SwingWorker<List<Path>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Path> doInBackground() throws Exception {
                if (!Files.isDirectory(pastaImagens)) return List.of();
                try (Stream<Path> s = Files.list(pastaImagens)) {
                    return s.filter(p -> Files.isRegularFile(p))
                            .filter(p -> isImagem(p.getFileName().toString()))
                            .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            @Override
            protected void done() {
                progress.setIndeterminate(false);
                try {
                    arquivosImagem = get();
                } catch (Exception ex) {
                    arquivosImagem = List.of();
                    JOptionPane.showMessageDialog(ImageCleanerViewer.this,
                            "Erro ao listar imagens: " + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
                if (arquivosImagem.isEmpty()) {
                    labelImagem.setText("Nenhuma imagem encontrada na pasta:\n" + pastaImagens);
                    toggleControles(false);
                } else {
                    toggleControles(true);
                    indiceAtual = 0;
                    atualizarImagemAsync();
                }
            }
        };
        worker.execute();
    }

    private void navegar(int delta) {
        if (arquivosImagem.isEmpty()) return;
        indiceAtual = (indiceAtual + delta) % arquivosImagem.size();
        if (indiceAtual < 0) indiceAtual += arquivosImagem.size();
        atualizarImagemAsync();
    }

    private void atualizarImagemAsync() {
        if (arquivosImagem.isEmpty()) return;
        final Path caminho = arquivosImagem.get(indiceAtual);

        // desabilita botões enquanto carrega
        toggleControles(false);
        progress.setIndeterminate(true);
        labelImagem.setText("Carregando: " + caminho.getFileName());
        labelImagem.setIcon(null);

        Dimension alvo = calcularTamanhoAlvo();

        SwingWorker<ImageIcon, Void> loader = new SwingWorker<>() {
            long size = -1L;
            @Override
            protected ImageIcon doInBackground() {
                try {
                    size = Files.size(caminho);
                    BufferedImage raw = ImageIO.read(caminho.toFile());
                    if (raw == null) return null;
                    Image scaled = scaleToFit(raw, alvo.width, alvo.height);
                    return new ImageIcon(scaled);
                } catch (IOException e) {
                    return null;
                }
            }
            @Override
            protected void done() {
                progress.setIndeterminate(false);
                try {
                    ImageIcon icon = get();
                    if (icon == null) {
                        labelImagem.setText("Arquivo não suportado: " + caminho.getFileName());
                        labelImagem.setIcon(null);
                    } else {
                        labelImagem.setIcon(icon);
                        labelImagem.setText(null);
                    }
                    labelInfo.setText(infoCurta(caminho, size));
                } catch (Exception ex) {
                    labelImagem.setText("Erro: " + ex.getMessage());
                    labelImagem.setIcon(null);
                } finally {
                    toggleControles(true);
                }
            }
        };
        loader.execute();
    }

    private void moverAtualParaQuarentena(ActionEvent e) {
        if (arquivosImagem.isEmpty()) return;
        Path alvo = arquivosImagem.get(indiceAtual);

        int opt = JOptionPane.showConfirmDialog(this,
                "Mover para Quarentena?\n" + alvo.getFileName(),
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        try {
            Files.createDirectories(pastaQuarentena);
            Path destino = pastaQuarentena.resolve(alvo.getFileName());
            Files.move(alvo, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            removerDaListaSemPerguntar();
            JOptionPane.showMessageDialog(this,
                    "Movido para: " + destino,
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Falha ao mover: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerAtualDaLista(ActionEvent e) {
        if (arquivosImagem.isEmpty()) return;
        Path alvo = arquivosImagem.get(indiceAtual);
        int opt = JOptionPane.showConfirmDialog(this,
                "Remover APENAS da lista (não apaga do disco)?\n" + alvo.getFileName(),
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            removerDaListaSemPerguntar();
        }
    }

    private void removerDaListaSemPerguntar() {
        if (arquivosImagem.isEmpty()) return;
        arquivosImagem.remove(indiceAtual);
        if (arquivosImagem.isEmpty()) {
            labelImagem.setIcon(null);
            labelImagem.setText("Lista vazia.");
            labelInfo.setText(" ");
            toggleControles(false);
        } else {
            if (indiceAtual >= arquivosImagem.size()) indiceAtual = 0;
            atualizarImagemAsync();
        }
    }

    private void toggleControles(boolean habilitar) {
        btnAnterior.setEnabled(habilitar);
        btnProximo.setEnabled(habilitar);
        btnMoverLixeira.setEnabled(habilitar);
        btnRemoverDaLista.setEnabled(habilitar);
    }

    private static boolean isImagem(String nome) {
        String n = nome.toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                || n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp");
    }

    private Dimension calcularTamanhoAlvo() {
        // Usa o tamanho atual do label; se zero (primeiro paint), usa um default confortável
        int w = labelImagem.getWidth();
        int h = labelImagem.getHeight();
        if (w <= 0 || h <= 0) return new Dimension(860, 520);
        // Dá uma folga para bordas/scroll
        return new Dimension(Math.max(200, w - 16), Math.max(200, h - 16));
    }

    private static Image scaleToFit(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        double rw = maxW / (double) w;
        double rh = maxH / (double) h;
        double r = Math.min(rw, rh);
        if (r >= 1.0) return src; // já cabe
        int nw = Math.max(1, (int) Math.round(w * r));
        int nh = Math.max(1, (int) Math.round(h * r));
        Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);

        // Opcional: materializar em BufferedImage para melhor qualidade no Swing (evita reescala on-the-fly)
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return out;
    }

    private static String humanSize(long bytes) {
        if (bytes < 0) return "?";
        String[] u = {"B","KB","MB","GB","TB"};
        double b = bytes;
        int ix = 0;
        while (b >= 1024 && ix < u.length - 1) { b /= 1024.0; ix++; }
        return String.format(Locale.ROOT, "%.1f %s", b, u[ix]);
    }

    private String infoCurta(Path p, long size) {
        String base = String.format("[%d/%d] %s",
                arquivosImagem.isEmpty() ? 0 : (indiceAtual + 1),
                arquivosImagem.size(),
                p.getFileName());
        if (size >= 0) base += " • " + humanSize(size);
        try {
            base += " • Modificado: " + Files.getLastModifiedTime(p);
        } catch (IOException ignored) {}
        return base;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Look & Feel do sistema (fica mais nativo)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ImageCleanerViewer().setVisible(true);
        });
    }
}