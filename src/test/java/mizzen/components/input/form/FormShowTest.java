package mizzen.components.input.form;

import java.util.Arrays;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.TextInput;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * form.show(object): the form edits another object - its values in the inputs, edits into it, its
 * options, labels and views; errors from before gone. And .view(label, text): a line worked out from
 * the object.
 */
public class FormShowTest extends TestCase {

    public static class Address {
        public String name;
        public String country;
        public String region;

        Address(String name, String country, String region) {
            this.name = name;
            this.country = country;
            this.region = region;
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private final Address ann = new Address("Ann", "Canada", "Quebec");
    private final Address bob = new Address("Bob", "USA", "Maine");
    private Form<Address> form;

    @Override
    protected void setUp() throws Exception {
        form = Form.of(ann)
                .text("Name", "name").required()
                .choice("Country", "country", Arrays.asList("Canada", "USA"))
                .choice((a) -> "USA".equals(a.country) ? "State" : "Province", "region",
                        (a) -> "USA".equals(a.country) ? Arrays.asList("Maine", "Vermont") : Arrays.asList("Ontario", "Quebec"))
                .view("Lives in", (a) -> a.region + ", " + a.country);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(form);
        bus.setRoot(root);
    }

    private TextInput name() {
        return form.row("name").input(TextInput.class);
    }

    private void typeIntoName(String text) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(name())));
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    public void testTheInputsShowTheNewObject() {
        form.show(bob);
        assertSame(bob, form.data());
        assertEquals("Bob", name().getText().trim());
        assertEquals("USA", form.row("country").input(DropdownListBox.class).getValue());
    }

    /** Maine is only among the options for the USA: they change before the value goes in */
    public void testItsOptionsAndLabelsComeFirst() {
        form.show(bob);
        assertEquals("Maine", form.row("region").input(DropdownListBox.class).getValue());
        assertEquals("State", form.row("region").label().getText().trim());
        assertEquals("the one before is untouched", "Quebec", ann.region);
    }

    public void testEditsGoIntoTheNewObjectNotTheOneBefore() {
        form.show(bob);
        typeIntoName("!");
        assertEquals("Bob!", bob.name);
        assertEquals("Ann", ann.name);
    }

    /** the text view row is drawn, and worked out again for the new object */
    public void testViewsFollow() {
        assertTrue(drawn(), drawn().contains("Quebec, Canada"));
        form.show(bob);
        assertTrue(drawn(), drawn().contains("Maine, USA"));
    }

    public void testErrorsFromTheOneBeforeAreGone() {
        ann.name = "";
        form.show(ann);
        assertFalse(form.validateAll());
        assertTrue(form.row("name").hasError());
        form.show(bob);
        assertFalse(form.row("name").hasError());
    }

    /** the same object again: read afresh, e.g. after the program put old values back */
    public void testShowingTheSameObjectAgainReadsItAfresh() {
        ann.name = "Annie";
        form.show(ann);
        assertEquals("Annie", name().getText().trim());
    }

    public void testShowNullFails() {
        try {
            form.show(null);
            fail("a form edits an object");
        } catch (NullPointerException expected) {
        }
    }

    // all the text the form draws
    private String drawn() {
        StringBuilder text = new StringBuilder();
        for (ScreenLineSegment segment : form.getSegmentsBuffer().getSegments()) {
            text.append(segment.getText()).append('|');
        }
        return text.toString();
    }
}
