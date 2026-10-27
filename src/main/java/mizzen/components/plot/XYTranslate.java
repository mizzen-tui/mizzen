package mizzen.components.plot;

import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;

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

    public XYTranslate(PointPlotter pointPlotter, Position delta) {
        this(pointPlotter, delta.getCol(), delta.getRow());
    }

    public void setyDelta(float yDelta) {
        ((PositionTransform.Translate) this.positionTransform).setRowDelta(yDelta);
    }

    public void setxDelta(float xDelta) {
        ((PositionTransform.Translate) this.positionTransform).setColDelta(xDelta);
    }

    public float getyDelta() {
        return ((PositionTransform.Translate) this.positionTransform).getRowDelta();
    }

    public float getxDelta() {
        return ((PositionTransform.Translate) this.positionTransform).getColDelta();
    }

}