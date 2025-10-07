import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ViewerFrame extends JFrame {

    private final Path pastaImagens;
    private final QuarantineService quarantine;

    private final JLabel labelImagem = new JLabel("Carregando…", SwingConstants.CENTER);
    private final JLabel labelInfo   = new JLabel(" ");
    private final JButton btnAnterior = new JButton("Anterior");
    private final JButton btnProximo  = new JButton("Próximo");
    private final JButton btnMoverQuarentena = new JButton("Mover p/ Quarentena");
    private final JButton btnRemoverDaLista  = new JButton("Remover da Lista");
    private final JProgressBar progress = new JProgressBar();

    private ImageList imageList = new ImageList(List.of());

    public ViewerFrame(Path pastaImagens, Path pastaQuarentena) {
        super("Visualizador de Imagens (com Quarentena)");
        this.pastaImagens = pastaImagens;
        this.quarantine = new QuarantineService(pastaQuarentena);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);

        montarUI();
        configurarAtalhos();
        carregarListaInicial();
    }

    private void montarUI() {
        labelImagem.setOpaque(true);
        labelImagem.setBackground(Color.DARK_GRAY);
        labelImagem.setForeground(Color.LIGHT_GRAY);

        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(new EmptyBorder(8, 8, 8, 8));
        topo.add(labelInfo, BorderLayout.WEST);

        JPanel barra = new JPanel(new GridBagLayout());
        barra.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.gridy = 0;
        c.gridx = 0; barra.add(btnAnterior, c);
        c.gridx = 1; barra.add(btnProximo, c);
        c.gridx = 2; barra.add(btnMoverQuarentena, c);
        c.gridx = 3; barra.add(btnRemoverDaLista, c);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 4; c.fill = GridBagConstraints.HORIZONTAL;
        barra.add(progress, c);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topo, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(labelImagem,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        getContentPane().add(barra, BorderLayout.SOUTH);

        btnAnterior.addActionListener(e -> navegar(-1));
        btnProximo.addActionListener(e -> navegar(+1));
        btnMoverQuarentena.addActionListener(this::moverAtualParaQuarentena);
        btnRemoverDaLista.addActionListener(this::removerAtualDaLista);
    }

    private void configurarAtalhos() {
        InputMap im = labelImagem.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = labelImagem.getActionMap();
        im.put(KeyStroke.getKeyStroke("RIGHT"), "next");
        am.put("next", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { navegar(+1); }});
        im.put(KeyStroke.getKeyStroke("LEFT"), "prev");
        am.put("prev", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { navegar(-1); }});
        labelImagem.addMouseWheelListener(e -> navegar(e.getWheelRotation() > 0 ? +1 : -1));
    }

    private void carregarListaInicial() {
        toggleControles(false);
        progress.setIndeterminate(true);
        labelImagem.setText("Indexando imagens…");

        new SwingWorker<List<Path>, Void>() {
            @Override protected List<Path> doInBackground() throws Exception {
                return ImageScanner.scanFlat(pastaImagens);
            }
            @Override protected void done() {
                progress.setIndeterminate(false);
                try {
                    List<Path> itens = get();
                    imageList = new ImageList(itens);
                    if (imageList.isEmpty()) {
                        labelImagem.setText("Nenhuma imagem encontrada em:\n" + pastaImagens);
                        labelInfo.setText(" ");
                    } else {
                        atualizarImagemAsync();
                    }
                } catch (Exception ex) {
                    labelImagem.setText("Erro ao listar imagens: " + ex.getMessage());
                    labelInfo.setText(" ");
                } finally {
                    toggleControles(!imageList.isEmpty());
                }
            }
        }.execute();
    }

    private void navegar(int delta) {
        if (imageList.isEmpty()) return;
        imageList.move(delta);
        atualizarImagemAsync();
    }

    private void atualizarImagemAsync() {
        if (imageList.isEmpty()) return;
        final Path caminho = imageList.current();

        toggleControles(false);
        progress.setIndeterminate(true);
        labelImagem.setText("Carregando: " + caminho.getFileName());
        labelImagem.setIcon(null);

        final Dimension alvo = calcularTamanhoAlvo();

        new SwingWorker<ImageIcon, Void>() {
            long size = -1L;
            @Override protected ImageIcon doInBackground() {
                try {
                    size = Files.size(caminho);
                    BufferedImage raw = ImageIO.read(caminho.toFile());
                    if (raw == null) return null;
                    return new ImageIcon(ImageLoader.scaleToFit(raw, alvo.width, alvo.height));
                } catch (IOException e) {
                    return null;
                }
            }
            @Override protected void done() {
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
                    labelInfo.setText(ImageLoader.infoCurta(caminho, size, imageList.index(), imageList.size()));
                } catch (Exception ex) {
                    labelImagem.setText("Erro: " + ex.getMessage());
                    labelImagem.setIcon(null);
                } finally {
                    toggleControles(!imageList.isEmpty());
                }
            }
        }.execute();
    }

    private void moverAtualParaQuarentena(ActionEvent e) {
        if (imageList.isEmpty()) return;
        Path alvo = imageList.current();
        int opt = JOptionPane.showConfirmDialog(this,
                "Mover para Quarentena?\n" + alvo.getFileName(),
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        try {
            Path destino = quarantine.moveToQuarantine(alvo);
            imageList.removeCurrent();
            JOptionPane.showMessageDialog(this, "Movido para: " + destino,
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            if (imageList.isEmpty()) {
                labelImagem.setIcon(null);
                labelImagem.setText("Lista vazia.");
                labelInfo.setText(" ");
                toggleControles(false);
            } else {
                atualizarImagemAsync();
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Falha ao mover: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removerAtualDaLista(ActionEvent e) {
        if (imageList.isEmpty()) return;
        Path alvo = imageList.current();
        int opt = JOptionPane.showConfirmDialog(this,
                "Remover APENAS da lista (não apaga do disco)?\n" + alvo.getFileName(),
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            imageList.removeCurrent();
            if (imageList.isEmpty()) {
                labelImagem.setIcon(null);
                labelImagem.setText("Lista vazia.");
                labelInfo.setText(" ");
                toggleControles(false);
            } else {
                atualizarImagemAsync();
            }
        }
    }

    private void toggleControles(boolean on) {
        btnAnterior.setEnabled(on);
        btnProximo.setEnabled(on);
        btnMoverQuarentena.setEnabled(on);
        btnRemoverDaLista.setEnabled(on);
    }

    private Dimension calcularTamanhoAlvo() {
        int w = labelImagem.getWidth();
        int h = labelImagem.getHeight();
        if (w <= 0 || h <= 0) return new Dimension(860, 520);
        return new Dimension(Math.max(200, w - 16), Math.max(200, h - 16));
    }
}