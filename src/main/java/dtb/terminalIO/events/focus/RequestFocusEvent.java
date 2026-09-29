package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.HasName;
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
    
    public static RequestFocusEvent fromSource(Object source) {
        Focusable sourceAsFocusable = ToFocusable.UTIL.toFocusable(source);
        if (sourceAsFocusable == null) {
            throw new IllegalArgumentException("RequestFocusEvent.fromSource("+HasName.UTIL.getName(source)+") Error! Tried to get a focusable from object of type " +source.getClass().getSimpleName());
        }
        return new RequestFocusEvent(sourceAsFocusable);
    } 

    public RequestFocusEvent(ToFocusable source) {
        this(ToFocusable.UTIL.toFocusable(source));
    }

}
