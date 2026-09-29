package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.dotmatrix.DotMatrixRenderer;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;

public class DotMatrixText extends AbstractShape<Triangle> implements Movable {

    String text;
    Color color;
    Position offset = Position.ZERO;

    public DotMatrixText(String text, Color color) {
        this.text = text != null ? text : "";
        this.color = color;
    }

    public DotMatrixText(String text) {
        this.text = text != null ? text : "";
        this.color = Colors.DEFAULT_COLOR;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setNextPlotColor(color);
        }

        DotMatrixRenderer.plotStringWithSpacing(pointPlotter, this.text, 2, offset.getX(), offset.getY());

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }
    }

    @Override
    public void moveTo(int row, int col) {
        this.offset = new Position.Simple(row, col);
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        this.offset = this.offset.addRows(rowOffset).addCols(colOffset);
    }

}
