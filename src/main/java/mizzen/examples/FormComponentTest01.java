package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.form.Form;
import mizzen.controlcodes.color.Colors;

/**
 * A person's form: names that are required and at least two letters, an age, a check box - each rule's
 * message under its row; Save checks them all. Click a label to go to its input. (FormTry shows every
 * kind of row.)
 * <pre>
 *     ./runDBG.sh FormComponentTest01
 * </pre>
 */
public class FormComponentTest01 {

    /** public fields: what a form binds, by name */
    public static class Person {
        public String firstName;
        public String lastName;
        public String email;
        public Integer age;
        public String phoneNumber;
        public Boolean employed;

        Person(String firstName, String lastName, String email, Integer age, String phoneNumber, Boolean employed) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.age = age;
            this.phoneNumber = phoneNumber;
            this.employed = employed;
        }

        @Override
        public String toString() {
            return firstName + " " + lastName + ", " + age + (Boolean.TRUE.equals(employed) ? ", employed" : "");
        }
    }

    public static void main(String[] args) {
        new TUIApp(args)
                .setDefaultMainApplicationPanel()
                .enableComponentInspector()
                .setOnReadyStart((tApp) -> {
                    Person george = new Person("George", "Washington", "george@example.com", 57, "555-0176", false);
                    TextComponent result = new TextComponent(Colors.IGre, " ");

                    Form<Person> form = Form.of(george)
                            .inputWidth(20)
                            .text("First name", "firstName").required()
                                .validate((String name) -> name.trim().length() > 1, "at least 2 letters")
                            .text("Last name", "lastName").required()
                                .validate((String name) -> name.trim().length() > 1, "at least 2 letters")
                            .text("Email", "email")
                            .number("Age", "age").validate((Integer age) -> age != null && age >= 0, "0 or more")
                            .text("Phone", "phoneNumber")
                            .checkBox("Employed", "employed")
                            .buttons((b) -> b.submit("Save", (f) -> result.setTextAndResize(" Saved: " + george)));

                    VerticalPanel page = new VerticalPanel();
                    page.add(new BorderedComponent(form));
                    page.add(result);
                    tApp.getMainApplicationPanel().add(2, 2, page);
                })
                .start();
    }
}
