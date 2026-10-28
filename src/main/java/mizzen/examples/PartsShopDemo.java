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

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.chart.DonutChart;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.frameset.HorizontalSplitter;
import mizzen.components.frameset.VerticalSplitter;
import mizzen.components.input.Button;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.components.input.MultiLineTextInput;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FormBinder;
import mizzen.components.input.form.FormErrorHandler;
import mizzen.components.plot.PlotArea;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;

/**
 * A parts shop: an ordinary business screen - sales in a table, the picked sale in a form, its
 * items in another table - with the picked part drawn, and a chart of shipped against not shipped.
 * <p/>
 * Everything is bound: an edit in the form goes straight into the sale, and the sales table, the
 * chart and the totals follow as you type; change an item's quantity and its line, the sale's total
 * and the totals follow. A customer is needed and a quantity must be 1 or more - the line at the
 * top says so. Revert puts the sale back as it was when picked. The dividers drag.
 */
public class PartsShopDemo {

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

    /** a line of a sale; public fields, so a form binds them by name */
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

        /** a copy of what can be edited - to put back on Revert */
        Runnable snapshot() {
            String customer = this.customer, payment = this.payment, notes = this.notes;
            Status status = this.status;
            Boolean paid = this.paid;
            List<Integer> quantities = new ArrayList<Integer>();
            for (SaleItem item : items) {
                quantities.add(item.quantity);
            }
            return () -> {
                this.customer = customer;
                this.status = status;
                this.payment = payment;
                this.paid = paid;
                this.notes = notes;
                for (int i = 0; i < items.size(); i++) {
                    items.get(i).quantity = quantities.get(i);
                }
            };
        }
    }

    static List<Sale> sampleSales() {
        return new ArrayList<Sale>(Arrays.asList(
                new Sale(1001, "2026-10-01", "Acme Repairs", Status.SHIPPED, "Card", true, "")
                        .item(GEAR_12, 4).item(HEX_NUT, 50).item(WASHER, 50),
                new Sale(1002, "2026-10-01", "Bolt & Sons", Status.PACKED, "Invoice", false, "Collect Friday")
                        .item(SPRING, 20).item(BRACE, 12),
                new Sale(1003, "2026-10-02", "Harbour Marine", Status.ORDERED, "Card", true, "Rush - boat\nin the yard")
                        .item(GEAR_20, 2).item(KNOB, 6).item(WASHER, 100),
                new Sale(1004, "2026-10-03", "M. Okafor", Status.SHIPPED, "Cash", true, "")
                        .item(SHED, 1).item(BRACE, 8),
                new Sale(1005, "2026-10-04", "Northside Garage", Status.ORDERED, "Invoice", false, "")
                        .item(HEX_NUT, 200).item(WASHER, 200).item(SPRING, 10),
                new Sale(1006, "2026-10-05", "Clockworks Ltd", Status.PACKED, "Card", true, "Gift wrap")
                        .item(GEAR_12, 10).item(GEAR_20, 10).item(KNOB, 4),
                new Sale(1007, "2026-10-06", "R. Lindqvist", Status.SHIPPED, "Card", true, "")
                        .item(KNOB, 2)));
    }

    static String money(float amount) {
        return String.format("%,.2f", amount);
    }

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit()
                .setOnReadyStart((tApp) -> new PartsShopDemo(tApp).build())
                .start();
    }

    private final TUIApp tApp;
    private final List<Sale> sales = sampleSales();
    private Sale sale;                                       // the one in the form
    private SaleItem item;                                   // the one drawn
    private Runnable revert = () -> {
    };

    private final TextComponent summary = new TextComponent(Colors.BWhi, "");
    private final TextComponent message = new TextComponent(Colors.IGre, " Pick a sale (arrows or click) - edit it below; the table, chart and totals follow");
    private final ObjectTableModel<Sale> salesModel = new ObjectTableModel<Sale>()
            .column("#", (s) -> s.number)
            .column("Date", (s) -> s.date)
            .column("Customer", (s) -> s.customer)
            .column("Status", (s) -> s.status)
            .column("Paid", (s) -> Boolean.TRUE.equals(s.paid) ? "yes" : "no")
            .column("Items", (s) -> s.items.size())
            .column("Total", (s) -> money(s.total()));
    private final ObjectTableModel<SaleItem> itemsModel = new ObjectTableModel<SaleItem>()
            .column("Part", (i) -> i.part.name)
            .column("Qty", (i) -> i.quantity)
            .column("Price", (i) -> money(i.part.price))
            .column("Total", (i) -> money(i.total()));
    private TableComponent<Sale> salesTable;
    private TableComponent<SaleItem> itemsTable;
    private DonutChart<Status> shipped;
    private PlotArea drawing;
    private final TextComponent itemLine = new TextComponent(Colors.BWhi, "");
    private FormBinder<Sale> saleForm;
    private Button revertButton;
    private final TextComponent notesLength = new TextComponent(Colors.IBla, "");   // under "Notes :", as you type
    private FormBinder<SaleItem> itemForm;

    PartsShopDemo(TUIApp tApp) {
        this.tApp = tApp;
    }

    void build() {
        Dimension screen = tApp.getDimensions();
        int height = screen.getHeight() - 3, width = screen.getWidth() - 1;
        int topHeight = Math.max(9, height * 2 / 5), chartWidth = 44;
        int bottomHeight = Math.max(0, height - topHeight - 1), formWidth = 46;   // none in a terminal too short for the top

        salesModel.setRows(sales);
        salesTable = new TableComponent<Sale>(Colors.Cya, new Dimension.Simple(topHeight - 2, Math.max(0, width - chartWidth - 3)), salesModel);   // none in a terminal narrower than the chart
        salesTable.setName("sales");
        salesTable.setLinesStyle(TableLinesStyle.HEADER_ALL_AND_BORDER);
        shipped = new DonutChart<Status>(Colors.Gre, new Dimension.Simple(topHeight - 2, chartWidth))
                .sliceSize((s) -> s == Status.SHIPPED ? count(true) : count(false))
                .sliceLabel((s) -> s == Status.SHIPPED ? "Shipped" : "Not shipped")
                .colors(Colors.BGre, Colors.BYel)
                .setItems(Arrays.asList(Status.SHIPPED, Status.ORDERED));
        shipped.setTitle(" Shipped? ");

        HorizontalSplitter top = new HorizontalSplitter(Colors.BBlu, new Dimension.Simple(topHeight, width));
        top.setBorder(false);
        top.addComponent(salesTable);
        top.addComponent(shipped);

        HorizontalSplitter bottom = new HorizontalSplitter(Colors.BBlu, new Dimension.Simple(bottomHeight, width));
        bottom.setBorder(false);
        bottom.addComponent(saleFormPanel(Math.max(0, bottomHeight - 2), formWidth));
        bottom.addComponent(itemsPanel(Math.max(0, bottomHeight - 2), Math.max(0, width - formWidth - 3)));

        VerticalSplitter screenSplit = new VerticalSplitter(Colors.BBlu, new Dimension.Simple(height, width));
        screenSplit.setBorder(true);
        screenSplit.addComponent(top);
        screenSplit.addComponent(bottom);

        HorizontalPanel heading = new HorizontalPanel(Colors.BWhi, 2);
        heading.add(new TextComponent(Colors.BYel, " Parts shop "));
        heading.add(summary);
        tApp.getMainApplicationPanel().add(0, 0, heading);
        tApp.getMainApplicationPanel().add(1, 0, message);
        tApp.getMainApplicationPanel().add(2, 0, screenSplit);

        tApp.appendOnResizeAction((ta, size) -> screenSplit.resize(new Dimension.Simple(size.getHeight() - 3, size.getWidth() - 1)));

        salesTable.onPick((picked, te) -> {
            showSale(picked);
            message.setColor(Colors.IGre);
            message.setTextAndResize(" Sale " + sale.number + " - edit it below; the table, chart and totals follow");
        });
        itemsTable.onPick((picked, te) -> {
            showItem(picked);
        });

        salesTable.setSelectedRow(0);
        showSale(sales.get(0));
        updateSummary();
    }

    /** the sale's own fields, bound by name to the sale shown */
    private Component<?> saleFormPanel(int height, int width) {
        TextInput customer = new TextInput(Colors.IYel, 26);
        customer.setName("customer");
        DropdownListBox<Status> status = new DropdownListBox<Status>(statuses());
        status.setName("status");
        RadioGroup<String> payment = RadioGroup.horizontal("Card", "Cash", "Invoice");
        payment.setName("payment");
        CheckBoxInput paid = new CheckBoxInput(Colors.IYel);
        paid.setName("paid");
        MultiLineTextInput notes = new MultiLineTextInput(new Colors.FGBGColors(Colors.BWhi, Colors.On_IBla));
        notes.setName("notes");
        ScrollableComponent notesBox = new ScrollableComponent(Colors.IYel, new Dimension.Simple(4, 30), notes);
        revertButton = new Button(Colors.BWhi, "Revert");
        revertButton.hide();                               // shown once the sale is edited

        VerticalPanel form = new VerticalPanel();
        form.add(new HorizontalPanel().addText("Customer : ").add(customer));
        form.add(new HorizontalPanel().addText("Status   : ").add(status));
        form.add(new HorizontalPanel().addText("Payment  : ").add(payment));
        form.add(new HorizontalPanel().addText("Paid     : ").add(paid));
        form.add(new HorizontalPanel().add(new VerticalPanel().addText("Notes    : ").add(notesLength)).add(notesBox));
        form.add(revertButton);
        form.appendOnResize((size) -> {                    // the notes take the room left - drag a divider and they follow
            int above = 4, revertRow = 1, label = 11;
            notesBox.resize(new Dimension.Simple(Math.max(3, size.getHeight() - above - revertRow),
                                                 Math.max(12, size.getWidth() - label - 2)));
        });
        form.resize(new Dimension.Simple(height, width));

        saleForm = FormBinder.UTIL.get(sales.get(0), tApp.getEventBus());
        saleForm.bindAll(form);
        saleForm.<String>getFormBinderField("customer").withValidator((String c) -> c != null && !c.trim().isEmpty(), "A sale needs a customer");
        saleForm.withValidatorHandler(showProblem());
        notes.onChange((te) -> showNotesLength());         // the app listens too - alongside the form's binding
        saleForm.onChange((te) -> {
            revertButton.show();
            salesTable.clearData();                        // the edit is in the sale already: show it
            shipped.redraw();
            updateSummary();
        });

        revertButton.onChange((te) -> {
            revert.run();
            saleForm.setFormData(sale);
            itemForm.setFormData(item);
            refreshAll();
            message.setTextAndResize(" Sale " + sale.number + " put back as it was");
            revertButton.hide();
        });
        return form;
    }

    /** the sale's items; under them the picked one - drawn, and its quantity to change */
    private Component<?> itemsPanel(int height, int width) {
        int tableHeight = Math.max(5, height / 2);
        itemsTable = new TableComponent<SaleItem>(Colors.Yel, new Dimension.Simple(tableHeight, width), itemsModel);
        itemsTable.setName("items");
        itemsTable.setLinesStyle(TableLinesStyle.HEADER_ALL_AND_BORDER);

        drawing = new PlotArea(Colors.Gre, new Dimension.Simple(Math.max(0, height - tableHeight - 1), Math.min(30, width / 2)))   // none when the items table takes the height
                .showing(-1.3, 1.3, -1.3, 1.3)
                .scaling(PlotArea.Scaling.EQUAL)
                .axes(false)
                .onDraw((pen) -> {
                    if (item != null) {
                        pen.color(item.part.color).draw(item.part.drawing.get());
                    }
                });

        IntegerInputWithIncrDecrButtons quantity = new IntegerInputWithIncrDecrButtons(Colors.IYel, "Quantity ", "-", "+");
        quantity.setName("quantity");
        VerticalPanel itemDetail = new VerticalPanel();
        itemDetail.add(itemLine);
        itemDetail.add(quantity);

        HorizontalPanel picked = new HorizontalPanel(Colors.BWhi, 2);
        picked.add(drawing);
        picked.add(itemDetail);

        itemForm = FormBinder.UTIL.get(sales.get(0).items.get(0), tApp.getEventBus());
        itemForm.bindAll(itemDetail);
        itemForm.<Integer>getFormBinderField("quantity").withValidator((Integer q) -> q != null && q >= 1, "A quantity is 1 or more");
        itemForm.withValidatorHandler(showProblem());
        itemForm.onChange((te) -> {
            revertButton.show();
            refreshAll();
        });

        VerticalSplitter items = new VerticalSplitter(Colors.BBlu, new Dimension.Simple(height, width));
        items.addComponent(itemsTable);
        items.addComponent(picked);
        return items;
    }

    private <T> FormErrorHandler<T> showProblem() {
        return (eventBus, problem, data) -> {
            message.setColor(problem == null ? Colors.IGre : Colors.BIRed);
            message.setTextAndResize(problem == null ? " All good" : " " + problem.getMessage());
        };
    }

    @SuppressWarnings("unchecked")
    private static DisplayableValue<Status>[] statuses() {
        DisplayableValue<Status>[] statuses = new DisplayableValue[Status.values().length];
        for (Status s : Status.values()) {
            statuses[s.ordinal()] = new DisplayableValue.SimpleDisplayValue<Status>(s.label, s);
        }
        return statuses;
    }

    private void showSale(Sale picked) {
        sale = picked;
        revert = picked.snapshot();
        revertButton.hide();
        saleForm.setFormData(picked);
        showNotesLength();
        itemsModel.setRows(picked.items);
        itemsTable.clearData();
        itemsTable.setSelectedRow(0);
        showItem(picked.items.get(0));
    }

    private void showNotesLength() {
        int length = sale.notes == null ? 0 : sale.notes.length();
        notesLength.setTextAndResize(String.format(" %3d chars", length));
    }

    private void showItem(SaleItem picked) {
        item = picked;
        itemForm.setFormData(picked);
        itemLine.setTextAndResize(picked.part.name + "  at " + money(picked.part.price));
        drawing.redraw();
    }

    private void refreshAll() {
        salesTable.clearData();
        itemsTable.clearData();
        shipped.redraw();
        drawing.redraw();
        updateSummary();
    }

    private double count(boolean shippedOnes) {
        int n = 0;
        for (Sale s : sales) {
            if ((s.status == Status.SHIPPED) == shippedOnes) {
                n++;
            }
        }
        return n;
    }

    private void updateSummary() {
        float total = 0;
        for (Sale s : sales) {
            total += s.total();
        }
        summary.setTextAndResize(String.format("%d sales   %d shipped   %d not shipped   total %s",
                sales.size(), (int) count(true), (int) count(false), money(total)));
    }
}
