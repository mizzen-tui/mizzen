package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.keyboard.Keys;

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
               size.onChange((te) -> { showValues.run(); te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(values)); });
               colour.onChange((te) -> { showValues.run(); te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(values)); });
               wifi.onChange((te) -> { showValues.run(); te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(values)); });
               plain.onChange((te) -> { showValues.run(); te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(values)); });

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
