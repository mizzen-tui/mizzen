package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

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
import dtb.terminalIO.events.focus.Focusable;
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
                                EventHandlerContainer {
    
    private Composite container;
    //private FocusManager parentFocusManager;
    private FocusManager focusManager;
    private ChainedEventHandler chainedEventHandler;
    private ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;

    private Function<EventBus, FocusManager> focusManagerBuilder;

    public ComponentWithFocusManager(Color color, Composite container) {
        super(color);
        this.container = container;
        this.container.setParentContainer(this);
        this.focusManager = FocusManager.UTIL.buildFocusManagerFromComponent(this.container, null);
        if (this.focusManager == null) {
            this.focusManager = new DefaultFocusManager().setNameFromOwner(this);
        }
        this.focusManager.setParentFocusManager(this);
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
        DefaultFocusManager.setDefaultNameFromParentObject(focusManager, this);
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
    
    protected FocusManager getFocusManager() {
        if (this.focusManager == null) {
            throw new NullPointerException("getFocusManager(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        return this.focusManager;
    }

    private void addFocusable(Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("addFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).addFocusable(focusable);
        //focusable.setParentFocusManager(this);
    }

    private void insertFocusable(int index, Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("addFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).insertFocusable(index, focusable);
        //focusable.setParentFocusManager(this);
    }

    private void removeFocusable(Focusable focusable) {
        if (this.focusManager == null) {
            throw new NullPointerException("removeFocusable(): focusManager is null - cannot get access to a focusManager for "+ HasName.UTIL.getName(this));
        }
        // bad form here.. 
        ((DefaultFocusManager)this.focusManager).removeFocusable(focusable);
        //focusable.setParentFocusManager(null);
    }
    
    private ChainedEventHandler getOrBuildChainedEventHandler() {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        }
        return chainedEventHandler;
    }
    
    private static final Focusable[] EMPTY_FOCUSABLES = new Focusable[0];
    
    // links component to its focusables 
    private static class ComponentToFocusables {
        Component<?> component;
        Focusable[] focusables;
        
        public ComponentToFocusables(Component<?> component, Collection<Focusable> focusables) {
            super();
            this.component = component;
            if (focusables == null) {
                this.focusables = EMPTY_FOCUSABLES;
            } else {
                this.focusables =    focusables.toArray(new Focusable[focusables.size()]);
            }
        }

        public ComponentToFocusables(Component<?> component) {
            this(component, FocusManager.UTIL.focusablesFromComponent(component));
        }
    }
    
    // note - you would think I need a map - nope.. I want a list - don't change it.
    List<ComponentToFocusables> mapChildComponentToFocusables = new ArrayList<ComponentToFocusables>();
    
    private int findInMap(Component<?> component) {
        for ( int i = 0;  i < mapChildComponentToFocusables.size(); i++) {
            if (component == mapChildComponentToFocusables.get(i).component) {
                return i;
            }
        }
        return -1;
    }
    
    private boolean isInMap(Component<?> component) {
        return findInMap(component) >= 0;
    }

    private Focusable[] toFocusables(Component<?> component) {
        int idx = findInMap(component);
        if (idx < 0) {
            return EMPTY_FOCUSABLES;
        }
        return mapChildComponentToFocusables.get(idx).focusables;
    }

    protected void addComponentAndItsFocusables(Position offset, Component<?> component) {
        getContainer().add(offset, component);
        addComponentFocusablesToFocusManager(component);
    }

    protected void addComponentAndItsFocusables(Component<?> component) {
        addComponentAndItsFocusables(Position.ZERO, component);
    }

    protected void addComponentAndItsFocusables(int row, int col, Component<?> component) {
        addComponentAndItsFocusables(new Position.Simple(row, col),component);
    }

    
    // ASSUMPTION:  component has already been added to the container
    protected void addComponentFocusablesToFocusManager(Component<?> component) {
        
        // sanity checks
        int idx = this.getContainer().indexOf(component);
        if (idx < 0) {
            throw new IllegalStateException("incorrect use of addComponentFocusablesToFocusManager().. Component parameter not found in internal container.  You first need to add component to container first!");
        }
        
        // if component's focusables are already added, then let's remove and later (below) re-add ( because related focusable's could now be different)
        removeComponentFocusablesFromFocusManager(component);

        ComponentToFocusables cf = new ComponentToFocusables(component);

        // find next component with focusables - if any
        OptionalInt nextComponentIdx = IntStream.range(idx+1, getContainer().getChildren().size()).filter( i ->  isInMap(getContainer().getChild(i))).findFirst();

        if ( ! nextComponentIdx.isPresent()) { // there is no next component, so easy (and common) case, we're the last 
            mapChildComponentToFocusables.add(cf);
            for ( Focusable f : cf.focusables) {
                addFocusable(f);
            }
            return;
        }

        // now the fun begins .. we need to figure out where to insert the focusables based on where component
        // is positioned with the childrenlist
        
        Component<?> nextComponent = getContainer().getChild(nextComponentIdx.getAsInt());
        int nextComponentIdxInMap = findInMap(nextComponent);
        // sanity check
        if (nextComponentIdxInMap < 0) {
            throw new IllegalStateException("LOGIC ERROR!!! nextComponent not in map");
        }
        
        int focusableIndex = 0;
        if (nextComponentIdxInMap > 0) {
            focusableIndex = this.getContainer().getChildrenList().subList(0, nextComponentIdxInMap)
                                                                  .stream().map((c)-> toFocusables(c).length )
                                                                           .mapToInt(Integer::intValue)
                                                                           .sum();
        }

        mapChildComponentToFocusables.add(nextComponentIdxInMap, cf);
        for ( Focusable f : cf.focusables) {
            insertFocusable(focusableIndex, f);
            focusableIndex++;
        }

    }

    protected void removeComponentFocusablesFromFocusManager(Component<?> component) {
        int idx = findInMap(component);
        if (idx == -1) {
            return ;
        }
        ComponentToFocusables cf = mapChildComponentToFocusables.get(idx);
        mapChildComponentToFocusables.remove(cf);
        for ( Focusable f : cf.focusables) {
            removeFocusable(f);
        }
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
        this.getFocusManager().setParentFocusManager(parentFocusManager);
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

}
