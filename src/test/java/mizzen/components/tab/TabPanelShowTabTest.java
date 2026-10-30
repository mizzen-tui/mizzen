package mizzen.components.tab;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/** A tab shown by name; a name it doesn't have fails, saying which it has. */
public class TabPanelShowTabTest extends TestCase {

    public void testNoSuchTabFailsSayingWhich() {
        SyncEventLoop bus = new SyncEventLoop();
        TabPanel panel = new TabPanel(Colors.IGre, new Dimension.Simple(10, 40));
        panel.addTab("Docs", new TextComponent(Colors.Whi, "docs"));
        panel.addTab("Code", new TextComponent(Colors.Whi, "code"));
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(panel);
        bus.setRoot(root);
        panel.showTab("Code", bus);
        bus.drain();
        try {
            panel.showTab("Nope", bus);
            fail("no such tab");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Docs") && e.getMessage().contains("Code"));
        }
    }
}
