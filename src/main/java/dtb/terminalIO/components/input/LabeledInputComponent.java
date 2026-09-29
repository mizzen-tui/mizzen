package dtb.terminalIO.components.input;

import dtb.terminalIO.components.AbstractComponentWithContainer;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.focus.FocusBehaviour;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.geom.Orientation;
import dtb.terminalIO.util.StringUtils;

public class LabeledInputComponent<V> extends AbstractComponentWithContainer<Composite>
        implements SettableHasValue<V> /* , ToFocusable */ {

    SettableHasValue<V> componentWithValue;
    FocusableLabel label;
    TextComponent requiredMarker;
    TextComponent errorText;
    boolean isRequired = false;
    boolean showRequiredIndicator = false;
    boolean showErrorIndicator = false;

    public <T extends Component<?>> LabeledInputComponent(Color color, String textLabel,
            SettableHasValueComponentMixen<T, V> component, Orientation orientation) {
        super(color, Orientation.VERTICAL.equals(orientation) ? new VerticalPanel(color) : new HorizontalPanel(color));
        if (ToFocusable.UTIL.toFocusable(component) == null) {
            throw new IllegalArgumentException("LabeledInputComponent error:  component "
                    + HasName.UTIL.getName(component) + "parameter must be resolvable to a focusable");
        }
        this.label = new FocusableLabel(color, textLabel);
        label.setOnClickAction((evtBus) -> {
            evtBus.raiseEvent(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
        });
//        label.setOnFocusAction( (focus, eventBus) -> {
//            if (focus) {
//                eventBus.raiseEvent(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
//            }
//        });

        this.componentWithValue = component;

        this.requiredMarker = new TextComponent(Colors.BCya, " ");

        this.errorText = new TextComponent(Colors.BRed, "");

        if (orientation == Orientation.VERTICAL) {
            HorizontalPanel labelRow = new HorizontalPanel(color);
            labelRow.add(label);
            labelRow.add(requiredMarker);
            getWrappedComponent().add(labelRow);
            getWrappedComponent().add(component);
            getWrappedComponent().add(errorText);
        } else { // HORIZONTAL orientation
            HorizontalPanel rowPanel = new HorizontalPanel(color);

            // Add label, required marker, and input component inline
            rowPanel.add(label);
            rowPanel.add(requiredMarker);
            rowPanel.add(component);

            // Create a vertical container to ensure error text is placed below the input
            // row
            VerticalPanel containerPanel = new VerticalPanel(color);
            containerPanel.add(rowPanel);
            containerPanel.add(errorText);

            // Add the vertical container to the wrapped component
            getWrappedComponent().add(containerPanel);
        }

        this.requiredMarker.hide(); // Hidden by default
        this.errorText.hide(); // Hidden by default

    }

    public LabeledInputComponent<V> setName(String name) {
        super.setName(name);
        // NOTE: Possible for setName to be called before instance vars have been set -
        //       hence the checks
        if (label != null && !StringUtils.isEmptyStr(name) && StringUtils.isEmptyStr(label.getName())) {
            this.label.setName(name + "-Label");
        }

        if (componentWithValue != null && (componentWithValue instanceof HasName) && !StringUtils.isEmptyStr(name)) {
            HasName<?> componentHasName = (HasName<?>) componentWithValue;
            if (StringUtils.isEmptyStr(componentHasName.getName())) {
                componentHasName.setName(name + "-Value-Container");
            }
        }
        return this;
    }

    public <T extends Component<?>> LabeledInputComponent(Color color, String textLabel,
            SettableHasValueComponentMixen<T, V> component) {
        this(color, textLabel, component, Orientation.HORIZONTAL);
    }

    public <T extends Component<?>> LabeledInputComponent(String textLabel,
            SettableHasValueComponentMixen<T, V> component) {
        this(Colors.DEFAULT_COLOR, textLabel, component, Orientation.HORIZONTAL);
    }

    @Override
    public V getValue() {
        return componentWithValue.getValue();
    }

    @Override
    public void setValue(V value) {
        componentWithValue.setValue(value);
    }

    public Focusable getFocusable() {
        return ToFocusable.UTIL.toFocusable(componentWithValue);
    }

    public LabeledInputComponent<V> setErrorColor(Color color) {
        this.errorText.setColor(color);
        return this;
    }

    public LabeledInputComponent<V> setRequiredMarkerColor(Color color) {
        this.requiredMarker.setColor(color);
        requiredIndicatorOnLatch();
        return this;
    }

    public LabeledInputComponent<V> setRequired(boolean required) {
        this.isRequired = required;
        requiredIndicatorOnLatch();
        if (required) {
            this.requiredMarker.setText("*");
        } else {
            this.requiredMarker.setText(" ");
        }
        return this;
    }
    
    private void requiredIndicatorOnLatch() {
        if (showRequiredIndicator) {
            return;
        }
        showRequiredIndicator = true; // once on always on
        this.requiredMarker.show();
    }

    public LabeledInputComponent<V> showErrorIndicator(boolean show) {
        this.showErrorIndicator = show;
        return this;
    }

    public LabeledInputComponent<V> setErrorMessage(String message) {
        if (showErrorIndicator && !StringUtils.isEmptyStr(message)) {
            this.errorText.setTextAndResize(message);
            this.errorText.show();
        } else {
            this.errorText.hide();
        }
        return this;
    }

}
