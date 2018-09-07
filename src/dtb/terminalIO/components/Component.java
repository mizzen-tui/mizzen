package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegmentsBuffer;


/**
 * A component is a "something" that maintains its own  IScreenLineSegmentsBuffer.
 * It knows how to draw itself into its IScreenLineSegmentsBuffer 
 * 
 * 
 * 
 * 
 * TODO:  Need to clean up the inheritance hierarchy of Components.. seems like 
 * 2 parallel heirarchies  - ( AbstractComponent & BoundedComponent)  
 * 
 * @author dbalme
 *
 */
public interface Component<T extends Component<?>> extends IScreenLineSegmentsBuffer.Provider , FGBGColors.Provider {

    T setColor(FGBGColors drawingcolor);
    
    void clear();

    public static abstract class AbstractComponent<T extends AbstractComponent<?>> implements Component<T> {

        private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

        private FGBGColors                color      = FGBGColors.DEFAULT;

        BlockChars                        blockChars = BlockChars.DEFAULT;

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

        protected AbstractComponent(FGBGColors color) {
            this.color = color == null? FGBGColors.NO_COLOR_SPECIFIED : color ;
        }

        boolean hasDrawn = false;

        protected void drawIfNeeded() {
            if (!hasDrawn) {
                clear();
                hasDrawn = true;
                draw(color, segments);
            }
        }

        public void clear() {
            segments.clear();
            fillPolicy.fillEmptyIfConfigured();
            this.hasDrawn = false;
        }

        protected abstract void draw(FGBGColors color, IScreenLineSegmentsBuffer segments);

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
        public T setColor(FGBGColors drawingcolor) {
            this.color = drawingcolor == null ? FGBGColors.NO_COLOR_SPECIFIED : drawingcolor;
            return (T) this;
        }

        public FGBGColors getColor() {
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

    }

}
