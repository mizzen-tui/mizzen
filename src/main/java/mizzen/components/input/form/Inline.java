package mizzen.components.input.form;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.LabelFor;
import mizzen.events.focus.ToFocusable;

/**
 * Rows side by side, on one line of a form - the same rows as the form's (FormLayout), each label
 * just before its input; a click on the label focuses the input, as in the form's own rows.
 * <pre>
 *     .inline(r -&gt; r.text("City", "city").text("Postal", "postal"))      City [_____] Postal [___]
 * </pre>
 */
public class Inline<T> implements FormLayout<T, Inline<T>> {

    private static final int APART = 2;                  // between one label + input and the next

    private final Form<T> form;
    final HorizontalPanel panel;

    Inline(Form<T> form) {
        this.form = form;
        this.panel = new HorizontalPanel(form.labelColor, APART);
    }

    @Override
    public Form<T> form() {
        return form;
    }

    @Override
    public Inline<T> self() {
        return this;
    }

    @Override
    public FormRow placeRow(String field, String labelText, Component<?> shown, Object input, TextComponent error) {
        TextComponent label = new TextComponent(form.labelColor, labelText);
        VerticalPanel inputAndError = new VerticalPanel();
        inputAndError.add(shown);
        inputAndError.add(error);
        Component<?> labelShown = ToFocusable.UTIL.toFocusable(input) == null ? label : new LabelFor(label).focuses(input);
        HorizontalPanel pair = new HorizontalPanel(form.labelColor, RowsPanel.GAP).add(labelShown).add(inputAndError);
        panel.add(pair);
        FormRow row = new FormRow(field, labelText, input, label, error);
        row.component = pair;
        return row;
    }

    @Override
    public Inline<T> note(String text) {
        panel.add(new TextComponent(form.noteColor, text));
        return this;
    }

    @Override
    public Inline<T> add(Component<?> component) {
        panel.add(component);
        return this;
    }
}
