import java.io.IOException;
import java.nio.file.*;

public class QuarantineService {
    private final Path quarantineDir;

    public QuarantineService(Path quarantineDir) {
        this.quarantineDir = quarantineDir;
    }

    public Path moveToQuarantine(Path source) throws IOException {
        Files.createDirectories(quarantineDir);
        Path target = resolveUnique(quarantineDir.resolve(source.getFileName()));
        return Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // Gera um nome único se já existir (arquivo (2).ext, arquivo (3).ext, …)
    private Path resolveUnique(Path target) throws IOException {
        if (!Files.exists(target)) return target;
        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext  = (dot > 0) ? name.substring(dot) : "";
        int i = 2;
        while (true) {
            Path alt = target.getParent().resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(alt)) return alt;
            i++;
        }
    }
}