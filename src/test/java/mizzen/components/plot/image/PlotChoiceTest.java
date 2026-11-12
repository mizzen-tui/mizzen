package mizzen.components.plot.image;

import mizzen.components.composite.Composite;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.Slider;
import mizzen.components.plot.image.ImageViewerFactory.GreyLevel;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.components.CompositeWithFocusManager;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * The image Options' Plot row: a kind and, for lighter / darker than, the grey level on a slider. It starts from the
 * request's predicate, and a move on the slider changes the request.
 */
public class PlotChoiceTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();

    private ImageManipulationControls controlsFor(ImageRenderRequest rqst) {
        ImageManipulationControls controls = new ImageManipulationControls(rqst);
        root.add(controls);
        bus.setRoot(root);
        return controls;
    }

    private static Composite panel(ImageManipulationControls controls) {
        return controls.getWrappedComponent();
    }

    public void testADarkerThanPredicateShowsAsDarkerThanItsLevel() {
        ImageManipulationControls controls = controlsFor(new ImageRenderRequest().setToPlotOrNot(ImageViewerFactory.PLOT_DARK_ONLY));
        Slider level = panel(controls).findComponentByNameOrFail("plotLevel", Slider.class);
        assertEquals(100, (int) level.getValue());
        assertEquals("DARKER", String.valueOf(panel(controls).findComponentByNameOrFail("plotKindDDLB", DropdownListBox.class).getValue()));
        assertTrue("the slider shows", level.getParentContainer() instanceof Composite
                                       && ((Composite) level.getParentContainer()).isShown(level));
    }

    public void testMovingTheSliderChangesTheRequest() {
        ImageRenderRequest rqst = new ImageRenderRequest().setToPlotOrNot(ImageViewerFactory.plotLighterThan(50));
        ImageManipulationControls controls = controlsFor(rqst);
        Slider level = panel(controls).findComponentByNameOrFail("plotLevel", Slider.class);
        bus.deliver(RequestFocusEvent.fromSource(level));
        bus.deliver(new KeyEvent("test", Keys.RIGHT_KEYSTROKE));
        GreyLevel now = (GreyLevel) rqst.getToPlotOrNot();
        assertTrue(now.lighter);
        assertEquals(51, now.grey);
    }

    public void testPlotAlwaysHidesTheSlider() {
        ImageManipulationControls controls = controlsFor(new ImageRenderRequest());
        Slider level = panel(controls).findComponentByNameOrFail("plotLevel", Slider.class);
        assertFalse(((Composite) level.getParentContainer()).isShown(level));
    }
}
