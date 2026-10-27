package mizzen.components.plot.grid;

import java.util.function.BiConsumer;

import mizzen.components.plot.shape.threeD.projection.Plot2DFromProjection;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class DragRotate3DOnGrid implements DraggableOnGrid {

    private final Plot2DFromProjection projection;
    private final BiConsumer<Plot2DFromProjection,Position>  onChanged;

    private Position lastGridPosition;

    private float sensitivity = 0.25f;

    public DragRotate3DOnGrid(Plot2DFromProjection projection, BiConsumer<Plot2DFromProjection,Position>  onChanged) {
        this.projection = projection;
        this.onChanged = onChanged;
    }

    public DragRotate3DOnGrid setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
        return this;
    }

    @Override
    public boolean onDragStart(Position gridPosition, Rectangle bounds) {
        this.lastGridPosition = gridPosition;
        return true;
    }

    @Override
    public boolean onDrag(Position gridPosition, Rectangle bounds) {
        if (lastGridPosition == null) {
            lastGridPosition = gridPosition;
            return true;
        }

        int dx = gridPosition.getCol() - lastGridPosition.getCol();
        int dy = gridPosition.getRow() - lastGridPosition.getRow();

        projection.setThetaY(projection.getThetaY() + dx * sensitivity);
        projection.setThetaX(projection.getThetaX() + dy * sensitivity);

        lastGridPosition = gridPosition;

        onChanged.accept(projection, lastGridPosition);
        return true;
    }

    @Override
    public boolean onDragStop(Position gridPosition, Rectangle bounds) {
        lastGridPosition = null;
        return true;
    }
}