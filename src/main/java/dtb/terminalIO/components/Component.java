package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorProvider;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.FindPathToTarget;


/**
 * A component is "something" that maintains its own  {@link IScreenLineSegmentsBuffer}.
 * It knows how to draw itself into its IScreenLineSegmentsBuffer 
 * 
 * <p/>
 * 
 * 
 * TODO:  Need to clean up the inheritance hierarchy of Components.. seems like 
 * I've created 2 parallel hierarchies  - ( AbstractComponent & BoundedComponent)  
 * 
 * @author dbalme
 *
 */
public interface Component<T extends Component<?>> 
                                extends IScreenLineSegmentsBuffer.Provider , 
                                        DirtyFlag, 
                                        ColorProvider, 
                                        Rectangle {

    T setColor(Color drawingcolor);
    
    void clear();
    
    void setParentContainer(ParentContainer composite);
    
    ParentContainer getParentContainer();
    
    default boolean hide() {
        if (UTIL.isAttachedDirectlyToCanvas(this)) {
            return true;
        }
        return UTIL.hide(this);
    }
    
    default boolean show() {
        if (UTIL.isAttachedDirectlyToCanvas(this)) {
            return true;
        }
        return UTIL.show(this);
    }

    /**
     * Canvas will have set this offset value if component
     * was added directly to the canvas
     */
    Position getCanvasOffset();
    
    /**
     * Canvas will set this offset value if component
     * is added directly to the canvas
     * @param canvasOffset
     */
    void setCanvasOffset(Position canvasOffset);
    
    default Rectangle getPreferredRectangleForLayout() {
        return this;
    }

    public static class UTIL {
        
                
        public static boolean isDirty(Component<?> ... c) {
            
            if (c == null || c.length==0) {
                return false;
            }
            
            for (Component<?> child : c) {
                if (child.isDirty()) {
                    return true;
                }
            }
            
            return false;
            
        }
        
        /**
         * Hopefully method name says it all?
         * @param child
         */
        public static void markParentDirtyIfChildDirty(Component<?> child) {
            if (child.getParentContainer() == null) {
                return;
            }
            if ( ! child.isDirty()) {
                return;
            }
            // child is dirty - and has a parent
            if (!(child.getParentContainer() instanceof Component<?>)) {
                return;
            }
            Component<?> parentComponent = (Component<?>)child.getParentContainer();
            if (parentComponent.isDirty()) {
                return;
            }
            // child is dirty, but parent is not dirty - so mark parent dirty
            parentComponent.markAsDirty();
        }
        
        
        static public boolean isAttachedDirectlyToCanvas(Component<?> component) {
            Position canvasOffset = component.getCanvasOffset();
            
            if (canvasOffset != null) { // not null means attached directly to canvas
                return true;
            }
            return false;
        }
        
        static public Component<?> getRootPanelOf(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            
            if (path == null || path.size()<1) {
                return null;
            }

            return path.get(path.size()-1);
        }
        
        static public List<Component<?>> pathToCanvas(Component<?> component) {

            if (component == null) {
                throw new NullPointerException("pathToCanvas() ERROR: component parameter null!");
            }

            if (isAttachedDirectlyToCanvas(component)) {
                return Collections.singletonList(component);
            }

            List<Component<?>> path = new ArrayList<Component<?>>();
            path.add(component);

            // we don't have an offset, so travel up component's ancestors
            ParentContainer parentContainer = component.getParentContainer();

            if (parentContainer != null && parentContainer instanceof Component<?>) {
                path.addAll(pathToCanvas((Component<?>)parentContainer));
            }

            return path;
        }

        static public boolean isAttachedToCanvas(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            if ( isAttachedDirectlyToCanvas(path.get(path.size()-1))) {
                return true;
            }
            return false;
        }

        // useful for debugging and when throwing some exceptions..
        static public String pathToCanvasAsString(Component<?> component) {
            List<Component<?>> path = pathToCanvas(component);
            StringJoiner sj = new StringJoiner(" -> ");
            for (Component<?> c : path) {
                sj.add( HasName.UTIL.getName(c) );
            }
            
            if ( isAttachedDirectlyToCanvas(path.get(path.size()-1))) {
                sj.add("[ CANVAS ]");
            } else {
                sj.add("[ NOT ATTACHED TO CANVAS ! ]");
            }
            
            return sj.toString();
        }

        public static void requestShowCursorAtComponent(Component<?> component, Position offset, EventBus eventBus) {

            Position canvasOffset = Component.UTIL.getCanvasOffsetFor(component);

            if (canvasOffset == null) {
                if ( isAttachedToCanvas(component)) {
                     throw new IllegalStateException("requestShowCursor(): Offset not calculated");
                } else {
                    throw new IllegalStateException("requestShowCursor(): Offset not calculated\n" +
                                                      "possible reason\n" +
                                                      pathToCanvasAsString(component));
                }
            }

            Position cursorPos = canvasOffset.offsetAdd(offset);
            
            if (cursorPos == null    ||
                cursorPos.getRow()<0 || 
                cursorPos.getCol()<0) {
                    return;
            }

            eventBus.raiseEvent(new RequestShowCursorEvent(component, cursorPos));
        }
        
        public static Position getCanvasOffsetFor(Component<?> component) {
            
            Position canvasOffset = component.getCanvasOffset();
            
            if (canvasOffset != null) {
                return  new Position.Simple(canvasOffset).offsetAdd(component.getPostion_TopLeft());
            }
            
            // we don't have an offset, so travel up component's ancestors and calculate it
            
            ParentContainer parentContainer = component.getParentContainer();
            
            if (parentContainer != null && parentContainer instanceof Component<?>) {
                Position parentCanvasOffset = getCanvasOffsetFor((Component<?>)parentContainer);
                if ( parentCanvasOffset == null) {
                    return null;
                }
                
                Position componentPositionRelativeToParent = parentContainer.getPositionRelativeToParent(component);
                
                if (componentPositionRelativeToParent == null) {
                    throw new NullPointerException("Unexpected null offset.  Component " + 
                                                   HasName.UTIL.getName(component) + 
                                                   " does not have a defined offset into parent " + 
                                                   HasName.UTIL.getName(parentContainer) );
                }
                
                Position newOffset = new Position.Simple(parentCanvasOffset).offsetAdd(componentPositionRelativeToParent).offsetAdd(component.getPostion_TopLeft());   
                return newOffset;
            }
            
            return null;
        }
        

        public static Position getOffsetFromComponentContainer(Component<?> component, ParentContainer container) {
            
            if (component == null || container == null) {
                return null;
            }
            
            if ( ! new FindPathToTarget<Object>().exists(container, component)) {
                return null;
            }
            
            if (container.equals(component.getParentContainer())) {
                return container.getPositionRelativeToParent(component);
            }
            
            // parent is different from container - so let's get parentOffset first ..
            
            ParentContainer parentContainer = component.getParentContainer();
            
            
            if (parentContainer != null && parentContainer instanceof Component<?>) {
               Position parentOffset = getOffsetFromComponentContainer((Component<?>)component.getParentContainer(), container);
               if (parentOffset == null) {
                   return null;
               }
               
               Position newOffset = new Position.Simple(parentOffset).offsetAdd(parentContainer.getPositionRelativeToParent(component)).offsetAdd(component.getPostion_TopLeft());   
               return newOffset;
            }
            
            return null;
            
        }

        private static Component<?> __findComponentInHierarchyWithCompositeParent(Component<?> component) {
            ParentContainer parentContainer = component.getParentContainer();
            if (parentContainer == null) {
                return null;
            }
            if ( ! (parentContainer instanceof Composite) ) {
                
                if ( ! (parentContainer instanceof Component)) {
                    return null;
                }
                return __findComponentInHierarchyWithCompositeParent((Component<?>)parentContainer);
            }
            return component;
        }
        
        
        public static boolean bringToFront(Component<?> component) {
            if (component == null || component.getParentContainer() == null) {
                return false;
            }
            
            component = __findComponentInHierarchyWithCompositeParent(component);
            if (component == null) {
                return false;
            }
            
            Composite parentContainer = (Composite) component.getParentContainer();
            
            parentContainer.bringToFront(component);
            
            bringToFront(parentContainer);
            
            return true;
        }
        
        
        public static <T extends Component<?>> boolean hide(T component) {

            ParentContainer parent = component.getParentContainer(); 

            if (component.getParentContainer() == null) {
                return false; // false not visible
            }

            if (parent instanceof ChildComponentManager) {
                ChildComponentManager<?> parentComponentManager = (ChildComponentManager<?>) parent;
                parentComponentManager.hide(component);
                component.markAsDirty();
                return false;  // false means hidden
            }
            
            return true; // has a parent - but parent cannot hide

        }

        public static <T extends Component<?>> boolean show(T component) {

            ParentContainer parent = component.getParentContainer();

            if (component.getParentContainer() == null) {
                return false;
            }

            if (parent instanceof ChildComponentManager) {
                ChildComponentManager<?> parentComponentManager = (ChildComponentManager<?>) parent;
                parentComponentManager.show(component);
                component.markAsDirty();
                return true;
            }
            
            return true; // has a parent, but parent cannot hide child

        }

    }

}
