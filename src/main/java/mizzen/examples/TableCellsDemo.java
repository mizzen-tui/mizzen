package mizzen.examples;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.plot.Pen;
import mizzen.components.table.CellRenderers;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.Parts.Part;

/**
 * Cells that are not just text: each column says how its cells are drawn (.renderer(..)), and the
 * table sizes its columns and rows from what each cell needs.
 * <pre>
 *     Part       text                  the part's name
 *     Drawing    a drawing             as wide as the table can spare, as tall as it is wide
 *     Price      text                  a number: never wrapped or cut
 *     Shelf      a meter               how full its shelf is
 *     Sold       a sparkline           a column a week
 *     By region  a small table         a table of its own in each row
 *     Picture    an image              kept in proportion
 * </pre>
 * Make the terminal narrower: the drawings and pictures shrink, rows get shorter; PgUp / PgDn scroll.
 * <p/>
 * cellsPanel() builds it all - no sizes: the divider shares out what main gives it.
 */
public class TableCellsDemo {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    VerticalPanel cells = cellsPanel();
                    tApp.getMainApplicationPanel().add(0, 0, cells);
                    cells.resize(PartsShopDemo.sizeFor(tApp.getDimensions()));
                    tApp.appendOnResizeAction((ta, size) -> cells.resize(PartsShopDemo.sizeFor(size)));
                })
                .start();
    }

    /** the two tables, one above the other, under a line that says what they show */
    static VerticalPanel cellsPanel() {
        ObjectTableModel<Stock> stockColumns = new ObjectTableModel<Stock>()
                .column("Part", (s) -> s.part.name)
                .column("Drawing", (s) -> s)
                        .renderer(CellRenderers.drawing((Stock s, Pen pen) -> pen.color(s.part.color).draw(s.part.drawing.get()),
                                -1.3, 1.3, -1.3, 1.3, 8, 16))
                .column("Price", (s) -> String.format("%,.2f", s.part.price))
                .column("Shelf", (s) -> s)
                        .renderer(CellRenderers.meter(Colors.Gre, (Stock s) -> (double) s.onShelf / s.shelfHolds))
                .column("Sold, by week", (s) -> s.soldByWeek)
                        .renderer(CellRenderers.sparkline(Colors.BCya, (List<Integer> sold) -> sold))
                .column("By region", (s) -> s)
                        .renderer(CellRenderers.table(Colors.Cya, (Stock s) -> s.byRegion()))
                .setRows(STOCK);
        TableComponent<Stock> stock = new TableComponent<Stock>(Colors.Yel, stockColumns);
        stock.setLinesStyle(TableLinesStyle.ALL);

        ObjectTableModel<Picture> pictureColumns = new ObjectTableModel<Picture>()
                .column("Picture", (p) -> p.image)
                        .renderer(CellRenderers.image((BufferedImage image) -> image, 8, 24))
                .column("Name", (p) -> p.name)
                .column("Size", (p) -> p.image.getWidth() + " x " + p.image.getHeight() + " pixels")
                .setRows(pictures());
        TableComponent<Picture> pictures = new TableComponent<Picture>(Colors.Cya, pictureColumns);
        pictures.setLinesStyle(TableLinesStyle.ALL);

        VerticalSplitter both = PartsShopDemo.aboveBelowWithSplitter(stock, pictures);
        both.onResizeExpandDown(stock, 60);

        VerticalPanel panel = new VerticalPanel();
        panel.add(new TextComponent(Colors.BWhi, " Each column draws its cells its own way - text, drawings, meters, sparklines, tables, images"));
        panel.add(both);
        panel.onResizeExpandDownAndRight(both);
        return panel;
    }

    // ---- the data: plain objects

    /** a part on the shelf: how many there are, how many the shelf holds, and how many sold each week */
    public static class Stock {
        public final Part part;
        public final int onShelf;
        public final int shelfHolds;
        public final List<Integer> soldByWeek;

        Stock(Part part, int onShelf, int shelfHolds, Integer... soldByWeek) {
            this.part = part;
            this.onShelf = onShelf;
            this.shelfHolds = shelfHolds;
            this.soldByWeek = Arrays.asList(soldByWeek);
        }

        /** what it sold, by region: a small table of its own */
        ObjectTableModel<RegionSales> byRegion() {
            int sold = soldByWeek.stream().mapToInt(Integer::intValue).sum();
            int north = sold / 2, south = sold * 3 / 10;
            return new ObjectTableModel<RegionSales>()
                    .column("Region", (r) -> r.region)
                    .column("Sold", (r) -> r.sold)
                    .setRows(Arrays.asList(new RegionSales("North", north), new RegionSales("South", south),
                                           new RegionSales("West", sold - north - south)));
        }

        @Override
        public String toString() {
            return part.name;
        }
    }

    /** how many of a part a region bought */
    public static class RegionSales {
        public final String region;
        public final int sold;

        RegionSales(String region, int sold) {
            this.region = region;
            this.sold = sold;
        }
    }

    static final List<Stock> STOCK = Arrays.asList(
            new Stock(Parts.GEAR_12,  34,  50,   6,  8,  5,  9, 12, 10,  7, 11, 14, 13,  9, 12),
            new Stock(Parts.GEAR_20,   8,  40,   3,  4,  2,  5,  4,  6,  5,  7,  6,  8,  9, 11),
            new Stock(Parts.HEX_NUT, 870, 1000, 90, 120, 85, 140, 110, 95, 130, 150, 125, 160, 140, 155),
            new Stock(Parts.WASHER,  410, 1000, 60, 55, 70, 65, 80, 75, 90, 85, 70, 95, 100, 90),
            new Stock(Parts.SPRING,   25, 100,  12, 10,  8,  6,  9,  7,  5,  4,  6,  3,  2,  4),
            new Stock(Parts.KNOB,     60,  80,   2,  3,  1,  4,  3,  5,  2,  6,  4,  3,  5,  4),
            new Stock(Parts.BRACE,    15,  60,   9, 11,  8, 12, 10, 14, 13, 15, 12, 16, 18, 17),
            new Stock(Parts.SHED,      2,   5,   0,  1,  0,  0,  1,  1,  0,  2,  1,  0,  1,  2));

    /** a picture: its name and the image itself */
    public static class Picture {
        public final String name;
        public final BufferedImage image;

        Picture(String name, BufferedImage image) {
            this.name = name;
            this.image = image;
        }
    }

    static List<Picture> pictures() {
        return Arrays.asList(picture("The earth, from Messenger", "earth-messenger.jpg"),
                             picture("Ball sprites", "ball-sprites.png"),
                             picture("Rocket sprites", "rocket-sprites.png"));
    }

    private static Picture picture(String name, String file) {
        try {
            return new Picture(name, ImageIO.read(TableCellsDemo.class.getResourceAsStream("sampledata/images/" + file)));
        } catch (IOException e) {
            throw new IllegalStateException("could not read the sample image " + file, e);
        }
    }
}
