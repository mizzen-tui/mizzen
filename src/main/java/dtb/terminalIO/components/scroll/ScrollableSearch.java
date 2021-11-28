package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;

// A work in progress..

public class ScrollableSearch extends AbstractComponentWrappper<HorizontalPanel> implements
                                                                                 Focusable {

    Button findButton;
    boolean inFindMode = false;
    TextInput textFilterInput;
    
    public ScrollableSearch(Color color) {
        super(color, new HorizontalPanel(color));
        this.findButton = new Button(color, "Find", true); // true = toggle button
        this.textFilterInput = new TextInput(10);
        this.getWrappedComponent().add(findButton);
        this.getWrappedComponent().add(textFilterInput);
        this.setInFindMode(false);
        
        this.findButton.onChange((te)->{
            setInFindMode(this.findButton.getValue());
        });
    }
    
    private ScrollableSearch setInFindMode(boolean inFindMode) {
        this.inFindMode = inFindMode;
        if (inFindMode) {
            this.textFilterInput.show();
        } else {
           this.textFilterInput.hide();
        }
        return this;
    }
    
    public static class UTIL {
        public static void addSearch(ScrollableComponent viewComp) {
            ScrollableSearch search = new ScrollableSearch(viewComp.getBorderColor());
            //viewComp.addFooterComponent(search,Keys.CTRL_F_KEYSTROKE,search);
            throw new IllegalStateException("NOT implemented!!!");
        }
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (this.findButton.canHandle(te) == null) {
            return null;
        }
        return this;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        this.findButton.handle(te);
    }

    @Override
    public boolean hasFocus() {
        return this.findButton.hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        this.findButton.setFocus(focus, eventBus);
    }
    
    @Override
    public FocusManager getParentFocusManager() {
        return this.findButton.getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        this.findButton.setParentFocusManager(focusManager);
    }

    
    
}
