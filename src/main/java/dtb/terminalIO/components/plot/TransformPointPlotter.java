package dtb.terminalIO.components.plot;

import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.PositionTransform;

/**
 * A PointPlotter that transforms the x, y coordinates (before plotting) using a
 * PositionTransform object.
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
    public void plot(int x, int y) {
        Position p = positionTransform.transform(new Position.Simple(y, x));
        super.plot(p.getCol(), p.getRow());
    }

}
