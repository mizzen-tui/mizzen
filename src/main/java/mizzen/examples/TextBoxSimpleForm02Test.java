package mizzen.examples;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DropdownListBox;
import mizzen.components.input.LabeledInputComponent;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FormBinder;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.Logging;
import mizzen.util.StringUtils;

public class TextBoxSimpleForm02Test {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .enableComponentInspector()
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

            testForm.add(new LabeledInputComponent<String>("Street   :",new TextInput(Colors.IYel, 15).setName("street")).setRequired(true));

            testForm.add(new LabeledInputComponent<String>("City     :", new TextInput(Colors.IYel, 15).setName("city")).setRequired(true).showErrorIndicator(true).setName("cityField"));

            testForm.add(new LabeledInputComponent<String>("Province :", DropdownListBox.fromStrings(new String[]{"Ontario", 
                                                                                                                  "Quebec",
                                                                                                                  "Sask",
                                                                                                                  "Alberta",
                                                                                                                  "British Columbia"})
                                                                   .setColor(Colors.IYel).setName("province")).setRequired(true).showErrorIndicator(true).setName("provinceField"));

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
                      .withValidator((evtBus, field) -> {
                            String city = (String) field;
                            if (validCity.test(city)) {
                                testForm.findComponentByName("cityField",LabeledInputComponent.class).setErrorMessage("");
                                return true;
                            }
                            evtBus.raiseEvent(RequestFocusEvent.fromSource(testForm.findComponentByName("city", Component.class))); 
                            testForm.findComponentByName("cityField",LabeledInputComponent.class).setErrorMessage("Bad city");
                            return false;
                        }, "city must be one of "+ validCities.keySet())
                      .onChange((te)->{
                            Logging.info("city changed");
                      });

            formBinder.getFormBinderField("province")
                    .withValidator((evtBus, field) -> {
                        String province = (String) field;
                        String city = formBinder.getFormData().city;
                        if (validProvince.test(city, province)) {
                            testForm.findComponentByName("street").setEnabled(true, evtBus);
                            testForm.findComponentByName("provinceField",LabeledInputComponent.class).setErrorMessage("");
                            return true;
                        }
                        evtBus.raiseEvent(RequestFocusEvent.fromSource(testForm.findComponentByName("province", Component.class)));
                        testForm.findComponentByName("provinceField",LabeledInputComponent.class).setErrorMessage("province  not compatible with city");
                        testForm.findComponentByName("street").setEnabled(false, evtBus);
                        return false;
                    }, "");

            formBinder.withValidatorHandler((evtBus, formError, formData) -> {
                if (formError != null) {
                    if (!StringUtils.isEmptyStr(formError.getMessage())) {
                        errorText.setTextAndResize(formError.getMessage());
                        testForm.show(errorContainer);
                    }
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
                te.getEventBus().raiseEvent( RequestFocusEvent.fromSource(testForm.findComponentByNameOrFail("street", Component.class)) );
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
