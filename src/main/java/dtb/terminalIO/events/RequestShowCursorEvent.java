package dtb.terminalIO.events;

import dtb.terminalIO.geom.position.Position;

public class RequestShowCursorEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = -1373775889825384714L;

    public RequestShowCursorEvent(Object source, Position showCursorPosition) {
        super(source, TYPE.REQUEST_SHOW_CURSOR, showCursorPosition);
        
        if (showCursorPosition == null    ||
            showCursorPosition.getRow()<0 || 
            showCursorPosition.getCol()<0) {
            throw new IllegalStateException("showCursor request illegal " + String.valueOf(showCursorPosition));
        }
    }

    @Override
    public String toString() {
       return "RequestShowCursorEvent [ eventData=" + getEventData()  + " source="+getSource()+"]";
    }
    
}
