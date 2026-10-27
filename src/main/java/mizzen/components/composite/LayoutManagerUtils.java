package mizzen.components.composite;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import mizzen.components.Component;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

// TODO:  Does not work yet - but keep trying ... will eventually be useful
public class LayoutManagerUtils {

    public static void distributeComponentsEvenly(Composite parent) {
        List<Component<?>> components = new ArrayList<Component<?>>(parent.getChildrenList());
        distributeComponentsEvenly(parent, components);
    }

    public static void distributeComponentsEvenly(Composite parent, Predicate<Component<?>> childFilter) {
        List<Component<?>> components = new ArrayList<Component<?>>(parent.getChildrenList()
                                                               .stream().filter(childFilter).collect(Collectors.toList()));
        distributeComponentsEvenly(parent, components);
    }

    public static void distributeComponentsEvenly(Composite parent, Predicate<Component<?>> childFilter, Position offset, Dimension boundary) {
        List<Component<?>> components = new ArrayList<Component<?>>(parent.getChildrenList()
                                                               .stream().filter(childFilter).collect(Collectors.toList()));
        distributeComponentsEvenly(parent, components, offset, boundary);
    }

    public static void distributeComponentsEvenly(Composite parent, List<Component<?>> components) {
        distributeComponentsEvenly(parent, components, Position.ZERO, parent);
    }
  
    public static void distributeComponentsEvenly(Composite parent, List<Component<?>> components, Position offset, Dimension boundary) {
        // Sort components by size in descending order
        components.sort(Comparator.comparingInt(c -> -c.getHeight() * c.getWidth()));

        Rectangle bounds = new Rectangle.Simple(offset, boundary);

        int currentRow = offset.getRow();
        int currentCol = offset.getCol();
        int maxHeightInRow = 0;

        for (Component<?> component : components) {
            // If component doesn't fit in the remaining space in the row, move to the next row
            if (currentCol + component.getWidth() > parent.getWidth()) {
                currentRow += maxHeightInRow;
                currentCol = offset.getCol();
                maxHeightInRow = 0;
            }

            // Move the component to the current position
            parent.moveChildTo(currentRow, currentCol, component);

            // Update the current position and the max height in the current row
            currentCol += component.getWidth();
            maxHeightInRow = Math.max(maxHeightInRow, component.getHeight());

            // If we've reached the end of the canvas, reset to the top and continue to fill in gaps
            if (currentRow + maxHeightInRow > parent.getHeight()) {
                currentRow = offset.getRow();
                currentCol = offset.getCol();
                maxHeightInRow = 0;
            }
        }
    }

    public static void distributeComponentsStaggered(Composite parent) {
        List<Component<?>> components = new ArrayList<Component<?>>(parent.getChildrenList());
        distributeComponentsStaggered(parent, components);
    }

    public static void distributeComponentsStaggered(Composite parent, Predicate<Component<?>> childFilter) {
        List<Component<?>> components = new ArrayList<Component<?>>(parent.getChildrenList()
                                                               .stream().filter(childFilter).collect(Collectors.toList()));
        distributeComponentsStaggered(parent, components);
    }
    public static void distributeComponentsStaggered(Composite parent, List<Component<?>> components) {
        distributeComponentsStaggered(parent, components, Position.ZERO, parent);
    }

    public static void distributeComponentsStaggered(Composite parent, List<Component<?>> components, Position offset, Dimension boundary) {
        // Sort components by size in descending order
        components.sort(Comparator.comparingInt(c -> -c.getHeight() * c.getWidth()));

        int staggeredRow = 0;
        int staggeredCol = 0;

        for (Component<?> component : components) {
            // Move the component to the current staggered position
            component.moveTo(staggeredRow, staggeredCol);
            parent.moveChildTo(staggeredRow, staggeredCol, component);

            // Update the staggered position
            staggeredRow += component.getHeight();
            staggeredCol += component.getWidth();

            // If we've reached the end of the canvas, reset to the top and continue to fill in gaps
            if (staggeredRow + component.getHeight() > parent.getHeight() || staggeredCol + component.getWidth() > parent.getWidth()) {
                staggeredRow = 0;
                staggeredCol = 0;
            }
        }
    }    

}
