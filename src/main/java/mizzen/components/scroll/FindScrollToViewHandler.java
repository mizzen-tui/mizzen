package mizzen.components.scroll;

import mizzen.components.Component;
import mizzen.components.ParentContainer;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.OnRequestScrollToViewEventHandler;
import mizzen.events.RequestScrollToView;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.ToFocusable;

/**
 * A {@link OnRequestScrollToViewEventHandler} that searches for a components ancestor that can handle
 * it's REQUEST_SCROLL_TO_VIEW request
 * 
 * @author dbalme
 *
 */
public class FindScrollToViewHandler extends OnRequestScrollToViewEventHandler {

    
    public FindScrollToViewHandler() {
                 // the condition
        super((te)->findEventHandlerToHandle(te) != null,
                 // the handler
               (te)->{
                   final ConditionalEventHandler handler = findEventHandlerToHandle(te);
                   handler.handle(te);
               });
    }

    /**
     * loop up the component's ancestor looking for a component that can handle the scroll to view request
     * Component asking to be scrolled to is embedded within the te (event)
     * @param te the scroll to view request event
     * @return ancestor component that can handle request, null otherwise
     */
    private static ConditionalEventHandler findEventHandlerToHandle(TerminalEvent<?> te) {

        Component<?> component = ((RequestScrollToView)te).getScrollToComponent();
        if (component == null) {
            return null;
        }
        boolean hasAncestors = true;
        while (hasAncestors) {
            ConditionalEventHandler conditionalHandler = asConditionalEventHandlerOrNull(component);
            if (conditionalHandler != null && conditionalHandler.canHandle(te)!=null) {
                return conditionalHandler;
            }
            ParentContainer parent = component.getParentContainer();
            if (parent == null || !(parent instanceof Component<?>)) {
                hasAncestors = false;
            } else {
                component = (Component<?>)parent;
            }
        }
        return null;
    }

    private static ConditionalEventHandler asConditionalEventHandlerOrNull(Component<?> component) {
        if (component instanceof ConditionalEventHandler) {
            return (ConditionalEventHandler) component;
        }
        if (component instanceof ToFocusable) {
            return ((ToFocusable)component).asFocusable();
        }
        return null;
    }


}
