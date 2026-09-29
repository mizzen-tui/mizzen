package dtb.terminalIO.components.input.form;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.LabeledInputComponent;
import dtb.terminalIO.components.input.SettableHasValue;
import dtb.terminalIO.components.input.SettableHasValueComponentMixen;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Orientation;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class FormComponent<T> extends AbstractComponentWithContainer<Composite> implements SettableHasValue<T> {

    boolean errorIndicatorGlobally = false;
    Orientation labelPositionOrientation = Orientation.HORIZONTAL;
    Align labelAlignment = Align.LEFT;
    boolean showRequiredIndicator = false;
    
    private boolean isBuilt = false;
    private boolean isDirty = false;
    private List<FormField<?>> fields = new ArrayList<>();
    private T formData;

    protected FormComponent(Color color) {
        super(color, new VerticalPanel(color));        // TODO Auto-generated constructor stub
    }

    FormField<?> currentFormField() {
        if (fields.size()<1) {
            throw new IllegalStateException("Tried to access a field before any were created");
        }
        return fields.get(fields.size()-1);
    }

    public <FT> FormComponent<T> withField(String name, SettableHasValueComponentMixen<?,FT> input) {
        if (hasFieldByName(name)) {
            throw new IllegalArgumentException("Attempt to add a duplicate field name "+name);
        }
        markFormLayoutAsDirty();
        fields.add(new FormField<FT>(name, input));
        return this;
    }

//    public FormComponent<T> updateFieldValidation(String name, Validator<?> validator) {
//        markFormLayoutAsDirty();
//        FormField field = findFieldByName(name);
//        if (field != null) {
//            field.setValidator(validator);
//        }
//        return this;
//    }

    private FormField<?> findFieldByName(String name) {
        return this.fields.stream()
                   .filter(f-> f.name.equals(name))
                   .findFirst()
                   .orElseThrow(()->new IllegalArgumentException(name + " not added to field list") ); 
    }

    private boolean hasFieldByName(String name) {
        try {
            findFieldByName(name);
            return true;
        } catch(IllegalArgumentException e) {
            return false;
        }
    }

    private void markFormLayoutAsDirty() {
        if (isBuilt) {
            isDirty = true;
        }
    }

    public void buildIfRequired() {
        if (!isBuilt || isDirty) {
            constructFormLayout();
            isBuilt = true;
            isDirty = false;
        }
    }
    
//    public void setParentContainer(ParentContainer parent) {
//        super.setParentContainer(parent);
//        if (parent != null) {
//            buildIfRequired();
//        }
//    }


    private void constructFormLayout() {
        // Rebuild the form layout
        getWrappedComponent().clear();
        for (FormField<?> field : fields) {
            getWrappedComponent().add(field.buildInputComponent());
        }
        applyLabelAlignment();
    }

    private void applyLabelAlignment() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        buildIfRequired();
        super.draw(color, segments);
    }

    @Override
    public T getValue() {
        buildIfRequired();
        return formData;
    }

    @Override
    public void setValue(T value) {
        markAsDirty();
        this.formData = value;
    }

    public static <T> FormComponent<T> build(T formData) {
        FormComponent<T> form = new FormComponent<T>(Colors.DEFAULT_COLOR);
        form.setValue(formData);
        return form;
    }

    public FormComponent<T> enableErrorIndicatorGlobally() {
        this.errorIndicatorGlobally = true;
        return this;
    }

    public FormComponent<T> setLabelPosition(Orientation orientation) {
        this.labelPositionOrientation = orientation;
        return this;
    }

    public FormComponent<T> setLabelAlignment(Align align) {
        this.labelAlignment = align;
        return this;
    }

    public FormComponent<T> showRequiredIndicator(boolean showRequiredIndicator) {
        this.showRequiredIndicator = showRequiredIndicator;
        return this;
    }

    public FormComponent<T> withLabel(String labelContents) {
        // TODO Auto-generated method stub
        return currentFormField().setLabel(labelContents);
    }

    public FormComponent<T> required() {
        return currentFormField().required();
    }

    public FormComponent<T> enableErrorIndicator() {
        return currentFormField().enableErrorIndicator();
    }

    public <FT> FormComponent<T> withValidator(Predicate<FT> fieldPredicate, String errorMessage) {
        BiPredicate<EventBus,FT> biPredicate = (evtBus, fieldValue)-> fieldPredicate.test((FT)fieldValue);
        return withValidator(biPredicate, errorMessage);
    }

    @SuppressWarnings("unchecked")
    public <FT> FormComponent<T> withValidator(BiPredicate<EventBus,FT> fieldPredicate, String errorMessage) {
        return ((FormField<FT>)currentFormField()).setValidator(fieldPredicate, errorMessage);
    }

    
    class FormField<FT> {

        String name;
        SettableHasValueComponentMixen<?, FT> inputComponent;
        boolean required;
        boolean enableErrorIndicator;
        String labelContents;
        FieldValidatorFunction<FT, T> errorHandler;

        public FormField(String name, SettableHasValueComponentMixen<?, FT> input) {
            this.name = name;
            this.inputComponent = input;
        }

//        public FormComponent<T> setValidator(Predicate<FT> fieldPredicate, String errorMessage) {
//            FieldValidatorFunction<FT, T> errorHandler = (eventBus,fieldValue, formData) -> fieldPredicate.test((FT) fieldValue) ? null: new FormValidatorException(errorMessage, fieldValue);
//            this.errorHandler = errorHandler;
//            return formComponent();
//        }

        public FormComponent<T> setValidator(BiPredicate<EventBus, FT> fieldPredicate, String errorMessage) {
            FieldValidatorFunction<FT, T> errorHandler = (evtBus, fieldValue, formData) -> fieldPredicate.test(evtBus, (FT) fieldValue) ? null: new FormValidatorException(errorMessage, fieldValue);
            this.errorHandler = errorHandler;
            return formComponent();
        }

        public FormComponent<T> setLabel(String labelContents) {
            this.labelContents = labelContents;
            return formComponent();
        }

        public FormComponent<T> required() {
            this.required = true;
            return formComponent();
        }

        public FormComponent<T> enableErrorIndicator() {
            this.enableErrorIndicator = true;
            return formComponent();
        }

        public FormComponent<T> disableErrorIndicator() {
            this.enableErrorIndicator = false;
            return formComponent();
        }
        
        public LabeledInputComponent<FT> buildInputComponent() {
            LabeledInputComponent<FT> component = new LabeledInputComponent<FT>(labelContents, inputComponent);
            if (required) {
                component.setRequired(true);
                // TODO: marker color?
            }
            if (enableErrorIndicator) {
                component.showErrorIndicator(true);
            }
            return component;
        }

        FormComponent<T> formComponent() {
            return FormComponent.this;
        }

    }
    

}
