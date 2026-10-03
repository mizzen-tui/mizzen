package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;

public class ButtonBar extends ComponentWithFocusManager<ButtonBar>   {


    private char leftMostBorderChar;
    private char middleBorderChar;
    private char rightMostBorderChar;

    private int count = 0;

    private Component<?> lastDivider = null;
    private List<Component<?>> nonDividerComponents;
    
    public ButtonBar(Color color, String name) {
        super(color, new HorizontalPanel().setHorizontalSpacer(0));
        BlockChars blockChars = this.getBlockChars();
        leftMostBorderChar = blockChars.getVERTICAL_LEFT(); 
        middleBorderChar = blockChars.getVERTICAL_LINE();
        rightMostBorderChar = blockChars.getVERTICAL_RIGHT();
        this.setName(name==null? "buttonBar" : name);
        getContainer().setName(getName() +".horizontalPanel");
        this.setFocusManager( new DefaultFocusManager()
                                          .setNameFromOwner(this)
                                          .setCycling(false)
                                          .setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                          .setNextKeyStroke(Keys.TAB_KEYSTROKE));
        nonDividerComponents = new ArrayList<>();
    }
    public ButtonBar(Color color) {
        this(color, "Buttonbar");
    }

    public ButtonBar() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public ButtonBar(String name) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, name);
    }
    
    public ButtonBar setHorizontalSpacer(int horizontalSpacer) {
        ((HorizontalPanel)getContainer()).setHorizontalSpacer(horizontalSpacer);
        getContainer().fullrecalcDim();
        return this;
    }
    
    public int getHorizontalSpacer() {
        return ((HorizontalPanel)getContainer()).getHorizontalSpacer();
    }

    private DefaultFocusManager getDefaultFocusManager() {
        return (DefaultFocusManager) super.getFocusManager();
    }

    public ButtonBar setPrevKeyStroke(KeyStroke prevKeyStroke) {
        getDefaultFocusManager().setPrevKeyStroke(prevKeyStroke);
        return this;
    }

    public ButtonBar setNextKeyStroke(KeyStroke nextKeyStroke) {
        getDefaultFocusManager().setNextKeyStroke(nextKeyStroke);
        return this;
    }

    public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
        return getDefaultFocusManager().setPreviousAsCurrentFocus(eventBus);
    }

    public Focusable getCurrentFocus() {
        return getDefaultFocusManager().getCurrentChildFocus();
    }

    public boolean setNextAsCurrentFocus(EventBus eventBus) {
        return getDefaultFocusManager().setNextAsCurrentFocus(eventBus);
    }
    
    public ButtonBar setCycling(boolean cycling) {
        getDefaultFocusManager().setCycling(cycling);
        return this;
    }

    public ButtonBar setLeftMostBorderChar(char leftMostBorderChar) {
        this.leftMostBorderChar = leftMostBorderChar;
        return this;
    }

    public ButtonBar setMiddleBorderChar(char middleBorderChar) {
        this.middleBorderChar = middleBorderChar;
        return this;
    }

    public ButtonBar setRightMostBorderChar(char rightMostBorderChar) {
        this.rightMostBorderChar = rightMostBorderChar;
        return this;
    }

    public ButtonBar setNoBorderChars() {
        // setting border characters to 0 signals no border
        setLeftMostBorderChar((char) 0);
        setMiddleBorderChar((char) 0);
        setRightMostBorderChar((char) 0);
        clearAndRebuild();
        return this;
    }

    public ButtonBar setColor(Color drawingcolor) {
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

    @Override
    protected ButtonBar addComponentToInternalContainer(Position offset, Component<?> component) {
        // we override ancestor to reroute to addComponent(..)
        if (offset != null && !offset.equals(Position.ZERO)) {
            throw new IllegalStateException("ButtonBar cannot specify position when adding components");
        }
        addComponent(component);
        return this;
    }


    public <C extends Component<?>> C addComponent(C component) {

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
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+": Duplicate child ("+HasName.UTIL.getName(component)+")!");
        }
        if (component == this) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+": Child ("+HasName.UTIL.getName(component)+") cannot be its parent!");
        }
        // TODO:  check for cycle?
        if (index<0) {
            throw new IndexOutOfBoundsException(HasName.UTIL.getName(this)+": index<0");
        }
        if (index > nonDividerComponents.size()) {
            throw new IndexOutOfBoundsException(HasName.UTIL.getName(this)+": index>buttonbar size");
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

    public ButtonBar remove(Component<?> component) {

        if ( ! nonDividerComponents.contains(component)) {
            throw new IllegalArgumentException("Remove: Could not find component"+HasName.UTIL.getName(component)+")!");
        }

        int index = getContainer().indexOf(component);
        if (index < 0) {
            throw new IllegalStateException("Remove: Could not find component("+HasName.UTIL.getName(component)+") in container!");
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

    public ButtonBar removeAt(int index) {
        return remove(nonDividerComponents.get(index));
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
