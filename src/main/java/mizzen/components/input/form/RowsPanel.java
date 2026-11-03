package mizzen.components.input.form;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.LabelFor;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.focus.ToFocusable;

/**
 * Rows of label and input in a VerticalPanel, the labels padded to the longest so the inputs line up
 * whatever order the rows come in - and again when a label changes. Notes go where the inputs start.
 */
class RowsPanel {

    static final int GAP = 1;                            // between a label and its input

    final VerticalPanel panel;
    private final List<TextComponent> labels = new ArrayList<TextComponent>();
    private final List<String> texts = new ArrayList<String>();
    private int width;

    RowsPanel(VerticalPanel panel) {
        this.panel = panel;
    }

    /** a label lined up with the others */
    TextComponent label(Color color, String text) {
        TextComponent label = new TextComponent(color, text);
        labels.add(label);
        texts.add(text);
        widen();
        return label;
    }

    /** label | input - with the error line under the input - and its handle. A click on the label focuses the input (a view's label: nothing to focus). */
    FormRow place(Color labelColor, String field, String labelText, Component<?> shown, Object input, TextComponent error) {
        TextComponent label = label(labelColor, labelText);
        VerticalPanel inputAndError = filled(new VerticalPanel(labelColor), labelColor);
        inputAndError.add(shown);
        inputAndError.add(error);
        Component<?> labelShown = ToFocusable.UTIL.toFocusable(input) == null ? label : new LabelFor(label).focuses(input);
        HorizontalPanel row = filled(new HorizontalPanel(labelColor, GAP), labelColor).add(labelShown).add(inputAndError);
        panel.add(row);
        FormRow handle = new FormRow(field, labelText, input, label, error);
        handle.component = row;
        handle.labels = this;
        return handle;
    }

    /** a line starting where the inputs start - a hint under an input */
    void note(Color color, String text) {
        panel.add(filled(new HorizontalPanel(color, GAP), color).add(label(color, "")).add(Form.text(color, Theme::dimColor, text)));
    }

    // what no child covers - under a label beside a two-line input, after a short error - in the row's colour, not
    // the terminal's: a form on a coloured background (a dialog) shows no dark patches. On the default background
    // it looks as before.
    private static <P extends Composite> P filled(P panel, Color color) {
        panel.setFillOnClear(true);
        panel.setEmptyColor(color);
        return panel;
    }

    void add(Component<?> component) {
        panel.add(component);
    }

    void relabel(TextComponent label, String text) {
        texts.set(labels.indexOf(label), text);
        width = 0;
        widen();
    }

    // every label as wide as the longest
    private void widen() {
        for (String text : texts) {
            width = Math.max(width, text.length());
        }
        for (int i = 0; i < labels.size(); i++) {
            labels.get(i).setTextAndResize(padded(texts.get(i), width));
        }
    }

    private static String padded(String text, int width) {
        StringBuilder padded = new StringBuilder(text);
        while (padded.length() < width) {
            padded.append(' ');
        }
        return padded.toString();
    }
}
