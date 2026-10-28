package mizzen.components.input;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventBus;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.mouse.Clickable;

/**
 * A label that a click acts on, though it takes no focus itself - most often to focus the input it
 * names, as HTML's &lt;label for=..&gt;. The click reaches it because nothing focusable lies between it
 * and the mouse (MouseEventOrchestrator: a clickable below focus).
 * <pre>
 *     row.add(new LabelFor(Colors.BWhi, "Name :").focuses(nameInput)).add(nameInput);
 * </pre>
 * It shows a TextComponent - its own, or one given (a form keeps its labels to line them up).
 */
public class LabelFor extends AbstractComponentWrappper<TextComponent> implements Clickable {

    Clickable clickable;

    public LabelFor(Color color, String text) {
        this(new TextComponent(color, text));
    }

    /** text: the label shown, kept by its maker (e.g. to change or pad it) */
    public LabelFor(TextComponent text) {
        super(text.getColor(), text);
        clickable = (eventBus) -> {
            Component.UTIL.bringToFront(LabelFor.this);
        };
    }

    /** a click focuses component - one that takes focus (an input); one that doesn't fails here */
    public LabelFor focuses(Object component) {
        Focusable focusable = ToFocusable.UTIL.toFocusable(component);
        if (focusable == null) {
            throw new IllegalArgumentException("LabelFor.focuses(" + HasName.UTIL.getName(component) + "): it takes no focus");
        }
        return setOnClickAction((eventBus) -> eventBus.raiseEvent(new RequestFocusEvent(focusable)));
    }

    public Clickable getClickableAction() {
        return this.clickable;
    }

    public LabelFor setOnClickAction(Clickable clickable) {
        this.clickable = clickable;
        return this;
    }

    public LabelFor appendOnClickAction(Clickable newClickable) {
        return setOnClickAction( Clickable.ChainedClickable.combineClickables(this.clickable, newClickable) );
    }

    @Override
    public void onClick(EventBus eventBus) {
        this.clickable.onClick(eventBus);
    }

}
