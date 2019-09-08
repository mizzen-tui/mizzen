package dtb.terminalIO.components.plot;

import dtb.terminalIO.geom.position.PositionTransform;

/**
 * A {@link TransformPointPlotter} that 'moves' any x,y coordinates
 * that it is about to plot an offset ( xDelta, yDelta ) from
 * the point it is about to plot..  
 * 
 * <p/>
 * 
 * Useful for moving shapes around a grid..  just change either
 * xDelta or yDelta ( or both ) to 'move' the shape.
 * 
 * @author dbalme
 *
 */
public class XYTranslate extends TransformPointPlotter {

    public XYTranslate(PointPlotter pointPlotter, int xDelta, int yDelta) {
        super(pointPlotter, new PositionTransform.Translate(yDelta, xDelta));
    }

    public void setyDelta(int yDelta) {
        ((PositionTransform.Translate) this.positionTransform).setRowDelta(yDelta);
    }

    public void setxDelta(int xDelta) {
        ((PositionTransform.Translate) this.positionTransform).setColDelta(xDelta);
    }

    public int getyDelta() {
        return ((PositionTransform.Translate) this.positionTransform).getRowDelta();
    }

    public int getxDelta() {
        return ((PositionTransform.Translate) this.positionTransform).getColDelta();
    }

}