package mizzen.components.theme;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import mizzen.app.TUIApp;
import mizzen.components.AbstractComponentWrappper;
import mizzen.components.HasTitleAndBorder;
import mizzen.components.TextComponent;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.CheckBoxInput;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.FormInput;
import mizzen.components.input.HasValue;
import mizzen.components.input.OnChangeListeners;
import mizzen.controlcodes.color.Color;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.TerminalEvent;

public class ThemePickerComponent extends AbstractComponentWrappper<HorizontalPanel>
        implements FormInput<Theme>, HasTitleAndBorder<ThemePickerComponent> {

    private final Map<Theme, Theme> themeToItsBase = new IdentityHashMap<Theme, Theme>();

    private DropdownListBox<Theme> themesDDLB;
    CheckBoxInput isRoundedCHBX;
    TextComponent checkBoxLabel;
    private String pickerLabelText = "";
    TextComponent pickerLabelComponent;
    private boolean showRoundedCheckBox = true;

    Theme value = null;

    public ThemePickerComponent(List<Theme> themes) {
        super(null, new HorizontalPanel());
        getWrappedComponent().setName("horizontal-Panel-theme-picker");
        getWrappedComponent().setHorizontalSpacer(0);

        // components
        pickerLabelComponent = new TextComponent("");
        themesDDLB = new DropdownListBox<Theme>(choices(themes));
        isRoundedCHBX = new CheckBoxInput();
        checkBoxLabel = new TextComponent("Rounded");

        // layout
        getWrappedComponent().add(this.pickerLabelComponent);
        getWrappedComponent().add(this.themesDDLB);
        getWrappedComponent().add(this.isRoundedCHBX);
        getWrappedComponent().add(this.checkBoxLabel);

        // selection
        themesDDLB.onChange((te) -> {
            this.setValue(selectedTheme());
            fireChanged(te);
        });

        isRoundedCHBX.onChange((te) -> {
            this.setValue(selectedTheme());
            fireChanged(te);
        });
    }

    public ThemePickerComponent() {
        this(applicationThemes());
        this.setValue(Theme.getDefault());

    }

    private Theme selectedTheme() {
        Theme base = themesDDLB.getValue();
        Theme theme = isRoundedCHBX.getValue()
                ? base.rounded()
                : base.traditional();
        // record the mapping
        themeToItsBase.put(theme, base);
        return theme;
    }

    public ThemePickerComponent setLabel(String text) {
        pickerLabelText = text == null ? "" : text;
        updateLabel();
        return this;
    }

    private void updateLabel() {
        boolean bordered = getWrappedComponent().hasBorder();

        if (bordered) {
            getWrappedComponent().setTitle(pickerLabelText);
        }

        pickerLabelComponent.setText(pickerLabelText);
        if (bordered || pickerLabelText.isEmpty()) {
           pickerLabelComponent.hide();
        } else {
          pickerLabelComponent.show();
        }
    }

    public ThemePickerComponent setShowRoundedOption(boolean show) {
        showRoundedCheckBox = show;

        if (show) {
            isRoundedCHBX.show();
            checkBoxLabel.show();
        } else {
            isRoundedCHBX.hide();
            checkBoxLabel.hide();
        }
        return this;
    }

    private final OnChangeListeners<Theme> changeListeners = new OnChangeListeners<Theme>(this);

    private void fireChanged(TerminalEvent<?> te) {
        HasValue.ValueChangedEvent<Theme> changedEvent = new HasValue.ValueChangedEvent<Theme>(
                ThemePickerComponent.this);
        changedEvent.setEventBus(te.getEventBus());
        this.changeListeners.fire(changedEvent);
    }

    @Override
    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    @Override
    public void setValue(Theme theme) {
        Theme baseTheme = themeToItsBase.get(theme);
        if (baseTheme == null) {
            // External theme: treat its traditional form as a new palette choice,
            // unless it is already an exact dropdown entry.
            baseTheme = theme.traditional();
            // Add base to dropdown choices here if necessary.
        }
        this.value = theme;
        boolean isRounded = (theme != null & theme.isRounded());
        this.isRoundedCHBX.setValue(isRounded);
        this.themesDDLB.setValue(baseTheme);
        this.markAsDirty();
    }

    @Override
    public Theme getValue() {
        return this.value;
    }

    public ThemePickerComponent setBorder(boolean bordered) {
        getWrappedComponent().setBorder(bordered);
        return this;
    }

    public ThemePickerComponent setTitle(String title) {
        getWrappedComponent().setTitle(title);
        return this;
    }

    public ThemePickerComponent setBorderColor(Color color) {
        getWrappedComponent().setBorderColor(color);
        return this;
    }


    @Override
    public void hilite() {
        getWrappedComponent().hilite();
    }

    @Override
    public void unHilite() {
        getWrappedComponent().unHilite();
    }

    @Override
    public boolean hasBorder() {
        return getWrappedComponent().hasBorder();
    }

    @Override
    public ThemePickerComponent setTitle(Color titleColors, String title) {
        getWrappedComponent().setTitle(titleColors, title);
        return this;
    }

    @Override
    public String getTitle() {
        return getWrappedComponent().getTitle();
    }


    @SuppressWarnings("unchecked")
    private static List<Theme> applicationThemes() {
        List<Theme> themes = new ArrayList<Theme>(Theme.all());
        if (!themes.contains(Theme.getDefault())) {
            themes.add(0, Theme.getDefault()); // a theme of the app's own, set as the default
        }
        return themes;
    }

    @SuppressWarnings("unchecked")
    private static DisplayableValue<Theme>[] choices(List<Theme> themes) {
        DisplayableValue<Theme>[] choices = new DisplayableValue[themes.size()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = new DisplayableValue.SimpleDisplayValue<Theme>(themes.get(i).name(), themes.get(i));
        }
        return choices;
    }

    /** return a ThemeComponent where on theme picked, do picked action */
    public static ThemePickerComponent of(Consumer<Theme> picked) {
        ThemePickerComponent picker = new ThemePickerComponent();
        EventHandler handler = (te) -> {
            picked.accept(picker.getValue());
        };
        picker.onChange(handler);
        return picker;
    }

    /** return a ThemeComponent where on theme picked, set application's theme */
    public static ThemePickerComponent of(TUIApp app) {
        return of(app::setTheme);
    }

}
