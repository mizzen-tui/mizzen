package mizzen.components.input.form;

import mizzen.events.EventBus;

@FunctionalInterface
public interface FormErrorHandler<T> {
    void handleError(EventBus eventBus, FormValidatorException exception, T FormData);
}
