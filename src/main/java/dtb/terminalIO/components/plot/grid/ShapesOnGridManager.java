package dtb.terminalIO.components.plot.grid;

import java.util.Optional;
import java.util.function.Predicate;

import dtb.terminalIO.components.plot.grid.drawable.PlotShapeController;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.ToRectangle;
import dtb.terminalIO.geom.position.Position;

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
