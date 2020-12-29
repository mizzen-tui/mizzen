package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.events.TerminalEvent;

/** 
 * A Focusable Component will raise a RequestFocusEvent when it wants to
 * acquire the input focus.   A {@link TextInput} Component will, for example,
 * request for focus (by raising this event) when the mouse has clicked within its text area.
 */
public class RequestFocusEvent extends TerminalEvent<Focusable> {

    private static final long serialVersionUID = -6351549290745592241L;

    public RequestFocusEvent(Focusable source) {
        super(source, TYPE.REQUEST_FOCUS, source, Q_FULL_POLICY.OFFER);
    }

}
