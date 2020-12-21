package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;

public class ButtonBar2 extends ComponentWithFocusManager  {


    private char leftMostBorderChar;
    private char middleBorderChar;
    private char rightMostBorderChar;

    private int count = 0;

    private Component<?> lastDivider = null;
    private List<Component<?>> nonDividerComponents;
    
    public ButtonBar2(Color color) {
        super(color, new HorizontalPanel().setHorizontalSpacer(0));
        BlockChars blockChars = this.getBlockChars();
        leftMostBorderChar = blockChars.getVERTICAL_LEFT();
        middleBorderChar = blockChars.getVERTICAL_LINE();
        rightMostBorderChar = blockChars.getVERTICAL_RIGHT();
        this.setFocusManager( new DefaultFocusManager()
                                          .setCycling(false)
                                          .setNavKeysFirst(true)
                                          .setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                          .setNextKeyStroke(Keys.TAB_KEYSTROKE));
        nonDividerComponents = new ArrayList<>();
    }

    public ButtonBar2() {
        this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public ButtonBar2 setHorizontalSpacer(int horizontalSpacer) {
        ((HorizontalPanel)getContainer()).setHorizontalSpacer(horizontalSpacer);
        getContainer().fullrecalcDim();
        return this;
    }

    private DefaultFocusManager getDefaultFocusManager() {
        return (DefaultFocusManager) super.getFocusManager();
    }

    public ButtonBar2 setPrevKeyStroke(KeyStroke prevKeyStroke) {
        getDefaultFocusManager().setPrevKeyStroke(prevKeyStroke);
        return this;
    }

    public ButtonBar2 setNextKeyStroke(KeyStroke nextKeyStroke) {
        getDefaultFocusManager().setPrevKeyStroke(nextKeyStroke);
        return this;
    }

    public ButtonBar2 setCycling(boolean cycling) {
        getDefaultFocusManager().setCycling(cycling);
        return this;
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
        // setting border characters to 0 signals no border
        setLeftMostBorderChar((char) 0);
        setMiddleBorderChar((char) 0);
        setRightMostBorderChar((char) 0);
        clearAndRebuild();
    }

    public ButtonBar2 setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (getContainer() != null) {
            getContainer().setColor(drawingcolor);
            for (Component<?> child : getChildren()) {
                if (child instanceof CharComponent) { // assume divider
                    child.setColor(drawingcolor);
                } else {
                    child.markAsDirty();
                }
            }
        }
        return this;
    }


    public <T extends Component<?>> T addComponent(T component) {

        // first let's deal with injecting divider characters in between each component on this ButtonBar
        if (count == 0 && this.leftMostBorderChar > 0) {
            getContainer().add(new CharComponent(this.getColor(), leftMostBorderChar));
        } else {
            if (lastDivider != null) {
                getContainer().remove(lastDivider);
            }
            if (count > 0 && this.middleBorderChar > 0) {
                getContainer().add(new CharComponent(this.getColor(), this.middleBorderChar));
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
                addFocusable((Focusable) componentAsEventHandler);
            }
        }

        getContainer().add(component);
        nonDividerComponents.add(component);

        if (this.rightMostBorderChar > 0) {
            lastDivider = new CharComponent(this.getColor(), this.rightMostBorderChar);
            getContainer().add(lastDivider);
        }
        getContainer().fullrecalcDim();

        return component;

    }

    public <T extends Component<?>> T insertComponent(int index, T component) {
        // Doing these checks preemptively (even tho' the child composite does the same checks)
        // because I don't want to add border components if add/inserting
        // component is in error
        if (getContainer().containsChild(component)) {
            throw new IllegalArgumentException("Duplicate child!");
        }
        if (component == this) {
            throw new IllegalArgumentException("Child cannot be its parent!");
        }
        // TODO:  check for cycle?
        if (index<0) {
            throw new IndexOutOfBoundsException("index<0");
        }
        if (index > nonDividerComponents.size()) {
            throw new IndexOutOfBoundsException("index>buttonbar size");
        }
        if (nonDividerComponents.size()==0) {
            return addComponent(component);
        }
        
        Component<?> originalComponentAtIndex = nonDividerComponents.get(index);
        int indexInContainer = getContainer().indexOf(originalComponentAtIndex);
        
        if (count > 0 && this.middleBorderChar > 0) {
            getContainer().insert(indexInContainer, new CharComponent(this.getColor(), this.middleBorderChar));
        }

        getContainer().insert(indexInContainer, component);
        nonDividerComponents.add(index, component);

        count++;
        return component;
    }

    public <T extends Component<?>> int indexOf(T component) {
        return nonDividerComponents.indexOf(component);
    }
    
    private boolean isDividerComponent(Component<?> component) {
        return (containsChild(component)  && indexOf(component)<0);
    }

    public boolean containsChild(Component<?> component) {
        return getContainer().containsChild(component);
    }

    public ButtonBar2 remove(Component<?> component) {

        if ( ! nonDividerComponents.contains(component)) {
            throw new IllegalArgumentException("Remove: Could not find component!");
        }
        
        int index = getContainer().indexOf(component);
        if (index < 0) {
            throw new IllegalStateException("Remove: Could not find component in container!");
        }
        Component<?> prevChild = null;
        Component<?> nextChild = null;
        if (index>0 && nonDividerComponents.indexOf(component)==nonDividerComponents.size()-1) {
            prevChild = getContainer().getChild(index-1);
        }
        if (index < getContainer().size()-2) {
            nextChild = getContainer().getChild(index+1);
        }
        
        if (nextChild != null && isDividerComponent(nextChild)) {
            getContainer().remove(nextChild);
        }
        
        getContainer().remove(component);
        nonDividerComponents.remove(component);
        count--;
        
        if (prevChild != null && isDividerComponent(prevChild)) {
            getContainer().remove(prevChild);
        }
        return this;
    }

    public void removeChildren() {
        super.removeChildren();
        nonDividerComponents.clear();
        count = 0;
        lastDivider = null;
    }
    
    protected void clearAndRebuild() {
        // clear
        super.removeChildren();
        count = 0;
        lastDivider = null;
        // rebuild
        for (Component<?> child : nonDividerComponents) {
            this.addComponent(child);
        }
    }

    public Button addButton(Button theButton, KeyStroke hotKey, EventHandler handler) {
        theButton.setHotKey(hotKey);
        theButton.onChange(handler);
        addComponent(theButton);
        return theButton;
    }

    public Button addButton(Color buttonColor, String buttonText, KeyStroke hotKey, EventHandler handler) {
        return addButton( new Button(buttonColor, buttonText), hotKey, handler);
    }

    public Button addButton(String buttonText, KeyStroke hotKey, EventHandler handler) {
        return addButton(this.getColor(), buttonText, hotKey, handler);
    }

}
