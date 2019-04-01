package dtb.terminalIO.components;

import java.util.Collection;
import java.util.function.Supplier;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.HasChildren;


/**
 * A component that has a defined fixed rectangular size.
 */
public abstract class BoundedComponent<T extends BoundedComponent<?>> extends Rectangle.Delegate 
            implements Component<T>, FillOnClearPolicy.Provider, CanResize  {

    protected IScreenLineSegmentsBuffer segments   = createSegmentsBuffer();

    private boolean                     hasDrawn   = false;
    private Color                       color      = FGBGColors.DEFAULT;
    private BlockChars                  blockChars = BlockChars.DEFAULT;

    private FillOnClearPolicy    fillPolicy = new FillOnClearPolicy.AbstractImpl(this) {
                                                     @Override
                                                     public Rectangle getRectangleForClearAndFill() {
                                                         return BoundedComponent.this.getRectangleForClearAndFill();
                                                     }
                                                     @Override
                                                     public IScreenLineSegmentsBuffer getSegmentsBuffer() {
                                                         return BoundedComponent.this.segments;
                                                     }
                                                 };

    private ParentContainer parentContainer;
    
    private Position canvasOffset;
                                                 
    protected IScreenLineSegmentsBuffer createSegmentsBuffer() {
        IScreenLineSegmentsBuffer segmentsBuffer   = new ScreenLineSegmentsBuffer() {
            protected void doAdd(Collection<ScreenLineSegment> segments, ScreenLineSegment sc) {
                // add to segments buffer (collection)
                ScreenLineSegment.UTIL.addSegmentBounded(BoundedComponent.this.getRectangle(), segments, sc);
            }
        };
        return segmentsBuffer;
    }

    public BoundedComponent(Color color, Rectangle r, ParentContainer parentContainer) {
        super(new Rectangle.Simple(r));
        this.setColor(color);
        this.setParentContainer(parentContainer);
    }

    
    public BoundedComponent(Color color, Rectangle r) {
        this(color, r, (ParentContainer)null);
    }

    public BoundedComponent(Color color, Position p, Dimension d, ParentContainer parentContainer) {
        this(color, new Rectangle.Simple(p, d), parentContainer);
    }

    public BoundedComponent(Color color, Position p, Dimension d) {
        this(color, new Rectangle.Simple(p, d));
    }
    

    public BoundedComponent(Color color, Dimension d, ParentContainer parentContainer) {
        this(color, new Rectangle.Simple(d), parentContainer);
    }

    public BoundedComponent(Color color, Dimension d) {
        this(color, d, (ParentContainer)null);
    }

    public void resize(Dimension d) {
        setRectangle(new Rectangle.Simple(this.getPostion_TopLeft(), d));
        markAsDirty();
    }


    public void clear() {
        segments.clear();
        fillPolicy.fillEmptyIfConfigured();
        markAsDirty();
    }
    
    public boolean isDirty() {
        return !this.hasDrawn;
    }
    
    
    public void markAsDirty() {
        this.hasDrawn = false;
    }
    
    protected void drawIfNeeded() {
        if (isDirty()) {
            clear();
            hasDrawn = true;
            Color currentColor = getColor();
            draw(currentColor, segments);
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
        this.markAsDirty();
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
    
    public boolean isFillOnClear() {
        return this.getFillOnClearPolicy().isFillOnClear();
    }

    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

    
    @Override
    public void setParentContainer(ParentContainer parentContainer) {
        this.parentContainer = parentContainer;
    }
    
    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }
    
    
    @Override
    public String toString() {
        return super.toString() + String.valueOf(fillPolicy.toString());
    }
    
    
    @SuppressWarnings("unchecked")
    protected <T extends BoundedComponent<?>> T makeChildComponent(Supplier<T> createComponent) {
       T component = createComponent.get();
       if (this instanceof ParentContainer) {
           component.setParentContainer( (ParentContainer) this);
       }
       component.setBlockChars(this.getBlockChars());

       component.getFillOnClearPolicy().setEmptyChar(this.getFillOnClearPolicy().getEmptyChar());
       component.getFillOnClearPolicy().setEmptyColor(this.getFillOnClearPolicy().getEmptyColor());
       component.getFillOnClearPolicy().setFillOnClear(this.getFillOnClearPolicy().isFillOnClear());
       return component;
    }
    
    @Override
    public Position getCanvasOffset() {
        return this.canvasOffset;
    }
    
    @Override
    public void setCanvasOffset(Position canvasOffset) {
        this.canvasOffset = canvasOffset;
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