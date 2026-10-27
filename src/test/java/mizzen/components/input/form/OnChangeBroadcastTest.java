package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.Registration;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.FocusGroup;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * onChange broadcasts: an app's listeners and a form's run side by side, whichever came first; each
 * Registration takes out only its own listener; a form bound again keeps one listener per field.
 */
public class OnChangeBroadcastTest extends TestCase {

    public static class Person {
        public String name = "";
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private TextInput name;
    private FocusGroup formPanel;

    @Override
    protected void setUp() {
        name = new TextInput(Colors.IYel, 20);
        name.setName("name");
        VerticalPanel inputs = new VerticalPanel();
        inputs.add(name);
        formPanel = new FocusGroup(inputs);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(formPanel);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(name)));
    }

    private void type(String text) {
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    public void testListenersAllRunInTheOrderAdded() {
        List<String> heard = new ArrayList<String>();
        name.onChange(te -> heard.add("first"));
        name.onChange(te -> heard.add("second"));
        type("a");
        assertEquals("[first, second]", heard.toString());
    }

    public void testUnregisterTakesOutOnlyItsOwnAndTwiceIsHarmless() {
        List<String> heard = new ArrayList<String>();
        Registration first = name.onChange(te -> heard.add("first"));
        name.onChange(te -> heard.add("second"));
        first.unregister();
        first.unregister();
        type("a");
        assertEquals("[second]", heard.toString());
    }

    public void testNullTakesOutEveryListener() {
        List<String> heard = new ArrayList<String>();
        name.onChange(te -> heard.add("first"));
        name.onChange(null);
        type("a");
        assertEquals("[]", heard.toString());
    }

    public void testTheAppAndAFormBothHearWhicheverCameFirst() {
        Person person = new Person();
        int[] app = { 0 };
        name.onChange(te -> app[0]++);                                   // the app first ...
        FormBinder.UTIL.get(person, formPanel, bus).bind("name", name);  // ... then the form
        type("a");
        assertEquals("the app's listener", 1, app[0]);
        assertEquals("the form's binding", "a", person.name);

        Person other = new Person();
        TextInput second = new TextInput(Colors.IYel, 20);
        FormBinder.UTIL.get(other, formPanel, bus).bind("name", second);  // the form first ...
        int[] later = { 0 };
        second.onChange(te -> later[0]++);                                // ... then the app
        assertEquals("nothing replaced: still nothing heard", 0, later[0]);
    }

    public void testAFormBoundAgainKeepsOneListenerAndItsRules() {
        Person person = new Person();
        FormBinder<Person> form = FormBinder.UTIL.get(person, formPanel, bus);
        int[] formChanged = { 0 };
        form.bindAll(formPanel);
        form.<String>getFormBinderField("name").withValidator((String v) -> !v.contains("x"), "no x");
        form.onChange(te -> formChanged[0]++);
        List<String> problems = new ArrayList<String>();
        form.withValidatorHandler((eventBus, problem, data) -> { if (problem != null) problems.add(problem.getMessage()); });
        form.bindAll(formPanel);                                        // again - as an app did inside a handler
        type("a");
        assertEquals("one binder listener: the object updated once", "a", person.name);
        assertEquals("form onChange once", 1, formChanged[0]);
        type("x");
        assertEquals("the rule set before binding again still applies", "a", person.name);
        assertEquals("the rule's message, once", "[no x]", problems.toString());
    }

    public void testUnbindTakesOutTheFormsListenersNotTheApps() {
        Person person = new Person();
        int[] app = { 0 };
        name.onChange(te -> app[0]++);
        FormBinder<Person> form = FormBinder.UTIL.get(person, formPanel, bus);
        form.bind("name", name);
        form.unbind();
        type("a");
        assertEquals("the object no longer follows", "", person.name);
        assertEquals("the app's listener still runs", 1, app[0]);
    }
}
