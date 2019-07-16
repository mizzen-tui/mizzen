package dtb.terminalIO.components.plot;

import dtb.terminalIO.geom.PositionTransform;

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