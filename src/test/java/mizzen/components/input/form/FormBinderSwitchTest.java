package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.MultiLineTextInput;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.FocusGroup;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * One form, many objects - the detail of a master-detail screen: setFormData shows another object
 * and edits go into it from then on; a check box and a multi-line input bind like any field.
 */
public class FormBinderSwitchTest extends TestCase {

    public static class Sale {
        public String customer;
        public boolean paid;
        public String notes;

        Sale(String customer, boolean paid, String notes) {
            this.customer = customer;
            this.paid = paid;
            this.notes = notes;
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private final TextInput customer = (TextInput) new TextInput(Colors.IYel, 20).setName("customer");
    private final CheckBoxInput paid = new CheckBoxInput();
    private final MultiLineTextInput notes = new MultiLineTextInput(Colors.IYel);
    private final VerticalPanel inputs = new VerticalPanel();
    private FocusGroup formPanel;

    @Override
    protected void setUp() {
        paid.setName("paid");
        notes.setName("notes");
        inputs.add(customer);
        inputs.add(paid);
        inputs.add(notes);
        formPanel = new FocusGroup(inputs);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(formPanel);
        bus.setRoot(root);
    }

    private void type(String text) {
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    private FormBinder<Sale> bind(Sale sale) {
        FormBinder<Sale> form = FormBinder.UTIL.get(sale, formPanel, bus);
        form.bindAll(inputs);
        return form;
    }

    public void testACheckBoxAndAMultiLineInputBindLikeAnyField() {
        bind(new Sale("Acme", true, "rush\norder"));
        assertEquals("Acme", customer.getValue());
        assertTrue(paid.getValue());
        assertEquals("rush\norder", notes.getValue());
    }

    public void testSetFormDataShowsAnotherObjectAndEditsGoIntoIt() {
        Sale first = new Sale("Acme", true, "rush"), second = new Sale("Bolt Co", false, "");
        FormBinder<Sale> form = bind(first);

        form.setFormData(second);
        assertEquals("Bolt Co", customer.getValue());
        assertFalse(paid.getValue());

        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(customer)));
        type("x");
        assertEquals("the edit went into the object shown", "Bolt Cox", second.customer);
        assertEquals("not into the one shown before", "Acme", first.customer);
    }

    public void testSetFormDataWithTheSameObjectShowsItsValuesAgain() {
        Sale sale = new Sale("Acme", false, "");
        FormBinder<Sale> form = bind(sale);
        sale.customer = "Acme Repairs";                  // changed in code (or put back from a copy)
        form.setFormData(sale);
        assertEquals("Acme Repairs", customer.getValue());
    }

    public void testSetFormDataClearsAnErrorShown() {
        Sale sale = new Sale("A", false, "");
        FormBinder<Sale> form = bind(sale);
        List<String> shown = new ArrayList<String>();
        form.withValidatorHandler((eventBus, error, data) -> shown.add(error == null ? "clear" : error.getMessage()));
        form.<String>getFormBinderField("customer").withValidator((String v) -> v != null && !v.isEmpty(), "a customer is needed");

        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(customer)));
        type("\u007f");                                      // backspace: now empty
        assertTrue(form.isInError());
        assertEquals("a customer is needed", shown.get(shown.size() - 1));

        form.setFormData(new Sale("Bolt Co", false, ""));
        assertFalse(form.isInError());
        assertEquals("clear", shown.get(shown.size() - 1));
    }
}
