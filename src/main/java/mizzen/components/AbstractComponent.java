package mizzen.components;

import java.util.Collection;
import java.util.function.Function;

import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegmentsBuffer;
import mizzen.traverse.HasChildrenAdapter;
import mizzen.util.StringUtils;

public abstract class AbstractComponent<T extends AbstractComponent<?>> implements 
                                                                         Component<T>, 
                                                                         FillOnClearPolicy.Provider, 
                                                                         EmptyAttributes<T>,
                                                                         HasName<T>, Theme.HasOwnTheme {

    private IScreenLineSegmentsBuffer segments   = new ScreenLineSegmentsBuffer();

    private Color                     color;                // its own: from the constructor or setColor; null - its theme's (getColor)
    private boolean                   showingDisabled;      // setEnabled(false): getColor is its theme's disabled colour
    private Theme                     ownTheme;             // set on this component: for it and what it holds (setTheme)
    private Theme                     theme;                // the theme that applies here, as last looked up (getTheme)
    private int                       themeVersion;
    private Function<Theme, Color>    colorFromTheme = Theme::textColor;   // which theme colour, when it has none of its own
    
    private boolean enabled = true;

    BlockChars                        blockChars = BlockChars.DEFAULT;

    ParentContainer parentContainer;
    Component<?> popupOwner;     // see Component.getPopupOwner

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
        this.color = ownColor(color);                      // none: its theme's
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
            draw(getColor(), segments);
            hasDrawn = true;
            fillOnClear();
            Component.UTIL.assertComponentSegmentsIntegrity(this);
        }
    }

    // making final to ensure behavior
    final protected void drawIfNeeded() {
        try {
            if (isDirty() || (!busyDrawingFLAG && themeChanged())) {
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
        this.color = ownColor(drawingcolor);
        this.markAsDirty();
        return (T) this;
    }

    /** the colour it draws with: its own if one was given (constructor, setColor), else its theme's */
    public Color getColor() {
        if (showingDisabled) {
            return getTheme().disabledColor();      // worked out as it is drawn: a later theme reaches it too
        }
        if (color != null) {
            return color;
        }
        return colorFromTheme.apply(getTheme());
    }

    /**
     * the colour it was given (constructor, setColor); null: none - it takes its theme's. Hand this, not
     * getColor(), to the parts a component makes: getColor() is the theme's colour resolved now, and given to
     * a part it would become the part's own - fixed, deaf to later theme changes (and looked up before the
     * component has a parent, so possibly the wrong theme).
     */
    public Color getOwnColor() {
        return color;
    }

    /**
     * which of its theme's colours it draws with, whichever theme applies - and it has no colour of its own then
     * (what it was given goes). The theme can change later: it follows.
     * <pre>
     *     TextComponent note = new TextComponent("No action picked").setThemeColor(Theme::emphasisColor);
     * </pre>
     * Not new TextComponent(Theme.getDefault().emphasisColor(), ..): that is one colour, fixed - it won't follow.
     */
    @SuppressWarnings("unchecked")
    public T setThemeColor(Function<Theme, Color> colorFromTheme) {
        this.colorFromTheme = colorFromTheme;
        this.color = null;
        markAsDirty();
        return (T) this;
    }

    // a colour given: Colors.DEFAULT_COLOR (also NO_COLOR_SPECIFIED) or null means none - the theme's
    private static Color ownColor(Color given) {
        return given == Colors.DEFAULT_COLOR ? null : given;
    }

    /** which of its theme's colours it draws with when it has none of its own: Theme::textColor, unless set (setThemeColor) */
    public Function<Theme, Color> getThemeColor() {
        return colorFromTheme;
    }

    /**
     * the theme colour this kind of component takes by default - for a component class, in an instance initializer,
     * so every constructor has it: { defaultThemeColor(Theme::inputColor); }. Unlike setThemeColor, a colour given
     * to the constructor stays.
     */
    protected void defaultThemeColor(Function<Theme, Color> colorFromTheme) {
        this.colorFromTheme = colorFromTheme;
    }

    // it has looked its theme up, and a theme was set (or it moved) since: it draws again, its parts too (they check the same
    // as they draw) - no walk over the tree, so parts a component keeps to itself are covered. Looked up again here, so
    // one with its own colours doesn't stay "changed".
    private boolean themeChanged() {
        if (theme == null || themeVersion == Theme.version()) {
            return false;
        }
        getTheme();
        return true;
    }

    /** the theme that applies here: the nearest one set on this component or an ancestor, else the app's, else the default */
    public Theme getTheme() {
        if (theme == null || themeVersion != Theme.version()) {   // looked up once; again only after a theme change or a move
            theme = Theme.lookUp(this);
            themeVersion = Theme.version();
        }
        return theme;
    }

    /** the theme for this component and what it holds; null: none - an ancestor's, the app's or the default applies */
    @SuppressWarnings("unchecked")
    public T setTheme(Theme theme) {
        this.ownTheme = theme;
        Theme.changed();
        Theme.repaint(this);
        return (T) this;
    }

    @Override
    public Theme getOwnTheme() {
        return ownTheme;
    }

    @Override
    public void forgetTheme() {
        theme = null;
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

        // disabled: getColor is the theme's disabled colour, worked out as it is drawn; its own colour is kept as it is
        showingDisabled = !enabled;

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

        if (parentContainer != this.parentContainer) {   // even with none of its own: what it holds may have looked one up
            Theme.forget(this);                            // its ancestors change: it, and what it holds, look again
        }
        this.parentContainer = parentContainer;

        Component.UTIL.markParentDirtyIfChildDirty(this);
    }

    public ParentContainer getParentContainer() {
        return this.parentContainer;
    }

    @Override
    public Component<?> getPopupOwner() {
        return this.popupOwner;
    }

}