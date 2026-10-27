package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.dotmatrix.DotMatrixRenderer;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Movable;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.Vec2;

public class DotMatrixText extends AbstractShape<DotMatrixText> implements Movable {

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

        PointPlotter letters = positionTransform == PositionTransform.NOOP ? pointPlotter : transformed(pointPlotter, positionTransform);
        DotMatrixRenderer.plotStringWithSpacing(letters, this.text, 2, offset.getX(), offset.getY());

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }
    }

    /**
     * Each letter dot through the transform (rotated, scaled, moved). Scaled up, a dot becomes a
     * square block as wide as the scale, so the letters stay solid instead of turning into spaced-out
     * dots.
     */
    private static PointPlotter transformed(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Vec2<?> origin = positionTransform.transform(0, 0);
        Vec2<?> oneAlong = positionTransform.transform(1, 0);
        float scale = (float) Math.hypot(oneAlong.getX() - origin.getX(), oneAlong.getY() - origin.getY());
        int block = Math.max(1, Math.round(scale));

        return new PointPlotter.Delegate(pointPlotter) {
            @Override
            public void plot(float x, float y) {
                Vec2<?> p = positionTransform.transform(x, y);
                float left = p.getX() - (block - 1) / 2f;
                float top = p.getY() - (block - 1) / 2f;
                for (int row = 0; row < block; row++) {
                    for (int col = 0; col < block; col++) {
                        super.plot(left + col, top + row);
                    }
                }
            }
        };
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
