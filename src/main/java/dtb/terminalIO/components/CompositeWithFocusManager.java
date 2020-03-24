package dtb.terminalIO.components;

import java.util.Collection;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.position.Position;

public class CompositeWithFocusManager  extends Composite implements FocusManager
{

    DefaultFocusManager focusManager;

    public CompositeWithFocusManager(Color color)
    {
        super(color);
    }

    public CompositeWithFocusManager()
    {
        super();
    }

    protected DefaultFocusManager getFocusManager() {
        if (focusManager == null) {
            this.focusManager = new DefaultFocusManager(Keys.TAB_KEYSTROKE);
        }
        return focusManager;
    }

    @Override
    protected ComponentWithOffset addComponentWithOffset(Position offset, Component<?> component) {
        ComponentWithOffset result = super.addComponentWithOffset(offset, component);
        if (component instanceof Focusable) {
            addFocusable((Focusable) component);
        } else if (component instanceof ToFocusable) {
            addFocusable(((ToFocusable) component).asFocusable());
        }
        return result;
    }

    public void addFocusable(Focusable focusable) {
        getFocusManager().addFocusable(focusable);
    }

    public void removeFocusable(Focusable focusable) {
        getFocusManager().removeFocusable(focusable);
    }

    @Override
    public Composite remove(Component<?> component) {
        Composite result = super.remove(component);
        if (component instanceof Focusable) {
            removeFocusable((Focusable) component);
        }
        return result;
    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager().setChildAsCurrentFocus(focusableChild, eventBus);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return getFocusManager().getChildFocusables();
    }

    @Override
    public boolean hasFocus()
    {
        return getFocusManager().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().setFocus(focus, eventBus);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return null;
        }
        return getFocusManager().canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te)
    {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager().getChildren().size()<1) {
            return;
        }
        getFocusManager().handle(te);
    }


}
