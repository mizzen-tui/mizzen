package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * Wraps a Composite component and manages events via an internal FocusManager.
 * 
 * Useful for building Composite components with focusable widgets..  ie Forms.
 * 
 */
public class ComponentWithFocusManager extends AbstractComponent<ComponentWithFocusManager> 
     implements ParentContainer, HasTitleAndBorder<ComponentWithFocusManager>, CanResize, 
                FocusManager, GetComponentAtOffset, EventHandlerContainer {
    
    private Composite container;
    private FocusManager focusManager;
    private ChainedEventHandler chainedEventHandler;
    private ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;

    private Function<EventBus, FocusManager> focusManagerBuilder;

    public ComponentWithFocusManager(Color color, Composite container) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        this.focusManager = FocusManager.UTIL.buildFocusManagerFromComponent(this.container, null);
    }

    public ComponentWithFocusManager(Composite container) {
        this(container.getColor(), container);
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

    private ChainedEventHandler getOrBuildChainedEventHandler() {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        }
        return chainedEventHandler;
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
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
    }

    @Override
    public boolean hasFocus() {
        return focusManager.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(eventBus).getChildFocusables().size()<1) {
            return;
        }
        getFocusManager(eventBus).setFocus(focus, eventBus);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(te.getEventBus()).getChildFocusables().size()<1) {
            return null;
        }
        if (focusManager.canHandle(te)!=null) {
            return this;
        }
        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return null;
        }
        if (chainedEventHandlerAsConditionalEventHandler.canHandle(te) == null) {
            return null;
        }
        return this;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(te.getEventBus()).getChildFocusables().size()<1) {
            return;
        }
        if (focusManager.canHandle(te)!=null) {
            focusManager.handle(te);
            return;
        }

        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return;
        }
        if (chainedEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
            chainedEventHandlerAsConditionalEventHandler.handle(te);
        }
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
        if (focusManager == null) {
            return Collections.emptyList();
        }
        return focusManager.getChildFocusables();
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        return getOrBuildChainedEventHandler().prependEventHandler(eventHandler);
    }

    @Override
    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
        return getOrBuildChainedEventHandler().appendEventHandler(eventHandler);
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        return getOrBuildChainedEventHandler().removeEventHandler(eventHandler);
    }

}
