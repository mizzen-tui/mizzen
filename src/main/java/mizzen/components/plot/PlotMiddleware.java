package mizzen.components.plot;

import java.util.List;
import java.util.function.Consumer;

import mizzen.components.plot.shape.Shape;
import mizzen.geom.Rectangle;

/**
 * A step wrapped around a drawing, to change how something looks without changing the thing itself
 * - cycle its colours, blow it apart, outline it. Each step gets the drawing's context and the rest
 * of the chain, and can:
 * <pre>
 *   pass it on      chain.forward(ctx)
 *   change it       swap ctx.plotter for a wrapping PointPlotter, forward, then put it back
 *   replace it      don't forward - the thing is not drawn this time
 * </pre>
 * Example - a shape drawn in cycling colours:
 * <pre>
 *     List&lt;PlotMiddleware&gt; looks = Arrays.asList(new ColorCycle(palette));
 *     PlotMiddleware.UTIL.draw(shape, grid.asPointPlotter(), bounds, looks);
 * </pre>
 * Steps run in list order, the first one outermost.
 */
@FunctionalInterface
public interface PlotMiddleware {

    void handlePlot(PlotContext ctx, PlotChain chain);

    public static class UTIL {

        /** runs ctx through the steps in order, then draw (the drawing itself) */
        public static void run(List<? extends PlotMiddleware> steps, PlotContext ctx, Consumer<PlotContext> draw) {
            run(steps, 0, ctx, draw);
        }

        private static void run(List<? extends PlotMiddleware> steps, int index, PlotContext ctx, Consumer<PlotContext> draw) {
            if (index < steps.size()) {
                steps.get(index).handlePlot(ctx, (next) -> run(steps, index + 1, next, draw));
            } else {
                draw.accept(ctx);
            }
        }

        /** draws shape into plotter, through the steps */
        public static void draw(Shape<?> shape, PointPlotter plotter, Rectangle bounds, List<? extends PlotMiddleware> steps) {
            run(steps, new PlotContext(plotter, bounds), (ctx) -> shape.draw(ctx.plotter));
        }
    }
}
