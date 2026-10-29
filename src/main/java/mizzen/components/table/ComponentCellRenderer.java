package mizzen.components.table;

import java.util.function.Function;

import mizzen.components.Component;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
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

                // no taller than at most: a column widened past it (to fill the table) leaves room around the value, not a bigger value
                public int heightAt(V value, int width) {
                    return Math.max(1, (int) Math.round(Math.min(width, most) * rowsPerColumn.apply(value)));
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
    private boolean framed = false;

    public ComponentCellRenderer(SizeRule<V> rule, Drawing<V> drawing) {
        this.rule = rule;
        this.drawing = drawing;
    }

    /**
     * A margin of one character all round the picture; selected, the margin frames it in the
     * selection's colour - a line along its edges (renderSelected). For a picture - an image, a
     * drawing - whose row should show it is selected.
     * <pre>
     *     ▗▄▄▄▄▄▄▖
     *     ▐ pic  ▌     the picture where it always is; the frame only when its row is selected
     *     ▝▀▀▀▀▀▀▘
     * </pre>
     */
    public ComponentCellRenderer<V> framedWhenSelected() {
        this.framed = true;
        return this;
    }

    private int margin() {
        return framed ? 1 : 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CellWidths widths(Object value) {
        CellWidths picture = rule.widths((V) value);
        int around = 2 * margin();
        return new CellWidths(picture.cut + around, picture.least + around, picture.most + around);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int heightAt(Object value, int width) {
        return rule.heightAt((V) value, Math.max(1, width - 2 * margin())) + 2 * margin();
    }

    /** value's component at size, inside the margin if framed; a null value: an empty cell */
    @SuppressWarnings("unchecked")
    @Override
    public IScreenLineSegmentsBuffer render(Object value, Dimension size, Align align) {
        int height = size.getHeight() - 2 * margin(), width = size.getWidth() - 2 * margin();
        if (value == null || height <= 0 || width <= 0) {
            return new ScreenLineSegmentsBuffer();
        }
        IScreenLineSegmentsBuffer drawn = drawing.draw((V) value, new Dimension.Simple(height, width)).getSegmentsBuffer();
        if (!framed) {
            return drawn;
        }
        IScreenLineSegmentsBuffer inside = new ScreenLineSegmentsBuffer();
        drawn.getSegments().stream().map((segment) -> segment.fromOffset(new Position.Simple(1, 1))).forEach(inside::add);
        return inside;
    }

    // the frame's pieces: top-left, top, top-right, left, right, bottom-left, bottom, bottom-right
    private static final String SOLID = "▗▄▖▐▌▝▀▘";      // half blocks: a focused selection - a line hugging the picture
    private static final String THIN = "┌─┐││└─┘";       // the selection kept while the table hasn't focus: dimmed, as its text is
    private static final char BLANK_BRAILLE = '\u2800';  // a drawing's empty space: no dots

    /**
     * Selected, a component keeps its own colours - painted over in the selection colour a picture
     * turns to stripes, a meter loses its bar. Framed (framedWhenSelected), the margin frames it:
     * solid while the table has focus, thin while the selection is only kept (dimmed). Otherwise it
     * shows as it is, and the row's other cells show the selection.
     */
    @Override
    public IScreenLineSegmentsBuffer renderSelected(Object value, Dimension size, Align align, Color selected) {
        IScreenLineSegmentsBuffer drawn = render(value, size, align);
        if (!framed || drawn.getSegments().isEmpty()) {
            return drawn;
        }
        int top = Integer.MAX_VALUE, left = Integer.MAX_VALUE, bottom = -1, right = -1;      // the picture's edges: what shows
        for (ScreenLineSegment segment : drawn.getSegments()) {
            String text = segment.getText();
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) != ' ' && text.charAt(i) != BLANK_BRAILLE) {
                    top = Math.min(top, segment.getRow());
                    bottom = Math.max(bottom, segment.getRow());
                    left = Math.min(left, segment.getCol() + i);
                    right = Math.max(right, segment.getCol() + i);
                }
            }
        }
        if (bottom < 0) {
            return drawn;                                                   // nothing shows: nothing to frame
        }
        Color behind = backgroundAsForeground(selected);
        String frame = behind != null ? SOLID : THIN;
        Color line = behind != null ? behind : selected;
        IScreenLineSegmentsBuffer withFrame = new ScreenLineSegmentsBuffer();
        drawn.getSegments().forEach(withFrame::add);
        String across = new String(new char[right - left + 1]);
        withFrame.add(new SegmentImpl(line, new Position.Simple(top - 1, left - 1), frame.charAt(0) + across.replace('\0', frame.charAt(1)) + frame.charAt(2)));
        withFrame.add(new SegmentImpl(line, new Position.Simple(bottom + 1, left - 1), frame.charAt(5) + across.replace('\0', frame.charAt(6)) + frame.charAt(7)));
        for (int row = top; row <= bottom; row++) {
            withFrame.add(new SegmentImpl(line, new Position.Simple(row, left - 1), String.valueOf(frame.charAt(3))));
            withFrame.add(new SegmentImpl(line, new Position.Simple(row, right + 1), String.valueOf(frame.charAt(4))));
        }
        return withFrame;
    }

    /**
     * A focused selection is a background (black on bright yellow), and a half block shows its
     * foreground: the background colour as a foreground, read from its codes - background 40-47 is
     * foreground 30-37, 100-107 is 90-97. null: the colour has no background - the selection is only
     * kept (dimmed), its colour a foreground already.
     * <pre>
     *     ESC[0;30;103m (black on bright yellow)    ESC[93m (bright yellow)
     *     ESC[93m (bright yellow)                   null
     * </pre>
     */
    private static Color backgroundAsForeground(Color selected) {
        String codes = selected.asString();
        int start = codes.indexOf('['), end = codes.lastIndexOf('m');
        if (start < 0 || end < start) {
            return null;
        }
        for (String code : codes.substring(start + 1, end).split(";")) {
            try {
                int value = Integer.parseInt(code);
                if (value >= 40 && value <= 47 || value >= 100 && value <= 107) {
                    return Colors.FgColor.instance(value - 10);
                }
            } catch (NumberFormatException notACode) {
                return null;
            }
        }
        return null;
    }
}
