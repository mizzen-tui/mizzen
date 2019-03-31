package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.HasChildren;


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
public interface Component<T extends Component<?>> extends IScreenLineSegmentsBuffer.Provider , ControlCode.ColorProvider, Rectangle {

    T setColor(Color drawingcolor);
    
    void clear();
    
    boolean isDirty();

    void markAsDirty();
    
    void setParentContainer(Component<? extends HasChildren<Component<?>>> composite);
    
    Component<? extends HasChildren<Component<?>>> getParentContainer();

    
    public static abstract class AbstractComponent<T extends AbstractComponent<?>> implements Component<T> {

        private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

        private Color                     color      = FGBGColors.DEFAULT;

        BlockChars                        blockChars = BlockChars.DEFAULT;
        
        Component<? extends HasChildren<Component<?>>> parentContainer;

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

        protected AbstractComponent(Color color) {
            this.color = color == null? FGBGColors.NO_COLOR_SPECIFIED : color ;
        }

        boolean hasDrawn = false;

        protected void clearBeforeDraw() {
            clear();
        }

        
        protected void drawIfNeeded() {
            if (isDirty()) {
                clearBeforeDraw();
                hasDrawn = true;
                draw(color, segments);
            }
        }
        
        public boolean isDirty() {
            return !this.hasDrawn;
        }
        
        public void markAsDirty() {
            this.hasDrawn = false;
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

        abstract protected Rectangle getRectangleForClearAndFill();

        @Override
        public void setParentContainer(Component<? extends HasChildren<Component<?>>> parentContainer) {
            this.parentContainer = parentContainer;
        }
        
        public Component<? extends HasChildren<Component<?>>> getParentContainer() {
            return this.parentContainer;
        }
        
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
    }


}
