package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import mizzen.components.Component;
import mizzen.components.ComponentFinder;
import mizzen.components.ComponentWithFocus;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FormBinder.FormBinderField;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.keyboard.KeyEvent;

/**
 * A form for an object: rows of labelled inputs, each bound to one of its fields by name - what is
 * typed goes into the object, checked as it comes. A component: add it to a panel like any other;
 * TAB goes through its inputs.
 * <pre>
 *     Form&lt;Login&gt; form = Form.of(login)
 *             .heading("Sign in")
 *             .text("User", "user").required()
 *             .password("Password", "password").required()
 *             .buttons(b -&gt; b.submit("Sign in", f -&gt; signIn(login))
 *                            .button("Cancel", f -&gt; close()));
 *     panel.add(form);
 * </pre>
 * Nothing it builds is out of reach: form.row(field) is a row's handle (its input, label, error
 * line), form.binder() the FormBinder underneath. The rows: FormLayout. Colours and the input width
 * apply to the rows added after they are set.
 */
public class Form<T> extends ComponentWithFocus<VerticalPanel, Form<T>> implements FormLayout<T, Form<T>> {

    final T data;
    private final FormBinder<T> binder;
    private final RowsPanel rows;
    private final Map<String, FormRow> byField = new LinkedHashMap<String, FormRow>();
    private final List<FormRow> all = new ArrayList<FormRow>();
    private FormRow last;
    private final List<Consumer<T>> views = new ArrayList<Consumer<T>>();   // view rows: brought up to date after each change
    private final ChainedEventHandler formEvents = ChainedEventHandler.useChainOfResponsibilityStrategy(this);   // field / form changed

    Color labelColor = Colors.BWhi;
    Color inputColor = Colors.IYel;
    Color noteColor = Colors.IBla;
    Color headingColor = Colors.BYel;
    Color errorColor = Colors.BIRed;
    int inputWidth = 30;
    char inputEmptyChar = '_';                           // shows how far a text input goes while it is empty

    public static <T> Form<T> of(T data) {
        return new Form<T>(data);
    }

    private Form(T data) {
        super(Colors.BWhi, new VerticalPanel());
        this.data = data;
        this.rows = new RowsPanel(getPanel());
        this.binder = FormBinder.UTIL.get(data, formEvents, null);   // no bus: taken from the first event
        // the binder's "field changed" / "form changed" reach the form through the focus tree, as
        // events from its inputs
        ConditionalEventHandler events = formEvents.asConditionalEventHandler();
        focus().handleEvents(new ConditionalEventHandler.Impl(
                (te) -> !(te instanceof KeyEvent) && events.canHandle(te) != null,
                (te) -> events.handle(te)));
        binder.onChange((te) -> refresh());
        binder.errorsTo((field, message) -> {             // errors under their rows (the app may add other displays)
            FormRow row = byField.get(field);
            if (row != null && message != null) {
                row.showError(message);
            } else if (row != null) {
                row.clearError();
            }
        });
    }

    // ---- looks: for the rows added after

    public Form<T> labelColor(Color color) {
        labelColor = color;
        return this;
    }

    public Form<T> inputColor(Color color) {
        inputColor = color;
        return this;
    }

    public Form<T> noteColor(Color color) {
        noteColor = color;
        return this;
    }

    public Form<T> headingColor(Color color) {
        headingColor = color;
        return this;
    }

    public Form<T> errorColor(Color color) {
        errorColor = color;
        return this;
    }

    /** how wide text inputs are */
    public Form<T> inputWidth(int width) {
        inputWidth = width;
        return this;
    }

    /** what fills the empty part of a text input, showing its extent ('_'; ' ' for nothing) */
    public Form<T> inputEmptyChar(char empty) {
        inputEmptyChar = empty;
        return this;
    }

    TextInput textInput() {
        TextInput input = new TextInput(inputColor, inputWidth);
        input.setEmptyChar(inputEmptyChar);
        return input;
    }

    // ---- what it built

    /** the row bound to field; fails at once if there is none */
    public FormRow row(String field) {
        FormRow row = byField.get(field);
        if (row == null) {
            throw new IllegalArgumentException("no row for \"" + field + "\" - the rows: " + byField.keySet());
        }
        return row;
    }

    public FormBinder<T> binder() {
        return binder;
    }

    public T data() {
        return data;
    }

    /**
     * Every field checked, as it is now: each error shows under its field, each good value is in the
     * object. True when all are good - what a submit button asks first.
     */
    public boolean validateAll() {
        return binder.validateAll();
    }

    // ---- FormLayout: the form's own rows

    @Override
    public Form<T> form() {
        return this;
    }

    @Override
    public Form<T> self() {
        return this;
    }

    @Override
    public FormRow placeRow(String field, String labelText, Component<?> shown, Object input, TextComponent error) {
        return rows.place(labelColor, field, labelText, shown, input, error);
    }

    @Override
    public Form<T> note(String text) {
        rows.note(noteColor, text);
        return this;
    }

    /** any component - its inputs named like fields are bound too (the same traversal as for a row) */
    @Override
    public Form<T> add(Component<?> component) {
        rows.add(component);
        bindNamed(component);
        return this;
    }

    /** binds what the component holds that is named like a field - the binder's traversal, as for a layout made by hand */
    void bindNamed(Component<?> component) {
        if (component instanceof ComponentFinder) {
            binder.bindAll((ComponentFinder) component);
        }
    }

    // ---- for the layouts

    /**
     * A row, placed by its layout: its input named after field - the row is then bound by the binder's
     * traversal, the same as a layout made by hand; nothing here binds it itself.
     */
    FormRow addRow(FormLayout<T, ?> layout, String field, String label, Component<?> shown, Object input) {
        if (field != null && byField.containsKey(field)) {
            throw new IllegalArgumentException("\"" + field + "\" has a row already");
        }
        if (field != null) {
            ((HasName<?>) input).setName(field);
        }
        TextComponent error = new TextComponent(errorColor, " ");
        FormRow row = layout.placeRow(field, label, shown, input, error);
        error.hide();
        all.add(row);
        last = row;
        if (field != null) {
            byField.put(field, row);
            bindNamed(row.component);
            if (binder.getFormBinderField(field) == null) {
                List<String> fields = new ArrayList<String>();
                for (java.lang.reflect.Field f : data.getClass().getFields()) {
                    fields.add(f.getName());
                }
                throw new IllegalArgumentException("\"" + field + "\": " + data.getClass().getSimpleName()
                        + " has no public field of that name to bind (it has " + fields + ")");
            }
        }
        return row;
    }

    /** a row showing the object: not bound, brought up to date now and after each change */
    void addView(FormLayout<T, ?> layout, String label, Component<?> shown, Consumer<T> update) {
        addRow(layout, null, label, shown, shown);
        views.add(update);
        update.accept(data);
    }

    FormRow lastRow() {
        if (last == null) {
            throw new IllegalStateException("required() / validate(..) go after a row");
        }
        return last;
    }

    void require(FormRow row) {
        binder.<Object>getFormBinderField(row.field).withValidator(
                (Object value) -> value != null && !(value instanceof String && ((String) value).trim().isEmpty()),
                row.labelText.trim() + " is required");
    }

    <V> void validate(FormRow row, Predicate<V> test, String message) {
        binder.<V>getFormBinderField(row.field).withValidator(test, message);
    }

    /** the options a choice wasn't given: an enum field's values; none - fails at once, saying what to do */
    List<Object> optionsOf(String field) {
        Class<?> type;
        try {
            type = data.getClass().getField(field).getType();
        } catch (NoSuchFieldException e) {
            type = null;
        }
        if (type != null && type.isEnum()) {
            return Arrays.<Object>asList(type.getEnumConstants());
        }
        throw new IllegalArgumentException("no options for \"" + field + "\": give a list, a supplier or a function"
                + " of the object - or make the field an enum");
    }

    /** after a change: labels and options that come from the object asked again, views brought up to date */
    void refresh() {
        refreshRows();
        for (Consumer<T> view : views) {
            view.accept(data);
        }
    }

    private void refreshRows() {
        for (FormRow row : all) {
            if (row.labelFrom != null) {
                String label = row.labelFrom.apply(data);
                if (!label.equals(row.labelText)) {
                    row.label(label);
                }
            }
            if (row.optionsFrom != null) {
                List<?> options = row.optionsFrom.apply(data);
                if (!options.equals(row.shownOptions)) {
                    row.options(options);
                    FormBinderField<Object, T> field = binder.getFormBinderField(row.field);
                    Object value = field.getFormValue();
                    if (value != null && !options.contains(value)) {     // no longer one of them: cleared
                        field.setFormValue(null);
                        field.setComponentValue(null);
                    }
                }
            }
        }
    }
}
