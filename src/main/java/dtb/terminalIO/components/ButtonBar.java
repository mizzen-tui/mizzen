package dtb.terminalIO.components;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.traverse.Traverser;
import dtb.terminalIO.traverse.Traverser.Visitor;

public class ButtonBar extends Composite implements FocusManager {
    
    HorizontalPanel horizontalPanel;
    
    int count = 0;
    
    Component<?> lastDivider = null;
    
    ChainedEventHandler chainedEventHandler;
    ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;
    
    private Focusable meAsFocusable; 
    private Focusable current;

    
    public ButtonBar(Color color) {
        super(color);
        horizontalPanel = new HorizontalPanel();
        horizontalPanel.setHorizontalSpacer(0);
        this.add(horizontalPanel);
    }

    public ButtonBar() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this) );
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

        current = null;
    }

    
    void addEventHandler(ConditionalEventHandler eventHandler) {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        }
        this.chainedEventHandler.appendEventHandler(eventHandler);
    }
    
    ConditionalEventHandler addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) { 
                                                        @Override
                                                        public void handle(TerminalEvent<?> te) {
                                                            super.handle(te);
                                                            updateCanvas(te.getEventBus());
                                                        }
                                                    };
        addEventHandler(handler);
        
        return handler;
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
            
            EventHandler componentHandler = handler;
            
           if (component instanceof Focusable) {
               componentHandler = (te) -> {
                   ((Focusable) component).setFocus(true, te.getEventBus());
                   handler.handle(te);
               };
           } else {
               if (component instanceof ToFocusable) {
                   Focusable focusable = ((ToFocusable) component).asFocusable();
                   componentHandler = (te) -> {
                       focusable.setFocus(true, te.getEventBus());
                       handler.handle(te);
                   };
               }
           }
            
           ConditionalEventHandler rememberHandlerSoWeCanRemove = addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(hotKey, componentHandler ));
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


    @Override
    public void hilite() {
        super.hilite();
	
        Traverser<Component<?>> componentTraverser = new Traverser<Component<?>>();
        Visitor<Component<?>> hilite = (c)-> {
                                                new HiliterAdapter<Component<?>>(c).getHiliter().hilite(); 
                                                return Traverser.CONTINUE_TRAVERSE;
                                             };
        componentTraverser.traverse(horizontalPanel, (c)-> hilite.visit(c) );

        this.markAsDirty();
    }

    @Override
    public void unHilite() {
        super.unHilite();
        horizontalPanel.unHilite();

        Traverser<Component<?>> componentTraverser = new Traverser<Component<?>>();
        Visitor<Component<?>> unHilite = (c)-> {new HiliterAdapter<Component<?>>(c).getHiliter().unHilite(); return Traverser.CONTINUE_TRAVERSE;};
        componentTraverser.traverse(horizontalPanel, (c)-> unHilite.visit(c) );

        this.markAsDirty();
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
    public Focusable getCurrentChildFocus() {
        return this.current;
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
        Focusable prevCurrent = this.current;
        this.current = null;
        FocusManager.UTIL.setNewFocusable(focusable, prevCurrent, eventBus, (f)-> updateCanvas(eventBus) );
        this.current = focusable;
    }
    
    @SuppressWarnings("unchecked")
    private Focusable asFocusable() {
        if (meAsFocusable == null) {
            this.meAsFocusable = new DefaultFocusableBehaviour.Simple(this, (ConditionalEventHandler) this) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (ButtonBar.this.current != null) {
                        ButtonBar.this.setChildAsCurrentFocus(focus ? ButtonBar.this.current : null, eventBus);
                    }
                }
            };
        }
        return meAsFocusable;
    }

    
}
