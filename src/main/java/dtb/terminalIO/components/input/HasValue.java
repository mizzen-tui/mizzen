package dtb.terminalIO.components.input;

import java.util.function.Predicate;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
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
 * <p/>
 * 
 * Components can implement HasValue.. ex  ListBox, TextInput, CheckBox
 * 
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

        private AbstractTimedRequestedController timer;
        private V lastValue;
        private V currentValue;

        public TimerControlledOnValueChangedEventHandler(HasValue<V> targetSource) {
            super(targetSource);
            makeTimer();
        }

        private void makeTimer() {
            timer = new AbstractTimedRequestedController((te) -> {
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

        /**
         * A utility method that builds and returns an OnValueChangedEventHandler object from a HasValue (container .. ex. a TextField)
         * and an EventHandler.
         * 
         * <p/>
         * 
         * Basic idea is we want to run/execute some action (EventHandler) when the internal value of a component (that implements HasValue) has changed.
         * 
         * <p/>
         * 
         * This utility method wraps the EventHandler (consumeChange) to look like HasValue.OnValueChangedEventHandler such that the EventHandler 
         * will get invoked whenever a change to the component (hasValueContainer) internal value has changed.
         * 
         * @param hasValueContainer
         * @param consumeChange
         * @return
         */
        public static <T> HasValue.OnValueChangedEventHandler<T> wrapHandlerIntoOnValueChangedHandler(HasValue<T> hasValueContainer, 
                                                                                                      EventHandler consumeChange) {
            
            if (consumeChange == null) {
                return null;
            }

            // build new handler
            HasValue.OnValueChangedEventHandler<T> newOnValueChangedHandler = new HasValue.OnValueChangedEventHandler<T>(hasValueContainer) {
                @Override
                public void valueChanged(TerminalEvent<?> te, HasValue<T> valueContainer) {
                    consumeChange.handle(te);
                }
            };
            
            HasName.UTIL.decorateWithParentsNamePrefix(newOnValueChangedHandler, hasValueContainer);
            
            return newOnValueChangedHandler;
        }

        /***
         * A convenience 'utility' method used by many event aware {@link Component}s that want to attach onChange(..) handlers to their 
         * internal event handler container.
         * 
         * <p/>
         * 
         * Ex:  myButton.onChange((te) -> .. );
         * 
         * <p/>
         * 
         * will call this method to bind the above handler to the button's event handler container.
         * 
         * 
         * @param hasValueContainer           the container containing the value.. ie a button or text field
         * @param previousOnChangeHandler     the previous handler that needs to be removed
         * @param eventHandlerContainer       the components event handler container where the handler gets added
         * @param consumeChange               the handler
         * @return
         */
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
            
            HasValue.OnValueChangedEventHandler<T> newOnValueChangedHandler = wrapHandlerIntoOnValueChangedHandler(hasValueContainer, consumeChange);
            
            if (newOnValueChangedHandler == null) {
                return null;
            }
            

            // attach new
            
            eventHandlerContainer.appendEventHandler(newOnValueChangedHandler);
            return newOnValueChangedHandler;
        }
    }
    
}
