package mizzen.components.input.form;

import mizzen.events.EventBus;

public interface FieldValidatorFunction<FT, T> {
    FormValidatorException validate(EventBus eventBus, FT field, T formData);
}
