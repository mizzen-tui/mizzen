package dtb.terminalIO.events.mouse;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public interface Draggable {

    boolean onDragStart(ApplicationMouseEvent appMouseEvent);
    boolean onDrag(ApplicationMouseEvent appMouseEvent);
    boolean onDragStop(ApplicationMouseEvent appMouseEvent);

    public static class NooP implements Draggable {

        boolean returnValue;

        public NooP(boolean returnValue) {
            super();
            this.returnValue = returnValue;
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            return returnValue;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            return returnValue;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            return returnValue;
        }
    }

    public static final Draggable NOOP = new NooP(false);

    public static final Draggable NOOP_TRUE = new NooP(true);

     public static class DragAND implements Draggable {

            Draggable d1;
            Draggable d2;

            public DragAND(Draggable d1, Draggable d2) {
                super();
                this.d1 = d1;
                this.d2 = d2;
            }

            @Override
            public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                boolean result = d1.onDragStart(appMouseEventRelativeToCanvas);
                if (!result) {
                    return result;
                }
                return d2.onDragStart(appMouseEventRelativeToCanvas);
            }

            @Override
            public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                boolean result = d1.onDrag(appMouseEventRelativeToCanvas);
                if (!result) {
                    return result;
                }
                return d2.onDrag(appMouseEventRelativeToCanvas);
            }

            @Override
            public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                boolean result = d1.onDragStop(appMouseEventRelativeToCanvas);
                if (!result) {
                    return result;
                }
                return d2.onDragStop(appMouseEventRelativeToCanvas);
            }
        }

      public static class DragOR implements Draggable {

            Draggable d1;
            boolean d1_started = false;
            Draggable d2;
            boolean d2_started = false;

            public DragOR(Draggable d1, Draggable d2) {
                super();
                this.d1 = d1;
                this.d2 = d2;
            }

            @Override
            public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                d1_started = false;
                d1_started = d1.onDragStart(appMouseEventRelativeToCanvas);
                d2_started = d2.onDragStart(appMouseEventRelativeToCanvas);
                return  d2_started || d1_started;
            }

            @Override
            public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                d1_started = d1_started && d1.onDrag(appMouseEventRelativeToCanvas);
                d2_started = d2_started && d2.onDrag(appMouseEventRelativeToCanvas);
                return  d2_started || d1_started;
            }

            @Override
            public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
                d1_started = d1.onDragStop(appMouseEventRelativeToCanvas);
                d2_started = d2.onDragStop(appMouseEventRelativeToCanvas);
                return  d2_started || d1_started;
            }
        }

    public static final int DRAGGABLE_FREEZE_NONE = 0;
    public static final int DRAGGABLE_FREEZE_COLUMN = 1;
    public static final int DRAGGABLE_FREEZE_ROW = 2;
      
    public static class DragAndMoveComponentBehaviour implements Draggable {
        Component<?>  draggableComponent;
        Component<?>  componentToMove;
        Position offsetFromComponentPosition;
        int freezePolicy = DRAGGABLE_FREEZE_NONE;

        public DragAndMoveComponentBehaviour(Component<?> draggedComponent) {
            super();
            this.draggableComponent = draggedComponent;
            if (this.draggableComponent == null) {
                throw new NullPointerException("draggableComponent not defined (null)");
            }
        }
        
        public DragAndMoveComponentBehaviour freezeColumn() {
            this.freezePolicy = DRAGGABLE_FREEZE_COLUMN;
            return this;
        }

        /**
         * Modify the drag behavior so that the drag action only happens in the column axis
         * meaning that you can only move left or right, not up or down, thus freezing the row
         */
        public DragAndMoveComponentBehaviour freezeRow() {
            this.freezePolicy = DRAGGABLE_FREEZE_ROW;
            return this;
        }

        public DragAndMoveComponentBehaviour unFreeze() {
            this.freezePolicy = DRAGGABLE_FREEZE_NONE;
            return this;
        }

        public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            if ( ! canDrag(appMouseEventRelativeToCanvas)) {
                return false;
            }

            componentToMove = draggableComponent.getParentContainerOrFail().findChildOrAncestorThatCanBeMoved(draggableComponent);
            if (componentToMove == null) {
                return false;
            }

            offsetFromComponentPosition = componentToMove == draggableComponent 
                                                       ? Position.ZERO 
                                                       : appMouseEventRelativeToCanvas.getMousePosition()
                                                                                      .sub(Component.UTIL.getCanvasOffsetFor(draggableComponent))
                                                                                      .negate();

            appMouseEventRelativeToCanvas.getEventBus().raiseEvent(new RequestHideCursorEvent(draggableComponent));
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            if (canDrag(appMouseEventRelativeToCanvas)) {

                if (componentToMove == null) {
                    componentToMove = draggableComponent.getParentContainerOrFail().findChildOrAncestorThatCanBeMoved(draggableComponent);
                    if (componentToMove == null) {
                        throw new NullPointerException( HasName.UTIL.getName(this) + ".onDrag():  componentToMove set to null - should not attempt onDrag when it is null!");
                    }
                }

                Component<?> parentOfcomponentToMove = (Component<?>)componentToMove.getParentContainerOrFail();
                Position parentOfcomponentToMovePosition = Component.UTIL.getCanvasOffsetFor(parentOfcomponentToMove);

                if (offsetFromComponentPosition == null) {
                    offsetFromComponentPosition = componentToMove == draggableComponent 
                                                    ? Position.ZERO 
                                                    : appMouseEventRelativeToCanvas.getMousePosition()
                                                                                   .sub(Component.UTIL.getCanvasOffsetFor(draggableComponent))
                                                                                   .negate();
                }

                Position moveTo = Position.ZERO;
                switch (this.freezePolicy) {
                    case DRAGGABLE_FREEZE_NONE:
                        moveTo = appMouseEventRelativeToCanvas.getMousePosition()
                                                              .offsetAdd(offsetFromComponentPosition)
                                                              .sub(parentOfcomponentToMovePosition);
                        break;
    
                    case DRAGGABLE_FREEZE_COLUMN:
                        moveTo = new Position.Simple(appMouseEventRelativeToCanvas.getMousePosition().getRow(),
                                                     componentToMove.getParentContainerOrFail()
                                                                    .getPositionRelativeToParent(componentToMove).getCol())
                                                                    .offsetAdd(new Position.Simple(offsetFromComponentPosition.getRow()-parentOfcomponentToMovePosition.getRow(),0));
                        break;

                    case DRAGGABLE_FREEZE_ROW:
                        moveTo = new Position.Simple(componentToMove.getParentContainerOrFail().getPositionRelativeToParent(componentToMove).getRow(), 
                                                     appMouseEventRelativeToCanvas.getMousePosition().getCol()).offsetAdd(new Position.Simple(0,offsetFromComponentPosition.getCol()-parentOfcomponentToMovePosition.getCol()));
                        break;
    
                    default:
                        break;
                }

                if ( ! parentOfcomponentToMove.containsPosition(moveTo) ) {
                    return true;
                }

                if (parentOfcomponentToMove instanceof HasTitleAndBorder && ((HasTitleAndBorder<?>)parentOfcomponentToMove).hasBorder()) {
                    Rectangle bounds = new Rectangle.Simple(parentOfcomponentToMove.getRow()+1, 
                                                            parentOfcomponentToMove.getCol()+1, 
                                                            parentOfcomponentToMove.getHeight()-2, 
                                                            parentOfcomponentToMove.getWidth()-2);
                    if (!bounds.containsPosition(moveTo)) {
                        return true;
                    }
                }

                ((ParentContainer)parentOfcomponentToMove).moveChildToPosition(moveTo, componentToMove);
                appMouseEventRelativeToCanvas.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(draggableComponent)  );
                return true;
            } else {
                return false;
            }
        }

        protected boolean canDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            // TODO:  Need to add a drag constraint behaviour.. possible delegate to another class
            //        options: 
            //                    1: draggableComponent must never been hidden as this disables future drag 
            //                    2: (1) + 
            //                             componentToMove must fit inside of its parent 
            //                             if componentToMove is partially outside parent on start of drag
            //                             only allow drag movements that do not decrease it area inside 
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            appMouseEventRelativeToCanvas.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(draggableComponent)  );
            offsetFromComponentPosition = null; // Position.ZERO;
            componentToMove = null;
            return true;
        }
    }

    /**
     * Use a "dragAnchor" to resize parent component
     *
     * <pre> 
     *      +---------------+
     *      |               | <-- componentToResize (parent)
     *      |               |
     *      |               |
     *      +--------------[+] <-- dragAnchor
     *
     * </pre>
     *
     * @author dbalme
     *
     */
    public static class DragAndResizeComponentBehaviour  implements Draggable {

        // assuming drag item is at the bottom right corner of resizable component
        CanResize componentToResize;
        Component<?>  dragAnchor;
        Position offsetFromComponentPosition;
        Dimension anchorToResizeableDimension;
        Dimension originalSizeDimension;

        public DragAndResizeComponentBehaviour(CanResize componentToResize, Component<?> dragAnchor) {
            super();
            this.componentToResize = componentToResize;
            this.dragAnchor = dragAnchor;
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            offsetFromComponentPosition = appMouseEventRelativeToCanvas.getMousePosition().sub(Component.UTIL.getCanvasOffsetFor(dragAnchor)).negate();
            this.originalSizeDimension = new Dimension.Simple((Component<?>)componentToResize);
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEventRelativeToCanvas) {

            Position componentOffset = Component.UTIL.getCanvasOffsetFor((Component<?>)componentToResize);

            Dimension newSize = new Dimension.Simple(appMouseEventRelativeToCanvas.getMousePosition().getRow()-componentOffset.getRow()+1, 
                                                     appMouseEventRelativeToCanvas.getMousePosition().getCol()-componentOffset.getCol()+1);

            if (newSize.getHeight()<2 || newSize.getWidth() < 2) {
                return false;
            }

            // TODO:  stop resizes that cause the one edge from moving outside of the parent

            componentToResize.resize(newSize);
            this.dragAnchor.markAsDirty();
            appMouseEventRelativeToCanvas.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(componentToResize)  );

            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEventRelativeToCanvas) {
            appMouseEventRelativeToCanvas.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(componentToResize)  );
            return true;
        }

    }

}
