package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.FocusManagerProvider;
import dtb.terminalIO.events.focus.FocusPolicy;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.DefaultFocusManager.NAV_STYLE;
import dtb.terminalIO.geom.CanResize;
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
public class ComponentWithFocusManager<T extends ComponentWithFocusManager<?>> 
                     extends AbstractComponent<T> 
                     implements ParentContainer, 
                                HasTitleAndBorder<T>, 
                                CanResize,
                                FocusManager,
                                GetComponentAtOffset,
                                EventHandlerContainer,
                                FocusManagerProvider {

    private Composite container;
    private FocusManager focusManager;
    private ChainedEventHandler chainedEventHandler;
    private ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;
    private BehaviourForCompositeAndFocusManager focusBehaviourManager;

    private Function<EventBus, FocusManager> focusManagerBuilder;

    public ComponentWithFocusManager(Color color, Composite container) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        FocusManager focusManager = FocusManager.UTIL.buildFocusManagerFromComponent(this.container, null);
        if (focusManager == null) {
            focusManager = new DefaultFocusManager().setNameFromOwner(this);
        }
        this.setFocusManager(focusManager);
        this.focusBehaviourManager = new BehaviourForCompositeAndFocusManager(this.container, this.focusManager);
    }

    public ComponentWithFocusManager(Composite container) {
        this(container.getColor(), container);
    }

    public ComponentWithFocusManager(Color color) {
        this(color, new Composite());
    }


    protected Composite getContainer() {
        return container;
    }

    public void setFocusManager(FocusManager focusManager) {
        this.focusManager = focusManager;
        // now give the focusManager a name that relates to this object (for debugging purposes) and also
        // if focusManager is DefaultFocusManager, tell DefaultFocusManager that this object is the actual FocusManager
        // which tells DefaultFocusManager it is purely acting as a proxy only needed for its "behaviour"
        // And yes .. this feels all very "hacky" - A weakness in my model I suspect
        DefaultFocusManager.setDefaultNameAndActualFromParentObject(focusManager, this);
        this.focusBehaviourManager = new BehaviourForCompositeAndFocusManager(this.container, this.focusManager);
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
    public FocusManager getFocusManager() {
        if (this.focusManager == null) {
            throw new NullPointerException("getFocusManager(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        return this.focusManager;
    }

    private DefaultFocusManager getAssertDefaultFocusManager(String funcName) {
        FocusManager fm = getFocusManager();
        if (!(fm instanceof DefaultFocusManager)) {
            throw new IllegalStateException(" Tried "+funcName+" on a focusManager that is not of type DefaultFocusManager ");
        }
        return ((DefaultFocusManager)fm);
    }

    public T setCycling(boolean cycling) {
        getAssertDefaultFocusManager("setCycling()").setCycling(cycling);
        return (T) this;
    }

    /** false: TAB / SHIFT-TAB skip this component and everything in it; clicks and hot keys still work */
    public T setTabStop(boolean tabStop) {
        getAssertDefaultFocusManager("setTabStop()").setTabStop(tabStop);
        return (T) this;
    }

    /** What kind of focus container this is: GROUP (default), WIDGET or TRAP - see FocusPolicy. */
    public T setFocusPolicy(FocusPolicy focusPolicy) {
        getAssertDefaultFocusManager("setFocusPolicy()").setFocusPolicy(focusPolicy);
        return (T) this;
    }

    public T setNavKeysFirst(boolean navKeysFirst) {
        getAssertDefaultFocusManager("setNavKeysFirst()").setNavKeysFirst(navKeysFirst);
        return (T) this;
    }

    public T setNavKeysNone() {
        getAssertDefaultFocusManager("setNavKeysNone()").setNavKeysNone();
        return (T) this;
    }

    private ChainedEventHandler getOrBuildChainedEventHandler() {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        }
        return chainedEventHandler;
    }

    protected EventHandlerContainer getEventHandlerContainer() {
        return getOrBuildChainedEventHandler();
    }

    protected void addComponentAndItsFocusables(Position offset, Component<?> component) {
        this.focusBehaviourManager.addComponentAndItsFocusables(offset, component);
    }

    protected void addComponentAndItsFocusables(Component<?> component) {
        this.focusBehaviourManager.addComponentAndItsFocusables(Position.ZERO, component);
    }

    // ASSUMPTION:  component has already been added to the container
    protected void addComponentFocusablesToFocusManager(Component<?> component) { 
        this.focusBehaviourManager.addComponentFocusablesToFocusManager(component);
    }

    protected void removeComponentFocusablesFromFocusManager(Component<?> component) {
        this.focusBehaviourManager.removeComponentFocusablesFromFocusManager(component);
    }

    public T setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (getContainer() != null) {
            getContainer().setEmptyChar(emptyChar);
        }
        return (T) this;
    }

    public T setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (getContainer() != null) {
            getContainer().setEmptyColor(emptyColor);
        }
        return (T) this;
    }

    public void markAsDirty() {
        super.markAsDirty();
        getContainer().markAsDirty();
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
    public FocusManager getParentFocusManager() {
        return getFocusManager().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager parentFocusManager) {
        Focusable.UTIL.setParentFocusManager(this, parentFocusManager, (fm)-> this.getFocusManager().setParentFocusManager(fm));
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
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(eventBus).getChildFocusables().size()<1) {
            return;
        }
        getFocusManager(eventBus).unFocus(newFocusable, eventBus);
    }

    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(eventBus).getChildFocusables().size()<1) {
            return;
        }
        getFocusManager(eventBus).focus(oldFocusable, eventBus);
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

        if (chainedEventHandlerAsConditionalEventHandler != null && 
                chainedEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
                chainedEventHandlerAsConditionalEventHandler.handle(te);
                return;
            }

        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(te.getEventBus()).getChildFocusables().size()<1) {
            return;
        }
        if (focusManager.canHandle(te)!=null) {
            focusManager.handle(te);
            return;
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

    public T setOnResize(Consumer<Dimension> onResize) {
        container.setOnResize(onResize);
        return (T) this;
    }

    @Override
    public boolean hasBorder() {
        return container.hasBorder();
    }

    @Override
    public T setBorder(boolean hasBorder) {
        container.setBorder(hasBorder);
        return (T) this;
    }

    @Override
    public T setBorderColor(Color borderColors) {
        container.setBorderColor(borderColors);
        return (T) this;
    }

    @Override
    public T setTitle(Color titleColors, String title) {
        container.setTitle(titleColors, title);
        return (T) this;
    }

    public String getTitle() {
       return container.getTitle();
    }

    @Override
    public T setTitle(String title) {
        container.setTitle(title);
        return (T) this;
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

    public void removeChildren() {

        for (Component<?> child : getContainer().getChildren()) {
            removeComponentFocusablesFromFocusManager(child);
            getContainer().remove(child);
        }
//        getContainer().removeChildren();
//        for (Focusable focusable : new ArrayList<Focusable>(getChildFocusables())) {
//            removeFocusable(focusable);
//        }
    }
    
    
    //  Some methods to make it look a bit like a Composite
    
    public T remove(Component<?> component) {
        this.getContainer().remove(component);
        return (T) this;
    }
    

    protected T addComponentToInternalContainer(Position offset, Component<?> component) {
        this.getContainer().add(offset, component);
        return (T) this;
    }

    public T add(Position offset, Component<?> component) {
        return addComponentToInternalContainer(offset, component);
    }
 
    public T hide(Component<?> component) {
        this.getContainer().hide();
        return (T) this;
    }

    public T show(Component<?> component)  {
        this.getContainer().show();
        return (T) this;
    }

    public T setDropShadow(boolean dropShadow, Component<?> component)  {
        return (T) this;
    }
    
    public T add(Component<?> component) {
        return this.add(Position.ZERO, component);
    }

    public T add(int offsetRow, int offsetCol, Component<?> component) {
        return this.add(new Position.Simple(offsetRow, offsetCol), component);
    }

}
