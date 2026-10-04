package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * LeafFocusBehaviour's onFocusGained / onFocusLost: told once each time focus really arrives /
 * leaves - by a focus request, TAB or SHIFT-TAB - whichever look. Layout: root[a, b].
 */
public class LeafFocusBehaviourTest extends TestCase {

    /** a leaf that records its focus changes */
    static class Leaf extends TextComponent implements ToFocusable {
        final LeafFocusBehaviour focus;

        Leaf(String name, LeafFocusBehaviour.Look look, List<String> heard) {
            super(Colors.IYel, name);
            setName(name);
            focus = LeafFocusBehaviour.of(this)
                                      .look(look)
                                      .onFocusGained((bus) -> heard.add("gained:" + name))
                                      .onFocusLost((bus) -> heard.add("lost:" + name));
        }

        @Override
        public Focusable asFocusable() {
            return focus.asFocusable();
        }
    }

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> heard = new ArrayList<String>();

    private void build(LeafFocusBehaviour.Look look) throws Exception {
        Leaf a = new Leaf("a", look, heard);
        Leaf b = new Leaf("b", look, heard);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(a);
        root.add(1, 0, b);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(a.asFocusable()));
    }

    private void press(dtb.terminalIO.events.keyboard.KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    public void testHilitedLeaf() throws Exception {
        build(LeafFocusBehaviour.Look.HILITE);
        press(Keys.TAB_KEYSTROKE);
        press(Keys.SHIFT_TAB_KEYSTROKE);

        assertEquals("[gained:a, lost:a, gained:b, lost:b, gained:a]", heard.toString());
    }

    public void testLeafThatShowsFocusItself() throws Exception {
        build(LeafFocusBehaviour.Look.NONE);
        press(Keys.TAB_KEYSTROKE);
        press(Keys.TAB_KEYSTROKE);       // wraps around to a

        assertEquals("[gained:a, lost:a, gained:b, lost:b, gained:a]", heard.toString());
    }

    /** the look decides how the focusable is built: setting it afterwards is a mistake, said at once */
    public void testLookAfterUseFails() {
        LeafFocusBehaviour focus = LeafFocusBehaviour.of(new TextComponent("x"));
        focus.asFocusable();
        try {
            focus.look(LeafFocusBehaviour.Look.NONE);
            fail("look after asFocusable() must fail");
        } catch (IllegalStateException expected) {
            // ok
        }
    }
}
