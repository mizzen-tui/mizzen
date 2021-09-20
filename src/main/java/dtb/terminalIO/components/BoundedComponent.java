package dtb.terminalIO.components;

import java.util.TreeSet;
import java.util.function.Supplier;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;


/**
 * A component that has a defined fixed rectangular size.
 */
public abstract class BoundedComponent<T extends BoundedComponent<?>> extends Rectangle.Delegate 
                                                                      implements Component<T>, 
                                                                                 FillOnClearPolicy.Provider, 
                                                                                 CanResize, 
                                                                                 EmptyAttributes<T> ,
                                                                                 HasName<T> {

    protected IScreenLineSegmentsBuffer segments   = createSegmentsBuffer();

    private boolean                     hasDrawn   = false;
    private Color                       color      = Colors.DEFAULT_COLOR;
    private BlockChars                  blockChars = BlockChars.DEFAULT;

    private FillOnClearPolicy<T>    fillPolicy = new FillOnClearPolicy.AbstractImpl<T>(this) {
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
    
    private String name;

    protected IScreenLineSegmentsBuffer createSegmentsBuffer() {
        IScreenLineSegmentsBuffer segmentsBuffer   = new ScreenLineSegmentsBuffer() {
            protected void doAdd(TreeSet<ScreenLineSegment> segments, ScreenLineSegment sc) {
                // add to segments buffer (collection)
                ScreenLineSegment.UTIL.addSegmentBounded(BoundedComponent.this.getRectangle(), segments, sc);
            }
        };
        return segmentsBuffer;
    }

    public BoundedComponent(Color color, Rectangle r, ParentContainer parentContainer) {
        super(new Rectangle.Simple(r));
        this.setColor(color);
        this.setEmptyColor(this.color);
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
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        //Dimension previousDimension = new Dimension.Simple(this);
        setRectangle(new Rectangle.Simple(this.getPostion_TopLeft(), d));
        if (this.getParentContainer()!=null /* && ! previousDimension.isSameDimension(this)*/) {
            this.getParentContainer().childResized(this);
        }
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
        if (isDirty()) {
            //Component.UTIL.markParentDirtyIfChildDirty(this);
            // this next lines is redundant for many but not
            // all components.  Remove at your peril.
            this.hasDrawn = false;
            return;
        }
        this.hasDrawn = false;
        // if parent exists, then mark it as dirty as well
        Component.UTIL.markParentDirtyIfChildDirty(this);

    }
    
    protected void drawIfNeeded() {
        if (isDirty()) {
            clear();
            hasDrawn = true;
            Color currentColor = getColor();
            draw(currentColor, segments);
            hasDrawn = true;
        }
    }
    
    protected abstract void draw(Color color, IScreenLineSegmentsBuffer mySegments);

    @Override
    public IScreenLineSegmentsBuffer getSegmentsBuffer() {
        drawIfNeeded();
        return segments;
    }

    @Override
    public T setColor(Color drawingcolor) {
        this.color = drawingcolor == null ? Colors.FGBGColors.NO_COLOR_SPECIFIED : drawingcolor;
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
    

    @Override
    public char getEmptyChar() {
        return this.fillPolicy.getEmptyChar();
    }

    @Override
    public Color getEmptyColor() {
        return this.fillPolicy.getEmptyColor();
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
        if (parentContainer != null && this.getParentContainer() != null && this.getParentContainer() != parentContainer) {
            throw new IllegalStateException("Parent("+HasName.UTIL.getName(this.getParentContainer())+") container already set to another component");
        }

        this.parentContainer = parentContainer;
        
        Component.UTIL.markParentDirtyIfChildDirty(this);
    }
    
    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }
    
    
    public String getName() {
        return this.name;
    }
    
    public T setName(String name) {
        this.name = name;
        return (T) this;
    }
    
    /**
     * Make a component that has the same empty char and empty color as well as making this its parent
     * @param createComponent
     * @return
     */
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
            super(Colors.FGBGColors.NO_COLOR_SPECIFIED, r);
        }

        public DONOTHING(int height, int width) {
            this(new Rectangle.Simple(0, 0, height, width));
        }

        public DONOTHING(dtb.terminalIO.geom.position.Position position, Dimension dimension) {
            this(new Rectangle.Simple(position, dimension));
	}

	@Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            // do nothing
        }

    @Override
    public String toString() {
        return  HasName.UTIL.getName(this) + toStringDetails();
    }

	
    }

}