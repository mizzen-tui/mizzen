package dtb.terminalIO.components;

import java.util.Collection;
import java.util.function.Function;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;

public  abstract class AbstractWrappedComponentWithFocusManager<T extends Component<?>> extends AbstractWrappedComponentAndFocusable<T> implements FocusManager {

    private FocusManager focusManager;
    private Function<EventBus, FocusManager> focusManagerBuilder;

    protected AbstractWrappedComponentWithFocusManager(Color color, T componentToWrap) {
        super(color, componentToWrap);
        if (componentToWrap instanceof FocusManager) {
            this.focusManagerBuilder = (eventBus)-> (FocusManager)componentToWrap;
            setFocusManager((FocusManager)componentToWrap);
        } else {
            this.focusManagerBuilder = null;
            this.focusManager = new DefaultFocusManager() {
                public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
                    Focusable prevFocus = getCurrentChildFocus();
                    super.setChildAsCurrentFocus(focusableChild, eventBus);
                    if (getCurrentChildFocus() != prevFocus && focusableChild != null) {
                        AbstractWrappedComponentWithFocusManager.this.childHasCurrentFocus(focusableChild, eventBus);
                    }
                }
            };
        }
    }

    public void setFocusManager(FocusManager focusManager) {
        this.focusManager = focusManager;
        DefaultFocusManager.setDefaultNameFromParentObject(focusManager, this);
    }

    public void setCustomExtendedEventHandler(ConditionalEventHandler customExtendedEventHandler) {
        ((DefaultFocusManager) this.focusManager).setCustomExtendedEventHandler(customExtendedEventHandler);
    }

    
    private FocusManager getFocusManager(EventBus eventBus) {
        if (this.focusManager == null) {
            if (focusManagerBuilder == null) {
                throw new IllegalStateException("focusManager and focusManagerBuilder are null - cannot get access to a focusManager for "+HasName.UTIL.getName(this));
            }
            setFocusManager(focusManagerBuilder.apply(eventBus));
        }
        return this.focusManager;
    }

    private FocusManager getFocusManager() {
        if (this.focusManager == null) {
            throw new NullPointerException("getFocusManager(): focusManager is null - cannot get access to a focusManager for "+HasName.UTIL.getName(this));
        }
        return this.focusManager;
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
    protected Focusable asFocusable() {
        return getFocusManager();
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager(eventBus).setChildAsCurrentFocus(focusableChild, eventBus);
    }

    public void childHasCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        // default - do nothing
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
        //return getFocusManager();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return getFocusManager().getChildFocusables();
        //return Collections.singleton(getFocusManager());
    }
    
    public void addFocusable(Focusable focusable) {
        if (getFocusManager() instanceof DefaultFocusManager) {
            ((DefaultFocusManager) getFocusManager()).addFocusable(focusable);
        } else {
            throw new IllegalStateException("Can only add a focusable to "+this.getName()+" when focusManager is a DefaultFocusManager");
        }
    }
    
    public void addAllFocusables(Collection<Focusable> focusables) {
        for (Focusable focusable : focusables) {
            addFocusable(focusable);
        }
    }

    public void registerAllFocusables() {
        Collection<Focusable> focusables = FocusManager.UTIL.focusablesFromComponent(this.getWrappedComponent());
        addAllFocusables(focusables);
    }

}
