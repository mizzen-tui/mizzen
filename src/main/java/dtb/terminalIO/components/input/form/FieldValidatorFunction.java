package dtb.terminalIO.components.input.form;

import dtb.terminalIO.events.EventBus;

public interface FieldValidatorFunction<FT, T> {
    FormValidatorException validate(EventBus eventBus, FT field, T formData);
}
