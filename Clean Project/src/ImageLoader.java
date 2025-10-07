import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ImageLoader {

    public static Image scaleToFit(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth(), h = src.getHeight();
        double r = Math.min(maxW / (double) w, maxH / (double) h);
        if (r >= 1.0) return src;
        int nw = Math.max(1, (int) Math.round(w * r));
        int nh = Math.max(1, (int) Math.round(h * r));

        Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return out;
    }

    public static String infoCurta(Path p, long size, int index, int total) {
        String base = String.format(Locale.ROOT, "[%d/%d] %s", index + 1, total, p.getFileName());
        if (size >= 0) base += " • " + humanSize(size);
        try { base += " • Modificado: " + Files.getLastModifiedTime(p); } catch (IOException ignored) {}
        return base;
    }

    public static String humanSize(long bytes) {
        if (bytes < 0) return "?";
        String[] u = {"B","KB","MB","GB","TB"};
        double b = bytes; int ix = 0;
        while (b >= 1024 && ix < u.length - 1) { b /= 1024.0; ix++; }
        return String.format(Locale.ROOT, "%.1f %s", b, u[ix]);
    }
}