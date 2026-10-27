package mizzen.geom;

import java.util.ArrayList;
import java.util.List;

public class QuadTree<T extends Rectangle2D> {

    private static final int DEFAULT_MAX_OBJECTS = 10;
    private static final int DEFAULT_MAX_LEVELS = 4;

    private final int maxObjects;
    private final int maxLevels;
    private final int level;
    private final Rectangle2D bounds;
    private final List<T> objects;
    private final List<QuadTree<T>> nodes;

    public QuadTree(Rectangle2D bounds) {
        this(bounds, DEFAULT_MAX_OBJECTS, DEFAULT_MAX_LEVELS, 0);
    }

    public QuadTree(Rectangle2D bounds, int maxObjects, int maxLevels, int level) {
        this.bounds = bounds;
        this.maxObjects = maxObjects;
        this.maxLevels = maxLevels;
        this.level = level;
        this.objects = new ArrayList<>();
        this.nodes = new ArrayList<>(4); // 4 nodes for quadtree
    }

    // Splits the node into 4 sub-nodes
    private void split() {
        float subWidth = bounds.getWidth() / 2;
        float subHeight = bounds.getHeight() / 2;
        float x = bounds.getX1();
        float y = bounds.getY1();

        // Create 4 child nodes
        nodes.add(new QuadTree<>(new Rectangle2D.Float2D(x + subWidth, y, x + 2 * subWidth, y + subHeight),
                maxObjects, maxLevels, level + 1));
        nodes.add(new QuadTree<>(new Rectangle2D.Float2D(x, y, x + subWidth, y + subHeight),
                maxObjects, maxLevels, level + 1));
        nodes.add(new QuadTree<>(new Rectangle2D.Float2D(x, y + subHeight, x + subWidth, y + 2 * subHeight),
                maxObjects, maxLevels, level + 1));
        nodes.add(new QuadTree<>(new Rectangle2D.Float2D(x + subWidth, y + subHeight, x + 2 * subWidth, y + 2 * subHeight),
                maxObjects, maxLevels, level + 1));
    }

    // Determine which nodes an object intersects
    private List<Integer> getIndexes(Rectangle2D rect) {
        List<Integer> indexes = new ArrayList<>();

        float verticalMidpoint = bounds.getX1() + (bounds.getWidth() / 2);
        float horizontalMidpoint = bounds.getY1() + (bounds.getHeight() / 2);

        boolean startIsNorth = rect.getY1() < horizontalMidpoint;
        boolean startIsWest = rect.getX1() < verticalMidpoint;
        boolean endIsEast = rect.getX2() > verticalMidpoint;
        boolean endIsSouth = rect.getY2() > horizontalMidpoint;

        if (startIsNorth && endIsEast) indexes.add(0); // Top-right
        if (startIsWest && startIsNorth) indexes.add(1); // Top-left
        if (startIsWest && endIsSouth) indexes.add(2); // Bottom-left
        if (endIsEast && endIsSouth) indexes.add(3); // Bottom-right

        return indexes;
    }

    // Inserts a rectangle into the quadtree
    public void insert(T rect) {
        if (!nodes.isEmpty()) {
            for (int index : getIndexes(rect)) {
                nodes.get(index).insert(rect);
            }
            return;
        }

        objects.add(rect);

        if (objects.size() > maxObjects && level < maxLevels) {
            if (nodes.isEmpty()) {
                split();
            }

            List<T> toReinsert = new ArrayList<>(objects);
            objects.clear();
            for (T obj : toReinsert) {
                for (int index : getIndexes(obj)) {
                    nodes.get(index).insert(obj);
                }
            }
        }
    }

    // Retrieves all objects that could collide with the given rectangle
    public List<T> retrieve(Rectangle2D rect) {
        List<T> results = new ArrayList<>(objects);

        if (!nodes.isEmpty()) {
            for (int index : getIndexes(rect)) {
                results.addAll(nodes.get(index).retrieve(rect));
            }
        }

        return results;
    }

    // Clears the quadtree
    public void clear() {
        objects.clear();
        for (QuadTree<T> node : nodes) {
            node.clear();
        }
        nodes.clear();
    }
}
