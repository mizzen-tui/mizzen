package mizzen.components.input.form;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.components.input.TextInput;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.util.ReflectionUtils;
import junit.framework.TestCase;

/**
 * A form binds a bean's properties as it binds public fields: by name, its getter first (getX, isX
 * for a boolean), else its public field; its setter likewise - so the class's own code runs.
 */
public class BeanFormTest extends TestCase {

    public enum Size { SMALL, LARGE }

    /** a getter a bean inherits */
    public static class Named {
        private String name = "";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name.trim();                     // the class's own rule: proof the setter is used, not the field
        }
    }

    public static class Person extends Named {
        private int age = 30;
        private boolean employed = true;
        private Size size = Size.LARGE;
        public String nickname = "Al";                   // a public field: still bound

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isEmployed() {
            return employed;
        }

        public void setEmployed(boolean employed) {
            this.employed = employed;
        }

        public Size getSize() {
            return size;
        }

        public void setSize(Size size) {
            this.size = size;
        }

        public String getSummary() {                     // read only: a getter, no setter
            return getName() + ", " + age;
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private final Person person = new Person();
    private Form<Person> form;

    @Override
    protected void setUp() throws Exception {
        person.setName("Alice");
        form = Form.of(person)
                .text("Name", "name")
                .text("Nickname", "nickname")
                .number("Age", "age")
                .checkBox("Employed", "employed")
                .choice("Size", "size")                      // an enum: options from the getter's type
                .view("Summary", (p) -> p.getSummary());
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(form);
        bus.setRoot(root);
    }

    private void typeInto(String field, String text) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(form.row(field).input(TextInput.class))));
        new InputBytesEventParser().parseInput(text.getBytes(), text.length(), bus);
        bus.drain();
    }

    public void testTheInputsShowTheBeansProperties() {
        assertEquals("Alice", form.row("name").input(TextInput.class).getText().trim());
        assertEquals(Integer.valueOf(30), form.row("age").input(IntegerInputWithIncrDecrButtons.class).getValue());
        assertEquals(Boolean.TRUE, form.row("employed").input(CheckBoxInput.class).getValue());
        assertEquals(Size.LARGE, form.row("size").input(DropdownListBox.class).getValue());
        assertEquals("Al", form.row("nickname").input(TextInput.class).getText().trim());
    }

    /** an inherited setter, and its own rule (trimming) runs */
    public void testEditsGoThroughTheSetter() {
        typeInto("name", " ");                           // the input says "Alice " ...
        assertEquals("... setName trimmed it (the field alone would keep the space)", "Alice", person.getName());
    }

    public void testAPublicFieldStillBinds() {
        typeInto("nickname", "!");
        assertEquals("Al!", person.nickname);
    }

    public void testAnUnknownPropertyFailsListingTheBeansProperties() {
        try {
            Form.of(new Person()).text("Nmae", "nmae");
            fail("no such property");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("name") && e.getMessage().contains("employed") && e.getMessage().contains("age"));
        }
    }

    /** the lookups themselves: getter before field, a primitive boolean's isX, an int setter taking an Integer */
    public void testTheReflectionLookups() throws Exception {
        assertEquals(Boolean.TRUE, ReflectionUtils.getValueFromTarget(person, "employed"));
        ReflectionUtils.setValueOnTarget(person, "age", Integer.valueOf(41));
        assertEquals(41, person.getAge());
        assertEquals(Size.class, ReflectionUtils.getPropertyType(person, "size"));
        assertTrue(ReflectionUtils.getFieldNames(person).containsAll(java.util.Arrays.asList("name", "age", "employed", "size", "summary", "nickname")));
        assertFalse("not Object's getClass()", ReflectionUtils.getFieldNames(person).contains("class"));
        try {
            ReflectionUtils.setValueOnTarget(person, "summary", "x");
            fail("read only");
        } catch (NoSuchFieldException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("read only"));
        }
    }
}
