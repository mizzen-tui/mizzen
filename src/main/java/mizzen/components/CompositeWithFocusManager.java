package mizzen.components;

import java.util.Collection;

import mizzen.components.composite.Composite;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.DefaultFocusManager;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.FocusManagerProvider;
import mizzen.events.focus.Focusable;
import mizzen.geom.Dimension;
/**
 * The root panel of an app: a Composite that is also the app's root focus manager - the one on the
 * event bus that routes every key (TUIApp makes it: setDefaultMainApplicationPanel). Not something to
 * extend: for a component made of focusable components extend ComponentWithFocus; for a layout, a
 * Composite is enough.
 */
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
