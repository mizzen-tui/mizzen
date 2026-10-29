package mizzen.components.table;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fits columns into a table's width, from what each column's cells measure (CellWidths) - nothing
 * else: no text, no rows. In this order:
 * <pre>
 *     1. all at their most fit       widen them to fill, in proportion
 *     2. squeeze                     towards their least, the one with most to spare first
 *     3. drop                        droppable columns, lowest order first (then right to left), fitting again after each
 *     4. cut                         towards their cut, right to left
 * </pre>
 * Still too wide after that, the table cuts the last columns at its border.
 * <p/>
 * Columns dropped before (held: a table keeps them dropped until it is resized) are left out from
 * the start, so scrolling never brings a column back and drops it again.
 */
final class ColumnFitting {

    /** one column to fit: index - where it is among all the columns; dropOrder null: never dropped */
    static final class Column {
        final int index;
        final CellWidths widths;
        final Integer dropOrder;

        Column(int index, CellWidths widths, Integer dropOrder) {
            this.index = index;
            this.widths = widths;
            this.dropOrder = dropOrder;
        }
    }

    /** the columns shown, left to right (their index), each one's width; and every column dropped, held ones too */
    static final class Fit {
        final List<Integer> shown;
        final int[] widths;
        final Set<Integer> dropped;

        Fit(List<Integer> shown, int[] widths, Set<Integer> dropped) {
            this.shown = shown;
            this.widths = widths;
            this.dropped = dropped;
        }
    }

    private static final Comparator<Column> DROP_ORDER = Comparator.<Column>comparingInt((c) -> c.dropOrder)
            .thenComparing(Comparator.<Column>comparingInt((c) -> c.index).reversed());

    private ColumnFitting() {
    }

    /** columns into width (inside the border: their widths plus one between each) - dropped: those held dropped */
    static Fit fit(List<Column> columns, int width, Set<Integer> dropped) {
        Set<Integer> droppedNow = new LinkedHashSet<Integer>(dropped);
        while (true) {
            List<Column> shown = new ArrayList<Column>();
            for (Column column : columns) {
                if (!droppedNow.contains(column.index)) {
                    shown.add(column);
                }
            }
            int[] widths = new int[shown.size()];
            for (int i = 0; i < widths.length; i++) {
                widths[i] = shown.get(i).widths.most;
            }
            if (total(widths) <= width) {
                return fit(shown, widen(widths, width), droppedNow);
            }
            squeeze(shown, widths, width);
            if (total(widths) <= width) {
                return fit(shown, widths, droppedNow);
            }
            Column next = shown.stream().filter((c) -> c.dropOrder != null).sorted(DROP_ORDER).findFirst().orElse(null);
            if (next != null) {
                droppedNow.add(next.index);
                continue;                                       // the rest fitted again, from their most
            }
            cut(shown, widths, width);
            return fit(shown, widths, droppedNow);
        }
    }

    private static Fit fit(List<Column> shown, int[] widths, Set<Integer> dropped) {
        List<Integer> indexes = new ArrayList<Integer>();
        for (Column column : shown) {
            indexes.add(column.index);
        }
        return new Fit(indexes, widths, dropped);
    }

    /** the columns' widths and one between each */
    static int total(int[] widths) {
        int total = Math.max(0, widths.length - 1);
        for (int w : widths) {
            total += w;
        }
        return total;
    }

    // a column at a time, the one furthest above its least first (the rightmost of equals): no column is squeezed far while another keeps room
    private static void squeeze(List<Column> shown, int[] widths, int width) {
        while (total(widths) > width) {
            int widest = -1;
            for (int i = 0; i < widths.length; i++) {
                int spare = widths[i] - shown.get(i).widths.least;
                if (spare > 0 && (widest < 0 || spare >= widths[widest] - shown.get(widest).widths.least)) {
                    widest = i;
                }
            }
            if (widest < 0) {
                return;
            }
            widths[widest]--;
        }
    }

    // the rightmost column down to its cut, then the one before it ...
    private static void cut(List<Column> shown, int[] widths, int width) {
        for (int i = widths.length - 1; i >= 0 && total(widths) > width; i--) {
            widths[i] -= Math.min(widths[i] - shown.get(i).widths.cut, total(widths) - width);
        }
    }

    /** widths widened to fill width, each in proportion to its own; what rounding leaves goes one each from the right */
    static int[] widen(int[] widths, int width) {
        int room = width - Math.max(0, widths.length - 1);
        int used = total(widths) - Math.max(0, widths.length - 1);
        if (widths.length == 0 || used >= room) {
            return widths;
        }
        int[] wider = widths.clone();
        if (used > 0) {
            for (int c = wider.length - 1; c >= 0; c--) {
                int proportional = (int) Math.ceil((double) room * widths[c] / used);
                if (proportional > wider[c] && room >= used - wider[c] + proportional) {
                    used = used - wider[c] + proportional;
                    wider[c] = proportional;
                }
            }
        }
        while (used < room) {
            for (int c = wider.length - 1; c >= 0 && used < room; c--) {
                wider[c]++;
                used++;
            }
        }
        return wider;
    }
}
