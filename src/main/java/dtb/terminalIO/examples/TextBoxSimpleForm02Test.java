package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.components.input.form.FormBinder;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;

public class TextBoxSimpleForm02Test {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        
        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .setOnReadyStart((tApp) -> {

            Address address = new Address("123 Main St", "SmallVille", "Ontario");

            Map<String, String>  validCities = new HashMap<String, String>();
            validCities.put("kingston", "ontario");
            validCities.put("ottawa", "ontario");
            validCities.put("calgary", "alberta");
            validCities.put("vancouver", "british columbia");
            validCities.put("montreal", "quebec");

            Predicate<String> validCity = (city) -> {
               if (StringUtils.isEmptyStr(city)) {
                   return false;
               }
               city = city.trim().toLowerCase();
               if (validCities.containsKey(city)) {
                   return true;
               }
               return false;
            };

            BiPredicate<String, String> validProvince = (city, province) -> {
                if ( ! validCity.test(city)) {
                    return false;
                }
                if (StringUtils.isEmptyStr(province)) {
                    return false;
                }
                city = city.trim().toLowerCase();
                province = province.trim().toLowerCase();
                String citysProvice = validCities.get(city);
                if (citysProvice.equalsIgnoreCase(province)) {
                    return true;
                }
                return false;
             };

            FormBinder<Address> formBinder = FormBinder.UTIL.get(address, tApp.getEventBus());

            Composite testForm = (new VerticalPanel() {
                protected void draw(Color color, IScreenLineSegmentsBuffer segments, boolean boundsFixedFlag) {
                    super.draw(color, segments, boundsFixedFlag);
                }
            }).setName("Address");

            TextComponent errorText = new TextComponent(Colors.IRed,"");
            BorderedComponent errorContainer = new BorderedComponent(Colors.IRed, errorText);

            testForm.add(new HorizontalPanel().addText("Street   : ")
                                              .add(new TextInput(Colors.IYel, 15)
                                                      .setName("street")));

            testForm.add(new HorizontalPanel().addText("City     : ")
                                              .add(new TextInput(Colors.IYel, 15).setName("city")));

            testForm.add(new HorizontalPanel().addText("Province :")
                                              .add(DropdownListBox.fromStrings(new String[]{"Ontario", 
                                                                                            "Quebec",
                                                                                            "Sask",
                                                                                            "Alberta",
                                                                                            "British Columbia"})
                                                                   .setName("province")
                                                                   .setColor(Colors.IYel)));

            Button saveButton = new Button("Save");
            saveButton.onChange((te) -> {
                formBinder.save();
            });

            formBinder.bindAll(testForm);

            formBinder.getFormBinderField("street")
                    .withValidator((street) -> ((String) street).length() >= 3, "street must contain at least three characters")
                    .onChange((te)->{
                        Logging.info("street changed");
                    });

            formBinder.getFormBinderField("city")
            .withValidator((field) -> {
                String city = (String) field;
                if (validCity.test(city)) {
                    return true;
                }
                return false;
            }, "city must be one of "+ validCities.keySet())
            .onChange((te)->{
                Logging.info("city changed");
            });

            formBinder.getFormBinderField("province")
            .withValidator((field) -> {
                String province = (String) field;
                String city = formBinder.getFormData().city;
                if (validProvince.test(city, province)) {
                    return true;
                }
                return false;
            }, "province be related to city ");

            formBinder.withValidatorHandler((formError, formData) -> {
                if (formError != null) {
                    errorText.setTextAndResize(formError.getMessage());
                    testForm.show(errorContainer);
                    saveButton.hide();
                } else {
                    testForm.hide(errorContainer);
                    saveButton.show();
                }
            });

            testForm.add(errorContainer);
            testForm.hide(errorContainer);

            testForm.add(saveButton);

            TextComponent addressRAW = new TextComponent("                 ");

            HorizontalPanel hpMain = new HorizontalPanel().setHorizontalSpacer(2);
            hpMain.add(testForm);
            hpMain.add( new BorderedComponent(addressRAW).setTitle("FormData"));

            tApp.getMainApplicationPanel().add(2,2, hpMain);

            tApp.getEventBus().asynch((te)->{
                te.getEventBus().raiseEvent( new RequestFocusEvent( testForm.findComponentByName("street", Focusable.class)  ) );
            });

            formBinder.onChange((te)->{
                addressRAW.setTextAndResize( address.toString() );
                Logging.info("form changed");
            });

        }).start();
    }

    static class Address {
        public String street;
        public String city;
        public String province;
        public String country;
        public Address(String street, String city, String province) {
            super();
            this.street = street;
            this.city = city;
            this.province = province;
        }

        @Override
        public String toString() {
            return "Address \n[street=" + street + ",\n city=" + city + ",\n province=" + province + "\n]";
        }
    }

}
