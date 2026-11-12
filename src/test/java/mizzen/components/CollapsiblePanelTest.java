package mizzen.components;

import mizzen.components.composite.VerticalPanel;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * A CollapsiblePanel's height follows open and closed, also once it has been given a size; given a width, its title
 * fills it, so the (+) is at the right edge; without colours it takes the theme's.
 */
public class CollapsiblePanelTest extends TestCase {

    private static VerticalPanel threeLines() {
        return new VerticalPanel().addText("one").addText("two").addText("three");
    }

    public void testSizedClosedItOpensToItsContentAndClosesAgain() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        VerticalPanel parent = new VerticalPanel();
        parent.add(panel);
        panel.resize(new Dimension.Simple(3, 40));
        assertFalse(panel.isOpen());
        assertEquals("closed: frame and title", 3, panel.getHeight());

        panel.setOpen(true);
        assertTrue(panel.isOpen());
        assertEquals("open: and the three lines", 6, panel.getHeight());
        assertEquals("the width is kept", 40, panel.getWidth());
        assertEquals("the parent follows", 6, parent.getHeight());

        panel.setOpen(false);
        assertEquals(3, panel.getHeight());
        assertEquals(3, parent.getHeight());
    }

    public void testNeverSizedItFollowsToo() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        int closed = panel.getHeight();
        panel.setOpen(true);
        assertEquals(closed + 3, panel.getHeight());
        panel.setOpen(false);
        assertEquals(closed, panel.getHeight());
    }

    public void testTheButtonShowsWhatAPressDoes() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        assertEquals("(+)", panel.openCloseButton.getButtonText());
        panel.setOpen(true);
        assertEquals("(-)", panel.openCloseButton.getButtonText());
        panel.setOpen(true);                                    // already open: no change
        assertEquals("(-)", panel.openCloseButton.getButtonText());
    }

    public void testTheTitleFillsTheWidthGivenLater() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        panel.resize(new Dimension.Simple(3, 40));
        assertEquals("the (+) at the right edge, inside the frame", 38, panel.titleBanner.getWidth());
        panel.resize(new Dimension.Simple(3, 60));
        assertEquals(58, panel.titleBanner.getWidth());
        panel.resize(new Dimension.Simple(3, 30));
        assertEquals("narrower again", 28, panel.titleBanner.getWidth());
    }

    public void testOpenItTakesTheRowsAParentGivesIt() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        panel.setOpen(true);
        panel.resize(new Dimension.Simple(10, 40));
        assertEquals(10, panel.getHeight());
        panel.setOpen(false);
        assertEquals("closed, whatever it was given", 3, panel.getHeight());
    }

    public void testWithoutColoursTheButtonTakesTheThemes() {
        CollapsiblePanel panel = new CollapsiblePanel("2 warnings", threeLines());
        assertNull("no colour of its own: the theme's button colour", panel.openCloseButton.getOwnColor());
    }
}
