package mizzen.core;

import java.util.Arrays;

import mizzen.components.BlockChars;
import mizzen.components.Component;
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
 * The result is exactly height lines, parted by \n with none after the last, each exactly width cells
 * wide, padded with spaces; what the component draws past that is cut. One character is one cell.
 * Encoding the String is the caller's: write it as UTF-8, whatever the JVM's default is.
 * <p/>
 * The same component and size give the same text. Snapshots can run on several threads at once, each
 * with its own components; a component isn't safe to draw on two threads at once.
 * <p/>
 * Draw what the screen would show: live or animated components show their state now. A bad size, or
 * text a component holds with colour codes still in it, throws.
 */
public final class TextSnapshot {

    private static final char ESC = '\u001b';
    private static final char NOT_ASCII = '?';

    private final int height;
    private final int width;
    private boolean ansiColors = false;
    private boolean asciiOnly = false;

    /** a snapshot height lines by width cells */
    public TextSnapshot(int height, int width) {
        if (height < 1 || width < 1) {
            throw new IllegalArgumentException("a snapshot needs at least one line and one cell: height " + height + ", width " + width);
        }
        this.height = height;
        this.width = width;
    }

    /** component as plain text, height lines by width cells */
    public static String of(Component<?> component, int height, int width) {
        return new TextSnapshot(height, width).render(component);
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
        if (component instanceof CanResize) {
            ((CanResize) component).resize(new Dimension.Simple(height, width));
        }
        char[][] cells = new char[height][width];
        Color[][] colors = new Color[height][width];
        for (char[] line : cells) {
            Arrays.fill(line, ' ');
        }
        MapChars ascii = asciiOnly ? asciiMap() : null;
        for (ScreenLineSegment segment : component.getSegmentsBuffer().getSegments()) {
            paint(segment, cells, colors, ascii);
        }
        return text(cells, colors);
    }

    private void paint(ScreenLineSegment segment, char[][] cells, Color[][] colors, MapChars ascii) {
        int row = segment.getRow();
        String text = segment.getText();
        if (row < 0 || row >= height) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            int col = segment.getCol() + i;
            char c = text.charAt(i);
            if (c == ESC) {
                throw new IllegalStateException("a segment holds a control code, at " + row + "," + col + ": it would break the layout");
            }
            if (col >= 0 && col < width) {
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
        for (int row = 0; row < height; row++) {
            if (row > 0) {
                text.append('\n');
            }
            if (!ansiColors) {
                text.append(cells[row]);
                continue;
            }
            Color current = null;
            for (int col = 0; col < width; col++) {
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
        ascii.map(new char[] { '┈', '~', '┊', '!', '░', '#', '▒', '=', '▫', 'o', '■', '*', '◍', '*', '□', 'o', '◌', 'o',
                               '█', '#', '▓', '#', '▀', '-', '▄', '_', '▌', '|', '▐', '|', '▪', '*', '•', '*',
                               '→', '>', '←', '<', '↑', '^', '↓', 'v', '«', '<', '»', '>', '━', '-', '┃', '|' });
        return ascii;
    }
}
