package mizzen.components.plot.grid;

import java.util.Optional;
import java.util.function.Predicate;

import mizzen.components.plot.grid.drawable.PlotShapeController;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.ToRectangle;
import mizzen.geom.position.Position;

public interface ShapesOnGridManager<T extends PlotShapeController> extends ToRectangle {
    int getHeight();
    int getWidth();
    Optional<T> getTopAtPosition(Position gridPosition, Predicate<T> predicate);
    void addShapeController(T shapeController);
    void removeShapeController(T shapeController);
    void bringToFront(T shapeController);
    void bringToBack(T shapeController);
    default Rectangle asRectangle() {
        return new Rectangle.Simple(Position.ZERO, new Dimension.Simple(getHeight(), getWidth()));
    }

}
