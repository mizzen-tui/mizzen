package mizzen.components.input.form;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;

/**
 * Rows in a titled box, inside a form - the same rows as the form's (FormLayout), lined up among
 * themselves.
 * <pre>
 *     .section("Ship to", s -&gt; s.text("Street", "street").text("City", "city"))
 * </pre>
 */
public class Section<T> implements FormLayout<T, Section<T>> {

    private final Form<T> form;
    final VerticalPanel panel;
    private final RowsPanel rows;

    Section(Form<T> form, String title) {
        this.form = form;
        this.panel = new VerticalPanel(form.labelColor);
        panel.setBorder(true);
        panel.setTitle(" " + title + " ");
        this.rows = new RowsPanel(panel);
    }

    @Override
    public Form<T> form() {
        return form;
    }

    @Override
    public Section<T> self() {
        return this;
    }

    @Override
    public FormRow placeRow(String field, String labelText, Component<?> shown, Object input, TextComponent error) {
        return rows.place(form.labelColor, field, labelText, shown, input, error);
    }

    @Override
    public Section<T> note(String text) {
        rows.note(form.noteColor, text);
        return this;
    }

    @Override
    public Section<T> add(Component<?> component) {
        rows.add(component);
        return this;
    }
}
