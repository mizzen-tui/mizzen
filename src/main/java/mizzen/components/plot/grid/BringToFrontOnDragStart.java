package mizzen.components.plot.grid;

import mizzen.components.plot.grid.drawable.PlotShapeController;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class BringToFrontOnDragStart implements DraggableOnGrid {

    PlotShapeController shapeController;
    ShapesOnGridManager shapesManager;

    
    public BringToFrontOnDragStart(PlotShapeController shapeController, ShapesOnGridManager shapesManager) {
        super();
        this.shapeController = shapeController;
        this.shapesManager = shapesManager;
    }

    @Override
    public boolean onDragStart(Position gridPosition, Rectangle bounds) {
        shapesManager.bringToFront(shapeController);
        return true;
    }

    @Override
    public boolean onDrag(Position gridPosition, Rectangle bounds) {
        return true;
    }

    @Override
    public boolean onDragStop(Position gridPosition, Rectangle bounds) {
        return true;
    }

}
