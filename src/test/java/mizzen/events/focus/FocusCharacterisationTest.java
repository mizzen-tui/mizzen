package mizzen.events.focus;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import mizzen.supportfortesting.FocusGroup;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.HasValue;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import junit.framework.TestCase;

/**
 * Characterisation tests: they record what the focus system does TODAY, right or wrong,
 * so that the focus redesign (docs/ARCHITECTURE.md §4) cannot change behaviour silently.
 * A failure here means "behaviour changed" - decide whether the new behaviour is wanted,
 * then update the expectation deliberately.
 *
 * Events are delivered straight to the root focus manager (the way the EventBus would
 * after nothing earlier on the bus claimed them).
 */
public class FocusCharacterisationTest extends TestCase {

    /** EventBus that records raised events instead of queueing them (no thread). */
    static class RecordingEventBus extends EventBus.STUBBED {
        final List<TerminalEvent<?>> raised = new ArrayList<>();

        @Override
        public EventBus raiseEvent(TerminalEvent<?> te) {
            raised.add(te);
            return this;
        }

        @Override
        public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
            return this;
        }
    }

    private final RecordingEventBus bus = new RecordingEventBus();

    private <E extends TerminalEvent<?>> void deliver(FocusManager root, E event) {
        event.setEventBus(bus);
        if (root.canHandle(event) != null) {
            root.handle(event);
        }
    }

    private void press(FocusManager root, KeyStroke key) {
        deliver(root, new KeyEvent("test", key));
    }

    /** what a mouse click on a component does: REQUEST_FOCUS for its focusable */
    private void click(FocusManager root, Object component) {
        deliver(root, new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private static String focused(TextInput... fields) {
        StringJoiner names = new StringJoiner("+");
        for (TextInput f : fields) {
            if (ToFocusable.UTIL.toFocusable(f).hasFocus()) {
                names.add(f.getName());
            }
        }
        return names.length() == 0 ? "-" : names.toString();
    }

    /** sequence of focused field names: start, then after each key press */
    private String traverse(FocusManager root, KeyStroke key, int presses, TextInput... fields) {
        StringJoiner seq = new StringJoiner(" ");
        seq.add(focused(fields));
        for (int i = 0; i < presses; i++) {
            press(root, key);
            seq.add(focused(fields));
        }
        return seq.toString();
    }

    /**
     * root[A, group[B, C], D] -- root as TUIApp builds it (CompositeWithFocusManager),
     * group a ComponentWithFocus.
     */
    private CompositeWithFocusManager nestedLayout(TextInput a, TextInput b, TextInput c, TextInput d) {
        VerticalPanel groupPanel = new VerticalPanel();
        groupPanel.add(b);
        groupPanel.add(c);
        FocusGroup group = new FocusGroup(groupPanel);

        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(a);
        root.add(group);
        root.add(d);
        return root;
    }

    /**
     * TAB visits every field on every lap. (Before the focus redesign's navigator, re-entering the
     * nested group resumed its remembered child, so B was skipped after the first lap: A B C D A C D.)
     */
    public void testNestedGroupTabOrder_visitsEveryFieldEveryLap() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = nestedLayout(a, b, c, d);

        click(root, a);
        assertEquals("A B C D A B C D A B",
                     traverse(root, Keys.TAB_KEYSTROKE, 9, a, b, c, d));
    }

    /**
     * SHIFT-TAB enters the group at its last field. (Before the navigator it resumed the
     * remembered child, so C was never reached: A D B A D B.)
     */
    public void testNestedGroupShiftTabOrder_entersGroupAtLastField() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = nestedLayout(a, b, c, d);

        click(root, a);
        assertEquals("A D C B A D C",
                     traverse(root, Keys.SHIFT_TAB_KEYSTROKE, 6, a, b, c, d));
    }

    /**
     * A Button's onChange handler lives on the button's own handler chain, not on the bus,
     * so its ValueChangedEvent reaches it only by travelling down the focus tree
     * (docs/ARCHITECTURE.md §2.5). Must keep working through nested focus managers.
     */
    public void testButtonOnChangeIsDeliveredThroughNestedFocusTree() {
        Button button = new Button(Colors.IYel, "Save");
        int[] fired = { 0 };
        button.onChange((te) -> fired[0]++);

        VerticalPanel groupPanel = new VerticalPanel();
        groupPanel.add(field("inGroup"));
        groupPanel.add(button);
        FocusGroup group = new FocusGroup(groupPanel);

        TextInput first = field("first");
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(first);
        root.add(group);

        // focus is elsewhere (not on the button) when the button's value-changed event arrives
        click(root, first);
        deliver(root, new HasValue.ValueChangedEvent<>(button));

        assertEquals("button onChange should fire exactly once", 1, fired[0]);
    }

    /**
     * TODAY: delivery depends on the button being in the focus tree. A button that is not
     * (e.g. missing from a stale focus tree, docs/ARCHITECTURE.md §2.5) never sees its onChange.
     * Also proves the previous test passes because of the focus tree, not by accident.
     */
    public void testButtonOnChangeNotDeliveredWhenButtonNotInFocusTree() {
        Button button = new Button(Colors.IYel, "Save");
        int[] fired = { 0 };
        button.onChange((te) -> fired[0]++);

        TextInput first = field("first");
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(first);            // button deliberately not added

        click(root, first);
        deliver(root, new HasValue.ValueChangedEvent<>(button));

        assertEquals(0, fired[0]);
    }
}
