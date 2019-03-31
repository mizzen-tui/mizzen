package dtb.terminalIO.components;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;

public class ButtonBar extends Composite implements ConditionalEventHandler {
    
    
    HorizontalPanel horizontalPanel;
    
    int count = 0;
    
    Component<?> lastDivider = null;
    
    ChainedEventHandler chainedEventHandler;
    ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;
    
    Runnable updateCanvas = null; 
    
    public ButtonBar(Color color) {
        super(color);
        horizontalPanel = new HorizontalPanel();
        horizontalPanel.setHorizontalSpacer(0);
        this.add(horizontalPanel);
    }

    public ButtonBar() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public void updateCanvas() {
        if (updateCanvas != null) {
            updateCanvas.run();
        }
    }

    public void removeChildren() {
        horizontalPanel.removeChildren();
        super.removeChildren();
        this.add(horizontalPanel);
        count = 0;
        lastDivider = null;
         
        // clear out event handlers as well
        chainedEventHandler = null;
        chainedEventHandlerAsConditionalEventHandler = null;
    }

    
    void addEventHandler(ConditionalEventHandler eventHandler) {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        }
        this.chainedEventHandler.addEventHandler(eventHandler);
    }
    
    ConditionalEventHandler addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) { 
                                                        @Override
                                                        public void handle(TerminalEvent<?> te) {
                                                            super.handle(te);
                                                            updateCanvas();
                                                        }
                                                    };
        addEventHandler(handler);
        
        return handler;
    }
    
    public void setUpdateCanvas(Runnable _updateCanvas) {
        this.updateCanvas = _updateCanvas;
    }
    
    
    public void addButton(String buttonText, KeyStroke hotKey, EventHandler handler) {
        
        addComponent(new Button(buttonText), hotKey, handler);  // <-- could be a simple text Component
        
    }

    public void addComponent(Component<?> component, KeyStroke hotKey, EventHandler handler) {
        
        BlockChars blockChars = this.getBlockChars();

        
        if (count == 0) {
            horizontalPanel.add(new CharComponent(this.getColor(), blockChars.getVERTICAL_LEFT()));
        } else {
            if (lastDivider != null) {
                horizontalPanel.remove(lastDivider);
            }
            horizontalPanel.add(new CharComponent(this.getColor(), blockChars.getVERTICAL_LINE()));
        }
                
        count++;
        
        if (hotKey != null && handler != null) {
           ConditionalEventHandler rememberHandlerSoWeCanRemove = addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(hotKey, handler ));
        }
        
        horizontalPanel.add(component);
        
        lastDivider = new CharComponent(this.getColor(), blockChars.getVERTICAL_RIGHT());
        horizontalPanel.add(lastDivider);
        fullrecalcDim();

    }

    
    
    @Override
    public void handle(TerminalEvent<?> te) {
        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return;
        }
        chainedEventHandlerAsConditionalEventHandler.handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (chainedEventHandlerAsConditionalEventHandler == null) {
            return null;
        }
        if (chainedEventHandlerAsConditionalEventHandler.canHandle(te)==null) {
            return null;
        }
        return this;
    }


}
