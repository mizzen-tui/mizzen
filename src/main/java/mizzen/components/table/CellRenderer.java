package mizzen.components.table;

import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

/**
 * How a column's cells are drawn, and the room they take. A cell is a rectangle: the table asks how
 * wide a value can be drawn and how tall it is at a width, then for exactly that rectangle of
 * segments.
 * <pre>
 *     widths("Ben is a pilot in Lyon")      cut 1, least 5 ("pilot"), most 22
 *     heightAt("Ben is a pilot in Lyon", 9)  3: "Ben is a" / "pilot in" / "Lyon"
 *     render(.., 3 x 9, LEFT)                those three lines, nothing outside 3 x 9
 * </pre>
 * Text, a drawing, an image, a bar, a table: whatever draws a value as segments - TextCellRenderer,
 * ComponentCellRenderer.
 * <p/>
 * One pass, from the outside in: the table asks, the renderer draws a picture of the value. A
 * renderer never redraws on its own, takes events or tells anything it changed - a component it
 * draws with is never in the component tree.
 */
public interface CellRenderer {

    /** how wide value can be drawn: clipped, whole, all it wants */
    CellWidths widths(Object value);

    /** how tall value is drawn this wide - text wrapped to the width, a drawing by its aspect */
    int heightAt(Object value, int width);

    /** value drawn in exactly size - no segment outside it; align: where in it, if it is narrower */
    IScreenLineSegmentsBuffer render(Object value, Dimension size, Align align);
}
