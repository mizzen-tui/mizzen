package dtb.terminalIO.components;

import java.util.Collection;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.HasChildrenAdapter;
import dtb.terminalIO.util.StringUtils;

public abstract class AbstractComponent<T extends AbstractComponent<?>> implements 
                                                                         Component<T>, 
                                                                         FillOnClearPolicy.Provider, 
                                                                         EmptyAttributes<T>,
                                                                         HasName<T>{

    private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

    private Color                     color         = Colors.DEFAULT_COLOR;
    private Color                     originalColor = Colors.DEFAULT_COLOR;  // used only in setEnabled() scenarios
    
    private boolean enabled = true;

    BlockChars                        blockChars = BlockChars.DEFAULT;

    ParentContainer parentContainer;

    FillOnClearPolicy.AbstractImpl<T>    fillPolicy = new FillOnClearPolicy.AbstractImpl<T>(this) {
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

    private String name;

    protected AbstractComponent(Color color) {
        this.color = color == null? Colors.FGBGColors.NO_COLOR_SPECIFIED : color ;
        this.setEmptyColor(this.color);
    }

    protected void clearBeforeDraw() {
        clear();
    }

    private boolean busyDrawingFLAG = false;
    
    // making final to ensure behavior
    final protected void drawIfNeeded11() {
        if (isDirty()) {
            clearBeforeDraw();
           // hasDrawn = true;
            draw(color, segments);
            hasDrawn = true;
            fillOnClear();
            Component.UTIL.assertComponentSegmentsIntegrity(this);
        }
    }

    // making final to ensure behavior
    final protected void drawIfNeeded() {
        try {
            if (isDirty()) {
                //hasDrawn = true;
                if (busyDrawingFLAG) {
                    throw new IllegalStateException("Already in the draw function.  Recursive call!");
                }
                busyDrawingFLAG = true;
                clearBeforeDraw();
                Color currentColor = getColor();
                draw(currentColor, segments);
                fillOnClear();
                hasDrawn = true;
            }
        } finally {
            busyDrawingFLAG= false;
        }
    }

    protected void fillOnClear() {
        if (fillPolicy.isFillOnClear()) {
            ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(getEmptyColor(), getEmptyChar(), this, segments.getSegments());
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
        if (this.segments != null) {
            segments.clear();
        }
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
        this.color = drawingcolor == null ? Colors.FGBGColors.NO_COLOR_SPECIFIED : drawingcolor;
        if (this.enabled) {
            originalColor = this.color;
        }
        this.markAsDirty();
        return (T) this;
    }

    public Color getColor() {
        return color;
    }

    public String toStringDetails() {
        return new Rectangle.Delegate(this).toStringDetails();
    }

    @Override
    public String toString() {
        String prefix = StringUtils.isEmptyStr(getName()) ? this.getClass().getName() : getName();
        return  prefix + ": " + toStringDetails();
    }

    public String getName() {
        return this.name;
    }

    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    public BlockChars getBlockChars() {
        return blockChars;
    }

    public T setBlockChars(BlockChars blockChars) {
        this.blockChars = blockChars;
        return (T) this;
    }

    public FillOnClearPolicy<T> getFillOnClearPolicy() {
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
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
    

    @Override
    public void setEnabled(boolean enabled, EventBus eventBus) {
        // Traverse children first and set their enabled state
        HasChildrenAdapter<Component<?>> hasChildrenAdapter = new HasChildrenAdapter.Simple<>();
        Collection<? extends Component<?>> children = hasChildrenAdapter.asHasChildren(this).getChildren();

        for (Component<?> child : children) {
            child.setEnabled(enabled, eventBus);
        }

        // Handle focus release if disabling
        if (!enabled && ToFocusable.UTIL.isFocusable(this)) {
            Focusable.UTIL.releaseFocusIfNeeded(ToFocusable.UTIL.toFocusable(this), eventBus);
        }

        // important to set this flag before setting color
        // because setColor looks at this flag
        this.enabled = enabled;

        // Preserve the original color when disabling
        if (!enabled) {
            if (originalColor == null) {
                originalColor = getColor();
            }
            // Apply the disabled color using the new asDisabled() method
            //this.setColor(getColor().asDisabled());
            this.setColor(Colors.IBla);
        } else {
            // Restore the original color when re-enabling
            if (originalColor != null) {
                this.setColor(originalColor);
                originalColor = null;
            }
        }

        this.markAsDirty();
    }


    abstract protected Rectangle getRectangleForClearAndFill();

    @Override
    public void setParentContainer(ParentContainer parentContainer) {
        if (parentContainer != null && this.getParentContainer() != null && this.getParentContainer() != parentContainer) {
            throw new IllegalStateException( HasName.UTIL.getName(this)+
                                             ".setParentContainer("+ HasName.UTIL.getName(parentContainer)+
                                             ") :Parent container already set to another component ("+HasName.UTIL.getName(this.getParentContainer())+")");
        }

        if (parentContainer == this) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+
                                               ".setParentContainer(): Parent container cannot be assigned to itself!");
        }

        this.parentContainer = parentContainer;

        Component.UTIL.markParentDirtyIfChildDirty(this);
    }

    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }

}