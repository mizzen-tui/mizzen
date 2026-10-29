package mizzen.components.table;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
import mizzen.textsegments.ScreenLineSegmentsBuffer;

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

    /**
     * value drawn as a cell of the selected row, in selected (the table's selection colour, focused or
     * not). By default as render draws it with every segment in selected - right for text. A
     * component keeps its own colours (ComponentCellRenderer.renderSelected): an image or a drawing
     * framed, a meter or a sparkline as it is.
     */
    default IScreenLineSegmentsBuffer renderSelected(Object value, Dimension size, Align align, Color selected) {
        return inColor(render(value, size, align), selected);
    }

    /** every segment of drawn in color */
    static IScreenLineSegmentsBuffer inColor(IScreenLineSegmentsBuffer drawn, Color color) {
        IScreenLineSegmentsBuffer colored = new ScreenLineSegmentsBuffer();
        for (ScreenLineSegment segment : drawn.getSegments()) {
            colored.add(new SegmentImpl(color, new Position.Simple(segment.getRow(), segment.getCol()), segment.getId(), segment.getText()));
        }
        return colored;
    }
}
