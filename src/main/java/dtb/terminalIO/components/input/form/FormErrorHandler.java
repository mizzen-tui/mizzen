package dtb.terminalIO.components.input.form;

import dtb.terminalIO.events.EventBus;

@FunctionalInterface
public interface FormErrorHandler<T> {
    void handleError(EventBus eventBus, FormValidatorException exception, T FormData);
}
