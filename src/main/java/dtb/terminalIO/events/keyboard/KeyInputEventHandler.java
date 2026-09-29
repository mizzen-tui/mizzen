package dtb.terminalIO.events.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * Handles {@link KeyEvent}s.
 * 
 * When a user presses a key, a KeyEvent is raised and will be handled by 
 * a KeyInputEventHandler.
 * 
 */
public class KeyInputEventHandler extends ConditionalEventHandler.Impl {

    IS_Event_TYPE filterOnKeyEvtType;

    public KeyInputEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(canHandlePredicate, handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    public KeyInputEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, boolean defer, EventHandler handler) {
        super(canHandlePredicate, (te) -> {
            if (defer) {
                te.getEventBus().asynch(handler);
            } else {
                handler.handle(te);
            }
        });
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
        
        public void addKeyStroke(KeyStroke keyStroke) {
            List<KeyStroke> keyStrokes = new ArrayList<KeyStroke>( Arrays.asList(onKeyStrokes));
            if (keyStrokes.contains(keyStroke)) {
                throw new IllegalArgumentException(String.valueOf(keyStroke));
            }
            keyStrokes.add(keyStroke);
            this.onKeyStrokes = keyStrokes.toArray(new KeyStroke[] {});
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
    
    public void addKeyStroke(KeyStroke keyStroke) {
        Predicate<TerminalEvent<?>> predicate = this.getCanHandlePredicate();
        if (predicate instanceof KeyStrokePredicate) {
            KeyStrokePredicate keyStrokePredicate = (KeyStrokePredicate) predicate;
            keyStrokePredicate.addKeyStroke(keyStroke);
            return;
        }
        throw new IllegalStateException("Can only append keystroke to handler with simple keysstroke predicate");
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (filterOnKeyEvtType.test(te)) {
            if (getCanHandlePredicate() == null) {
                return this;
            }
            // we have a predicate object so let the super eval the 
            // predicate and decide if we are handling the event
            return super.canHandle(te);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString().replace("Impl", "KeyInputEventHandler"+HasName.UTIL.getName(this));
    }
}
