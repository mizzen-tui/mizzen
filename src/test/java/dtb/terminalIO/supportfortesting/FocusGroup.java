package dtb.terminalIO.supportfortesting;

import dtb.terminalIO.components.ComponentWithFocus;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandlerContainer;

/**
 * For tests: a group of focusable components in a panel - a ComponentWithFocus with nothing else.
 * Handlers added to it get the events that reach it through focus (as a form's onChange does).
 */
public class FocusGroup extends ComponentWithFocus<Composite, FocusGroup> implements EventHandlerContainer {

    private final ChainedEventHandler handlers = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

    public FocusGroup(Composite panel) {
        super(Colors.DEFAULT_COLOR, panel);
        focus().handleEvents(handlers.asConditionalEventHandler());
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        handlers.prependEventHandler(eventHandler);
        return this;
    }

    @Override
    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
        handlers.appendEventHandler(eventHandler);
        return this;
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        handlers.removeEventHandler(eventHandler);
        return this;
    }
}
