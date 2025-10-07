import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImageList {
    private final List<Path> items;
    private int index = 0;

    public ImageList(List<Path> items) {
        this.items = new ArrayList<>(items);
        this.index = 0;
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public int size() { return items.size(); }
    public int index() { return isEmpty() ? -1 : index; }
    public Path current() { return isEmpty() ? null : items.get(index); }

    public void move(int delta) {
        if (isEmpty()) return;
        index = (index + delta) % items.size();
        if (index < 0) index += items.size();
    }

    public void removeCurrent() {
        if (isEmpty()) return;
        items.remove(index);
        if (!items.isEmpty() && index >= items.size()) index = 0;
    }
}