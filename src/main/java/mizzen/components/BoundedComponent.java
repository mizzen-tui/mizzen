package mizzen.components;

import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;

import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegmentsBuffer;
import mizzen.traverse.HasChildrenAdapter;
import mizzen.util.StringUtils;


/**
 * A component that has a defined fixed rectangular size.
 */
public abstract class BoundedComponent<T extends BoundedComponent<?>> extends Rectangle.Delegate 
                                                                      implements Component<T>, 
                                                                                 FillOnClearPolicy.Provider, 
                                                                                 CanResize, 
                                                                                 EmptyAttributes<T> ,
                                                                                 HasName<T>, Theme.HasOwnTheme {

    protected IScreenLineSegmentsBuffer segments   = createSegmentsBuffer();

    private boolean                     hasDrawn   = false;
    private Color                     color;                // its own: from the constructor or setColor; null - its theme's (getColor)
    private Color                     colorWhileEnabled;    // its own colour, kept while setEnabled(false) shows the disabled one
    private boolean                   showingDisabled;
    private Theme                     ownTheme;             // set on this component: for it and what it holds (setTheme)
    private Theme                     theme;                // the theme that applies here, as last looked up (getTheme)
    private int                       themeVersion;
    private Function<Theme, Color>    colorFromTheme = Theme::textColor;   // which theme colour, when it has none of its own
    private BlockChars                  blockChars = BlockChars.DEFAULT;
    private boolean enabled = true;

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
        if (d != null) {
            Dimension.UTIL.failIfNegative(HasName.UTIL.getName(this), d.getHeight(), d.getWidth());   // names this component: d may be worked out lazily (a delegate)
        }
        //Dimension previousDimension = new Dimension.Simple(this);
        setRectangle(new Rectangle.Simple(this.getPostion_TopLeft(), d));
        if (this.getParentContainer()!=null /* && ! previousDimension.isSameDimension(this)*/) {
            this.getParentContainer().childResized(this);
        }
        markAsDirty();
    }

    /**
     * The size this component's own resize left it at, for a resize that passes its size on to its
     * parts. super.resize(d) tells the parent, and the parent's rules can resize this again before it
     * returns (e.g. a width rule after a height rule) - so the parts take this size, not d.
     * <pre>
     *     public void resize(Dimension d) {
     *         super.resize(d);
     *         inner.resize(sizeAfterSuperResize());
     *     }
     * </pre>
     * It reads the size stored by the resize, so it holds when getWidth()/getHeight() are overridden.
     */
    protected Dimension sizeAfterSuperResize() {
        return new Dimension.Simple(getRectangle());
    }

    public void clear() {
        segments.clear();
        //fillPolicy.fillEmptyIfConfigured();
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
            //this.hasDrawn = false;
            return;
        }
        this.hasDrawn = false;
        // if parent exists, then mark it as dirty as well
        Component.UTIL.markParentDirtyIfChildDirty(this);
    }

    private boolean busyDrawingFLAG = false;

    // making final to ensure behavior
    final protected void drawIfNeeded() {
        try {
            if (isDirty() || (!busyDrawingFLAG && themeChanged())) {
                if (busyDrawingFLAG) {
                    throw new IllegalStateException("Already in the draw function.  Recursive call!");
                }
                busyDrawingFLAG = true;
                clear();
                if (getHeight() > 0 && getWidth() > 0) {   // squeezed to no rows or no columns: nothing to draw into
                    Color currentColor = getColor();
                    draw(currentColor, this.segments);
                    fillOnClear();
                }
                hasDrawn = true;
                Component.UTIL.assertComponentSegmentsIntegrity(this);
            }
        } finally {
            busyDrawingFLAG= false;
        }
    }

    protected void fillOnClear() {
        if (fillPolicy.isFillOnClear()) {
            //Logging.info("fillOnClear() after draw !" + String.valueOf(this));
            ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(getEmptyColor(), getEmptyChar(), this, this.segments.getSegments());
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
        this.color = ownColor(drawingcolor);
        if (!showingDisabled) {
            colorWhileEnabled = this.color;
        }
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

    /** the colour it draws with: its own if one was given (constructor, setColor), else its theme's */
    public Color getColor() {
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
        if (!showingDisabled) {
            colorWhileEnabled = null;
        }
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

        if (parentContainer != this.parentContainer) {   // even with none of its own: what it holds may have looked one up
            Theme.forget(this);                            // its ancestors change: it, and what it holds, look again
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

    public void moveTo(int row, int col) {
        if (row != getRow() || col != getCol()) {
            markAsDirty();
        }
        super.moveTo(row, col);
    }

    @Override
    public Position getCanvasOffset() {
        return this.canvasOffset;
    }

    @Override
    public void setCanvasOffset(Position canvasOffset) {
        this.canvasOffset = canvasOffset;
    }

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

        // disabled: shown in the theme's disabled colour; enabled again: its own colour back (none, if it had none)
        if (!enabled) {
            if (!showingDisabled) {
                showingDisabled = true;
                Color own = colorWhileEnabled;
                this.setColor(getTheme().disabledColor());
                colorWhileEnabled = own;
            }
        } else if (showingDisabled) {
            showingDisabled = false;
            this.setColor(colorWhileEnabled);
        }

        this.markAsDirty();
    }
  
    
    public static class DONOTHING extends BoundedComponent<DONOTHING> {

        public DONOTHING(Rectangle r) {
            super(Colors.FGBGColors.NO_COLOR_SPECIFIED, r);
        }

        public DONOTHING(int height, int width) {
            this(new Rectangle.Simple(0, 0, height, width));
        }

        public DONOTHING(mizzen.geom.position.Position position, Dimension dimension) {
            this(new Rectangle.Simple(position, dimension));
	}

	@Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            // do nothing
        }

    @Override
    public String toString() {
            String prefix = StringUtils.isEmptyStr(getName()) ? this.getClass().getName() : getName();
            return  prefix + ": " + toStringDetails();
    }

    }

}