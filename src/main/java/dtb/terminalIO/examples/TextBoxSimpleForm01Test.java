package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.LabeledInputComponent;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.input.form.NamedComponentsFormBinder;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.util.Logging;

public class TextBoxSimpleForm01Test {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp) -> {

               Address address = new Address("123 Main St", "SmallVille", "Ontario");

               Composite testForm = new VerticalPanel().setName("Address");

               testForm.add(new LabeledInputComponent<String>("Street   : ",new TextInput(Colors.IYel, 15).setName("street")));

               testForm.add(new LabeledInputComponent<String>("City     : ", new TextInput(Colors.IYel, 15).setName("city")));

               testForm.add(new LabeledInputComponent<String>("Province :", DropdownListBox.fromStrings(new String[]{"Ontario", 
                                                                                               "Quebec",
                                                                                               "Sask",
                                                                                               "Alberta",
                                                                                               "British Columbia"})
                                                                       .setName("province").setColor(Colors.IYel)));


               Button saveButton = new Button("Save");

               NamedComponentsFormBinder<Address> form = NamedComponentsFormBinder.bindNamed(testForm, address );

               saveButton.onChange((te)-> {
                   form.validate();
                   Logging.info("SAVE : " + address.toString());
               });

               testForm.add(saveButton);

               tApp.getMainApplicationPanel().add(2,2, testForm);

           }).start();
    }

    static class Address {
        public String street;
        public String city;
        public String province;
        public Address(String street, String city, String province) {
            super();
            this.street = street;
            this.city = city;
            this.province = province;
        }

        @Override
        public String toString() {
            return "Address [street=" + street + ", city=" + city + ", province=" + province + "]";
        }
    }

}
