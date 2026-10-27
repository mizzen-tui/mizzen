package mizzen.components.input;

import java.util.function.Predicate;

import mizzen.components.HasName;
import mizzen.events.TimedRequestedController;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;

/**
 * A container that has a value. It just allows for an extra level of
 * indirection.
 * 
 * <p/>
 * 
 * Components can implement HasValue.. ex  ListBox, TextInput, CheckBox
 * 
 *
 * @param <V>
 */
public interface HasValue<V> {  // TODO:  Make a SettableValue that extends this.. ( HasValue components can implement this, then later we can 'bindable' forms - bind based on 'getName')

    V getValue();

    /*
     * When a component you pick a value with (ListBox, DropdownListBox, TreeView ...) reports a
     * change - flags for its setEventTriggerForValueChanged, combined with |. Default: all of them,
     * so the value is reported on every move (e.g. to preview as you go); ENTER | MOUSE_CLICK only
     * reports a pick.
     */
    int VALUE_CHANGED_ON_PREV_KEYSTROKE = 1;
    int VALUE_CHANGED_ON_NEXT_KEYSTROKE = 2;
    int VALUE_CHANGED_ON_FOCUS = 4;
    int VALUE_CHANGED_ON_ENTER_KEY = 8;
    int VALUE_CHANGED_ON_MOUSE_CLICK = 16;
    int DEFAULT_TRIGGER_FOR_ONVALUE_CHANGED = VALUE_CHANGED_ON_PREV_KEYSTROKE | VALUE_CHANGED_ON_NEXT_KEYSTROKE
                                            | VALUE_CHANGED_ON_FOCUS | VALUE_CHANGED_ON_ENTER_KEY | VALUE_CHANGED_ON_MOUSE_CLICK;

    public static class ValueChangedEvent<V> extends TerminalEvent<HasValue<V>> {

        private static final long serialVersionUID = 4544682502113778402L;

        public ValueChangedEvent(HasValue<V> eventData) {
            super(eventData, TYPE.VALUE_CHANGED, eventData);
        }
        
        public String toString() {
            return "HasValue.ValueChangedEvent [source=" + getSource() + ", eventData=" + this.getEventData() + "]";
        }
    }

    public abstract class OnValueChangedEventHandler<V> implements ConditionalEventHandler , HasName<OnValueChangedEventHandler<V>> {

        static Predicate<TerminalEvent<?>> valueChangedPredicate = new IS_Event_TYPE(TerminalEvent.TYPE.VALUE_CHANGED);

        HasValue<V> targetSource;

        Object[] valuesOfInterest;
        
        String name;

        public OnValueChangedEventHandler(HasValue<V> targetSource, V[] valuesOfInterest) {
            this.targetSource = targetSource;
            this.valuesOfInterest = valuesOfInterest;
        }

        public OnValueChangedEventHandler(HasValue<V> targetSource, V valueOfInterest) {
            this.targetSource = targetSource;
            this.valuesOfInterest = new Object[] { valueOfInterest };
        }

        public OnValueChangedEventHandler(HasValue<V> targetSource) {
            this.targetSource = targetSource;
            this.valuesOfInterest = null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (!valueChangedPredicate.test(te)) {
                return null; // null means - can't handle.
            }

            HasValue<V> source = (HasValue<V>) te.getEventData();

            if (!source.equals(targetSource)) {
                return null;
            }

            if (valuesOfInterest == null || valuesOfInterest.length == 0) {
                return this; // listener doesn't care about value - usual case
            }

            V value = source.getValue();

            for (int i = 0; i < valuesOfInterest.length; i++) {
                if (valuesOfInterest[i] != null && valuesOfInterest[i].equals(value)) {
                    return this;
                }
            }
            return null; // no match!
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handle(TerminalEvent<?> te) {
            valueChanged(te, (HasValue<V>) te.getEventData());
        }

        public String getName() {
            return this.name;
        }
        
        public OnValueChangedEventHandler<V> setName(String name) {
            this.name = name;
            return this;
        }
        
        
        abstract public void valueChanged(TerminalEvent<?> te, HasValue<V> source);

    }

    public static abstract class TimerControlledOnValueChangedEventHandler<V> extends OnValueChangedEventHandler<V> {

        private TimedRequestedController timer;
        private V lastValue;
        private V currentValue;

        public TimerControlledOnValueChangedEventHandler(HasValue<V> targetSource) {
            super(targetSource);
            makeTimer();
        }

        private void makeTimer() {
            timer = new TimedRequestedController((te) -> {
                // When the timed request fires, finally we fire off the 
                // original value changed event
                valueChanged(te, targetSource);
            }) {

                @Override
                protected boolean isRequestDifferentFromPrevious() {
                    if (lastValue == null && currentValue == null) {
                        return false;
                    }
                    if (lastValue != null && lastValue.equals(currentValue)) {
                        return false;
                    }
                    if (currentValue != null && currentValue.equals(lastValue)) {
                        return false;
                    }
                    lastValue = currentValue;
                    return true;
                }
            };
        }
        
        public TimerControlledOnValueChangedEventHandler<V> setTimerDelayMillis(int timerDelayMillis) {
            timer.setTimerDelayMillis(timerDelayMillis);
            if (timer.getMaxStarvationTime() < timerDelayMillis) {
                setMaxStarvationTime(timerDelayMillis * 3);
            }
            return this;
        }

        public TimerControlledOnValueChangedEventHandler<V>  setMaxStarvationTime(int maxStarvationTime) {
            timer.setMaxStarvationTime(maxStarvationTime);
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handle(TerminalEvent<?> te) {
            this.lastValue = this.currentValue;
            this.currentValue = ((HasValue<V>) te.getEventData()).getValue();
            timer.onRequest(te.getEventBus());
            // valueChanged(te, (HasValue<V>) te.getEventData());
        }

    }
    
}
