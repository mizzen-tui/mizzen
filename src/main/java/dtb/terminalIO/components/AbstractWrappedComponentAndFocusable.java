package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;


public abstract class AbstractWrappedComponentAndFocusable<T extends Component<?>> 
                        extends AbstractComponentWrappper<T> implements Focusable {


    protected AbstractWrappedComponentAndFocusable(Color color, T container) {
        super(color, container);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if ( eventHandler().canHandle(te) == null ) {
            return null;
        }
        return this;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        eventHandler().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        asFocusable().unFocus(newFocusable, eventBus);
    }
    
    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        asFocusable().focus(oldFocusable, eventBus);
    }
    
    @Override
    public FocusManager getParentFocusManager() {
        return asFocusable().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
    }
    
    private ConditionalEventHandler conditionalEventHandler;

    public ConditionalEventHandler getConditionalEventHandler() {
        return conditionalEventHandler;
    }

    public void setConditionalEventHandler(ConditionalEventHandler conditionalEventHandler) {
        this.conditionalEventHandler = conditionalEventHandler;
    }


    private ConditionalEventHandler evtHandler;

    protected ConditionalEventHandler eventHandler() {
        
        if (evtHandler != null) {
            return evtHandler;
        }
        
        evtHandler = new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                if ( asFocusable().canHandle(te) != null ) {
                    asFocusable().handle(te);
                    return;
                }
                if (conditionalEventHandler != null) {
                    conditionalEventHandler.handle(te);
                }
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if ( asFocusable().canHandle(te) != null ) {
                    return AbstractWrappedComponentAndFocusable.this;
                }
                
                if (conditionalEventHandler != null && conditionalEventHandler.canHandle(te) != null) {
                    return AbstractWrappedComponentAndFocusable.this;
                }

                return null;
            }
            
        };
        
        return evtHandler;
    }
    
    abstract protected Focusable asFocusable();

}
