package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.composite.Composite;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.LabeledInputComponent;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FormBinder;
import mizzen.controlcodes.color.Colors;
import mizzen.util.Logging;

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

               // the inputs are named after Address's fields: the binder finds and binds them
               FormBinder<Address> form = FormBinder.UTIL.get(address, tApp.getEventBus());
               form.bindAll(testForm);

               saveButton.onChange((te)-> {
                   if (form.validateAll()) {
                       Logging.info("SAVE : " + address.toString());
                   }
               });

               testForm.add(saveButton);

               tApp.getMainApplicationPanel().add(2,2, testForm);

           }).start();
    }

    public static class Address {
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
