package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * RadioGroup: one TAB stop; arrows move between the options and choose as they go (wrapping);
 * TAB lands on the chosen option; a click chooses; the hot key chooses the next option from anywhere.
 * Layout: root[agree (checkbox), size (radio group: Small, Medium, Large), after (text field)].
 */
public class RadioGroupTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it
    private final CheckBoxInput agree = new CheckBoxInput(Colors.IYel);
    private final RadioGroup<String> size = RadioGroup.vertical("Small", "Medium", "Large");
    private final TextInput after = new TextInput(Colors.IYel, 10);
    private final List<String> changes = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        agree.setName("agree");
        after.setName("after");
        root.add(agree);
        root.add(size);
        root.add(after);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        size.onChange((te) -> changes.add(size.getValue()));
        bus.deliver(new RequestFocusEvent(agree.asFocusable()));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    /** the focused component: an option's text ("(•) Medium"), else its name */
    private String focused() {
        Focusable focusable = FocusManager.UTIL.findCurrentFocus(root);
        Component<?> component = focusable == null ? null : focusable.getFocusComponent();
        if (component instanceof TextComponent) {
            return ((TextComponent) component).getText().trim();
        }
        return dtb.terminalIO.components.HasName.UTIL.getName(component);
    }

    public void testStartsOnTheFirstOption() {
        assertEquals("Small", size.getValue());
    }

    /** TAB into the group lands on the chosen option, without changing the value */
    public void testTabLandsOnTheChosenOption() {
        press(Keys.TAB_KEYSTROKE);

        assertEquals("(•) Small", focused());
        assertEquals("Small", size.getValue());
        assertTrue("no change", changes.isEmpty());
    }

    /** DOWN moves to the next option and chooses it; past the last it wraps to the first */
    public void testArrowsChooseAndWrap() {
        press(Keys.TAB_KEYSTROKE);

        press(Keys.DOWN_KEYSTROKE);
        assertEquals("(•) Medium", focused());
        assertEquals("Medium", size.getValue());

        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);
        assertEquals("(•) Small", focused());
        assertEquals("[Medium, Large, Small]", changes.toString());

        press(Keys.UP_KEYSTROKE);
        assertEquals("Large", size.getValue());
    }

    /** the group is one TAB stop: TAB leaves it; SHIFT-TAB comes back to the chosen option */
    public void testOneTabStop() {
        press(Keys.TAB_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);          // Medium

        press(Keys.TAB_KEYSTROKE);
        assertEquals("after", focused());

        press(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals("(•) Medium", focused());
    }

    /** setValue: TAB then lands on that option */
    public void testSetValueMovesTheEntry() {
        size.setValue("Large");

        press(Keys.TAB_KEYSTROKE);

        assertEquals("(•) Large", focused());
    }

    /** a click on an option chooses it */
    public void testClickChooses() {
        Component<?> large = new ArrayList<Component<?>>(size.getWrappedComponent().getChildren()).get(2);

        ((Clickable) large).onClick(bus);
        bus.drain();

        assertEquals("Large", size.getValue());
    }

    /** the hot key, pressed while another component has focus, chooses the next option */
    public void testHotKeyFromElsewhere() {
        size.setHotKey(Keys.F2_KEYSTROKE);

        press(Keys.F2_KEYSTROKE);
        assertEquals("Medium", size.getValue());

        press(Keys.F2_KEYSTROKE);
        assertEquals("Large", size.getValue());
    }

    /** onOff: the two-option case, value true / false; its hot key toggles */
    public void testOnOff() {
        RadioGroup<Boolean> wifi = RadioGroup.onOff("Wifi", true).setHotKey(Keys.F5_KEYSTROKE);
        root.add(wifi);

        assertEquals(Boolean.TRUE, wifi.getValue());
        press(Keys.F5_KEYSTROKE);
        assertEquals(Boolean.FALSE, wifi.getValue());
        press(Keys.F5_KEYSTROKE);
        assertEquals(Boolean.TRUE, wifi.getValue());
    }

    /** the look: markers, ASCII style, text before the marker, a label */
    public void testLooks() {
        RadioGroup<String> look = RadioGroup.horizontal("A", "B").setLabel("Pick");
        assertEquals("[Pick, (•) A, ( ) B]", texts(look));

        look.asciiStyle();
        assertEquals("[Pick, (*) A, ( ) B]", texts(look));

        look.textBeforeMarker(true);
        assertEquals("[Pick, A (*), B ( )]", texts(look));

        look.setHotKey(Keys.F2_KEYSTROKE);
        assertEquals("[F2: Pick, A (*), B ( )]", texts(look));
    }

    /** options are DisplayableValues, like ListBox items: the value and the text shown can differ */
    public void testDisplayableValueOptions() {
        RadioGroup<Integer> level = RadioGroup.vertical(new DisplayableValue.SimpleDisplayValue<Integer>("Low", 1),
                                                        new DisplayableValue.SimpleDisplayValue<Integer>("High", 9));
        assertEquals("[(•) Low, ( ) High]", texts(level));
        assertEquals(Integer.valueOf(1), level.getValue());

        level.setValue(9);
        assertEquals("[( ) Low, (•) High]", texts(level));
    }

    /** setItems replaces the options (like ListBox.setItems); the value stays if it is still there, else the first */
    public void testSetItems() {
        size.setValue("Large");

        size.setItems(java.util.Arrays.asList(DisplayableValue.StringDisplayValue.fromStringValues(new String[] { "Medium", "Large", "Huge" })));
        assertEquals("Large", size.getValue());
        assertEquals("[( ) Medium, (•) Large, ( ) Huge]", texts(size));

        size.setItems(java.util.Arrays.asList(DisplayableValue.StringDisplayValue.fromStringValues(new String[] { "S", "M" })));
        assertEquals("S", size.getValue());
    }

    /** it is a settable value, so it works as a form field (LabeledInputComponent, form binders) */
    public void testFormField() {
        RadioGroup<String> colour = RadioGroup.horizontal("Red", "Green", "Blue");
        LabeledInputComponent<String> field = new LabeledInputComponent<String>("Colour : ", colour);

        field.setValue("Blue");

        assertEquals("Blue", colour.getValue());
        assertEquals("Blue", field.getValue());
    }

    private static String texts(RadioGroup<?> group) {
        List<String> texts = new ArrayList<String>();
        for (Component<?> child : group.getWrappedComponent().getChildren()) {
            texts.add(((TextComponent) child).getText().trim());
        }
        return texts.toString();
    }
}
