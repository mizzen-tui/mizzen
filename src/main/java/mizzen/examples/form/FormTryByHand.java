package mizzen.examples.form;

import java.util.List;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.components.input.MultiLineTextInput;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FieldErrors;
import mizzen.components.input.form.FormBinder;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.input.tree.TreeView;
import mizzen.components.plot.PlotArea;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.Parts;
import mizzen.examples.form.FormTry.Order;
import mizzen.examples.form.FormTry.Size;
import mizzen.geom.Dimension;

/**
 * FormTry's order without the builder - the same rows, rules, dependent choices and picture, done by
 * hand, to show what the builder does for you:
 * <pre>
 *     1. make the inputs, each named after the object's field
 *     2. lay them out - any way at all (here: like FormTry's rows)
 *     3. bind: binder.bindAll(panel) finds the named inputs, wherever they sit
 *     4. rules and where errors show: the binder's (here: one status line)
 *     5. what follows what: the binder's field onChange
 *
 *     ./runDBG.sh form.FormTryByHand
 * </pre>
 */
public class FormTryByHand {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    Order order = new Order();

                    // 1. the inputs, each named after its field
                    TextInput customer = text("customer");
                    TextInput pin = text("pin");
                    pin.setAsPassword();
                    IntegerInputWithIncrDecrButtons quantity = new IntegerInputWithIncrDecrButtons(Colors.IYel, "", "-", "+");
                    quantity.setName("quantity");
                    CheckBoxInput giftWrap = new CheckBoxInput(Colors.IYel);
                    giftWrap.setName("giftWrap");
                    RadioGroup<Size> size = RadioGroup.horizontal(choices(java.util.Arrays.asList(Size.values())));
                    size.setName("size");
                    DropdownListBox<String> country = dropdown("country", FormTry.COUNTRIES);
                    TextComponent regionLabel = new TextComponent(Colors.BWhi, "Province ");
                    DropdownListBox<String> region = dropdown("region", FormTry.of(FormTry.REGIONS, order.country));
                    DropdownListBox<String> city = dropdown("city", FormTry.of(FormTry.CITIES, order.region));
                    TextInput street = text("street");
                    TextInput postal = text("postal");
                    TreeView<DefaultTreeNode<Object>> catalogue = new TreeView<DefaultTreeNode<Object>>(Parts.CATALOGUE.asModel());
                    catalogue.expandAll();
                    catalogue.setName("part");
                    PlotArea picture = FormTry.preview(order);
                    MultiLineTextInput notes = new MultiLineTextInput(Colors.IYel);
                    notes.setName("notes");
                    ScrollableComponent notesBox = new ScrollableComponent(Colors.IYel, new Dimension.Simple(5, 26), notes);
                    notesBox.removeFooter();
                    TextComponent status = new TextComponent(Colors.BIRed, " ");
                    TextComponent result = new TextComponent(Colors.IGre, " Laid out by hand, bound by traversal");
                    Button orderButton = new Button(Colors.BWhi, "Order");
                    Button showButton = new Button(Colors.BWhi, "Show the object");

                    // 2. the layout, by hand - labels padded to line up
                    VerticalPanel ship = new VerticalPanel(Colors.BWhi);
                    ship.setBorder(true);
                    ship.setTitle(" Ship to ");
                    ship.add(row("Country  ", country));
                    ship.add(new HorizontalPanel(Colors.BWhi, 1).add(regionLabel).add(region));
                    ship.add(row("City     ", city));
                    ship.add(new HorizontalPanel(Colors.BWhi, 2).add(row("Street", street)).add(row("Postal", postal)));

                    VerticalPanel panel = new VerticalPanel();
                    panel.add(new TextComponent(Colors.BYel, "Order (by hand)"));
                    panel.add(row("Customer ", customer));
                    panel.add(row("PIN      ", pin));
                    panel.add(row("Quantity ", quantity));
                    panel.add(row("Gift wrap", giftWrap));
                    panel.add(row("Size     ", size));
                    panel.add(ship);
                    panel.add(new HorizontalPanel(Colors.BWhi, 2).add(row("Part", catalogue)).add(picture));
                    panel.add(row("Notes    ", notesBox));
                    panel.add(status);
                    panel.add(new HorizontalPanel(Colors.BWhi, 2).add(orderButton).add(showButton));
                    panel.add(result);

                    // 3. binding: the traversal finds the named inputs, wherever they sit
                    FormBinder<Order> binder = FormBinder.UTIL.get(order, tApp.getEventBus());
                    binder.bindAll(panel);

                    // 4. rules, and where their errors show
                    binder.<String>getFormBinderField("customer").withValidator((String c) -> !c.trim().isEmpty(), "Customer is required");
                    binder.<String>getFormBinderField("pin").withValidator((String p) -> p.isEmpty() || p.matches("\\d{4}"), "PIN: 4 digits, or none");
                    binder.<Integer>getFormBinderField("quantity").withValidator((Integer q) -> q != null && q >= 1, "Quantity: 1 or more");
                    binder.errorsTo(FieldErrors.into(status));

                    // 5. what follows what: region and city follow the country, the picture the part
                    binder.getFormBinderField("country").onChange((te) -> {
                        regionLabel.setTextAndResize("USA".equals(order.country) ? "State    " : "Province ");
                        order.region = keepIfAmong(order.region, region, FormTry.of(FormTry.REGIONS, order.country));
                        order.city = keepIfAmong(order.city, city, FormTry.of(FormTry.CITIES, order.region));
                    });
                    binder.getFormBinderField("region").onChange((te) ->
                            order.city = keepIfAmong(order.city, city, FormTry.of(FormTry.CITIES, order.region)));
                    binder.getFormBinderField("part").onChange((te) -> picture.redraw());

                    orderButton.onChange((te) -> {
                        if (binder.validateAll()) {
                            result.setTextAndResize(" Ordered: " + order);
                        }
                    });
                    showButton.onChange((te) -> result.setTextAndResize(" Now: " + order));

                    tApp.getMainApplicationPanel().add(1, 1, panel);
                })
                .start();
    }

    private static TextInput text(String field) {
        TextInput input = new TextInput(Colors.IYel, 24);
        input.setName(field);
        input.setEmptyChar('_');
        return input;
    }

    private static DropdownListBox<String> dropdown(String field, List<String> options) {
        DropdownListBox<String> dropdown = new DropdownListBox<String>(Colors.IYel, choices(options));
        dropdown.setName(field);
        return dropdown;
    }

    private static HorizontalPanel row(String label, mizzen.components.Component<?> input) {
        return new HorizontalPanel(Colors.BWhi, 1).addText(label).add(input);
    }

    @SuppressWarnings("unchecked")
    private static <V> DisplayableValue<V>[] choices(List<V> values) {
        DisplayableValue<V>[] choices = new DisplayableValue[values.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = new DisplayableValue.SimpleDisplayValue<V>(String.valueOf(values.get(i)), values.get(i));
        }
        return choices;
    }

    // new options for a dropdown; the value kept if still one of them, else cleared - returned for the object
    private static String keepIfAmong(String value, DropdownListBox<String> dropdown, List<String> options) {
        dropdown.setItems(java.util.Arrays.asList(choices(options)));
        String kept = options.contains(value) ? value : null;
        dropdown.setValue(kept);
        return kept;
    }
}
