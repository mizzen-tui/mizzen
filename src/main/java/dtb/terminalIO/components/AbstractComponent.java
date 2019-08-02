package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;

public abstract class AbstractComponent<T extends AbstractComponent<?>> implements Component<T> {

    private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

    private Color                     color      = ControlCode.DEFAULT_COLOR;

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
            Component.UTIL.markParentDirtyIfChildDirty(this);
            return;
        }
        this.hasDrawn = false;
        
        // if parent exists, then mark it as dirty as well
        Component.UTIL.markParentDirtyIfChildDirty(this);

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

    public T setEmptyChar(char emptyChar) {
        this.fillPolicy.setEmptyChar(emptyChar);
        return (T) this;
    }

    public T setEmptyColor(Color emptyColor) {
        this.fillPolicy.setEmptyColor(emptyColor);
        return (T) this;
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
        if (parentContainer != null && this.getParentContainer() != null && this.getParentContainer() != parentContainer) {
            throw new IllegalStateException("Parent container already set to another component");
        }
        this.parentContainer = parentContainer;
        
        Component.UTIL.markParentDirtyIfChildDirty(this);
    }
    
    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }
    
}