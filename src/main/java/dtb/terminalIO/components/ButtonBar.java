package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

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
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.traverse.Traverser;
import dtb.terminalIO.traverse.Traverser.Visitor;

public class ButtonBar extends Composite implements FocusManager {

    private HorizontalPanel horizontalPanel;
    

    private char leftMostBorderChar;
    private char middleBorderChar;
    private char rightMostBorderChar;
    
    private boolean useNavKeys;

    private int count = 0;

    private Component<?> lastDivider = null;

    private ChainedEventHandler chainedEventHandler;
    private ConditionalEventHandler chainedEventHandlerAsConditionalEventHandler;

    private Focusable meAsFocusable; 
    private Focusable current;

    public ButtonBar(Color color) {
        super(color);
        horizontalPanel = new HorizontalPanel();
        horizontalPanel.setHorizontalSpacer(0);
        this.add(horizontalPanel);
        BlockChars blockChars = this.getBlockChars();
        leftMostBorderChar = blockChars.getVERTICAL_LEFT();
        middleBorderChar = blockChars.getVERTICAL_LINE();
        rightMostBorderChar = blockChars.getVERTICAL_RIGHT();
    }

    public ButtonBar() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public void setLeftMostBorderChar(char leftMostBorderChar) {
        this.leftMostBorderChar = leftMostBorderChar;
    }

    public void setMiddleBorderChar(char middleBorderChar) {
        this.middleBorderChar = middleBorderChar;
    }

    public void setRightMostBorderChar(char rightMostBorderChar) {
        this.rightMostBorderChar = rightMostBorderChar;
    }

    public void setNoBorderChars() {
        setLeftMostBorderChar((char)0);
        setMiddleBorderChar((char)0);
        setRightMostBorderChar((char)0);
    }
    
    public ButtonBar setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        horizontalPanel.setColor(drawingcolor);
        for (Component<?> child : horizontalPanel.getChildren()) {
            if (child instanceof CharComponent) { // assume divider
                child.setColor(drawingcolor);
            } else {
                child.markAsDirty();
            }
            
        }
        return this;
    }
    
    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this) );
    }

    public void setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        horizontalPanel.setEmptyChar(emptyChar);
    }

    public void setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        horizontalPanel.setEmptyColor(emptyColor);
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        horizontalPanel.markAsDirty();
    }
    
    public void removeChildren() {
        // TODO: What about the events that have been added to eventBus - how to get rid of?
        horizontalPanel.removeChildren();
        super.removeChildren();
        this.add(horizontalPanel);
        count = 0;
        lastDivider = null;

        // clear out event handlers as well - it will get rebuilt automatically as needed
        chainedEventHandler = null;
        chainedEventHandlerAsConditionalEventHandler = null;

        current = null;
    }

    /**
     * Builds a focus event handler that looks for object within this focusManager's
     * descendant objects
     */
    private ConditionalEventHandler buildRequestFocusEventHandler() {

        ConditionalEventHandler requestFocusEventHandler = FocusManager.UTIL.buildRequestFocusEventHandler(this);

        return requestFocusEventHandler;
    }

    
    private void addEventHandler(ConditionalEventHandler eventHandler) {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
            
            chainedEventHandler.appendEventHandler(buildRequestFocusEventHandler());

        }
        this.chainedEventHandler.appendEventHandler(eventHandler);
    }

    private ConditionalEventHandler addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
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

    public Button addButton(String buttonText, KeyStroke hotKey, EventHandler handler) {
        Button theButton = new Button(this.getColor(), buttonText);
        addComponent(theButton, hotKey, handler);
        return theButton;
    }

    private List<Focusable> eventConsumers = new ArrayList<Focusable>();

    public void addComponent(Component<?> component, KeyStroke hotKey, EventHandler handler) {
        
        if (count == 0  && this.leftMostBorderChar > 0) {
            horizontalPanel.add(new CharComponent(this.getColor(), leftMostBorderChar));
        } else {
            if (lastDivider != null) {
                horizontalPanel.remove(lastDivider);
            }
            if (this.middleBorderChar > 0 ) {
               horizontalPanel.add(new CharComponent(this.getColor(), this.middleBorderChar));
            }
        }

        count++;
        
        if (hotKey == null && component instanceof HasHotKey && ((HasHotKey)component).getHotKey() != null) {
            hotKey = ((HasHotKey)component).getHotKey();
        }
        
        if (hotKey != null && handler != null) {
            
            EventHandler componentHandler = handler;
            
           if (component instanceof Focusable) {
               componentHandler = (te) -> {
                   setChildAsCurrentFocus(((Focusable) component), te.getEventBus());
                   //((Focusable) component).setFocus(true, te.getEventBus());
                   handler.handle(te);
               };
               if (count == 1)  this.current = (Focusable)component;
               eventConsumers.add((Focusable)component);
           } else {
               if (component instanceof ToFocusable) {
                   Focusable focusable = ((ToFocusable) component).asFocusable();
                   componentHandler = (te) -> {
                       focusable.setFocus(true, te.getEventBus());
                       setChildAsCurrentFocus(focusable, te.getEventBus());
                       handler.handle(te);
                   };
                   if (count == 1)  this.current = focusable;
                   eventConsumers.add(focusable);
               }
           }
 
           ConditionalEventHandler rememberHandlerSoWeCanRemove = addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(hotKey, componentHandler ));
        }

        horizontalPanel.add(component);
        
        if (this.rightMostBorderChar > 0) {
            lastDivider = new CharComponent(this.getColor(), this.rightMostBorderChar);
            horizontalPanel.add(lastDivider);
        }
        fullrecalcDim();

    }

    public ButtonBar setHorizontalSpacer(int horizontalSpacer) {
        horizontalPanel.setHorizontalSpacer(horizontalSpacer);
        fullrecalcDim();
        return this;
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

    private Focusable getPrevFocusable() {
        if (eventConsumers.size() < 1) {
            return null;
        }
        int index = current==null? 0:  eventConsumers.indexOf(current);
        if (index < 1) {
            return eventConsumers.get(eventConsumers.size() - 1);
        }
        return eventConsumers.get(index - 1);
    }

    private Focusable getNextFocusable() {
        if (eventConsumers.size() < 1) {
            return null;
        }
        if (eventConsumers.size() < 2) {
            return null;
        }

        int index = current==null? 0:  eventConsumers.indexOf(current);
        if (index < 0 || index == eventConsumers.size() - 1) {
            return eventConsumers.get(0);
        }
        return eventConsumers.get(index + 1);
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
