package mizzen.components.plot.shape;

import java.util.Collection;
import java.util.function.Function;

import mizzen.components.plot.PointPlotter;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.geom.AsRectangle2D;
import mizzen.geom.Rectangle2D;
import mizzen.geom.position.PositionTransform;
import mizzen.traverse.HasChildren;

public abstract class AbstractCompositeShape<T extends AbstractCompositeShape<?>> implements Shape<T> , HasChildren<Shape<?>>, AsRectangle2D.AsFloat2D {
    
    private Collection<Shape<?>> shapes = null;
    private PositionTransform positionTransform = PositionTransform.NOOP;
    private String name;
    private Function<Theme, Color> themeColor;      // null: none - each part's own, else the plotter's

    protected void drawChild(Shape<?> childShape, PointPlotter pointPlotter) {
        childShape.draw(pointPlotter);
    }

    protected Collection<Shape<?>> getShapes() {
        if (shapes == null) {
            shapes = buildShapes();
        }
        return shapes;
    }

    abstract protected Collection<Shape<?>> buildShapes();

    @Override
    public void draw(PointPlotter pointPlotter) {
        if (themeColor == null) {                           // nearly every composite: drawn as before, nothing made
            drawParts(pointPlotter);
            return;
        }
        Shape.UTIL.drawInThemeColor(pointPlotter, themeColor, () -> drawParts(pointPlotter));
    }

    private void drawParts(PointPlotter pointPlotter) {
        for (Shape<?> shape : getShapes()) {
            drawChild(shape, pointPlotter);
        }
    }

    /**
     * Its parts with no colour of their own draw in this colour of its theme - the theme of what it is plotted into, worked
     * out as it is drawn (Shape.AbstractShape.setThemeColor).
     */
    @SuppressWarnings("unchecked")
    public T setThemeColor(Function<Theme, Color> themeColor) {
        this.themeColor = themeColor;
        return (T) this;
    }

    public Function<Theme, Color> getThemeColor() {
        return themeColor;
    }

    @Override
    public PositionTransform getPositionTransform() {
        return this.positionTransform;
    }

    @Override
    public void setPositionTransform(PositionTransform positionTransform) {
        this.positionTransform = positionTransform;
        for (Shape<?> shape : getShapes()) {
            shape.setPositionTransform(positionTransform);
        }
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public Rectangle2D.Float2D getRectangle2D() {
        return Shape.UTIL.getAsRectangle( getShapes() );
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setName(String name) {
        this.name = name;
        return (T)this;
    }

    @Override
    public Collection<? extends Shape<?>> getChildren() {
        return getShapes();
    }
}