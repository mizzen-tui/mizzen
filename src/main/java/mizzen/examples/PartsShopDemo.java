package mizzen.examples;

import static mizzen.examples.Parts.BRACE;
import static mizzen.examples.Parts.GEAR_12;
import static mizzen.examples.Parts.GEAR_20;
import static mizzen.examples.Parts.HEX_NUT;
import static mizzen.examples.Parts.KNOB;
import static mizzen.examples.Parts.SHED;
import static mizzen.examples.Parts.SPRING;
import static mizzen.examples.Parts.WASHER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.chart.DonutChart;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.input.form.Form;
import mizzen.components.plot.PlotArea;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;

/**
 * A parts shop: sales in a table, the picked sale in a form, its items in another table, the picked
 * item drawn with its quantity to change, and a chart of shipped against not shipped.
 * <p/>
 * Edits go straight into the objects; the tables, chart and totals follow as you type. A customer is
 * needed and a quantity is 1 or more - the form says so under the field. Revert puts a sale back as
 * it was when the shop opened. Click a label to go to its input; the dividers drag.
 * <p/>
 * shopPanel() builds the whole shop - no sizes: the dividers share out what main gives it.
 */
public class PartsShopDemo {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> {
                    VerticalPanel shop = shopPanel();
                    tApp.getMainApplicationPanel().add(0, 0, shop);
                    shop.resize(sizeFor(tApp.getDimensions()));
                    tApp.appendOnResizeAction((ta, size) -> shop.resize(sizeFor(size)));
                })
                .start();
    }

    // the terminal less a column, so the right border shows
    static Dimension sizeFor(Dimension terminal) {
        return new Dimension.Simple(terminal.getHeight(), Math.max(0, terminal.getWidth() - 1));
    }

    /** the whole shop: its parts, how they are laid out, what happens */
    static VerticalPanel shopPanel() {
        List<Sale> sales = sampleSales();
        Sale first = sales.get(0);

        // ---- the parts

        TextComponent summary = new TextComponent(Colors.BWhi, "");

        ObjectTableModel<Sale> salesModel = salesColumns().setRows(sales);
        TableComponent<Sale> salesTable = new TableComponent<Sale>(Colors.Cya, salesModel);
        salesTable.setLinesStyle(TableLinesStyle.HEADER_ALL_AND_BORDER);

        ObjectTableModel<SaleItem> itemsModel = itemsColumns();
        TableComponent<SaleItem> itemsTable = new TableComponent<SaleItem>(Colors.Yel, itemsModel);
        itemsTable.setLinesStyle(TableLinesStyle.HEADER_ALL_AND_BORDER);

        DonutChart<Status> shippedChart = new DonutChart<Status>(Colors.Gre)
                .sliceSize((s) -> s == Status.SHIPPED ? count(sales, true) : count(sales, false))
                .sliceLabel((s) -> s == Status.SHIPPED ? "Shipped" : "Not shipped")
                .colors(Colors.BGre, Colors.BYel)
                .setItems(Arrays.asList(Status.SHIPPED, Status.ORDERED));
        shippedChart.setTitle(" Shipped? ");

        Form<SaleItem> itemForm = Form.of(first.items.get(0))
                .view("Part", (i) -> i.part.name + "  at " + money(i.part.price))
                .number("Quantity", "quantity").validate((Integer q) -> q != null && q >= 1, "1 or more");

        PlotArea drawing = new PlotArea(Colors.Gre)
                .showing(-1.3, 1.3, -1.3, 1.3)                  // x and y from -1.3 to 1.3: a part, about 1 from its middle, fits
                .scaling(PlotArea.Scaling.EQUAL)
                .axes(false)
                .onDraw((pen) -> pen.color(itemForm.data().part.color).draw(itemForm.data().part.drawing.get()));

        // the objects changed: everything that shows them works itself out again
        Runnable refreshViews = () -> {
            salesTable.clearData();
            itemsTable.clearData();
            shippedChart.redraw();
            drawing.redraw();
            summary.setTextAndResize(String.format(" Parts shop   %d sales   %d shipped   %d not shipped   total %s",
                    sales.size(), (int) count(sales, true), (int) count(sales, false), money(totalOfAll(sales))));
        };

        Form<Sale> saleForm = Form.of(first)
                .text("Customer", "customer").required()
                .choice("Status", "status")                     // an enum: its values are the options
                .radio("Payment", "payment", Arrays.asList("Card", "Cash", "Invoice"))
                .checkBox("Paid", "paid")
                .area("Notes", "notes", 3)
                .buttons((b) -> b.button("Revert", (f) -> {     // the sale as it was when the shop opened
                    f.data().revert();
                    f.show(f.data());
                    itemForm.show(itemForm.data());
                    refreshViews.run();
                }));

        // ---- the layout: no sizes - each divider shares out what it is given

        HorizontalSplitter top = sideBySideWithSplitter(salesTable, shippedChart);
        top.onResizeExpandRight(salesTable, 60);               // the chart: the rest - room for its legend
        VerticalSplitter items = aboveBelowWithSplitter(itemsTable, sideBySideWithSplitter(drawing, itemForm));
        HorizontalSplitter bottom = sideBySideWithSplitter(saleForm, items);
        bottom.onResizeExpandRight(saleForm, 35).atLeast(saleForm.getWidth());   // never narrower than the form needs
        VerticalSplitter screen = aboveBelowWithSplitter(top, bottom);
        screen.setBorder(true);
        screen.onResizeExpandDown(top, 40).atLeast(9);

        VerticalPanel shopPanel = new VerticalPanel();
        shopPanel.add(summary);
        shopPanel.add(screen);
        shopPanel.onResizeExpandDownAndRight(screen);          // the screen takes what the panel is given, below the summary

        // ---- what happens

        Consumer<SaleItem> showItem = (item) -> {
            itemForm.show(item);
            drawing.redraw();
        };
        Consumer<Sale> showSale = (sale) -> {                  // what picking a sale does - and the start
            saleForm.show(sale);
            itemsModel.setRows(sale.items);
            itemsTable.clearData();
            itemsTable.setSelectedRow(0);
            showItem.accept(sale.items.get(0));
        };
        salesTable.onPick((sale, te) -> showSale.accept(sale));
        itemsTable.onPick((item, te) -> showItem.accept(item));
        saleForm.binder().onChange((te) -> refreshViews.run());    // an edit is already in the sale: show it everywhere
        itemForm.binder().onChange((te) -> refreshViews.run());

        salesTable.setSelectedRow(0);
        showSale.accept(first);
        refreshViews.run();
        return shopPanel;
    }

    // ---- helpers: no state

    static HorizontalSplitter sideBySideWithSplitter(Component<?> left, Component<?> right) {
        HorizontalSplitter split = new HorizontalSplitter(Colors.BBlu);
        split.setBorder(false);
        split.addComponent(left);
        split.addComponent(right);
        return split;
    }

    static VerticalSplitter aboveBelowWithSplitter(Component<?> upper, Component<?> lower) {
        VerticalSplitter split = new VerticalSplitter(Colors.BBlu);
        split.addComponent(upper);
        split.addComponent(lower);
        return split;
    }

    static ObjectTableModel<Sale> salesColumns() {
        return new ObjectTableModel<Sale>()
                .column("#", (s) -> s.number)
                .column("Date", (s) -> s.date)
                .column("Customer", (s) -> s.customer)
                .column("Status", (s) -> s.status)
                .column("Paid", (s) -> Boolean.TRUE.equals(s.paid) ? "yes" : "no")
                .column("Items", (s) -> s.items.size())
                .column("Total", (s) -> money(s.total()));
    }

    static ObjectTableModel<SaleItem> itemsColumns() {
        return new ObjectTableModel<SaleItem>()
                .column("Part", (i) -> i.part.name)
                .column("Qty", (i) -> i.quantity)
                .column("Price", (i) -> money(i.part.price))
                .column("Total", (i) -> money(i.total()));
    }

    static double count(List<Sale> sales, boolean shipped) {
        int n = 0;
        for (Sale s : sales) {
            if ((s.status == Status.SHIPPED) == shipped) {
                n++;
            }
        }
        return n;
    }

    static float totalOfAll(List<Sale> sales) {
        float total = 0;
        for (Sale s : sales) {
            total += s.total();
        }
        return total;
    }

    static String money(float amount) {
        return String.format("%,.2f", amount);
    }

    // ---- the data: plain objects; public fields, so a form binds them by name

    enum Status {
        ORDERED("Ordered"), PACKED("Packed"), SHIPPED("Shipped");

        final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** a line of a sale */
    public static class SaleItem {
        public final Parts.Part part;
        public Integer quantity;

        SaleItem(Parts.Part part, int quantity) {
            this.part = part;
            this.quantity = quantity;
        }

        float total() {
            return quantity == null ? 0 : quantity * part.price;
        }
    }

    /** a sale; its public fields are what the form edits */
    public static class Sale {
        final int number;
        final String date;
        public String customer;
        public Status status;
        public String payment;
        public Boolean paid;
        public String notes;
        final List<SaleItem> items = new ArrayList<SaleItem>();
        private Runnable asItWas = () -> {
        };

        Sale(int number, String date, String customer, Status status, String payment, boolean paid, String notes) {
            this.number = number;
            this.date = date;
            this.customer = customer;
            this.status = status;
            this.payment = payment;
            this.paid = paid;
            this.notes = notes;
        }

        Sale item(Parts.Part part, int quantity) {
            items.add(new SaleItem(part, quantity));
            return this;
        }

        float total() {
            float total = 0;
            for (SaleItem item : items) {
                total += item.total();
            }
            return total;
        }

        /** keep what can be edited as it is now - revert() puts it back */
        Sale remember() {
            String customer = this.customer, payment = this.payment, notes = this.notes;
            Status status = this.status;
            Boolean paid = this.paid;
            List<Integer> quantities = new ArrayList<Integer>();
            for (SaleItem item : items) {
                quantities.add(item.quantity);
            }
            asItWas = () -> {
                this.customer = customer;
                this.status = status;
                this.payment = payment;
                this.paid = paid;
                this.notes = notes;
                for (int i = 0; i < items.size(); i++) {
                    items.get(i).quantity = quantities.get(i);
                }
            };
            return this;
        }

        void revert() {
            asItWas.run();
        }
    }

    static List<Sale> sampleSales() {
        return new ArrayList<Sale>(Arrays.asList(
                new Sale(1001, "2026-10-01", "Acme Repairs", Status.SHIPPED, "Card", true, "")
                        .item(GEAR_12, 4).item(HEX_NUT, 50).item(WASHER, 50).remember(),
                new Sale(1002, "2026-10-01", "Bolt & Sons", Status.PACKED, "Invoice", false, "Collect Friday")
                        .item(SPRING, 20).item(BRACE, 12).remember(),
                new Sale(1003, "2026-10-02", "Harbour Marine", Status.ORDERED, "Card", true, "Rush - boat\nin the yard")
                        .item(GEAR_20, 2).item(KNOB, 6).item(WASHER, 100).remember(),
                new Sale(1004, "2026-10-03", "M. Okafor", Status.SHIPPED, "Cash", true, "")
                        .item(SHED, 1).item(BRACE, 8).remember(),
                new Sale(1005, "2026-10-04", "Northside Garage", Status.ORDERED, "Invoice", false, "")
                        .item(HEX_NUT, 200).item(WASHER, 200).item(SPRING, 10).remember(),
                new Sale(1006, "2026-10-05", "Clockworks Ltd", Status.PACKED, "Card", true, "Gift wrap")
                        .item(GEAR_12, 10).item(GEAR_20, 10).item(KNOB, 4).remember(),
                new Sale(1007, "2026-10-06", "R. Lindqvist", Status.SHIPPED, "Card", true, "")
                        .item(KNOB, 2).remember()));
    }
}
