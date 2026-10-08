package dtb.terminalIO.components.input.form;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//import java.util.function.BiPredicate;
//import java.util.function.Consumer;
//import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import dtb.terminalIO.components.ComponentFinder;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.SettableHasValue;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.util.ReflectionUtils;

public interface FormBinder<T> extends HasValue<T>{

    T getFormData();

    /**
     * Shows another object in the form: its values go onto the components, and from now on edits go
     * into it - e.g. the sale picked in a table. Given the same object, the form shows its values
     * again (after the object was changed in code, or put back from a copy). Any field errors shown
     * are cleared.
     */
    void setFormData(T formData);

    void onChange(EventHandler consumeChange);

    default T getValue() {
        return getFormData();
    }

    public void bindAll(ComponentFinder componentFinder);

    public <FT> FormBinderField<FT, T> bindField(String name, GetterSetter<FT> getterSetterComponent, GetterSetter<FT> getterSetterFormField);

    public <FT> FormBinderField<FT, T> bindField(String propertyName, GetterSetter<FT> getterSetterComponent);

    default public <FT> FormBinderField<FT, T> bindField(String propertyName, SettableHasValue<FT> settableValue) {
        GetterSetter<FT> getterSetterComponent = new GetterSetter<FT>(settableValue);
        FormBinderField<FT, T> formBinderField = bindField(propertyName, getterSetterComponent);
        UTIL.bindToOnChange(formBinderField, propertyName, settableValue);
        return formBinderField;
    }

    @SuppressWarnings("unchecked")
    default public <FT> FormBinderField<FT, T> bindField(ComponentFinder componentFinder, String name) {
        SettableHasValue<FT> component = componentFinder.findComponentByName(name, SettableHasValue.class);
        if (component == null) {
            throw new IllegalArgumentException("Could not bind to named component "+name+" of type SettableHasValue");
        }
        return this.bindField(name, component);
    }

    public <FT> FormBinderField<FT, T> getFormBinderField(String name);

    FormErrorHandler<T> getErrorHandler();

    public boolean isInError();

    public void save();

    public <FT> FormBinder<T> withValidatorHandler(FormErrorHandler<T> errorHandler);

    EventBus getEventBus();

    EventHandlerContainer getEventHandlerContainerForForm();

//    @FunctionalInterface
//    public static interface FieldValidatorFunction<FT, T> {
//        FormValidatorException validate(FT formBinder, T formData);
//    }

    public static interface FormBinderField<FT, T> extends HasName<FormBinderField<FT, T>>, HasValue<FT> {

        public FormBinder<T> getFormBinder();

        FormErrorHandler<T> getErrorHandler();

        public FormBinderField<FT, T> withValidator( FieldValidatorFunction<FT, T> formFieldValidatorFunction);

        default public FormBinderField<FT, T> withValidator(BiPredicate<EventBus, FT> formFieldValidatorPredicate, String errorMsg) {
            return withValidator((evtBus, fieldValue, formData) -> formFieldValidatorPredicate.test(evtBus, (FT) fieldValue) ? null: new FormValidatorException(errorMsg, fieldValue));
        }

        default public FormBinderField<FT, T> withValidator(Predicate<FT> formFieldValidatorPredicate, String errorMsg) {
            return withValidator((eventBus,fieldValue, formData) -> formFieldValidatorPredicate.test((FT) fieldValue) ? null: new FormValidatorException(errorMsg, fieldValue));
        }

        public FormBinderField<FT, T> withValidatorHandler(FormErrorHandler<T> errorHandler);

//        public FormBinderField<FT, T> withValidatorHandler(Consumer<FormValidatorException> errorHandler);

        public FT getFormValue();

        default public FT getValue() {
            return getFormValue();
        }

        public void setComponentValue(FT value);

        public void assertValid();

        public boolean isInError();

        public void setFormValue(FT value);

        public void copyComponentValueToFormField();

        public FormBinderField<FT, T> onChange(EventHandler consumeChange);

    }


    public static class DefaultFormBinder<T> implements FormBinder<T> {

        T formData;
        List<FormBinderField<?,T>> formFields;
        FormErrorHandler<T> errorHandler = null; // TODO: default to catastrophic fail in order to force developer to set this value to something more reasonable.

        EventBus eventBus;
        EventHandlerContainer formPanelComponent;

        public DefaultFormBinder(T formData, EventHandlerContainer formPanelComponent, EventBus eventBus) {
            this.formData = formData;
            this.formFields = new ArrayList<FormBinderField<?,T>>();
            this.eventBus = eventBus;
            this.formPanelComponent = formPanelComponent;
        }

        @Override
        public <FT> FormBinderField<FT, T> bindField(String name, 
                                                     GetterSetter<FT> getterSetterComponent,
                                                     GetterSetter<FT> getterSetterFormField) {

            FormBinderField<FT, T> formBinderField = new DefaultFormBinderField<FT, T>(name, 
                                                                                       getterSetterComponent, 
                                                                                       getterSetterFormField, 
                                                                                       this);

            this.formFields.add( formBinderField  );
            return formBinderField;
        }

        @Override
        public <FT> FormBinderField<FT, T> bindField(String propertyName, GetterSetter<FT> getterSetterComponent) {
            return bindField(propertyName, getterSetterComponent, formDataGetterSetterFromPropertyName(propertyName));
        }

        public <FT> FormBinderField<FT, T> getFormBinderField(String name) {
            @SuppressWarnings("unchecked")
            FormBinderField<FT, T> found = (FormBinderField<FT, T>) this.formFields.stream().filter( f-> name.equals(f.getName())).findFirst().orElse(null);
            return found;
        }

        @Override
        public <FT> FormBinder<T> withValidatorHandler(FormErrorHandler<T> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        protected <FT> GetterSetter<FT>  formDataGetterSetterFromPropertyName(String propertyName) {
            @SuppressWarnings("unchecked")
            Supplier<FT> getter = () -> {
                try {
                    return (FT) ReflectionUtils.getValueFromTarget(this.formData, propertyName);
                } catch (Exception e) {
                    // TODO: Not sure if this is correct behaviour
                    return null;
                }
            };
            Consumer<FT> setter = (value) -> {
                try {
                    ReflectionUtils.setValueOnTarget(this.formData, propertyName, value);
                } catch (Exception e) {
                    // TODO maybe route to error handler?
                    //e.printStackTrace();
                }
            };
            return new GetterSetter<>(getter, setter);
        }

        protected List<String> getFieldNames() {
            return ReflectionUtils.getFieldNames(this.formData);
            
        }

        protected void setValuesOntoComponents() {
            for (String fieldName : getFieldNames()) {
                FormBinderField<Object, T> formBinderField = getFormBinderField(fieldName);
                if (formBinderField != null) {
                    setValueOntoComponent(fieldName);
                }
            }
        }

        // copy data from formData field onto corresponding form
        protected void setValueOntoComponent(String fieldName) {
            FormBinderField<Object, T> formBinderField = getFormBinderField(fieldName);
            if (formBinderField == null) {
                throw new NullPointerException("setValueOntoComponent('"+fieldName+"').  No form component for field " + fieldName);
            }
            Object formDataValue = formBinderField.getFormValue();
            formBinderField.setComponentValue(formDataValue);
        }

        protected void setValueOntoFormData(String fieldName) {
            FormBinderField<?, T> formBinderField = getFormBinderField(fieldName);
            if (formBinderField == null) {
                throw new NullPointerException("setValueOntoFormData('"+fieldName+"').  No form component for field " + fieldName);
            }
            formBinderField.assertValid(); // runtime exception (ideally FormValidatorException) thrown on validate failure
            formBinderField.copyComponentValueToFormField();
        }

        protected void setValuesOntoFormData() {
            for (String fieldName : getFieldNames()) {
                FormBinderField<?, T> formBinderField = getFormBinderField(fieldName);
                if (formBinderField != null) {
                    setValueOntoFormData(fieldName);
                }
            }
        }

        @Override
        public void bindAll(ComponentFinder componentFinder) {
            for (String fieldName : getFieldNames()) {
                SettableHasValue<?> component = componentFinder.findComponentByName(fieldName, SettableHasValue.class);
                if (component != null) {
                    this.bindField(fieldName, component);
                }
            }
            setValuesOntoComponents();
        }

        @Override
        public T getFormData() {
            return this.formData;
        }

        @Override
        public void setFormData(T formData) {
            this.formData = formData;
            setValuesOntoComponents();
            boolean hadErrors = false;
            for (FormBinderField<?, T> field : formFields) {
                if (field instanceof DefaultFormBinderField) {
                    hadErrors |= field.isInError();
                    ((DefaultFormBinderField<?, T>) field).inError = false;
                }
            }
            if (hadErrors && this.errorHandler != null) {
                this.errorHandler.handleError(this.eventBus, null, formData);   // null: no error now
            }
        }

        @Override
        public void save() {
            try {
                // copies values from components onto formData
                setValuesOntoFormData();
            } catch (FormValidatorException formError) {
                if (this.errorHandler != null) {
                    this.errorHandler.handleError(this.eventBus, formError, this.getFormData());
                } else {
                    throw formError;
                }
                return;
            }
            // no errors so clear errors
            if (this.errorHandler != null) {
                this.errorHandler.handleError(this.eventBus,  null, this.getFormData());
            }
        }

        @Override
        public FormErrorHandler<T> getErrorHandler() {
            return this.errorHandler;
        }

        @Override
        public boolean isInError() {
            for (FormBinderField<?, T> formField : formFields) {
                if (formField.isInError()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @Override
        public EventHandlerContainer getEventHandlerContainerForForm() {
            return formPanelComponent;
        }

        HasValue.OnValueChangedEventHandler<T> onChangeHandler;

        @Override
        public void onChange(EventHandler consumeChange) {

            onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, this.formPanelComponent, consumeChange);

        }

    }


    public static class DefaultFormBinderField<FT, T> implements FormBinderField<FT, T> {

        private FormBinder<T> formBinder;
        private String name;
        private GetterSetter<FT> getterSetterComponent;
        private GetterSetter<FT> getterSetterFormField;
        private FieldValidatorFunction<FT, T> formFieldValidatorFunction;
        private FormErrorHandler<T> errorHandler;

        public DefaultFormBinderField(String name,
                                      GetterSetter<FT> getterSetterComponent,
                                      GetterSetter<FT> getterSetterFormField, 
                                      FormBinder<T> formBinder) {
            this.name = name;
            this.getterSetterComponent = getterSetterComponent;
            this.getterSetterFormField = getterSetterFormField;
            this.formBinder = formBinder;
        }

        @Override
        public FormBinder<T> getFormBinder() {
            return this.formBinder;
        }

        @Override
        public FormBinderField<FT, T> withValidator(FieldValidatorFunction<FT, T> formFieldValidatorFunction) {
            if (formFieldValidatorFunction == null) {
                this.formFieldValidatorFunction = null;
                return this;
            }
            if (this.formFieldValidatorFunction == null) {
                this.formFieldValidatorFunction = formFieldValidatorFunction;
                return this;
            }
            FieldValidatorFunction<FT, T> headFieldValidatorFunction = this.formFieldValidatorFunction;
            this.formFieldValidatorFunction = (evtBus, field, formData)-> {
                FormValidatorException firstValidateResult = headFieldValidatorFunction.validate(evtBus, field, formData);
                if (firstValidateResult != null) {
                    return firstValidateResult;
                }
                return formFieldValidatorFunction.validate(evtBus, field, formData);
            };
            return this;
        }

//        @Override
//        public FormBinderField<FT, T> withValidator(Function<FT, FormValidatorException> formFieldValidatorFunction) {
//            FieldValidatorFunction<FT, T>  formFieldValidator = (field, formData) -> {
//                return formFieldValidatorFunction.apply(field);
//            };
//            return withValidator(formFieldValidator);
//        }

        @Override
        public FormBinderField<FT, T> withValidatorHandler(FormErrorHandler<T> errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

//        @Override
//        public FormBinderField<FT, T> withValidatorHandler(Consumer<FormValidatorException> errorHandler) {
//            this.errorHandler = (fieldException, form) -> {
//                errorHandler.accept(fieldException);
//            };
//            return this;
//        }

        @Override
        public FormErrorHandler<T> getErrorHandler() {
            return this.errorHandler;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public FormBinderField<FT, T> setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public FT getFormValue() {
            return this.getterSetterFormField.get();
        }

        @Override
        public void setFormValue(FT value) {
            this.getterSetterFormField.set(value);
        }

        @Override
        public void setComponentValue(FT value) {
            this.getterSetterComponent.set(value);
        }

        @Override
        public void copyComponentValueToFormField() {
            FT value = this.getterSetterComponent.get();
            setFormValue(value);
        }

        @Override
        public void assertValid() {
            if (this.formFieldValidatorFunction == null) {
                return;
            }
            FT value = this.getterSetterComponent.get();
            FormValidatorException exception = formFieldValidatorFunction.validate(this.getFormBinder().getEventBus(), value, this.formBinder.getFormData());
            if (exception != null) {
                inError = true;
                throw exception;
            }
            inError = false;
        }

        boolean inError = false;

        @Override
        public boolean isInError() {
            return inError;
        }

        HasValue.OnValueChangedEventHandler<FT> onChangeHandler;

        @Override
        public FormBinderField<FT, T> onChange(EventHandler consumeChange) {

            onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, getFormBinder().getEventHandlerContainerForForm(), consumeChange);

            return this;
        }
    }


    public static class UTIL {

        public static <T> FormBinder<T> get(T formData, EventHandlerContainer eveHandlerContainer, EventBus eventBus) {
            return new DefaultFormBinder<T>(formData, eveHandlerContainer, eventBus);
        }

        public static <T> FormBinder<T> get(T formData, EventBus eventBus) {
            return new DefaultFormBinder<T>(formData, eventBus, eventBus);
        }
        
        static <FT, T> void handleError(FormBinder<T> formBinder, FormBinderField<FT, T> formBinderField, FormValidatorException formError) {
            if (formBinderField != null && formBinderField.getErrorHandler() != null) {
                formBinderField.getErrorHandler().handleError(formBinder.getEventBus(), formError, formBinder.getFormData());
                return;
            }
            if (formBinder.getErrorHandler() != null) {
                formBinder.getErrorHandler().handleError(formBinder.getEventBus(), formError, formBinder.getFormData());
            } else {
                throw formError;
            }
        }

        static <FT, T> void bindToOnChange(FormBinderField<FT, T> formBinderField, String propertyName, SettableHasValue<FT> settableValue) {

            // Check if the widget has an onChange method
            try {
                Method onChangeMethod = settableValue.getClass().getMethod("onChange", EventHandler.class);

                EventHandler widget_onChangeHandler = (te) -> {
                    try {
                        boolean prevFormInError = formBinderField.isInError();
                        FT prevValue = formBinderField.getFormValue();
                        formBinderField.assertValid();
                        formBinderField.copyComponentValueToFormField();
                        boolean postFormInError = formBinderField.isInError();
                        if (prevFormInError && !postFormInError) {
                            handleError(formBinderField.getFormBinder(), formBinderField, null);
                        }
                        FT postValue = formBinderField.getFormValue();
                        if (postValue != prevValue) {
                            EventBus eventBus = te.getEventBus();
                            if ((postValue != null && !postValue.equals(prevValue)) || 
                                (prevValue != null && !prevValue.equals(postValue))) {
                                // signal field changed
                                te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(formBinderField)
                                        .then(()->{
                                            // then signal form changed
                                            eventBus.raiseEvent(new HasValue.ValueChangedEvent<>(formBinderField.getFormBinder()));
                                        }));
                            }
                        }

                    } catch (FormValidatorException formError) {
                        handleError(formBinderField.getFormBinder(), formBinderField, formError);
                    }
                };

                onChangeMethod.invoke(settableValue, widget_onChangeHandler);

            } catch (NoSuchMethodException | SecurityException e) {
                // TODO Auto-generated catch block
                return ;
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
    }

}
