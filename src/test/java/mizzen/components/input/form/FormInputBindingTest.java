package mizzen.components.input.form;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.FormInput;
import mizzen.components.input.TextInput;
import mizzen.components.input.ToFormInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventHandler;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.FocusGroup;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Binding through FormInput: a component's own (it is one, or a ToFormInput), a registered one (global
 * or the binder's), or one given for the field - in that order of preference, the last first - and
 * failing at once when there is none.
 */
public class FormInputBindingTest extends TestCase {

    public static class Customer {
        public String name = "Ann";
        public String colour = "red";
        public String note = "";
    }

    /** a component that isn't an input: binds only through a registered or given FormInput */
    static class Swatch extends TextComponent {
        Swatch(String colour) {
            super(colour);
        }
    }

    /** a FormInput over a Swatch's text - what an app would write for someone else's component */
    static class SwatchInput implements FormInput<String> {
        final Swatch swatch;
        final String tag;                                // which way it was found, for the tests
        EventHandler changed;

        SwatchInput(Swatch swatch, String tag) {
            this.swatch = swatch;
            this.tag = tag;
        }

        public String getValue() {
            return swatch.getText();
        }

        public void setValue(String value) {
            swatch.setTextAndResize(value + "(" + tag + ")");
        }

        public void onChange(EventHandler handler) {
            this.changed = handler;
        }
    }

    /** a component that can act as an input: ToFormInput */
    static class Picker extends TextComponent implements ToFormInput<String> {
        Picker() {
            super("?");
        }

        public FormInput<String> asFormInput() {
            return new FormInput<String>() {
                public String getValue() {
                    return getText();
                }

                public void setValue(String value) {
                    setTextAndResize(value);
                }

                public void onChange(EventHandler handler) {
                }
            };
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();

    public void testAnInputBindsItselfShowsTheValueAndTypingUpdatesTheObject() {
        Customer customer = new Customer();
        TextInput name = new TextInput(Colors.IYel, 20);
        VerticalPanel inputs = new VerticalPanel();
        inputs.add(name);
        FocusGroup formPanel = new FocusGroup(inputs);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(formPanel);
        bus.setRoot(root);

        FormBinder<Customer> form = FormBinder.UTIL.get(customer, formPanel, bus);
        form.bind("name", name);
        assertEquals("the object's value shows at once", "Ann", name.getValue());

        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(name)));
        new InputBytesEventParser().parseInput("x".getBytes(), 1, bus);
        bus.drain();
        assertTrue("typing reaches the object: " + customer.name, customer.name.contains("x"));
    }

    public void testAToFormInputBindsThroughAsFormInput() {
        Customer customer = new Customer();
        Picker picker = new Picker();
        FormBinder.UTIL.get(customer, bus).bind("colour", picker);
        assertEquals("red", picker.getText());
    }

    public void testRegisteredFormInputsAndTheirOrder() {
        Customer customer = new Customer();
        FormInputs.register(Swatch.class, s -> new SwatchInput(s, "global"));

        Swatch a = new Swatch("");
        FormBinder.UTIL.get(customer, bus).bind("colour", a);
        assertEquals("the global registry", "red(global)", a.getText());

        Swatch b = new Swatch("");
        FormBinder.UTIL.get(customer, bus).register(Swatch.class, s -> new SwatchInput(s, "binder")).bind("colour", b);
        assertEquals("the binder's registry before the global one", "red(binder)", b.getText());

        Swatch c = new Swatch("");
        FormBinder<Customer> form = FormBinder.UTIL.get(customer, bus).register(Swatch.class, s -> new SwatchInput(s, "binder"));
        form.bind("colour", c, s -> new SwatchInput(s, "field"));
        assertEquals("given for the field, before any registry", "red(field)", c.getText());
    }

    public void testNoFormInputFailsAtOnceSayingWhatToAdd() {
        TextComponent label = new TextComponent("not an input");
        try {
            FormBinder.UTIL.get(new Customer(), bus).bind("note", label);
            fail("binding a component with no FormInput should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("No FormInput for TextComponent"));
            assertTrue(e.getMessage(), e.getMessage().contains("FormInputs.register(TextComponent.class"));
            assertTrue(e.getMessage(), e.getMessage().contains("field \"note\""));
        }
    }

    public void testBindAllFindsAToFormInputByName() {
        Customer customer = new Customer();
        Picker picker = new Picker();
        picker.setName("colour");
        VerticalPanel panel = new VerticalPanel();
        panel.add(picker);
        FormBinder.UTIL.get(customer, bus).bindAll(panel);
        assertEquals("red", picker.getText());
    }
}
