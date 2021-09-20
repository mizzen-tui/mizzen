package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.OnRequestScrollToViewEventHandler;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.ToFocusable;

/**
 * A ScrollToViewEventHandler that searches for a components ancestor that can handle
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
     * Component asking to scrolled to is embedded within the te
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
