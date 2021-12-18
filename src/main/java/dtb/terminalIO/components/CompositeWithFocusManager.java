package dtb.terminalIO.components;

import java.util.Collection;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.FocusManagerProvider;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;

public class CompositeWithFocusManager  extends Composite implements FocusManager, FocusManagerProvider
{

    DefaultFocusManager focusManager;

    public CompositeWithFocusManager(Color color)
    {
        super(color);
    }

    public CompositeWithFocusManager()
    {
        super();
    }

    public DefaultFocusManager getFocusManager() {
        if (focusManager == null) {
            this.focusManager = buildFocusManager();
            populateFocusManagerWithFocusablesFromChildren();
        }
        return focusManager;
    }
    
    protected DefaultFocusManager buildFocusManager() {
        DefaultFocusManager focusManager = new DefaultFocusManager(Keys.TAB_KEYSTROKE);
        focusManager.setNavKeysFirst(false);
        focusManager.setName(this.getClass().getSimpleName()+"-FocusManager");
        return focusManager;
    }
    
    // only call this once
    private void populateFocusManagerWithFocusablesFromChildren() {
        for (Component<?> child : getChildren()) {
            addFocusableFromChildComponent(child);
        }
    }

    private void addFocusableFromChildComponent(Component<?> component) {

        ComponentWithOffsetWithFocusable child = (ComponentWithOffsetWithFocusable)findChildOrFail(component, "addFocusableFromChildComponent");
        // add as a focusable if relevant
        
        Focusable asFocusable = FocusManager.UTIL.findOrBuildFocusableFromComponent(component, null);
        
        if (asFocusable != null) {
            addFocusable(asFocusable);
            child.asFocusable = asFocusable;
        }
        
    }

    static class ComponentWithOffsetWithFocusable extends ComponentWithOffset {

        Focusable asFocusable;
        
        public ComponentWithOffsetWithFocusable(Position offset, Component<?> component) {
            super(offset, component);
        }
        
    }

    protected ComponentWithOffset buildCompositeWithOffset(Position offset, Component<?> component) {
        return new ComponentWithOffsetWithFocusable(offset, component);
    }
    

    
    @Override
    protected ComponentWithOffset addComponentWithOffset(Position offset, Component<?> component) {
        ComponentWithOffset result = super.addComponentWithOffset(offset, component);
        if (focusManager == null) {
            return result;
        }
        addFocusableFromChildComponent(component);
        return result;
    }


    public void addFocusable(Focusable focusable) {
        getFocusManager().addFocusable(focusable);
    }

    public void removeFocusable(Focusable focusable) {
        getFocusManager().removeFocusable(focusable);
    }

    @Override
    public Composite remove(Component<?> component) {
        ComponentWithOffsetWithFocusable child = (ComponentWithOffsetWithFocusable)findChildOrFail(component, "remove");
        Composite result = super.remove(component);
        if (child.asFocusable != null) {
            removeFocusable(child.asFocusable);
        }
        return result;
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
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
        getFocusManager().setParentFocusManager(focusManager);
    }
    

}
