package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Form, the builder: rows bound by the binder's traversal, rules and their errors under the rows,
 * validateAll, options and labels from the object, a panel of one's own bound by name.
 */
public class FormBuilderTest extends TestCase {

    public static class Address {
        public String name = "";
        public String country = "Canada";
        public String region = "Ontario";
        public Integer quantity = 1;
        public int count = 1;
    }

    private final SyncEventLoop bus = new SyncEventLoop();

    private void show(Form<?> form) {
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(form);
        bus.setRoot(root);
    }

    private void typeInto(Form<?> form, String field, String text) {
        TextInput input = form.row(field).input(TextInput.class);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(input)));
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    public void testARuleShowsItsErrorUnderItsRowAndClearsWhenFixed() {
        Address address = new Address();
        Form<Address> form = Form.of(address).text("Name", "name").validate((String n) -> !n.contains("!"), "no !");
        show(form);
        typeInto(form, "name", "a");                    // good: into the object
        typeInto(form, "name", "!");                    // breaks the rule
        assertTrue("error shown", form.row("name").hasError());
        assertEquals("no !", form.row("name").error().getText());
        assertEquals("the object keeps its last good value", "a", address.name);
        new InputBytesEventParser().parseInput(new byte[] { 127 }, 1, bus);   // backspace
        bus.drain();
        assertFalse("error gone", form.row("name").hasError());
    }

    public void testRequiredAndValidateAll() {
        Address address = new Address();
        Form<Address> form = Form.of(address).text("Name", "name").required();
        show(form);
        assertFalse("empty name: not valid", form.validateAll());
        assertEquals("Name is required", form.row("name").error().getText());
        typeInto(form, "name", "Ann");
        assertTrue(form.validateAll());
        assertFalse(form.row("name").hasError());
    }

    public void testOptionsAndLabelsFollowTheObjectAndStaleValuesAreCleared() {
        Address address = new Address();
        Form<Address> form = Form.of(address)
                .choice("Country", "country", Arrays.asList("Canada", "USA"))
                .choice((a) -> "USA".equals(a.country) ? "State" : "Province", "region",
                        (a) -> "USA".equals(a.country) ? Arrays.asList("Maine") : Arrays.asList("Ontario", "Quebec"));
        address.country = "USA";
        form.refresh();
        assertEquals("State", form.row("region").label().getText().trim());
        assertNull("Ontario is no longer an option: cleared", address.region);
    }

    public void testAPanelOfOnesOwnIsBoundByName() {
        Address address = new Address();
        TextInput name = new TextInput(Colors.IYel, 10);
        name.setName("name");
        VerticalPanel mine = new VerticalPanel();
        mine.add(name);
        Form<Address> form = Form.of(address).add(mine);
        show(form);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(name)));
        new InputBytesEventParser().parseInput("Bo".getBytes(), 2, bus);
        bus.drain();
        assertEquals("bound by the traversal, like a row", "Bo", address.name);
        assertNotNull(form.binder().getFormBinderField("name"));
    }

    public void testAnUnknownFieldFailsSayingWhatThereIs() {
        try {
            Form.of(new Address()).text("Nmae", "nmae");
            fail("a row for a field the object hasn't should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("no public field") && e.getMessage().contains("name"));
        }
    }

    /** null and primitives reach the object (the reflection setter swallowed both before) */
    public void testNullAndPrimitiveFieldsAreWritten() {
        Address address = new Address();
        FormBinder<Address> binder = FormBinder.UTIL.get(address, bus);
        GetterSetter<Integer> count = new GetterSetter<Integer>(() -> 7, (v) -> { });
        binder.bindField("count", count).copyComponentValueToFormField();
        assertEquals("an Integer into an int field", 7, address.count);
        GetterSetter<String> region = new GetterSetter<String>(() -> null, (v) -> { });
        binder.bindField("region", region).copyComponentValueToFormField();
        assertNull("null into an object field", address.region);
    }

    public void testErrorsGoToEveryDisplay() {
        Address address = new Address();
        Form<Address> form = Form.of(address).text("Name", "name").required();
        List<String> shown = new ArrayList<String>();
        form.binder().errorsTo((field, message) -> shown.add(field + ":" + message));
        form.validateAll();
        assertEquals(Collections.singletonList("name:Name is required"), shown);
        assertTrue("and under the row, the form's own display", form.row("name").hasError());
    }
}
