package dtb.terminalIO.components.tab;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;

public abstract class TabAction<T extends Component<?>> {
    
    T theComponent;
    boolean isActivated = false;
    
    public TabAction() {
    }
    
    public final T getComponentForTab(TabContext tabContext) {
        if (theComponent != null) {
            return theComponent;
        }
        theComponent = getComponent(tabContext);
        return theComponent;
    }

    protected abstract T getComponent(TabContext tabContext);

    public void resize(TabContext tabContext) {
        Component<?> component = getComponentForTab(tabContext);
        if (component instanceof CanResize) {
            Dimension size = new Dimension.Simple(tabContext.getBounds());
            ((CanResize)component).resize(size);
        }
    }

    final void activate(EventBus eventBus, TabContext tabContext) {
        activateComponent(eventBus, tabContext, theComponent);
        isActivated = true;
        resize(tabContext);
    }

    /**
     * The focusable to move focus to when this tab is explicitly chosen (click / hot key),
     * or null if focus should stay where it is.
     */
    final Focusable getFocusableForActivation(TabContext tabContext) {
        Focusable focusable =  tabContext.toFocusable(getComponentForTab(tabContext));
        // following is a hack ... if the first focusable is a drop-down and then
        // we set it to focusssed it will "drop-down" when tab activated - which is annoying
        // this code might not be good and not last long - time will tell
        if (focusable != null && (!(focusable instanceof DropdownListBox))) {
            return focusable;
        }
        return null;
    }

    protected final void deactivate(EventBus eventBus, TabContext tabContext) {
        if (isActivated) {
            deactivateComponent(eventBus, tabContext, theComponent);
            isActivated = false;
        }
        
    }

    public void activateComponent(EventBus eventBus, TabContext tabContext, T theComponent) {
        
    }

    public void deactivateComponent(EventBus eventBus, TabContext tabContext, T theComponent) {
        
    }


}