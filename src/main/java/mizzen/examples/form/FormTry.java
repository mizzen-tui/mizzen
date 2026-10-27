package mizzen.examples.form;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mizzen.app.TUIApp;
import mizzen.examples.Parts;
import mizzen.examples.Parts.Part;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.form.Form;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.input.tree.TreeView;
import mizzen.components.plot.PlotArea;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;

/**
 * Tries Form with every kind of row: text, password, number, check box, radios, choices whose label
 * and options follow other fields, an inline group in a section, a tree whose pick is drawn beside it
 * (a view row), a notes area, buttons. FormTryByHand is the same order without the builder.
 * <pre>
 *     ./runDBG.sh form.FormTry
 * </pre>
 */
public class FormTry {

    public enum Size { SMALL, MEDIUM, LARGE }

    public static class Order {
        public String customer = "";
        public String pin = "";
        public Integer quantity = 1;
        public Boolean giftWrap = false;
        public Size size = Size.MEDIUM;
        public String country = "Canada";
        public String region = "Ontario";
        public String city = "Toronto";
        public String street = "";
        public String postal = "";
        public DefaultTreeNode<Object> part = Parts.CATALOGUE;   // a tree needs something picked: the whole catalogue to start
        public String notes = "";

        /** the picked part; null when a group is picked */
        Part pickedPart() {
            return part != null && part.getValue() instanceof Part ? (Part) part.getValue() : null;
        }

        @Override
        public String toString() {
            return customer + " x" + quantity + " " + size + (Boolean.TRUE.equals(giftWrap) ? " gift" : "") + " to " + city
                    + ", " + region + ", " + country + " - " + (pickedPart() == null ? "no part picked" : pickedPart().name);
        }
    }

    static final List<String> COUNTRIES = Arrays.asList("Canada", "USA");
    static final Map<String, List<String>> REGIONS = new HashMap<String, List<String>>();
    static final Map<String, List<String>> CITIES = new HashMap<String, List<String>>();
    static {
        REGIONS.put("Canada", Arrays.asList("Ontario", "Quebec", "Nova Scotia"));
        REGIONS.put("USA", Arrays.asList("Maine", "Vermont"));
        CITIES.put("Ontario", Arrays.asList("Toronto", "Ottawa"));
        CITIES.put("Quebec", Arrays.asList("Montreal", "Quebec City"));
        CITIES.put("Nova Scotia", Arrays.asList("Halifax", "Lunenburg"));
        CITIES.put("Maine", Arrays.asList("Portland", "Bar Harbor"));
        CITIES.put("Vermont", Arrays.asList("Burlington"));
    }

    static <K> List<String> of(Map<K, List<String>> map, K key) {
        return map.containsKey(key) ? map.get(key) : Collections.<String>emptyList();
    }

    /** where a picked part is drawn: a plot that draws the order's part, or nothing for a group */
    static PlotArea preview(Order order) {
        return new PlotArea(Colors.Gre, new Dimension.Simple(9, 24))
                .showing(-1.3, 1.3, -1.3, 1.3)
                .scaling(PlotArea.Scaling.EQUAL)
                .axes(false)
                .onDraw((pen) -> {
                    Part part = order.pickedPart();
                    if (part != null) {
                        pen.color(part.color).draw(part.drawing.get());
                    }
                });
    }

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    Order order = new Order();
                    TextComponent result = new TextComponent(Colors.IGre, " Fill it in, then Order");
                    TreeView<DefaultTreeNode<Object>> catalogue = new TreeView<DefaultTreeNode<Object>>(Parts.CATALOGUE.asModel());
                    catalogue.expandAll();
                    PlotArea picture = preview(order);

                    Form<Order> form = Form.of(order)
                            .inputWidth(24)
                            .heading("Order")
                            .text("Customer", "customer").required()
                            .password("PIN", "pin").validate((String p) -> p.isEmpty() || p.matches("\\d{4}"), "4 digits, or none")
                            .number("Quantity", "quantity").validate((Integer q) -> q != null && q >= 1, "1 or more")
                            .checkBox("Gift wrap", "giftWrap")
                            .radio("Size", "size")
                            .section("Ship to", (s) -> s
                                    .choice("Country", "country", COUNTRIES)
                                    .choice((c) -> "USA".equals(c.country) ? "State" : "Province", "region", (c) -> of(REGIONS, c.country))
                                    .choice("City", "city", (c) -> of(CITIES, c.region))
                                    .inline((r) -> r.text("Street", "street").text("Postal", "postal")))
                            .inline((r) -> r.row("Part", "part", catalogue)                 // pick it in the tree ...
                                            .view("", picture, (o) -> picture.redraw()))    // ... see it drawn beside it
                            .area("Notes", "notes", 3)
                            .note("CTRL-C leaves")
                            .buttons((b) -> b.submit("Order", (f) -> result.setTextAndResize(" Ordered: " + order))
                                             .button("Show the object", (f) -> result.setTextAndResize(" Now: " + order)));

                    VerticalPanel page = new VerticalPanel();
                    page.add(form);
                    page.add(result);
                    tApp.getMainApplicationPanel().add(1, 1, page);
                })
                .start();
    }
}
