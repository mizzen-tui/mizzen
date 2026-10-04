package dtb.terminalIO.components.input.form;

import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.InputBytesEventParser;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation test for form-level change handling.
 *
 * <p/>
 *
 * Typing in a field travels as a relay of VALUE_CHANGED events, all delivered through the focus
 * tree: TextInput -> its own onChange (bound by the FormBinder) -> field-changed event -> handlers
 * registered on the FORM PANEL (an ancestor of the input) -> form-changed event. The focus
 * redesign must keep delivering events through ancestors (docs/ARCHITECTURE.md §4.3 B).
 */
public class FormChangeCharacterisationTest extends TestCase {

    public static class Address {
        public String street = "";
        public String city = "";
    }

    private final SyncEventLoop bus = new SyncEventLoop();

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 20);
        field.setName(name);
        return field;
    }

    /** keystrokes go through the real input parser, as typed */
    private void type(String text) {
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    public void testTypingInFieldUpdatesDataAndFiresFieldAndFormOnChange() {
        Address address = new Address();
        TextInput street = field("street"), city = field("city");

        // form panel nested inside the root, as in a typical app
        VerticalPanel inputs = new VerticalPanel();
        inputs.add(street);
        inputs.add(city);
        ComponentWithFocusManager<?> formPanel = new ComponentWithFocusManager<>(inputs);

        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(field("elsewhere"));
        root.add(formPanel);
        bus.setRoot(root);

        FormBinder<Address> form = FormBinder.UTIL.get(address, formPanel, bus);
        int[] fieldChanged = { 0 }, formChanged = { 0 };
        form.bindField("street", street).onChange(te -> fieldChanged[0]++);
        form.bindField("city", city);
        form.onChange(te -> formChanged[0]++);

        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(street)));
        type("x");

        assertEquals("x", address.street);
        assertEquals("field onChange", 1, fieldChanged[0]);
        assertEquals("form onChange", 1, formChanged[0]);
    }

    /**
     * TODAY: the field / form onChange handlers live on the form panel's chain, so they only fire
     * if the form panel is an ancestor of the input in the focus tree. Here the panel holding the
     * handlers is not in the tree: the data still updates (that handler is on the TextInput itself)
     * but field and form onChange never fire. Also proves the previous test passes because of
     * delivery through the ancestor.
     */
    public void testFormOnChangeNeedsFormPanelAsAncestorInFocusTree() {
        Address address = new Address();
        TextInput street = field("street");

        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(street);                                   // input directly under root
        bus.setRoot(root);
        ComponentWithFocusManager<?> detachedPanel = new ComponentWithFocusManager<>(new VerticalPanel());

        FormBinder<Address> form = FormBinder.UTIL.get(address, detachedPanel, bus);
        int[] fieldChanged = { 0 }, formChanged = { 0 };
        form.bindField("street", street).onChange(te -> fieldChanged[0]++);
        form.onChange(te -> formChanged[0]++);

        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(street)));
        type("x");

        assertEquals("x", address.street);
        assertEquals("field onChange", 0, fieldChanged[0]);
        assertEquals("form onChange", 0, formChanged[0]);
    }
}
