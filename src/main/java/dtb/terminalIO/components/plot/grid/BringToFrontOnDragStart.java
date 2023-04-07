package dtb.terminalIO.components.plot.grid;

import dtb.terminalIO.components.plot.grid.drawable.PlotShapeController;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

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
