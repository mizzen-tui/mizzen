package mizzen.components.plot.effects;

import mizzen.components.plot.PlotChain;
import mizzen.components.plot.PlotContext;
import mizzen.components.plot.PlotMiddleware;
import mizzen.components.plot.PointPlotter;
import mizzen.controlcodes.color.Color;

/**
 * Draws a thing in a cycling palette, whatever colour it asks for: each drawing moves on one tick,
 * and every ticksPerColor ticks the next colour is used. Example: a warning marker flashing red /
 * yellow, redrawn on a timer.
 * <p/>
 * It wraps the plotter for one drawing and swaps the colour the thing asks for (setNextPlotColor);
 * the thing's own drawing code is unchanged.
 */
public class ColorCycle implements PlotMiddleware {

    private final Color[] colors;
    private final int ticksPerColor;
    private int tickCounter = 0;
    private int colorIndex = 0;

    public ColorCycle(Color[] colors) {
        this(colors, 3);
    }

    public ColorCycle(Color[] colors, int ticksPerColor) {
        this.colors = colors;
        this.ticksPerColor = Math.max(1, ticksPerColor);
    }

    @Override
    public void handlePlot(PlotContext ctx, PlotChain chain) {
        if (colors == null || colors.length == 0) {
            chain.forward(ctx);
            return;
        }

        tickCounter++;
        if (tickCounter % ticksPerColor == 0) {
            colorIndex = (colorIndex + 1) % colors.length;
        }
        final Color forcedColor = colors[colorIndex];

        PointPlotter real = ctx.plotter;
        ctx.plotter = new PointPlotter.Delegate(real) {
            @Override
            public Color setNextPlotColor(Color color) {
                return super.setNextPlotColor(forcedColor);     // whatever the thing asked for
            }
        };
        Color realColor = real.setNextPlotColor(forcedColor);   // also for a thing that never sets a colour
        try {
            chain.forward(ctx);
        } finally {
            ctx.plotter = real;
            real.setNextPlotColor(realColor);
        }
    }
}
