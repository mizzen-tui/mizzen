package dtb.terminalIO.events.keyboard;

import java.util.Arrays;
import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * Handles {@link KeyEvent}s
 */
public class KeyInputEventHandler extends ConditionalEventHandler.Impl {

    IS_Event_TYPE filterOnKeyEvtType;

    public KeyInputEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(canHandlePredicate, handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    static class KeyStrokePredicate implements Predicate<TerminalEvent<?>> {

        private KeyStroke[] onKeyStrokes;

        public KeyStrokePredicate(KeyStroke[] onKeyStrokes) {
            if (onKeyStrokes == null || onKeyStrokes.length<1) {
                throw new IllegalArgumentException("keystrokes not defined");
            }
            this.onKeyStrokes = onKeyStrokes;
        }

        public KeyStrokePredicate(KeyStroke onKeyStroke) {
            this(new KeyStroke[] {onKeyStroke});
        }

        public KeyStrokePredicate(char ... ch) {
            
            if (ch == null || ch.length < 1) {
                throw new IllegalArgumentException("keystrokes not defined");
            }
            
            KeyStroke[] chKeys = new KeyStroke[ch.length];
            for (int i=0; i< ch.length; i++) {
                chKeys[i] = new KeyStroke(ch[i]);
            }
            onKeyStrokes = chKeys;
        }

        @Override
        public boolean test(TerminalEvent<?> te) {
            KeyEvent ke = (KeyEvent) te;
            for (int i = 0; i < onKeyStrokes.length; i++) {
                if (ke.isKeyStroke(onKeyStrokes[i])) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "KeyStrokePredicate [onKeyStrokes=" + Arrays.toString(onKeyStrokes) + "]";
        }

    }

    public KeyInputEventHandler(EventHandler handler, KeyStroke ... onKeyStroke) {
        super(new KeyStrokePredicate(onKeyStroke), handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    public KeyInputEventHandler(EventHandler handler, char ... chars) {
        super(new KeyStrokePredicate(chars), handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    public KeyInputEventHandler(KeyStroke onKeyStroke, EventHandler handler) {
        super(new KeyStrokePredicate(onKeyStroke), handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    public KeyInputEventHandler(KeyStroke[] onKeyStrokes, 
                                Predicate<TerminalEvent<?>> canHandlePredicate,
                                EventHandler handler) {
                 super(new KeyStrokePredicate(onKeyStrokes).and(canHandlePredicate), handler);
                 this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    
    
    public KeyInputEventHandler(KeyStroke onKeyStroke, 
                                Predicate<TerminalEvent<?>> canHandlePredicate,
                                EventHandler handler) {
        super(new KeyStrokePredicate(onKeyStroke).and(canHandlePredicate), handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (filterOnKeyEvtType.test(te)) {
            if (getCanHandlePredicate() == null) {
                return this;
            }
            return super.canHandle(te);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString().replace("Impl", "KeyInputEventHandler");
    }
}
