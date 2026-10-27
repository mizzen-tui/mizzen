package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.RadioGroup;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.keyboard.Keys;

/**
 * RadioGroup in its main looks. TAB / SHIFT-TAB move between the groups (one stop each, landing on
 * the chosen option); the arrows choose within a group; a click chooses; F2 toggles "Wifi" and F5
 * cycles "Size" from anywhere. The last line shows the values as they change.
 */
public class RadioGroupSimpleTest {

    public static void main(String[] args) throws IOException {

        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp) -> {

               RadioGroup<String> size = RadioGroup.vertical("Small", "Medium", "Large").setHotKey(Keys.F5_KEYSTROKE);
               size.setValue("Medium");
               RadioGroup<String> colour = RadioGroup.horizontal("Red", "Green", "Blue").setLabel("Colour");
               RadioGroup<Boolean> wifi = RadioGroup.onOff("Wifi", true).setHotKey(Keys.F2_KEYSTROKE);
               RadioGroup<String> plain = RadioGroup.horizontal("Yes", "No").setLabel("ASCII").asciiStyle();
               TextInput name = new TextInput(Colors.IYel, 15);
               TextComponent values = new TextComponent(Colors.IGre, "");

               Runnable showValues = () -> values.setTextAndResize("size=" + size.getValue() + "  colour=" + colour.getValue()
                                                                   + "  wifi=" + wifi.getValue() + "  ascii=" + plain.getValue());
               showValues.run();
               size.onChange((te) -> showValues.run());
               colour.onChange((te) -> showValues.run());
               wifi.onChange((te) -> showValues.run());
               plain.onChange((te) -> showValues.run());

               VerticalPanel form = new VerticalPanel();
               form.add(new TextComponent(Colors.ICya, "TAB between groups, arrows choose, F2 toggles Wifi, F5 cycles Size, CTRL-C quits"));
               form.add(new TextComponent(""));
               form.add(new TextComponent("Size"));
               form.add(size);
               form.add(new TextComponent(""));
               form.add(colour);
               form.add(wifi);
               form.add(plain);
               form.add(new TextComponent(""));
               form.add(name);
               form.add(new TextComponent(""));
               form.add(values);

               tApp.getMainApplicationPanel().add(2, 2, form);

           }).start();
    }
}
