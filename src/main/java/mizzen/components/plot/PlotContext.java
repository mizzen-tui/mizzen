package mizzen.components.plot;

import mizzen.geom.Rectangle;

/**
 * What a drawing passes down a chain of PlotMiddleware: where the points go, and the area drawn in.
 * <p/>
 * plotter is deliberately changeable: a middleware that wants to change what gets drawn swaps it for
 * a wrapping PointPlotter before passing the drawing on, and puts it back after - the shape keeps
 * plotting into whatever it is handed and never knows. (A program needing more here - a game's
 * engine, say - extends this class.)
 */
public class PlotContext {

    public PointPlotter plotter;
    public Rectangle bounds;

    public PlotContext(PointPlotter plotter, Rectangle bounds) {
        this.plotter = plotter;
        this.bounds = bounds;
    }
}
