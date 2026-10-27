package mizzen.components.plot.grid;

import java.util.function.Function;

import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Draggable;
import mizzen.geom.Rectangle;
import mizzen.geom.ToRectangle;
import mizzen.geom.position.Position;

public interface DraggableOnGrid {
    boolean onDragStart(Position gridPosition, Rectangle bounds);
    boolean onDrag(Position gridPosition, Rectangle bounds);
    boolean onDragStop(Position gridPosition, Rectangle bounds);

    public static class DraggableOnGridAND implements DraggableOnGrid {
        
        DraggableOnGrid d1;
        DraggableOnGrid d2;
        
        public DraggableOnGridAND(DraggableOnGrid d1, DraggableOnGrid d2) {
            super();
            this.d1 = d1;
            this.d2 = d2;
        }

        @Override
        public boolean onDragStart(Position gridPosition, Rectangle bounds) {
            boolean result = d1.onDragStart(gridPosition, bounds);
            if (!result) {
                return result;
            }
            return d2.onDragStart(gridPosition, bounds);
        }

        @Override
        public boolean onDrag(Position gridPosition, Rectangle bounds) {
            boolean result = d1.onDrag(gridPosition, bounds);
            if (!result) {
                return result;
            }
            return d2.onDrag(gridPosition, bounds);
        }

        @Override
        public boolean onDragStop(Position gridPosition, Rectangle bounds) {
            boolean result = d1.onDragStop(gridPosition, bounds);
            if (!result) {
                return result;
            }
            return d2.onDragStop(gridPosition, bounds);
        }
        
    }

  public static class DraggableOnGridOR implements DraggableOnGrid {

        DraggableOnGrid d1;
        boolean d1_started = false;
        DraggableOnGrid d2;
        boolean d2_started = false;

        public DraggableOnGridOR(DraggableOnGrid d1, DraggableOnGrid d2) {
            super();
            this.d1 = d1;
            this.d2 = d2;
        }

        @Override
        public boolean onDragStart(Position gridPosition, Rectangle bounds) {
            d1_started = false;
            d1_started = d1.onDragStart(gridPosition, bounds);
            d2_started = d2.onDragStart(gridPosition, bounds);
            return  d2_started || d1_started;
        }

        @Override
        public boolean onDrag(Position gridPosition, Rectangle bounds) {
            d1_started = d1_started && d1.onDrag(gridPosition, bounds);
            d2_started = d2_started && d2.onDrag(gridPosition, bounds);
            return  d2_started || d1_started;
        }

        @Override
        public boolean onDragStop(Position gridPosition, Rectangle bounds) {
            d1_started = d1.onDragStop(gridPosition, bounds);
            d2_started = d2.onDragStop(gridPosition, bounds);
            return  d2_started || d1_started;
        }
    }
  
   public static class AsOnMouseMoveDraggableAdapter implements Draggable {

    DraggableOnGrid gridDraggable;
    ToRectangle bounds;
    Function<Position, Position> mapToGridFunction;

    public AsOnMouseMoveDraggableAdapter(DraggableOnGrid gridDraggable, ToRectangle bounds,
            Function<Position, Position> mapToGridFunction) {
        super();
        this.gridDraggable = gridDraggable;
        this.bounds = bounds;
        this.mapToGridFunction = mapToGridFunction;
    }

    Position mapToGridPosition(ApplicationMouseEvent appMouseEvent) {
        return mapToGridFunction.apply(appMouseEvent.getMousePosition());
    }
    @Override
    public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
        return gridDraggable.onDragStart(mapToGridPosition(appMouseEvent), bounds.asRectangle());
    }

    @Override
    public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
        return gridDraggable.onDrag(mapToGridPosition(appMouseEvent), bounds.asRectangle());
    }

    @Override
    public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
        return gridDraggable.onDragStop(mapToGridPosition(appMouseEvent), bounds.asRectangle());
    }
       
   } 
    

}
