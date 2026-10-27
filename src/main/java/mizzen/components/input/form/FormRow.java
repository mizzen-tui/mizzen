package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;

/**
 * A handle to one row a Form built: its label, its input, the line under the input where its error
 * shows, and the whole row - nothing the builder made is out of reach.
 * <pre>
 *     TextInput qty = form.row("qty").input(TextInput.class);
 *     form.row("region").label("State").options(STATES);
 *     form.row("notes").hide();
 * </pre>
 */
public class FormRow {

    final String field;                                  // the object's field it is bound to (null: none)
    final TextComponent label;
    final Object input;
    final TextComponent error;
    Component<?> component;                              // the whole row, as its layout placed it
    RowsPanel labels;                                    // who lines its label up (null: inline)

    String labelText;
    Function<Object, String> labelFrom;                  // the label from the object, asked after each change
    Function<Object, List<?>> optionsFrom;               // the options from the object, likewise
    List<?> shownOptions;

    FormRow(String field, String labelText, Object input, TextComponent label, TextComponent error) {
        this.field = field;
        this.labelText = labelText;
        this.input = input;
        this.label = label;
        this.error = error;
    }

    /** the input, as the class you expect - a wrong guess fails at once, saying what it is */
    public <C> C input(Class<C> type) {
        if (!type.isInstance(input)) {
            throw new IllegalStateException("row \"" + field + "\": its input is a " + input.getClass().getSimpleName()
                    + ", not a " + type.getSimpleName());
        }
        return type.cast(input);
    }

    public TextComponent label() {
        return label;
    }

    /** the line under the input where its error shows (hidden while there is none) */
    public TextComponent error() {
        return error;
    }

    /** the whole row: label, input and error line */
    public Component<?> component() {
        return component;
    }

    /** a new label; the labels around it line up again */
    public FormRow label(String text) {
        labelText = text;
        if (labels != null) {
            labels.relabel(label, text);
        } else {
            label.setTextAndResize(text);
        }
        return this;
    }

    /** new options for a choice; fails at once for a row that has none */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FormRow options(List<?> options) {
        if (!(input instanceof DropdownListBox)) {
            throw new IllegalStateException("row \"" + field + "\" (a " + input.getClass().getSimpleName() + ") has no options");
        }
        if (options.equals(shownOptions)) {
            return this;
        }
        shownOptions = new ArrayList<Object>(options);
        ((DropdownListBox) input).setItems(displayable(options));
        return this;
    }

    public FormRow show() {
        component.show();
        return this;
    }

    public FormRow hide() {
        component.hide();
        return this;
    }

    /** its error line is showing */
    public boolean hasError() {
        return erring;
    }

    private boolean erring;

    void showError(String message) {
        error.setTextAndResize(message);
        error.show();
        erring = true;
    }

    void clearError() {
        error.hide();
        erring = false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static List<DisplayableValue<Object>> displayable(List<?> options) {
        List<DisplayableValue<Object>> items = new ArrayList<DisplayableValue<Object>>();
        for (Object option : options) {
            items.add(new DisplayableValue.SimpleDisplayValue(String.valueOf(option), option));
        }
        return items;
    }
}
