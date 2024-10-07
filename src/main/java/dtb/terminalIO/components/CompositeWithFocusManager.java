package dtb.terminalIO.components;

import java.util.Collection;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.FocusManagerProvider;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class CompositeWithFocusManager  extends Composite implements FocusManager, FocusManagerProvider
{

    DefaultFocusManager focusManager;
    private BehaviourForCompositeAndFocusManager focusBehaviourManager;


    public CompositeWithFocusManager(Color color)
    {
        super(color);
        getFocusManager();
    }

    public CompositeWithFocusManager(Color color, Dimension bounds)  {
        super(color, bounds);
        getFocusManager();
    }

    public CompositeWithFocusManager()
    {
        super();
        getFocusManager();
    }

    public CompositeWithFocusManager setName(String name) {
        super.setName(name);
        if ( this.focusManager != null ) {
            this.focusManager.setName(name+"-focusManager");
        }
        return this;
    }

    public void rebuildFocusManager() {
        if (focusManager == null) {
            getFocusManager(); // this will build it
            return;
        }
        // delete existing focusables - then rebuild
        for (Component<?> child : getChildren()) {
            this.focusBehaviourManager.removeComponentFocusablesFromFocusManager(child);
        }
        populateFocusManagerWithFocusablesFromChildren();
    }

    public DefaultFocusManager getFocusManager() {
        if (focusManager == null) {
            this.focusManager = buildFocusManager();
            this.focusBehaviourManager = new BehaviourForCompositeAndFocusManager(this, getFocusManager());
            populateFocusManagerWithFocusablesFromChildren();
        }
        return focusManager;
    }

    protected DefaultFocusManager buildFocusManager() {
        DefaultFocusManager focusManager = new DefaultFocusManager();
        focusManager.setNavKeysFirst(false);
        DefaultFocusManager.setDefaultNameAndActualFromParentObject(focusManager, this);
        //focusManager.setNameFromOwner(this);
        return focusManager;
    }

    // only call this once
    private void populateFocusManagerWithFocusablesFromChildren() {
        for (Component<?> child : getChildren()) {
            addComponentFocusablesToFocusManager(child);
        }
    }


    protected void addComponentFocusablesToFocusManager(Component<?> component) { 
        this.focusBehaviourManager.addComponentFocusablesToFocusManager(component);
    }


    @Override
    protected ComponentWithOffset addComponentWithOffset(Position offset, Component<?> component) {
        ComponentWithOffset result = super.addComponentWithOffset(offset, component);
        if (focusManager == null) {
            return result;
        }
        addComponentFocusablesToFocusManager(component);
        return result;
    }

    protected ComponentWithOffset addComponentWithOffset(int index, Position offset, Component<?> component) {
        ComponentWithOffset result = super.addComponentWithOffset(index, offset, component);
        if (focusManager == null) {
            return result;
        }
        addComponentFocusablesToFocusManager(component);
        return result;
    }

    @Override
    public Composite remove(Component<?> component) {
        Composite result = super.remove(component);
        this.focusBehaviourManager.removeComponentFocusablesFromFocusManager(component);
        return result;
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
    }

    public Focusable getFocusableForChild(Component<?> component) {
        Focusable[] focusables = this.focusBehaviourManager.toFocusables(component);
        if (focusables == null || focusables.length == 0) {
            return null;
        }
        return focusables[0];
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return getFocusManager().getChildFocusables();
    }

    @Override
    public boolean hasFocus()
    {
        return getFocusManager().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().setFocus(focus, eventBus);
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().unFocus(newFocusable, eventBus);
    }

    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().focus(oldFocusable, eventBus);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return null;
        }
        return getFocusManager().canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().handle(te);
    }

    @Override
    public FocusManager getParentFocusManager() {
        return getFocusManager().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> getFocusManager().setParentFocusManager(fm));
    }
    

}
