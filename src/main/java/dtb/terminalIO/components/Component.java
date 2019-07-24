package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
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
 * 2 parallel hierarchies  - ( AbstractComponent & BoundedComponent)  
 * 
 * @author dbalme
 *
 */
public interface Component<T extends Component<?>> extends IScreenLineSegmentsBuffer.Provider , DirtyFlag, ControlCode.ColorProvider, Rectangle {

    T setColor(Color drawingcolor);
    
    void clear();
    
    void setParentContainer(ParentContainer composite);
    
    ParentContainer getParentContainer();

    Position getCanvasOffset();
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
                
                Position newOffset = new Position.Simple(parentCanvasOffset).offsetAdd(parentContainer.getPositionRelativeToParent(component)).offsetAdd(component.getPostion_TopLeft());   
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

        public static boolean bringToFront(Component<?> component) {
            if (component == null || component.getParentContainer() == null) {
                return false;
            }
            ParentContainer parentContainer = component.getParentContainer();
            if ( ! (parentContainer instanceof Composite) ) {
                return false;
            }
            
            ((Composite)parentContainer).bringToFront(component);
            return true;
        }
        
        
    }

}
