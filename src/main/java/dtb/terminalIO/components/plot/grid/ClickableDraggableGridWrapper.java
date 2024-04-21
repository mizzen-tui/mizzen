package dtb.terminalIO.components.plot.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import dtb.terminalIO.components.ClickableDraggableComponentWrapper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.grid.drawable.PlotShapeController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class ClickableDraggableGridWrapper<T extends PlotShapeController>  extends ClickableDraggableComponentWrapper {

    ShapesOnGridManager<T> shapesManager;
    GridContext gridContext;
    
    T draggingCursor = null;
    
    protected ClickableDraggableGridWrapper(ShapesOnGridManager<T> shapesManager, GridContext gameGrid, T cursor) {
        super(gameGrid.asComponent().getColor(), gameGrid.asComponent());
        this.shapesManager = shapesManager;
        this.gridContext = gameGrid;
        this.draggingCursor = cursor;

        
        this.setFocusBehaviour( new DefaultFocusableBehaviour() );
        
        Function<Position, Position> mapToGridFunction = (appMousePosition)-> this.gridContext.mapCharPositionToGridPosition(appMousePosition);
        
        this.setClickableAction( new Clickable() {
            @Override
            public void onClick(EventBus eventBus) {
            }
            public void onClick(ApplicationMouseEvent appMouseEvent) {
                
                Logging.info("ClickableDraggableGridWrapper.onClick():  "+ appMouseEvent);
                
                Position gridPosition = mapToGridFunction.apply(appMouseEvent.getMousePosition());
                Optional<T> entityOptional = shapesManager.getTopAtPosition(gridPosition, (pe) -> pe != draggingCursor);

                if (! entityOptional.isPresent()) {
                    return;
                }
                PlotShapeController theClickedEntity = entityOptional.get();

                Logging.info("ClickableDraggableGridWrapper.onClick():  2 "+ appMouseEvent+   "  entity " + HasName.UTIL.getName(theClickedEntity) +"\n");

                if (theClickedEntity instanceof Hiliter) {
                    if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
                        ((Hiliter)theClickedEntity).hilite();
                    }
                    if (MOUSE_ACTION.UP.equals(appMouseEvent.getMouseAction())) {
                        ((Hiliter)theClickedEntity).unHilite();
                    }
                }
            }
        } );
        
        
        DraggableOnGrid moveDraggingCursor = new DraggableOnGrid() {

            @Override
            public boolean onDragStart(Position gridPosition, Rectangle bounds) {
                ((Movable)draggingCursor).moveTo(gridPosition);
                ClickableDraggableGridWrapper.this.shapesManager.addShapeController(draggingCursor);
                ClickableDraggableGridWrapper.this.shapesManager.bringToFront(draggingCursor);
                return true;
            }

            @Override
            public boolean onDrag(Position gridPosition, Rectangle bounds) {
                ((Movable)draggingCursor).moveTo(gridPosition);
                return true;
            }

            @Override
            public boolean onDragStop(Position gridPosition, Rectangle bounds) {
                ClickableDraggableGridWrapper.this.shapesManager.removeShapeController(draggingCursor);
                return true;
            }

        };
        
        DraggableOnGrid moveEntityOnGrid = new DraggableOnGrid() {

            DraggableOnGrid draggableEntity;

            @Override
            public boolean onDragStart(Position gridPosition, Rectangle bounds) {
                Optional<T> entityOptional = shapesManager.getTopAtPosition(gridPosition, (pe) -> pe != draggingCursor);

                if (! entityOptional.isPresent()) {
                    return false;
                }

                T theClickedEntity = entityOptional.get();
                if (! (theClickedEntity instanceof DraggableOnGrid)) {
                    return false;
                }

                draggableEntity = (DraggableOnGrid)theClickedEntity;

                boolean canDrag = draggableEntity.onDragStart(gridPosition, ClickableDraggableGridWrapper.this.shapesManager.asRectangle());

                if (! canDrag) {
                    draggableEntity = null;
                } else {
                    ClickableDraggableGridWrapper.this.shapesManager.bringToFront(theClickedEntity);
                }
                return canDrag;
            }

            @Override
            public boolean onDrag(Position gridPosition, Rectangle bounds) {
                if (draggableEntity == null) {
                    return false;
                }
                return draggableEntity.onDrag(gridPosition, ClickableDraggableGridWrapper.this.shapesManager.asRectangle());
            }

            @Override
            public boolean onDragStop(Position gridPosition,  Rectangle bounds) {
                if (draggableEntity == null) {
                    return false;
                }
                return draggableEntity.onDragStop(gridPosition, ClickableDraggableGridWrapper.this.shapesManager.asRectangle());
            }};

            DraggableOnGrid dragOnGrid = new DraggableOnGrid.DraggableOnGridOR(moveEntityOnGrid, moveDraggingCursor);

        Draggable dragOnMouseMove = new DraggableOnGrid.AsOnMouseMoveDraggableAdapter(dragOnGrid, shapesManager, mapToGridFunction);

        this.setDraggable( dragOnMouseMove );

    }

    public static class ClickableDraggableGrid<T extends PlotShapeController> implements GridContext {

        ShapesOnGridManager<T> shapesManager;
        GridContext gridContext;
        T cursor;

        ClickableDraggableGridWrapper<T> wrappingComponent;

        ClickableDraggableGrid(ShapesOnGridManager<T> shapesManager, GridContext gridContext, T cursor) {
            this.shapesManager = shapesManager;
            this.gridContext = gridContext;
            this.cursor = cursor;
        }

        @Override
        public PointPlotter asPointPlotter() {
            return gridContext.asPointPlotter();
        }

        @Override
        public Rectangle getGridRectangle() {
            return gridContext.getGridRectangle();
        }

        @Override
        public Component<?> asComponent() {
            List<T> a = new ArrayList<T>();
            if (wrappingComponent == null) {
                wrappingComponent = new ClickableDraggableGridWrapper<T>(shapesManager, gridContext, cursor);
            }
            return wrappingComponent;
        }

        @Override
        public boolean isDirty() {
            return gridContext.isDirty();
        }

        @Override
        public Position mapCharPositionToGridPosition(Position mousePosition) {
            return gridContext.mapCharPositionToGridPosition(mousePosition);
        }

    }
    
    public static class UTIL {

        public static <T extends PlotShapeController> GridContext getGridGridFromType(ShapesOnGridManager<T> shapesManager, String gameGridType) {
            return new ClickableDraggableGrid<T>(shapesManager, GridContext.getGridContextFromType(gameGridType), null);
        }


        public static <T extends PlotShapeController> GridContext getGridGridFromType(ShapesOnGridManager<T> shapesManager, String gameGridType, T cursor) {
            return new ClickableDraggableGrid<T>(shapesManager, GridContext.getGridContextFromType(gameGridType), cursor);
        }

    }

}
