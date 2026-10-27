package mizzen.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.ParentFocusBehaviour;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.focus.FocusPolicy;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;

/**
 * A row of buttons (or any components) with divider characters between them. Its buttons are TAB
 * stops; for a tab strip make it one stop moving with the arrows:
 * focus().singleTabStop().arrowsMoveInside(ArrowKeys.LEFT_RIGHT).wrapAround().
 */
public class ButtonBar extends ComponentWithFocus<HorizontalPanel, ButtonBar> {


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

    private HorizontalPanel getContainer() {
        return getPanel();
    }
    
    public ButtonBar setHorizontalSpacer(int horizontalSpacer) {
        getContainer().setHorizontalSpacer(horizontalSpacer);
        getContainer().fullrecalcDim();
        return this;
    }
    
    public int getHorizontalSpacer() {
        return getContainer().getHorizontalSpacer();
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
            for (Component<?> child : getContainer().getChildren()) {
                if (child instanceof CharComponent) { // assume divider
                    child.setColor(drawingcolor);
                } else {
                    child.markAsDirty();
                }
            }
        }
        return this;
    }

    /** a ButtonBar places its components itself: add(..) is addComponent(..), with no position */
    public ButtonBar add(Position offset, Component<?> component) {
        if (offset != null && !offset.equals(Position.ZERO)) {
            throw new IllegalStateException("ButtonBar cannot specify position when adding components");
        }
        addComponent(component);
        return this;
    }

    public ButtonBar add(Component<?> component) {
        return add(Position.ZERO, component);
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
        if (index == nonDividerComponents.size()) {
            return addComponent(component);            // at the end (or into an empty bar): an append
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
        removeAllFromPanel();
        nonDividerComponents.clear();
        count = 0;
        lastDivider = null;
    }

    protected void clearAndRebuild() {
        // clear
        removeAllFromPanel();
        count = 0;
        lastDivider = null;
        // rebuild
        for (Component<?> child : nonDividerComponents) {
            this.addComponent(child);
        }
    }

    private void removeAllFromPanel() {
        for (Component<?> child : new ArrayList<Component<?>>(getContainer().getChildren())) {
            getContainer().remove(child);
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
