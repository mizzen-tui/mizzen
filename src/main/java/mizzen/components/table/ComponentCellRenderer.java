package mizzen.components.table;

import java.util.function.Function;

import mizzen.components.Component;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegmentsBuffer;

/**
 * A component drawn in a cell - a drawing, an image, a meter, a sparkline: the component shows the
 * value at the cell's size, and its segments are the cell.
 * <pre>
 *     PlotArea area = new PlotArea(Colors.Gre).showing(-1.3, 1.3, -1.3, 1.3).axes(false);
 *     new ComponentCellRenderer&lt;Part&gt;(SizeRule.byAspect((p) -&gt; 0.5, 6, 20),
 *             (part, size) -&gt; { area.onDraw((pen) -&gt; pen.draw(part.drawing.get())); area.resize(size); return area; });
 * </pre>
 * Components have no size they want - they fill what they are given. So the size comes from the
 * rule: by aspect (a drawing, an image), a fixed height (a bar), or one of its own. The component
 * just fills the cell.
 * <p/>
 * A picture of the component: it is drawn here, off to one side, never put in the component tree -
 * no events, no focus. A component that draws later (on a timer, when something loads) shows
 * nothing here: give it one that draws when asked (e.g. ImageViewerFactory.fromImage, not
 * ResizableImageViewer).
 */
public class ComponentCellRenderer<V> implements CellRenderer {

    /** how big a value's cell is - the component's own idea of size never counts */
    public interface SizeRule<V> {

        CellWidths widths(V value);

        int heightAt(V value, int width);

        /** height from width by the value's shape: rowsPerColumn 0.5 is a square drawn in braille or blocks (a character is twice as tall as wide) */
        static <V> SizeRule<V> byAspect(Function<V, Double> rowsPerColumn, int least, int most) {
            return new SizeRule<V>() {
                public CellWidths widths(V value) {
                    return new CellWidths(least, least, most);      // drawn smaller, never clipped
                }

                public int heightAt(V value, int width) {
                    return Math.max(1, (int) Math.round(width * rowsPerColumn.apply(value)));
                }
            };
        }

        /** always height high, from least to most wide - a bar, a sparkline */
        static <V> SizeRule<V> fixedHeight(int height, int least, int most) {
            return new SizeRule<V>() {
                public CellWidths widths(V value) {
                    return new CellWidths(least, least, most);
                }

                public int heightAt(V value, int width) {
                    return height;
                }
            };
        }
    }

    /** the component showing a value, sized to size */
    public interface Drawing<V> {
        Component<?> draw(V value, Dimension size);
    }

    private final SizeRule<V> rule;
    private final Drawing<V> drawing;

    public ComponentCellRenderer(SizeRule<V> rule, Drawing<V> drawing) {
        this.rule = rule;
        this.drawing = drawing;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CellWidths widths(Object value) {
        return rule.widths((V) value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int heightAt(Object value, int width) {
        return rule.heightAt((V) value, width);
    }

    /** value's component at size; a null value: an empty cell */
    @SuppressWarnings("unchecked")
    @Override
    public IScreenLineSegmentsBuffer render(Object value, Dimension size, Align align) {
        if (value == null || size.getHeight() == 0 || size.getWidth() == 0) {
            return new ScreenLineSegmentsBuffer();
        }
        return drawing.draw((V) value, size).getSegmentsBuffer();
    }
}
