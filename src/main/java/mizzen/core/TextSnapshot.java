package mizzen.core;

import java.util.Arrays;

import mizzen.components.BlockChars;
import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.table.TableComponent;
import mizzen.components.plot.braille.BrailleUtils;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.util.MapChars;
import mizzen.util.SparkLine;

/**
 * A component as a String, with no terminal: for a report, an email, a log, a job run by cron. It needs
 * no TUIApp and no event loop: it sizes the component, asks for its segments and returns them as lines,
 * on the caller's thread. It reads no terminal, runs nothing, writes nothing and starts no thread.
 * <pre>
 *     String report = TextSnapshot.of(table, 12, 80);                         // plain text
 *     String email  = new TextSnapshot(12, 80).asciiOnly().render(table);     // 7-bit characters
 *     String log    = new TextSnapshot(12, 80).ansiColors().render(chart);    // colours, no cursor movement
 * </pre>
 * With a size, the result is exactly height lines, parted by \n with none after the last, each exactly
 * width cells wide, padded with spaces; what the component draws past that is cut. One character is one
 * cell.
 * <p/>
 * Leave out the size, or either part (NO_LIMIT), and it is found: the component's own, or what it drew if
 * that is more. A table with no size works out what all its rows need - give it a width to keep long
 * columns in check. A drawing has no size of its own: with none given it fails, saying so.
 * <pre>
 *     TextSnapshot.of(panel);                       // as big as it is
 *     TextSnapshot.of(table, 80);                   // 80 wide (columns squeeze or drop), as tall as its rows
 *     TextSnapshot.of(chart, new Dimension.Simple(12, 80));
 * </pre>
 * Encoding the String is the caller's: write it as UTF-8, whatever the JVM's default is.
 * <p/>
 * The same component and size give the same text. Snapshots can run on several threads at once, each
 * with its own components; a component isn't safe to draw on two threads at once.
 * <p/>
 * It never exits the JVM and leaves nothing running, so no exit call is needed just to finish. An
 * application may call System.exit at the very end to set its exit code: nothing here waits to be flushed.
 * <p/>
 * Draw what the screen would show: live or animated components show their state now. A bad size, or
 * text a component holds with colour codes still in it, throws.
 */
public final class TextSnapshot {

    private static final char ESC = '\u001b';
    private static final char NOT_ASCII = '?';

    /** for a height or width: no limit - it is found (see the class comment) */
    public static final int NO_LIMIT = -1;

    private final int height;
    private final int width;
    private boolean ansiColors = false;
    private boolean asciiOnly = false;

    /** a snapshot height lines by width cells; either can be NO_LIMIT. Anything else below 1 fails */
    public TextSnapshot(int height, int width) {
        if ((height < 1 && height != NO_LIMIT) || (width < 1 && width != NO_LIMIT)) {
            throw new IllegalArgumentException("a snapshot needs at least one line and one cell, or NO_LIMIT: height " + height + ", width " + width);
        }
        this.height = height;
        this.width = width;
    }

    /** the size found: the component's own, or what it draws */
    public TextSnapshot() {
        this(NO_LIMIT, NO_LIMIT);
    }

    /** width cells wide, as tall as it turns out */
    public TextSnapshot(int width) {
        this(NO_LIMIT, width);
    }

    public TextSnapshot(Dimension size) {
        this(size.getHeight(), size.getWidth());
    }

    /** component as plain text, height lines by width cells */
    public static String of(Component<?> component, int height, int width) {
        return new TextSnapshot(height, width).render(component);
    }

    /** component as plain text, no limit on height or width */
    public static String of(Component<?> component) {
        return new TextSnapshot().render(component);
    }

    /** component as plain text, limit the width, no limit on height */
    public static String of(Component<?> component, int width) {
        return new TextSnapshot(width).render(component);
    }

    /** component as plain text, at size */
    public static String of(Component<?> component, Dimension size) {
        return new TextSnapshot(size).render(component);
    }

    /** with colour codes - and only those, no cursor movement. By default colours are dropped */
    public TextSnapshot ansiColors() {
        this.ansiColors = true;
        return this;
    }

    /** only 7-bit characters: box lines as + - |, braille as a dot, ... the layout is the same. By default what the component draws */
    public TextSnapshot asciiOnly() {
        this.asciiOnly = true;
        return this;
    }

    /** component sized to the snapshot, as text */
    public String render(Component<?> component) {
        if (component == null) {
            throw new IllegalArgumentException("no component to render");
        }
        size(component);

        // the result's size: what was asked for, else the component's own, or what it drew if that is more
        int drawnHeight = 0, drawnWidth = 0;
        boolean anythingShows = false;
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            drawnHeight = Math.max(drawnHeight, segment.getRow() + 1);
            drawnWidth = Math.max(drawnWidth, segment.getCol() + segment.getText().length());
            anythingShows |= !segment.getText().trim().isEmpty();
        }
        int rows = height != NO_LIMIT ? height : Math.max(component.getHeight(), drawnHeight);
        int cols = width != NO_LIMIT ? width : Math.max(component.getWidth(), drawnWidth);
        if (rows == 0 || cols == 0 || (height == NO_LIMIT && width == NO_LIMIT && component.getHeight() == 0 && !anythingShows)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(component) + " has no size of its own: give it one, or give the snapshot one");
        }

        char[][] cells = new char[rows][cols];
        Color[][] colors = new Color[rows][cols];
        for (char[] line : cells) {
            Arrays.fill(line, ' ');
        }
        MapChars ascii = asciiOnly ? asciiMap() : null;
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            paint(segment, cells, colors, ascii);
        }
        return text(cells, colors);
    }

    // the size asked for goes to the component. A table with part of it missing works the rest out from its rows
    private void size(Component<?> component) {
        if (!(component instanceof CanResize)) {
            return;
        }
        if (component instanceof TableComponent && height == NO_LIMIT) {
            TableComponent<?> table = (TableComponent<?>) component;
            int wide = width != NO_LIMIT ? width : (table.getWidth() > 0 ? table.getWidth() : table.widthNeeded());
            table.resize(new Dimension.Simple(table.heightAt(wide), wide));
        } else if (height != NO_LIMIT || width != NO_LIMIT) {
            ((CanResize) component).resize(new Dimension.Simple(height != NO_LIMIT ? height : component.getHeight(),
                                                                width != NO_LIMIT ? width : component.getWidth()));
        }
    }

    private void paint(ScreenLineSegment segment, char[][] cells, Color[][] colors, MapChars ascii) {
        int row = segment.getRow();
        String text = segment.getText();
        if (row < 0 || row >= cells.length) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            int col = segment.getCol() + i;
            char c = text.charAt(i);
            if (c == ESC) {
                throw new IllegalStateException("a segment holds a control code, at " + row + "," + col + ": it would break the layout");
            }
            if (col >= 0 && col < cells[row].length) {
                cells[row][col] = ascii == null ? c : toAscii(c, ascii);
                colors[row][col] = segment.getColors();
            }
        }
    }

    private static char toAscii(char c, MapChars ascii) {
        char mapped = ascii.get(c);
        return mapped >= ' ' && mapped < 127 ? mapped : NOT_ASCII;
    }

    private String text(char[][] cells, Color[][] colors) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < cells.length; row++) {
            if (row > 0) {
                text.append('\n');
            }
            if (!ansiColors) {
                text.append(cells[row]);
                continue;
            }
            Color current = null;
            for (int col = 0; col < cells[row].length; col++) {
                Color color = colors[row][col];
                if (color != current) {
                    text.append(color != null ? color.asString() : Colors.RCol.asString());
                    current = color;
                }
                text.append(cells[row][col]);
            }
            if (current != null) {
                text.append(Colors.RCol.asString());             // nothing carries over to the next line
            }
        }
        return text.toString();
    }

    /** what each drawing character is in 7 bits: box lines, braille, blocks, sparklines, meters */
    public static MapChars asciiMap() {
        MapChars ascii = new MapChars();
        ascii.mapAll(BlockChars.UTIL.getNICE_TO_BASIC());
        ascii.mapAll(BrailleUtils.getBrailleToAsciiMapChars());
        ascii.mapAll(SparkLine.UTIL.getSparklineToAsciiMapChars());
        ascii.map(new char[] { '┈', '~', '┊', '!', '░', '.', '▒', '=', '▫', 'o', '■', '*', '◍', '*', '□', 'o', '◌', 'o',
                               '█', '#', '▓', '%', '▀', '-', '▄', '_', '▌', '|', '▐', '|', '▪', '*', '•', '*',
                               '→', '>', '←', '<', '↑', '^', '↓', 'v', '«', '<', '»', '>', '━', '-', '┃', '|' });
        return ascii;
    }
}
