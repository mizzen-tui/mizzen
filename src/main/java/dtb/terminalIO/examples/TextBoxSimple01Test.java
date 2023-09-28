package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.util.Logging;

public class TextBoxSimple01Test {

    
    public static void main(String[] args) throws IOException {
        
        Logging.setEnabled(true);
        
        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp)->{
               
               Composite testForm = new VerticalPanel();
               TextInput strnum = new TextInput(Colors.IYel, 10).setName("street");
               TextInput city = new TextInput(Colors.IYel, 10).setName("city");
               TextInput province = new TextInput(Colors.IYel, 10).setName("province");

               testForm.add(new HorizontalPanel().addText("Street   : ").add(strnum));
               testForm.add(new HorizontalPanel().addText("City     : ").add(city));
               testForm.add(new HorizontalPanel().addText("Province : ").add(province));
               
               tApp.getMainApplicationPanel().add(2,2, testForm);
               
               
               
           }).start();
        
        
    }



}
