package mizzen.events.mouse;

import static mizzen.events.mouse.MouseEventsForTesting.click;

import mizzen.components.Component;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.LabelFor;
import mizzen.components.input.LabeledInputComponent;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.Form;
import mizzen.controlcodes.color.Colors;
import mizzen.core.Canvas;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A click reaches a LabelFor though it takes no focus (a clickable below focus), and focuses the input
 * it names - on its own, in a LabeledInputComponent, on a Form's row. Through MouseEventOrchestrator,
 * as an app gets its clicks.
 */
public class LabelForClickTest extends TestCase {

    /** the orchestrator's mouse handler goes in front of the root, as in an app */
    private final SyncEventLoop bus = new SyncEventLoop() {
        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            return prependEventHandler(eventHandler);
        }
    };
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final TextInput first = new TextInput(Colors.IYel, 10);

    @Override
    protected void setUp() throws Exception {
        first.setName("first");
        root.add(0, 0, first);
    }

    private void start() throws Exception {
        Canvas canvas = CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput();
        canvas.addComponentSegments(root);
        bus.setRoot(root);
        new MouseEventOrchestrator(null, canvas, bus).appendApplicationMouseEventToClickable(root, null);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(first)));
        assertEquals("first", focused());
    }

    private void clickOn(Component<?> component) {
        Position at = Component.UTIL.getCanvasOffsetFor(component);
        bus.deliver(click(at.getRow(), at.getCol() + 1, bus));
    }

    private String focused() {
        Focusable focus = FocusManager.UTIL.findCurrentFocus(root);
        return focus == null ? "-" : HasName.UTIL.getName(focus.getFocusComponent());
    }

    public void testAClickOnALabelForFocusesItsInput() throws Exception {
        TextInput name = new TextInput(Colors.IYel, 10);
        name.setName("name");
        TextComponent text = new TextComponent(Colors.BWhi, "Name :");
        root.add(2, 0, new HorizontalPanel().add(new LabelFor(text).focuses(name)).add(name));
        start();
        clickOn(text);
        assertEquals("name", focused());
    }

    public void testAClickOnALabeledInputsLabelFocusesItsInput() throws Exception {
        TextInput city = new TextInput(Colors.IYel, 10);
        city.setName("city");
        LabeledInputComponent<String> field = new LabeledInputComponent<String>("City :", city);
        root.add(2, 0, field);
        start();
        clickOn(field);                                  // the label is first in the field's row
        assertEquals("city", focused());
    }

    public void testAClickOnAFormRowsLabelFocusesItsInput() throws Exception {
        Form<Place> form = Form.of(new Place()).text("Name", "name").text("City", "city");
        root.add(2, 0, form);
        start();
        clickOn(form.row("city").label());
        assertSame(form.row("city").input(TextInput.class), FocusManager.UTIL.findCurrentFocus(root).getFocusComponent());
    }

    /** text that isn't a LabelFor: a click on it still goes where it always went - nothing to focus */
    public void testAClickOnPlainTextFocusesNothingNew() throws Exception {
        TextComponent plain = new TextComponent(Colors.BWhi, "just text");
        root.add(2, 0, plain);
        start();
        clickOn(plain);
        assertEquals("first", focused());
    }

    public static class Place {
        public String name = "";
        public String city = "";
    }
}
