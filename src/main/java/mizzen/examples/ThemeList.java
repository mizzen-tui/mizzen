package mizzen.examples;

import java.util.List;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.theme.Theme;

/**
 * A dropdown of the standard themes, starting on the default; picking one hands it on. Used by the examples that let
 * you switch theme while they run.
 * <pre>
 *     screen.add(0, 60, ThemeList.of(tApp));                         the app's theme
 *     ThemeList.of((theme) -&gt; host.setTheme(theme))                  anything else that takes a theme
 * </pre>
 */
public final class ThemeList {

    private ThemeList() {
    }

    /** picking a theme sets it on app */
    public static DropdownListBox<Theme> of(TUIApp app) {
        return of(app::setTheme);
    }

    /** picking a theme gives it to picked */
    public static DropdownListBox<Theme> of(Consumer<Theme> picked) {
        DropdownListBox<Theme> list = new DropdownListBox<Theme>(choices(Theme.standard()));
        list.setValue(Theme.getDefault());
        list.onChange((te) -> picked.accept(list.getValue()));
        return list;
    }

    @SuppressWarnings("unchecked")
    private static DisplayableValue<Theme>[] choices(List<Theme> themes) {
        DisplayableValue<Theme>[] choices = new DisplayableValue[themes.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = new DisplayableValue.SimpleDisplayValue<Theme>(themes.get(i).name(), themes.get(i));
        }
        return choices;
    }
}
