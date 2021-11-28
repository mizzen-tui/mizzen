package dtb.terminalIO.events.mouse;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public interface Draggable {

    boolean onDragStart(ApplicationMouseEvent appMouseEvent);
    boolean onDrag(ApplicationMouseEvent appMouseEvent);
    boolean onDragStop(ApplicationMouseEvent appMouseEvent);

    public static class DragAndMoveComponentBehaviour implements Draggable {
        
        Component<?>  draggableComponent;
        Component<?>  componentToMove;
        Position offsetFromComponentPosition;
        
        public DragAndMoveComponentBehaviour(Component<?> draggedComponent) {
            super();
            this.draggableComponent = draggedComponent;
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
            componentToMove = draggableComponent.getParentContainer().findChildOrAncestorThatCanBeMoved(draggableComponent);
            if (componentToMove == null) {
                return false;
            }
            Component<?> parentOfcomponentToMove = (Component<?>)componentToMove.getParentContainer();
            offsetFromComponentPosition = appMouseEvent.getMousePosition().sub(Component.UTIL.getCanvasOffsetFor(draggableComponent)).offsetAdd(Component.UTIL.getCanvasOffsetFor(parentOfcomponentToMove)).negate();
            appMouseEvent.getEventBus().raiseEvent(new RequestHideCursorEvent(draggableComponent));
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEvent) {
            if (canDrag(appMouseEvent)) {
                componentToMove.getParentContainer().moveChildToPosition(appMouseEvent.getMousePosition().offsetAdd(offsetFromComponentPosition), componentToMove);
                appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(draggableComponent)  );
            return true;
            } else {
                return false;
            }
        }
        
        protected boolean canDrag(ApplicationMouseEvent appMouseEvent) {
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
        public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(draggableComponent)  );
            offsetFromComponentPosition = null;
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
        public boolean onDragStart(ApplicationMouseEvent appMouseEvent) {
            offsetFromComponentPosition = appMouseEvent.getMousePosition().sub(Component.UTIL.getCanvasOffsetFor(dragAnchor)).negate();
            this.originalSizeDimension = new Dimension.Simple((Component<?>)componentToResize);
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent appMouseEvent) {

            Position componentOffset = Component.UTIL.getCanvasOffsetFor((Component<?>)componentToResize);
            
            Dimension newSize = new Dimension.Simple(appMouseEvent.getMousePosition().getRow()-componentOffset.getRow()+1, 
                                                     appMouseEvent.getMousePosition().getCol()-componentOffset.getCol()+1);

            if (newSize.getHeight()<2 || newSize.getWidth() < 2) {
                return false;
            }
            
            // TODO:  stop resizes that cause the one edge from moving outside of the parent

            componentToResize.resize(newSize);
            this.dragAnchor.markAsDirty();
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(componentToResize)  );
            
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent appMouseEvent) {
            appMouseEvent.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(componentToResize)  );
            return true;
        }
        
    }
    
    
    
}
