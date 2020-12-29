package dtb.terminalIO.components.input;

import java.util.function.Predicate;

import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * A container that has a value. It just allows for an extra level of
 * indirection.
 *
 * @param <V>
 */
public interface HasValue<V> {  //TODO:  Make a SettableValue that extends this.. ( HasValue components can implement this, then later we can 'bindable' forms - bind based on 'getName')

    V getValue();

    public static class ValueChangedEvent<V> extends TerminalEvent<HasValue<V>> {

        private static final long serialVersionUID = 4544682502113778402L;

        public ValueChangedEvent(HasValue<V> eventData) {
            super(eventData, TYPE.VALUE_CHANGED, eventData);
        }
    }

    public abstract class OnValueChangedEventHandler<V> implements ConditionalEventHandler {

        static Predicate<TerminalEvent<?>> valueChangedPredicate = new IS_Event_TYPE(TerminalEvent.TYPE.VALUE_CHANGED);

        HasValue<V> targetSource;

        Object[] valuesOfInterest;

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

        abstract public void valueChanged(TerminalEvent<?> te, HasValue<V> source);

    }

    public static abstract class TimerControlledOnValueChangedEventHandler<V> extends OnValueChangedEventHandler<V> {

        private AbstractTimedRequestedController timer;
        private V lastValue;
        private V currentValue;

        public TimerControlledOnValueChangedEventHandler(HasValue<V> targetSource) {
            super(targetSource);
            makeTimer();
        }

        private void makeTimer() {
            timer = new AbstractTimedRequestedController(() -> {
                valueChanged(null, targetSource);
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

        @SuppressWarnings("unchecked")
        @Override
        public void handle(TerminalEvent<?> te) {
            this.lastValue = this.currentValue;
            this.currentValue = ((HasValue<V>) te.getEventData()).getValue();
            timer.onRequest(te.getEventBus());
            valueChanged(te, (HasValue<V>) te.getEventData());
        }

    }

    public static class UTIL {

        public static <T> HasValue.OnValueChangedEventHandler<T> wrapHandlerInOnValueChangedHandler(HasValue<T> hasValueContainer, 
                                                                                                    EventHandler consumeChange) {
            
            if (consumeChange == null) {
                return null;
            }

            // build new handler
            HasValue.OnValueChangedEventHandler<T> newOnChange = new HasValue.OnValueChangedEventHandler<T>(hasValueContainer) {
                @Override
                public void valueChanged(TerminalEvent<?> te, HasValue<T> valueContainer) {
                    consumeChange.handle(te);
                }
            };
            
            return newOnChange;
        }

        
        public static <T> HasValue.OnValueChangedEventHandler<T> bindConsumerAsOnValueChangedHandler(HasValue<T> hasValueContainer, 
                                                                              HasValue.OnValueChangedEventHandler<T> previousOnChangeHandler,
                                                                              EventHandlerContainer eventHandlerContainer,
                                                                              EventHandler consumeChange) {

            // remove old
            if (previousOnChangeHandler != null) {
                eventHandlerContainer.removeEventHandler(previousOnChangeHandler);
            }

            if (consumeChange == null) {
                return null;
            }
            
            
            // build new handler
            HasValue.OnValueChangedEventHandler<T> newOnChange = new HasValue.OnValueChangedEventHandler<T>(hasValueContainer) {
                @Override
                public void valueChanged(TerminalEvent<?> te, HasValue<T> valueContainer) {
                    consumeChange.handle(te);
                }
            };

            // attach new
            eventHandlerContainer.appendEventHandler(newOnChange);
            return newOnChange;
        }
    }
    
}
