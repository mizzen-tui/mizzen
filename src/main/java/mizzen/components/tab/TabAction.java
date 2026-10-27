package mizzen.components.tab;

import mizzen.components.Component;
import mizzen.components.input.DropdownListBox;
import mizzen.components.tab.TabPanel.TabContext;
import mizzen.events.EventBus;
import mizzen.events.focus.Focusable;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;

/**
 * What one tab of a TabPanel shows, and what happens when the tab is chosen or left.
 * <p/>
 * getComponent builds the tab's content - once, the first time the tab is shown (it is kept after
 * that). activateComponent / deactivateComponent run each time the tab is chosen / left - e.g. to
 * start and stop an animation, or to add the tab's own key handlers to the event bus and take them
 * off again. When a tab is chosen, focus moves to its first focusable component - unless that is a
 * dropdown, which would open its list.
 * <p/>
 * Example:
 * <pre>
 *     tabPanel.addTab("Animation", Keys.F4_KEYSTROKE, new TabAction&lt;VerticalPanel&gt;() {
 *         protected VerticalPanel getComponent(TabContext tabContext) {
 *             return buildAnimationPanel(tabContext.getBounds());       // built once
 *         }
 *         public void activateComponent(EventBus eventBus, TabContext tabContext, VerticalPanel panel) {
 *             animation.start();                                        // each time the tab is chosen
 *         }
 *         public void deactivateComponent(EventBus eventBus, TabContext tabContext, VerticalPanel panel) {
 *             animation.stop();                                         // each time another tab is chosen
 *         }
 *     });
 * </pre>
 * For a tab that only shows a component, TabPanel.addTab(text, component) makes the TabAction.
 */
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
        // (the dropdown hands out its focusable - check the component it stands for)
        if (focusable != null && !(focusable instanceof DropdownListBox) && !(focusable.getFocusComponent() instanceof DropdownListBox)) {
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