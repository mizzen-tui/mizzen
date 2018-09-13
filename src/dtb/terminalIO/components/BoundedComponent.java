package dtb.terminalIO.components;

import java.util.Collection;
import java.util.function.Supplier;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.ScreenLineSegmentsBuffer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;


/**
 * A component that has a defined fixed rectangular size.
 */
public abstract class BoundedComponent<T extends BoundedComponent<?>> extends Rectangle.Delegate implements Component<T>, FillOnClearPolicy.Provider {

    private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer() {
                                                     protected void doAdd(Collection<ScreenLineSegment> segments, ScreenLineSegment sc) {
                                                         // add to segments buffer (collection)
                                                         ScreenLineSegment.UTIL.addSegmentBounded(BoundedComponent.this.getRectangle(), segments, sc);
                                                     }
                                                 };

    boolean                           hasDrawn   = false;
    Color                             color      = FGBGColors.DEFAULT;
    BlockChars                        blockChars = BlockChars.DEFAULT;

    FillOnClearPolicy    fillPolicy = new FillOnClearPolicy.AbstractImpl(this) {

                                                     @Override
                                                     public Rectangle getRectangleForClearAndFill() {
                                                         return BoundedComponent.this.getRectangleForClearAndFill();
                                                     }

                                                     @Override
                                                     public IScreenLineSegmentsBuffer getSegmentsBuffer() {
                                                         return BoundedComponent.this.segments;
                                                     }

                                                 };

    public BoundedComponent(Color color, Rectangle r) {
        super(new Rectangle.Simple(r));
        this.setColor(color);
    }

    public BoundedComponent(Color color, Position p, Dimension d) {
        super(new Rectangle.Simple(p, d));
        this.color = color;
    }

    public BoundedComponent(Color color, Dimension d) {
        super(new Rectangle.Simple(d));
        setColor(color);
    }

    public void clear() {
        segments.clear();
        fillPolicy.fillEmptyIfConfigured();
        this.hasDrawn = false;
    }

    protected void drawIfNeeded() {
        if (!hasDrawn) {
            clear();
            hasDrawn = true;
            draw(color, segments);
        }
    }

    protected abstract void draw(Color color, IScreenLineSegmentsBuffer segments);

    @Override
    public IScreenLineSegmentsBuffer getSegmentsBuffer() {
        drawIfNeeded();
        return segments;
    }

    @Override
    public T setColor(Color drawingcolor) {
        this.color = drawingcolor == null ? FGBGColors.NO_COLOR_SPECIFIED : drawingcolor;
        return (T) this;
    }

    public BlockChars getBlockChars() {
        return blockChars;
    }

    public T setBlockChars(BlockChars blockChars) {
        this.blockChars = blockChars;
        return (T) this;
    }

    public Color getColor() {
        return color;
    }

    public FillOnClearPolicy getFillOnClearPolicy() {
        return this.fillPolicy;
    }
    
    public void setFillOnClearPolicy(FillOnClearPolicy fillerPolicy) {
        this.fillPolicy = fillerPolicy;
    }
    

    public T setEmptyChar(char emptyChar) {
        this.fillPolicy.setEmptyChar(emptyChar);
        return (T) this;
    }

    public T setEmptyColor(Color emptyColor) {
        this.fillPolicy.setEmptyColor(emptyColor);
        return (T) this;
    }

    public T setFillOnClear(boolean fillOnClear) {
        this.fillPolicy.setFillOnClear(fillOnClear);
        return (T) this;
    }

    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

    
    @Override
    public String toString() {
        return super.toString() + String.valueOf(fillPolicy.toString());
    }
    
    
    protected <T extends BoundedComponent<?>> T makeChildComponent(Supplier<T> createComponent) {
       T component = createComponent.get();
       component.setBlockChars(this.getBlockChars());

       component.getFillOnClearPolicy().setEmptyChar(this.getFillOnClearPolicy().getEmptyChar());
       component.getFillOnClearPolicy().setEmptyColor(this.getFillOnClearPolicy().getEmptyColor());
       component.getFillOnClearPolicy().setFillOnClear(this.getFillOnClearPolicy().isFillOnClear());
       return component;
    }


    public static class DONOTHING extends BoundedComponent<DONOTHING> {

        public DONOTHING(Rectangle r) {
            super(FGBGColors.NO_COLOR_SPECIFIED, r);
        }

        public DONOTHING(int height, int width) {
            this(new Rectangle.Simple(0, 0, height, width));
        }

        public DONOTHING(dtb.terminalIO.geom.Position position, Dimension dimension) {
            this(new Rectangle.Simple(position, dimension));
	}

	@Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            // do nothing
        }
    }

}