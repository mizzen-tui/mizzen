package dtb.terminalIO.components.tab;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import dtb.terminalIO.traverse.Traverser;
import junit.framework.TestCase;

/**
 * Characterisation tests for TabPanel focus behaviour (document-style tabs with a navigable
 * strip, see TabPanel): click / shortcut activate a tab and focus its content; Left/Right on the
 * strip show tabs while focus stays on the strip; TAB goes strip -> content.
 * A failure means "behaviour changed" - decide whether that is wanted, then update.
 */
public class TabPanelCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;
    private TabPanel tabPanel;

    @Override
    protected void setUp() {
        // root[before, tabPanel{One: plain text, Two: input "two", Three: input "three"}]
        tabPanel = new TabPanel(Colors.IYel, new Dimension.Simple(12, 60));
        tabPanel.addTab("One", new TextComponent("just text, nothing focusable"));
        tabPanel.addTab("Two", field("two"));
        tabPanel.addTab("Three", field("three"));

        root = new CompositeWithFocusManager();
        root.add(field("before"));
        root.add(tabPanel);
        bus.setRoot(root);
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private TabButtonComponent tab(String text) {
        for (Component<?> c : Traverser.<Component<?>>asList((Component<?>) tabPanel,
                (Component<?> c) -> c instanceof TabButtonComponent)) {
            if (((TabButtonComponent) c).getTabText().trim().equals(text)) {
                return (TabButtonComponent) c;
            }
        }
        throw new IllegalArgumentException("no tab " + text);
    }

    /** a click: REQUEST_FOCUS for the tab button, then its onClick (as MouseEventOrchestrator does) */
    private void clickTab(String text) {
        TabButtonComponent tab = tab(text);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(tab)));
        tab.onClick(bus);
        bus.drain();
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focusedName() {
        Object f = FocusManager.UTIL.findCurrentFocus(root);
        if (f instanceof TabButtonComponent) return "tab " + ((TabButtonComponent) f).getTabText().trim();
        String name = String.valueOf(HasName.UTIL.getName(f));
        if (name.startsWith("TextInput[")) return name.substring("TextInput[".length(), name.indexOf(']'));
        return name;
    }

    private String activeTab() {
        for (String t : new String[] { "One", "Two", "Three" }) if (tab(t).isActive()) return t;
        return "-";
    }

    /** clicking a tab whose content can take focus: tab shown, focus goes into the content */
    public void testClickTabFocusesItsContent() {
        clickTab("Two");

        assertEquals("Two", activeTab());
        assertEquals("two", focusedName());
    }

    /** clicking a tab whose content cannot take focus: focus stays on the tab */
    public void testClickTabWithNonFocusableContentKeepsFocusOnTab() {
        clickTab("One");

        assertEquals("One", activeTab());
        assertEquals("tab One", focusedName());
    }

    /** Left/Right on the strip show tabs (wrapping) while focus stays on the strip */
    public void testArrowsOnStripShowTabsAndKeepFocusOnStrip() {
        clickTab("One");
        StringBuilder seq = new StringBuilder();
        for (KeyStroke k : new KeyStroke[] { Keys.RIGHT_KEYSTROKE, Keys.RIGHT_KEYSTROKE,
                                             Keys.RIGHT_KEYSTROKE, Keys.LEFT_KEYSTROKE }) {
            press(k);
            seq.append(activeTab()).append('/').append(focusedName()).append(' ');
        }

        assertEquals("Two/tab Two Three/tab Three One/tab One Three/tab Three", seq.toString().trim());
    }

    /** TAB from the strip goes into the shown tab's content */
    public void testTabFromStripGoesIntoContent() {
        clickTab("One");
        press(Keys.RIGHT_KEYSTROKE);         // show Two, focus on the strip

        press(Keys.TAB_KEYSTROKE);

        assertEquals("two", focusedName());
    }

    /** prev/next shortcuts work from inside the content: switch tab and focus its content */
    public void testShortcutFromContentSwitchesTabAndFocusesContent() {
        tabPanel.setPrevKeyStroke(Keys.CTRL_LEFT_KEYSTROKE);
        tabPanel.setNextKeyStroke(Keys.CTRL_RIGHT_KEYSTROKE);
        clickTab("Two");                     // focus in "two"

        press(Keys.CTRL_RIGHT_KEYSTROKE);
        String afterNext = activeTab() + "/" + focusedName();
        press(Keys.CTRL_LEFT_KEYSTROKE);

        assertEquals("Three/three", afterNext);
        assertEquals("Two/two", activeTab() + "/" + focusedName());
    }

    /** no shortcuts are set by default: Ctrl-Right inside the content does nothing */
    public void testNoShortcutsByDefault() {
        clickTab("Two");

        press(Keys.CTRL_RIGHT_KEYSTROKE);

        assertEquals("Two/two", activeTab() + "/" + focusedName());
    }
}
