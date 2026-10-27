package mizzen.components.input.form;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
//import java.util.function.BiConsumer;
//import java.util.function.BiFunction;
//import java.util.function.BiPredicate;
//import java.util.function.Consumer;
//import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mizzen.components.Component;
import mizzen.components.ComponentFinder;
import mizzen.components.HasName;
import mizzen.components.input.FormInput;
import mizzen.components.input.HasValue;
import mizzen.components.input.OnChangeListeners;
import mizzen.components.input.SettableHasValue;
import mizzen.components.input.ToFormInput;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.Registration;
import mizzen.util.ReflectionUtils;

public interface FormBinder<T> extends HasValue<T>{

    T getFormData();

    /**
     * Shows another object in the form: its values go onto the components, and from now on edits go
     * into it - e.g. the sale picked in a table. Given the same object, the form shows its values
     * again (after the object was changed in code, or put back from a copy). Any field errors shown
     * are cleared.
     */
    void setFormData(T formData);

    Registration onChange(EventHandler consumeChange);

    default T getValue() {
        return getFormData();
    }

    public void bindAll(ComponentFinder componentFinder);

    /**
     * Binds the field to a component through the component's FormInput - given, registered, or its own
     * (a FormInput, or a ToFormInput): FormInputs gives the order. The field's value shows in the
     * component at once. Fails at once when the component has no FormInput.
     * <pre>
     *     binder.bind("country", countryDropdown);
     *     binder.bind("colour", colourPicker);         // a ToFormInput: the binder calls asFormInput()
     * </pre>
     */
    <FT> FormBinderField<FT, T> bind(String field, Object component);

    /** as bind(field, component), the component's FormInput made by factory - for this field only */
    default <C, FT> FormBinderField<FT, T> bind(String field, C component, Function<? super C, ? extends FormInput<FT>> factory) {
        FormBinderField<FT, T> bound = bindInput(field, factory.apply(component));
        bound.setComponentValue(bound.getFormValue());
        return bound;
    }

    /** for this form only: components of this type bind through the FormInput the factory makes (FormInputs) */
    <C> FormBinder<T> register(Class<C> type, Function<? super C, ? extends FormInput<?>> factory);

    /** the field bound to a FormInput: values both ways; each change is checked, then copied into the object */
    default <FT> FormBinderField<FT, T> bindInput(String field, FormInput<FT> input) {
        FormBinderField<FT, T> formBinderField = bindField(field, new GetterSetter<FT>(input));
        listening(field, input.onChange(UTIL.changeHandler(formBinderField)));
        return formBinderField;
    }

    /**
     * The binder's own listener for a field, kept so it can be taken out: a field bound again replaces
     * it (never a second one), unbind() takes them all out. The app's listeners are never touched.
     */
    void listening(String field, Registration registration);

    /** every listener this binder put on its inputs taken out; the inputs and the object stay as they are */
    void unbind();

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

    /**
     * Every field checked against its rules, as its input is now: good values go into the object,
     * each error goes to the error displays (errorsTo). True when all are good - e.g. before saving.
     */
    boolean validateAll();

    /** errors also show here - any number of displays; with one, an error is shown, not thrown */
    FormBinder<T> errorsTo(FieldErrors display);

    /** the displays given (errorsTo) */
    List<FieldErrors> errorDisplays();

    public void save();

    public <FT> FormBinder<T> withValidatorHandler(FormErrorHandler<T> errorHandler);

    EventBus getEventBus();

    /**
     * The bus, if the binder was made without one (FormBinder.UTIL.get(data, container, null)): taken from
     * the first event it handles - a form needn't be given one. A bus it already has stays.
     */
    void useEventBusIfNone(EventBus eventBus);

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

        public Registration onChange(EventHandler consumeChange);

    }


    public static class DefaultFormBinder<T> implements FormBinder<T> {

        T formData;
        List<FormBinderField<?,T>> formFields;
        FormErrorHandler<T> errorHandler = null; // TODO: default to catastrophic fail in order to force developer to set this value to something more reasonable.
        private final FormInputs formInputs = FormInputs.forBinder();   // this form's registry, then the global one

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

            FormBinderField<FT, T> bound = getFormBinderField(name);
            if (bound instanceof DefaultFormBinderField) {           // bound before: the same field, now on this component -
                ((DefaultFormBinderField<FT, T>) bound).getterSetterComponent = getterSetterComponent;   // its rules and handlers kept
                return bound;
            }

            FormBinderField<FT, T> formBinderField = new DefaultFormBinderField<FT, T>(name, 
                                                                                       getterSetterComponent, 
                                                                                       getterSetterFormField, 
                                                                                       this);

            this.formFields.add( formBinderField  );
            return formBinderField;
        }

        private final Map<String, Registration> listening = new HashMap<String, Registration>();   // the binder's own, per field
        private final List<FieldErrors> errorDisplays = new ArrayList<FieldErrors>();

        @Override
        public FormBinder<T> errorsTo(FieldErrors display) {
            errorDisplays.add(display);
            return this;
        }

        @Override
        public List<FieldErrors> errorDisplays() {
            return errorDisplays;
        }

        @Override
        public boolean validateAll() {
            boolean good = true;
            for (FormBinderField<?, T> field : formFields) {
                try {
                    field.assertValid();
                    field.copyComponentValueToFormField();
                    UTIL.report(this, field, null);
                } catch (FormValidatorException problem) {
                    UTIL.report(this, field, problem);
                    good = false;
                }
            }
            return good;
        }

        @Override
        public void listening(String field, Registration registration) {
            Registration before = listening.put(field, registration);
            if (before != null) {
                before.unregister();
            }
        }

        @Override
        public void unbind() {
            for (Registration registration : listening.values()) {
                registration.unregister();
            }
            listening.clear();
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

        /**
         * Each of the object's fields bound to the component of the same name, if there is one: an
         * input, a ToFormInput, or a component with a registered FormInput. Fields with
         * no such component are left out.
         */
        @Override
        public void bindAll(ComponentFinder componentFinder) {
            for (String fieldName : getFieldNames()) {
                Object component = componentFinder.findComponentByName(fieldName, SettableHasValue.class);
                if (component == null) {
                    component = componentFinder.findComponentByName(fieldName, ToFormInput.class);
                }
                if (component == null) {
                    Component<?> named = componentFinder.findComponentByName(fieldName);
                    component = named != null && formInputs.canResolve(named) ? named : null;
                }
                if (component != null) {
                    this.bind(fieldName, component);
                }
            }
            setValuesOntoComponents();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <FT> FormBinderField<FT, T> bind(String field, Object component) {
            FormBinderField<FT, T> bound;
            if (!formInputs.canResolve(component) && component instanceof SettableHasValue) {
                bound = bindField(field, (SettableHasValue<FT>) component);   // an input with no onChange: bound as it always was
            } else {
                bound = bindInput(field, (FormInput<FT>) formInputs.resolve(field, component));
            }
            setValueOntoComponent(field);
            return bound;
        }

        @Override
        public <C> FormBinder<T> register(Class<C> type, Function<? super C, ? extends FormInput<?>> factory) {
            formInputs.add(type, factory);
            return this;
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
        public void useEventBusIfNone(EventBus eventBus) {
            if (this.eventBus == null) {
                this.eventBus = eventBus;
            }
        }

        @Override
        public EventHandlerContainer getEventHandlerContainerForForm() {
            return formPanelComponent;
        }

        private final OnChangeListeners<T> changeListeners = new OnChangeListeners<T>(this);

        /** handler runs after any field's value went into the object - alongside any others */
        @Override
        public Registration onChange(EventHandler consumeChange) {
            return changeListeners.add(this.formPanelComponent, consumeChange);
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

        private final OnChangeListeners<FT> changeListeners = new OnChangeListeners<FT>(this);

        /** handler runs after this field's value went into the object - alongside any others */
        @Override
        public Registration onChange(EventHandler consumeChange) {
            return changeListeners.add(getFormBinder().getEventHandlerContainerForForm(), consumeChange);
        }
    }


    public static class UTIL {

        public static <T> FormBinder<T> get(T formData, EventHandlerContainer eveHandlerContainer, EventBus eventBus) {
            return new DefaultFormBinder<T>(formData, eveHandlerContainer, eventBus);
        }

        public static <T> FormBinder<T> get(T formData, EventBus eventBus) {
            return new DefaultFormBinder<T>(formData, eventBus, eventBus);
        }
        
        // the error (null: none now) to the binder's displays, by field name
        static <FT, T> void report(FormBinder<T> formBinder, FormBinderField<FT, T> formBinderField, FormValidatorException formError) {
            for (FieldErrors display : formBinder.errorDisplays()) {
                display.show(formBinderField.getName(), formError == null ? null : formError.getMessage());
            }
        }

        static <FT, T> void handleError(FormBinder<T> formBinder, FormBinderField<FT, T> formBinderField, FormValidatorException formError) {
            if (formBinderField != null) {
                report(formBinder, formBinderField, formError);
            }
            if (formBinderField != null && formBinderField.getErrorHandler() != null) {
                formBinderField.getErrorHandler().handleError(formBinder.getEventBus(), formError, formBinder.getFormData());
                return;
            }
            if (formBinder.getErrorHandler() != null) {
                formBinder.getErrorHandler().handleError(formBinder.getEventBus(), formError, formBinder.getFormData());
            } else if (formBinder.errorDisplays().isEmpty() && formError != null) {
                throw formError;                         // nobody shows it: not lost silently
            }
        }

        /**
         * What a field does when its input changes: check the input's value (the field's rules); if good,
         * copy it into the object and say the field and the form changed; if not, hand the error to the
         * error handler (the object keeps its last good value).
         */
        static <FT, T> EventHandler changeHandler(FormBinderField<FT, T> formBinderField) {
            return (te) -> {
                formBinderField.getFormBinder().useEventBusIfNone(te.getEventBus());
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
        }

        static <FT, T> void bindToOnChange(FormBinderField<FT, T> formBinderField, String propertyName, SettableHasValue<FT> settableValue) {

            // Check if the widget has an onChange method
            try {
                Method onChangeMethod = settableValue.getClass().getMethod("onChange", EventHandler.class);

                EventHandler widget_onChangeHandler = changeHandler(formBinderField);
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
