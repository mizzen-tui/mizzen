package dtb.terminalIO.components.plot;

/**
 * The rest of a drawing, handed to a PlotMiddleware: forward(ctx) runs the next middleware and, at the
 * end, the drawing itself.
 */
@FunctionalInterface
public interface PlotChain {

    void forward(PlotContext ctx);
}
