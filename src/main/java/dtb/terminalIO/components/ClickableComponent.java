package dtb.terminalIO.components;

import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.mouse.Clickable;

public class ClickableComponent<T extends Component<?>> extends AbstractComponentWrappper<T> implements ToFocusable, Clickable {

    Clickable clickable;
    Focusable focusable = null;

    public ClickableComponent(Color color, T container) {
        super(color, container);
        // specify default click behaviour
        clickable = (eventBus) -> {
            Component.UTIL.bringToFront(ClickableComponent.this);
        };
        focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this.getWrappedComponent());
    }
    
    public ClickableComponent(T container) {
        this(container.getColor(), container);
    }

    public Clickable getClickableAction() {
        return this.clickable;
    }

    public ClickableComponent<T> setOnClickAction(Clickable clickable) {
        this.clickable = clickable;
        return this;
    }

    public ClickableComponent<T> appendOnClickAction(Clickable newClickable) {
        return setOnClickAction( Clickable.ChainedClickable.combineClickables(this.clickable, newClickable) );
    }

    @Override
    public void onClick(EventBus eventBus) {
        this.clickable.onClick(eventBus);
    }

    @Override
    public Focusable asFocusable() {
        return focusable;
    }


}
