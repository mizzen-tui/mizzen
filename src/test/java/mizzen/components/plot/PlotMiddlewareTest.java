package mizzen.components.plot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mizzen.components.plot.PointPlotter.PointPlot;
import mizzen.components.plot.PointPlotter.PointPlotterList;
import mizzen.components.plot.effects.ColorCycle;
import mizzen.components.plot.effects.ExplodingPointPlotter;
import mizzen.components.plot.shape.Line;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Rectangle;
import junit.framework.TestCase;

/**
 * PlotMiddleware: steps run in order around a drawing, and can pass it on, change it or replace it.
 * The effects (ColorCycle, ExplodingPointPlotter) change what a shape draws without the shape knowing.
 */
public class PlotMiddlewareTest extends TestCase {

    private final Rectangle bounds = new Rectangle.Simple(0, 0, 20, 20);
    private final PointPlotterList plotted = new PointPlotterList();
    private final Line line = new Line(0, 0, 9, 0, Colors.Red);          // 10 points, asks for red

    public void testStepsRunInOrderThenTheDrawing() {
        List<String> order = new ArrayList<String>();
        PlotMiddleware a = (ctx, chain) -> { order.add("a"); chain.forward(ctx); };
        PlotMiddleware b = (ctx, chain) -> { order.add("b"); chain.forward(ctx); };
        PlotMiddleware.UTIL.run(Arrays.asList(a, b), new PlotContext(plotted, bounds), (ctx) -> order.add("draw"));
        assertEquals(Arrays.asList("a", "b", "draw"), order);
    }

    public void testAStepThatDoesNotForwardReplacesTheDrawing() {
        PlotMiddleware hide = (ctx, chain) -> { };
        PlotMiddleware.UTIL.draw(line, plotted, bounds, Collections.singletonList(hide));
        assertEquals(0, plotted.size());
    }

    public void testNoStepsJustDraws() {
        PlotMiddleware.UTIL.draw(line, plotted, bounds, Collections.<PlotMiddleware>emptyList());
        assertEquals(10, plotted.size());
    }

    public void testColorCycleOverridesTheShapesColourAndPutsThePlottersBack() {
        plotted.setNextPlotColor(Colors.Blu);
        PlotMiddleware.UTIL.draw(line, plotted, bounds, Collections.singletonList(new ColorCycle(new Color[] { Colors.Gre }, 1)));
        assertEquals(10, plotted.size());
        for (PointPlot p : plotted.getPlots()) {
            assertEquals(Colors.Gre, p.color);
        }
        assertEquals("the plotter's own colour is back", Colors.Blu, plotted.setNextPlotColor(null));
    }

    public void testExplosionAtZeroDrawsTheShapeAsIs() {
        ExplodingPointPlotter boom = new ExplodingPointPlotter(plotted);
        line.draw(boom);
        assertEquals(10, plotted.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(i, plotted.getPlots().get(i).getCol());
            assertEquals(0, plotted.getPlots().get(i).getRow());
        }
    }

    public void testFullyExplodedDrawsNothing() {
        ExplodingPointPlotter boom = new ExplodingPointPlotter(plotted);
        boom.setSequence(boom.getMaxSequence());
        assertTrue(boom.isFullyDestroyed());
        line.draw(boom);
        assertEquals(0, plotted.size());
    }

    public void testHalfExplodedDropsSomeAndKeepsTheirColour() {
        ExplodingPointPlotter boom = new ExplodingPointPlotter(plotted);
        boom.setRandomSeed(42);
        boom.setSequence(70);
        for (int i = 0; i < 20; i++) {
            line.draw(boom);
        }
        assertTrue("some dropped: " + plotted.size(), plotted.size() < 200);
        assertTrue("some kept: " + plotted.size(), plotted.size() > 0);
        for (PointPlot p : plotted.getPlots()) {
            assertEquals(Colors.Red, p.color);                           // no flash colours given
        }
    }

    public void testExplosionAsAStepDrawsIntoTheDrawingsPlotter() {
        ExplodingPointPlotter boom = new ExplodingPointPlotter(null);
        PlotMiddleware.UTIL.draw(line, plotted, bounds, Collections.singletonList(boom.asStep()));
        assertEquals(10, plotted.size());
    }
}
