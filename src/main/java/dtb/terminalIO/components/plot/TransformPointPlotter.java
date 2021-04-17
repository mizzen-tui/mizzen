package dtb.terminalIO.components.plot;

import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;

/**
 * A {@link PointPlotter} that transforms the x, y coordinates (before plotting) using a
 * {@link PositionTransform} object.
 *
 * <p/>
 *
 * This is useful when you want to translate or rotate x,y coordinates when
 * performing plots of said x,y coordinates.
 *
 */
public class TransformPointPlotter extends PointPlotter.Delegate {

    protected PositionTransform positionTransform;

    public TransformPointPlotter(PointPlotter pointPlotter, PositionTransform positionTransform) {
        super(pointPlotter);
        this.positionTransform = positionTransform;
    }

    @Override
    public void plot(float x, float y) {
        // transform the points
        Position p = positionTransform.transform(new Position.Simple(y, x));
        // now plot onto the 'real' PointPlotter object
        super.plot(p.getCol(), p.getRow());
    }

}
