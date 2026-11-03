package mizzen.examples;

import java.util.ArrayList;
import java.util.List;

import mizzen.app.TUIApp;
import mizzen.components.composite.Composite;
import mizzen.components.input.DropdownListBox;
import mizzen.components.theme.Theme;
import mizzen.components.chart.LineChart;
import mizzen.components.markdown.MarkdownView;
import mizzen.components.progressmeter.Counter;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.VAlign;
import mizzen.geom.position.PositionRelativeFactory;

/**
 * A status report written as Markdown, refreshed every two seconds - what an ops console shows, and could mail.
 * <pre>
 *     > [!TIP] / > [!CAUTION]       an alert in the theme's colour: the health line
 *     | | |  (empty headings)       a key / value list, no heading row
 *     |---|---:|---:|               columns right-aligned; a file name too wide is cut with …
 *     ```chart  /  ```counter       fences drawn by components the app gives (embed): a LineChart, a Counter
 * </pre>
 * setMarkdown puts the new report in the same view: scroll down and it stays where you are.
 */
public class MarkdownReport {

    private static final List<Double> SIZES = new ArrayList<Double>();
    private static int tick = 0;

    static {                                                            // the last weeks' sizes: the chart starts with history
        for (int day = 0; day < 19; day++) {
            SIZES.add(1.80 + 0.006 * day + (day % 4 == 0 ? 0.015 : 0));
        }
    }

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    Composite screen = (Composite) tApp.getMainApplicationPanel();
                    MarkdownView view = new MarkdownView(report())
                            .embed("chart", (block, width) -> chart(block.codeLines, width))
                            .embed("counter", (block, width) -> counter(block.codeLines));
                    ScrollableComponent pane = MarkdownView.inScrollPane(null, view);
                    pane.setTitle(" Status - refreshed every 2 s; scroll, and it stays where you are ");
                    screen.add(0, 0, pane);
                    pane.resize(tApp.getDimensions());
                    // on the top border, at its right end: the theme - the chart and the counter are made again in it
                    DropdownListBox<Theme> theme = ThemeList.of(tApp);
                    screen.add(PositionRelativeFactory.fromAlign(VAlign.TOP, 0, Align.RIGHT, 2, screen, theme), theme);
                    screen.keepOnTop(theme);
                    tApp.appendOnResizeAction((ta, size) -> pane.resize(size));
                    tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(2000, (te) -> {
                        tick++;
                        view.setMarkdown(report());
                    }));
                })
                .start();
    }

    // the report as text: what a script would write, and what an email would carry
    private static String report() {
        SIZES.add(1.91 + 0.004 * (tick % 10) + (tick % 3 == 0 ? 0.012 : 0));
        if (SIZES.size() > 20) {
            SIZES.remove(0);
        }
        boolean alert = tick % 4 == 3;                                  // every fourth refresh, something is wrong
        StringBuilder sizes = new StringBuilder("size GB:");
        for (double size : SIZES) {
            sizes.append(String.format(" %.2f", size));
        }
        return "# Status\n\n"
             + (alert ? "> [!CAUTION]\n> Health: ALERT - the site answered HTTP 500\n\n" : "> [!TIP]\n> Health: OK\n\n")
             + "| | |\n|---|---|\n"
             + "| Site | " + (alert ? "HTTP 500" : "HTTP 200") + " |\n"
             + "| Disk | " + (41 + tick % 5) + "% |\n"
             + "| Backups | 7 kept |\n\n"
             + "## Backups\n\n"
             + "| Backup | Size | Age |\n|---|---:|---:|\n"
             + "| wp_backup_20261101_201702.tar.gz.gpg | 1904 MB | " + (9 + tick / 30) + " h |\n"
             + "| wp_backup_20261031_201655.tar.gz.gpg | 1901 MB | 33 h |\n\n"
             + "## Backup size, last 20\n\n"
             + "```chart\n" + sizes + "\n```\n\n"
             + "## Tests\n\n"
             + "```counter\npassed " + Math.min(30, 12 + tick) + " 30\n```\n\n"
             + "> [!NOTE]\n> A fence in a language no one registered stays text:\n\n"
             + "```text\nsize GB: 1.90 1.91 1.93\n```\n";
    }

    // ```chart: "size GB: 1.90 1.91 ..." - a line chart of the numbers, the document's width
    private static LineChart chart(List<String> lines, int width) {
        LineChart chart = new LineChart(new Dimension.Simple(8, Math.min(width, 70))).series("size").legend(false);
        String[] parts = lines.isEmpty() ? new String[0] : lines.get(0).replaceAll("^[^:]*:", "").trim().split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            chart.add("size", i + 1, Double.parseDouble(parts[i]));
        }
        chart.redraw();
        return chart;
    }

    // ```counter: "passed 18 30" - a Counter
    private static Counter counter(List<String> lines) {
        String[] parts = lines.get(0).trim().split("\\s+");
        return new Counter(10).label(parts[0]).set(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
    }
}
