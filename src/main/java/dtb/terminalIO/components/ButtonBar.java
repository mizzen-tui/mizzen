package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.traverse.Traverser;
import dtb.terminalIO.traverse.Traverser.Visitor;

public class ButtonBar extends Composite implements FocusManager, EventHandlerContainer {

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
        setLeftMostBorderChar((char) 0);
        setMiddleBorderChar((char) 0);
        setRightMostBorderChar((char) 0);
    }

    public ButtonBar setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (horizontalPanel != null) {
            horizontalPanel.setColor(drawingcolor);
            for (Component<?> child : horizontalPanel.getChildren()) {
                if (child instanceof CharComponent) { // assume divider
                    child.setColor(drawingcolor);
                } else {
                    // child.setColor(drawingcolor);
                    child.markAsDirty();
                }
            }
        }
        return this;
    }

    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    public ButtonBar setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (horizontalPanel != null) {
            horizontalPanel.setEmptyChar(emptyChar);
        }
        return this;
    }

    public ButtonBar setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (horizontalPanel != null) {
            horizontalPanel.setEmptyColor(emptyColor);
        }
        return this;
    }

    public void markAsDirty() {
        super.markAsDirty();
        horizontalPanel.markAsDirty();
    }

    public void removeChildren() {
        // TODO: What about the events that have been added to eventBus - how to get rid
        // of?
        horizontalPanel.removeChildren();
        super.removeChildren();
        this.add(horizontalPanel);
        count = 0;
        lastDivider = null;

        // clear out event handlers as well - it will get rebuilt automatically as
        // needed
        chainedEventHandler = null;
        chainedEventHandlerAsConditionalEventHandler = null;

        current = null;
    }

    /**
     * Builds a focus event handler that looks for object within this focusManager's
     * child objects
     */
    private ConditionalEventHandler buildRequestFocusEventHandler() {

        ConditionalEventHandler requestFocusEventHandler = FocusManager.UTIL.buildRequestFocusEventHandler(this);

        return requestFocusEventHandler;
    }

    private ChainedEventHandler getOrBuildChainedEventHandler() {
        if (chainedEventHandler == null) {
            chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
            chainedEventHandlerAsConditionalEventHandler = chainedEventHandler.asConditionalEventHandler();

            chainedEventHandler.appendEventHandler(buildRequestFocusEventHandler());
        }
        return chainedEventHandler;
    }

    private ConditionalEventHandler makeEventHandlerWithCanvasupdate(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) {
            @Override
            public void handle(TerminalEvent<?> te) {
                super.handle(te);
                updateCanvas(te.getEventBus());
            }
        };
        return handler;
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = makeEventHandlerWithCanvasupdate(eventHandler);
        getOrBuildChainedEventHandler().prependEventHandler(handler);
        return this;
    }

    @Override
    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = makeEventHandlerWithCanvasupdate(eventHandler);
        getOrBuildChainedEventHandler().appendEventHandler(handler);
        return this;
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        getOrBuildChainedEventHandler().removeEventHandler(eventHandler);
        return this;
    }

    @Override
    public Composite add(Component<?> component) {

        // special-case - this should only ever happen during the constructor ( see
        // constructor )
        if (component == this.horizontalPanel) {
            return super.add(component);
        }

        addComponent(component);

        return this;
    }

    public Composite add(int offsetRow, int offsetCol, Component<?> component) {
        throw new IllegalArgumentException("ButtonBar cannot place components at specific locations");
    }

    public Button addButton(Color buttonColor, String buttonText, KeyStroke hotKey, EventHandler handler) {
        Button theButton = new Button(buttonColor, buttonText);
        theButton.setHotKey(hotKey);
        theButton.setActionHandler(handler);
        addComponent(theButton);
        return theButton;
    }

    public Button addButton(String buttonText, KeyStroke hotKey, EventHandler handler) {
        return addButton(this.getColor(), buttonText, hotKey, handler);
    }

    private List<Focusable> eventConsumers = new ArrayList<Focusable>();

    // public void addComponent(Component<?> component, KeyStroke hotKey,
    // EventHandler handler) {
    public void addComponent(Component<?> component) {

        if (count == 0 && this.leftMostBorderChar > 0) {
            horizontalPanel.add(new CharComponent(this.getColor(), leftMostBorderChar));
        } else {
            if (lastDivider != null) {
                horizontalPanel.remove(lastDivider);
            }
            if (this.middleBorderChar > 0) {
                horizontalPanel.add(new CharComponent(this.getColor(), this.middleBorderChar));
            }
        }

        count++;

        ConditionalEventHandler componentAsEventHandler = null;
        if (component instanceof ConditionalEventHandler) {
            componentAsEventHandler = (ConditionalEventHandler) component;
        } else {
            if (component instanceof ToFocusable) {
                componentAsEventHandler = ((ToFocusable) component).asFocusable();
            }
        }
        if (componentAsEventHandler != null) {
            appendEventHandler(componentAsEventHandler);
            if (componentAsEventHandler instanceof Focusable) {
                eventConsumers.add((Focusable) componentAsEventHandler);
            }
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
        if (chainedEventHandlerAsConditionalEventHandler.canHandle(te) == null) {
            return null;
        }
        return this;
    }

    @Override
    public void hilite() {
        super.hilite();

        // We will visit this component and all its children using a Traverser
        Traverser<Component<?>> componentTraverser = new Traverser<Component<?>>();

        // We will hilite any node that we visit
        Visitor<Component<?>> hilite = (c) -> {
            new HiliterAdapter<Component<?>>(c).getHiliter().hilite();
            return Traverser.CONTINUE_TRAVERSE;
        };
        // now finally, let's traverse and hilite
        componentTraverser.traverse(horizontalPanel, (c) -> hilite.visit(c));

        this.markAsDirty();
    }

    @Override
    public void unHilite() {
        super.unHilite();
        horizontalPanel.unHilite();

        // traverse and unilte buttonbar and all its children
        Traverser<Component<?>> componentTraverser = new Traverser<Component<?>>();
        Visitor<Component<?>> unHilite = (c) -> {
            new HiliterAdapter<Component<?>>(c).getHiliter().unHilite();
            return Traverser.CONTINUE_TRAVERSE;
        };
        componentTraverser.traverse(horizontalPanel, (c) -> unHilite.visit(c));

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
        int index = current == null ? 0 : eventConsumers.indexOf(current);
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

        int index = current == null ? 0 : eventConsumers.indexOf(current);
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
        FocusManager.UTIL.setNewFocusable(focusable, prevCurrent, eventBus, (f, evtBus) -> updateCanvas(evtBus));
        this.current = focusable;
    }

    private Focusable asFocusable() {
        if (meAsFocusable == null) {
            this.meAsFocusable = new DefaultFocusableBehaviour.Simple(this, (ConditionalEventHandler) this) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (ButtonBar.this.current != null) {
                        if (focus) {
                            ButtonBar.this.setChildAsCurrentFocus(ButtonBar.this.current, eventBus);
                        } else {
                            ButtonBar.this.current.setFocus(false, eventBus);
                            ButtonBar.this.setChildAsCurrentFocus(null, eventBus);
                        }
                    }
                }
            };
        }
        return meAsFocusable;
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return eventConsumers;
    }

}
