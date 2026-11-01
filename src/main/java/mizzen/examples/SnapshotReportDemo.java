package mizzen.examples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.chart.BarChart;
import mizzen.components.chart.DonutChart;
import mizzen.components.chart.LineChart;
import mizzen.components.chart.SparkLineComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.plot.PlotArea;
import mizzen.components.progressmeter.ProgressMeter;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.core.TextSnapshot;
import mizzen.components.plot.shape.catalog.Ring;
import mizzen.geom.Dimension;
import mizzen.util.VeryBasicArgsHelper;

/**
 * A report with no terminal, as a cron job makes one: one of each kind of component as text (TextSnapshot),
 * in the three styles, to the file named, or to stdout. No TUIApp, no event loop; main just returns.
 * <pre>
 *     java -cp mizzen.jar mizzen.examples.SnapshotReportDemo
 *     java -cp mizzen.jar mizzen.examples.SnapshotReportDemo /tmp/report.txt
 * </pre>
 */
public class SnapshotReportDemo {

    /** a month's sales for one region */
    public static class Sale {
        public final String region;
        public final int sold;

        Sale(String region, int sold) {
            this.region = region;
            this.sold = sold;
        }
    }

    private static final List<Sale> SALES = Arrays.asList(new Sale("North", 58), new Sale("South", 34), new Sale("West", 24));

    public static void main(String[] args) throws IOException {
        if (Stream.of(args).anyMatch(a -> VeryBasicArgsHelper.isArgName(a, "HELP") || "-h".equals(a) || "-?".equals(a))) {
            printHelp();
            return;
        }
        StringBuilder report = new StringBuilder();
        for (Section section : SECTIONS) {
            report.append("=== ").append(section.name).append(' ').append(section.height).append('x').append(section.width).append('\n');
            report.append(new TextSnapshot(section.height, section.width).render(section.make())).append('\n');
            report.append("=== ascii ").append(section.name).append(' ').append(section.height).append('x').append(section.width).append('\n');
            report.append(new TextSnapshot(section.height, section.width).asciiOnly().render(section.make())).append('\n');
        }
        report.append("=== colours table\n").append(new TextSnapshot(8, 40).ansiColors().render(table())).append('\n');
        byte[] utf8 = report.toString().getBytes(StandardCharsets.UTF_8);      // as UTF-8 whatever the JVM's default: cron's may not be
        if (args.length > 0) {
            Files.write(Paths.get(args[0]), utf8);
        } else {
            System.out.write(utf8);
            System.out.flush();
        }
    }

    static void printHelp() {
        String cls = SnapshotReportDemo.class.getName();
        System.out.println(String.join("\n",
            "SnapshotReportDemo - components as text, with no terminal (TextSnapshot): one of each kind, plain and",
            "7-bit, and a table with colour codes. Runs under cron: no TUIApp, nothing read from a terminal.",
            "",
            "usage: java -cp mizzen.jar " + cls + " [file]",
            "",
            "  file                   write the report here, as UTF-8 (default: stdout, as UTF-8)",
            "  HELP | -h | -?         this help",
            "",
            "example: java -cp mizzen.jar " + cls + " /tmp/report.txt"));
    }

    // ---- one of each

    interface Maker {
        Component<?> make();
    }

    public static final class Section {
        public final String name;
        public final int height;
        public final int width;
        final Maker maker;

        Section(String name, int height, int width, Maker maker) {
            this.name = name;
            this.height = height;
            this.width = width;
            this.maker = maker;
        }

        public Component<?> make() {
            return maker.make();
        }
    }

    public static final List<Section> SECTIONS = Arrays.asList(
            new Section("text", 2, 40, () -> new TextComponent(Colors.Whi, "Weekly status\nAll regions reporting")),
            new Section("panels", 4, 60, SnapshotReportDemo::panels),
            new Section("table", 8, 40, SnapshotReportDemo::table),
            new Section("braille shapes", 12, 48, SnapshotReportDemo::shapes),
            new Section("line chart", 12, 60, SnapshotReportDemo::lineChart),
            new Section("bar chart", 12, 60, SnapshotReportDemo::barChart),
            new Section("donut chart", 12, 60, SnapshotReportDemo::donut),
            new Section("sparkline", 1, 20, SnapshotReportDemo::sparkline),
            new Section("progress meter", 1, 40, SnapshotReportDemo::meter));

    static VerticalPanel panels() {
        HorizontalPanel row = new HorizontalPanel().setHorizontalSpacer(2);
        row.add(new TextComponent(Colors.Whi, "left"));
        row.add(new TextComponent(Colors.Cya, "right"));
        VerticalPanel panel = new VerticalPanel();
        panel.add(new TextComponent(Colors.BWhi, "Above"));
        panel.add(row);
        return panel;
    }

    public static TableComponent<Sale> table() {
        TableComponent<Sale> table = new TableComponent<Sale>(Colors.Yel,
                new ObjectTableModel<Sale>().column("Region", (s) -> s.region).column("Sold", (s) -> s.sold).setRows(SALES));
        table.setSpacedRows(false);
        return table;
    }

    /** braille from the plot: a gear inside a ring, in the plot's own units, kept in proportion */
    public static PlotArea shapes() {
        return new PlotArea(Colors.Gre).showing(-1.3, 1.3, -1.3, 1.3).scaling(PlotArea.Scaling.EQUAL).axes(false)
                .onDraw((pen) -> {
                    pen.color(Colors.BCya).draw(new Ring(0, 0, 1.25f, 1.15f).setYUp(true).setFilled(true));
                    pen.color(Colors.BYel).draw(Parts.GEAR_12.drawing.get());
                });
    }

    public static LineChart lineChart() {
        LineChart chart = new LineChart(Colors.Cya).series("sold", Colors.BCya);
        for (int week = 0; week < 12; week++) {
            chart.add("sold", week, 10 + week * week % 7);
        }
        return chart;
    }

    static BarChart<Sale> barChart() {
        return new BarChart<Sale>(Colors.Gre).barSize((s) -> s.sold).barLabel((s) -> s.region).setItems(SALES);
    }

    static DonutChart<Sale> donut() {
        return new DonutChart<Sale>(Colors.Gre).sliceSize((s) -> s.sold).sliceLabel((s) -> s.region).setItems(SALES);
    }

    static SparkLineComponent sparkline() {
        SparkLineComponent line = new SparkLineComponent(Colors.BCya, new Dimension.Simple(1, 20));
        line.renderPoints(Arrays.asList(3, 5, 2, 8, 6, 9, 4, 7, 1, 5));
        return line;
    }

    static ProgressMeter meter() {
        ProgressMeter meter = new ProgressMeter(Colors.Gre, 20).setLayout(ProgressMeter.Layout.ONE_ROW);
        meter.setProgress(0.68);
        return meter;
    }
}
