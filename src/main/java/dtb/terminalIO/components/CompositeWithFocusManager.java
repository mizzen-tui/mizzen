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
public class CompositeWithFocusManager  extends Composite implements FocusManager, FocusManagerProvider
{

    DefaultFocusManager focusManager;


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

    public DefaultFocusManager getFocusManager() {
        if (focusManager == null) {
            this.focusManager = buildFocusManager();
        }
        return focusManager;
    }

    protected DefaultFocusManager buildFocusManager() {
        DefaultFocusManager focusManager = new DefaultFocusManager();
        DefaultFocusManager.setDefaultNameAndActualFromParentObject(focusManager, this);
        //focusManager.setNameFromOwner(this);
        return focusManager;
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
    }

    /** the focusable that stands for child component (itself, or the first focusable inside it); null if none */
    public Focusable getFocusableForChild(Component<?> component) {
        Collection<Focusable> focusables = FocusManager.UTIL.focusablesFromComponent(component);
        return focusables.isEmpty() ? null : focusables.iterator().next();
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
