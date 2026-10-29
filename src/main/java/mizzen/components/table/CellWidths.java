package mizzen.components.table;

/**
 * How wide a cell can be drawn - its renderer's answer, which the table fits its columns from:
 * <pre>
 *     cut    the narrowest it can be clipped to         text that may be truncated: 1
 *     least  the narrowest it still reads whole         wrapping text: its longest word
 *     most   all the width it wants                     text: its longest line
 * </pre>
 * cut &lt;= least &lt;= most, or it fails at once. For a drawing that scales, cut is least: it is
 * drawn smaller rather than clipped.
 */
public final class CellWidths {

    public final int cut;
    public final int least;
    public final int most;

    public CellWidths(int cut, int least, int most) {
        if (cut < 0 || cut > least || least > most) {
            throw new IllegalArgumentException("cell widths must be 0 <= cut <= least <= most: cut " + cut + ", least "
                    + least + ", most " + most);
        }
        this.cut = cut;
        this.least = least;
        this.most = most;
    }

    /** one width: never narrower, never wider - e.g. a number */
    public static CellWidths exactly(int width) {
        return new CellWidths(width, width, width);
    }

    @Override
    public String toString() {
        return "cut " + cut + ", least " + least + ", most " + most;
    }
}
