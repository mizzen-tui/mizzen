package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * Wraps a Composite component and manages events via an internal FocusManager.
 * 
 * Useful for building Composite components with focusable widgets..  ie Forms.
 * 
 */
public class ComponentWithFocusManager extends AbstractComponent<ComponentWithFocusManager> 
     implements ParentContainer, HasTitleAndBorder<ComponentWithFocusManager>, CanResize, FocusManager, GetComponentAtOffset {
    
    private Composite container;
    private FocusManager focusManager;

    private Function<EventBus, FocusManager> focusManagerBuilder;

    public ComponentWithFocusManager(Color color, Composite container) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        this.focusManager = new DefaultFocusManager();
    }

    public ComponentWithFocusManager(Composite container) {
        this(container.getColor(), container);
    }

    public ComponentWithFocusManager(Color color, Composite container, Function<EventBus, FocusManager> focusManagerBuilder) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        this.focusManagerBuilder = focusManagerBuilder;
    }
    
    public ComponentWithFocusManager(Color color, Composite container, FocusManager focusManager) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        this.focusManager = focusManager;
    }

    public ComponentWithFocusManager(Color color) {
        this(color, new Composite(color));
    }
    
    
    public Composite getContainer() {
        return container;
    }

    public void setFocusManager(FocusManager focusManager) {
        this.focusManager = focusManager;
    }


    public void setFocusManagerBuilder(Function<EventBus, FocusManager> focusManagerBuilder) {
        if (focusManagerBuilder != null) {
            this.focusManager = null;
        }
        this.focusManagerBuilder = focusManagerBuilder;
    }


    private FocusManager getFocusManager(EventBus eventBus) {
        if (this.focusManager == null) {
            if (focusManagerBuilder == null) {
                throw new IllegalStateException("focusManager and focusManagerBuilder are null - cannot get access to a focusManager for "+ this.getClass().getName());
            }
            this.focusManager = focusManagerBuilder.apply(eventBus);
        }
        return this.focusManager;
    }
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.container.getSegmentsBuffer());
    }

    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.container.getComponentAtOffset(offset);
        if (component == this.container) {
            return this;
        }
        return component;
    }
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.container);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public boolean hasFocus() {
        return focusManager.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        getFocusManager(eventBus).setFocus(focus, eventBus);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (focusManager.canHandle(te)!=null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        focusManager.handle(te);
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager(eventBus).setChildAsCurrentFocus(focusableChild, eventBus);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return focusManager.getCurrentChildFocus();
    }

    @Override
    public void hilite() {
        container.hilite();
    }

    @Override
    public void unHilite() {
        container.unHilite();
    }

    @Override
    public int getCol() {
        return container.getCol();
    }

    @Override
    public int getRow() {
        return container.getRow();
    }

    @Override
    public int getWidth() {
        return container.getWidth();
    }

    @Override
    public int getHeight() {
        return container.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        container.moveTo(row, col);
    }

    @Override
    public void resize(Dimension d) {
        container.resize(d);
    }

    public ComponentWithFocusManager setOnResize(Consumer<Dimension> onResize) {
        container.setOnResize(onResize);
        return this;
    }

    @Override
    public ComponentWithFocusManager setBorder(boolean hasBorder) {
        container.setBorder(hasBorder);
        return this;
    }

    @Override
    public ComponentWithFocusManager setBorderColor(Color borderColors) {
        container.setBorderColor(borderColors);
        return this;
    }

    @Override
    public ComponentWithFocusManager setTitle(Color titleColors, String title) {
        container.setTitle(titleColors, title);
        return this;
    }

    @Override
    public ComponentWithFocusManager setTitle(String title) {
        container.setTitle(title);
        return this;
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return container.getRectangleForClearAndFill();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return focusManager.getChildFocusables();
    }
    
}
