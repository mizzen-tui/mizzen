package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;


/**
 * A component is "something" that maintains its own  {@link IScreenLineSegmentsBuffer}.
 * It knows how to draw itself into its IScreenLineSegmentsBuffer 
 * 
 * <p/>
 * 
 * 
 * TODO:  Need to clean up the inheritance hierarchy of Components.. seems like 
 * 2 parallel heirarchies  - ( AbstractComponent & BoundedComponent)  
 * 
 * @author dbalme
 *
 */
public interface Component<T extends Component<?>> extends IScreenLineSegmentsBuffer.Provider , DirtyFlag, ControlCode.ColorProvider, Rectangle {

    T setColor(Color drawingcolor);
    
    void clear();
    
    void setParentContainer(ParentContainer composite);
    
    ParentContainer getParentContainer();

    
    public static abstract class AbstractComponent<T extends AbstractComponent<?>> implements Component<T> {

        private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

        private Color                     color      = FGBGColors.DEFAULT;

        BlockChars                        blockChars = BlockChars.DEFAULT;
        
        ParentContainer parentContainer;

        FillOnClearPolicy.AbstractImpl    fillPolicy = new FillOnClearPolicy.AbstractImpl(this) {
                                                         @Override
                                                         public Rectangle getRectangleForClearAndFill() {
                                                             return AbstractComponent.this
                                                                     .getRectangleForClearAndFill();
                                                         }
                                                         @Override
                                                         public IScreenLineSegmentsBuffer getSegmentsBuffer() {
                                                             return AbstractComponent.this.segments;
                                                         }
                                                     };

        boolean hasDrawn = false;

        private Position canvasOffset;

        protected AbstractComponent(Color color) {
            this.color = color == null? FGBGColors.NO_COLOR_SPECIFIED : color ;
        }

        protected void clearBeforeDraw() {
            clear();
        }
        
        protected void drawIfNeeded() {
            if (isDirty()) {
                clearBeforeDraw();
                hasDrawn = true;
                draw(color, segments);
                hasDrawn = true;
            }
        }
        
        public boolean isDirty() {
            return !this.hasDrawn;
        }
        
        public void markAsDirty() {
            if (isDirty()) {
                return;
            }
            this.hasDrawn = false;
            if (this.getParentContainer()!=null && this.getParentContainer() instanceof DirtyFlag) {
                if ( ! ((DirtyFlag)this.getParentContainer()).isDirty()) {
                   ((DirtyFlag)this.getParentContainer()).markAsDirty();
                }
            }
        }

        public void clear() {
            segments.clear();
            fillPolicy.fillEmptyIfConfigured();
            markAsDirty();
        }

        protected abstract void draw(Color color, IScreenLineSegmentsBuffer segments);

        protected IScreenLineSegmentsBuffer buildBuffer() {
            return new ScreenLineSegmentsBuffer();
        }

        @Override
        public IScreenLineSegmentsBuffer getSegmentsBuffer() {
            if (this.segments == null) {
                this.segments = buildBuffer();
            }
            drawIfNeeded();
            return segments;
        }

        @Override
        public T setColor(Color drawingcolor) {
            this.color = drawingcolor == null ? FGBGColors.NO_COLOR_SPECIFIED : drawingcolor;
            this.markAsDirty();
            return (T) this;
        }

        public Color getColor() {
            return color;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getSimpleName()).append(": ");
            if (this instanceof Rectangle) {
                Rectangle r = (Rectangle)this;
                sb.append( new Rectangle.Delegate(r).toStringDetails());
                sb.append(" ");
            }
            sb.append(this.fillPolicy);
                
            return sb.toString();
        }
        
        
        public BlockChars getBlockChars() {
            return blockChars;
        }

        public T setBlockChars(BlockChars blockChars) {
            this.blockChars = blockChars;
            return (T) this;
        }

        public void setEmptyChar(char emptyChar) {
            this.fillPolicy.setEmptyChar(emptyChar);
        }

        public void setEmptyColor(FGBGColors emptyColor) {
            this.fillPolicy.setEmptyColor(emptyColor);
        }

        public void setFillOnClear(boolean fillOnClear) {
            this.fillPolicy.setFillOnClear(fillOnClear);
        }

        @Override
        public Position getCanvasOffset() {
            return this.canvasOffset;
        }
        
        @Override
        public void setCanvasOffset(Position canvasOffset) {
            this.canvasOffset = canvasOffset;
        }
        
        
        abstract protected Rectangle getRectangleForClearAndFill();

        @Override
        public void setParentContainer(ParentContainer parentContainer) {
            this.parentContainer = parentContainer;
        }
        
        public ParentContainer getParentContainer() {
            return this.parentContainer;
        }
        
    }

    Position getCanvasOffset();
    void setCanvasOffset(Position canvasOffset);

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
        
        
    }



}
