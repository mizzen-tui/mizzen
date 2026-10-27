package mizzen.examples;

import java.io.IOException;
import java.util.Objects;

import mizzen.app.TUIApp;
import mizzen.components.BorderedComponent;
import mizzen.components.input.TextInput;
import mizzen.components.input.form.FormComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Align;
import mizzen.geom.Orientation;
import mizzen.util.Logging;

public class FormComponentTest01 {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args).setDefaultMainApplicationPanel().enableComponentInspector().setOnReadyStart((tApp) -> {

            Person GeorgeWashington = new Person("George", "Washington", "gwashington@arlingtoncemetary@.gov.us", 300, "1-800 call-me", null, false);

            FormComponent<Person> form =  FormComponent.build(GeorgeWashington)
                                                .enableErrorIndicatorGlobally()
                                                .setLabelPosition(Orientation.HORIZONTAL)
                                                .setLabelAlignment(Align.RIGHT)
                                                .showRequiredIndicator(true) // Show "*" for required fields
                                                .withField("firstName", new TextInput(Colors.IYel, " 12123  "))
                                                    .withLabel("First Name")
                                                        .required()
                                                        .enableErrorIndicator()
                                                        .withValidator((name)-> ((String)name).length()>1,"Name must be at least 2 characters")
                                                    .withField("lastName", new TextInput(Colors.IYel, 15))
                                                        .withLabel("Last Name")
                                                        .required()
                                                        .withValidator((evtBus, lastName)-> ((String)lastName).length()>1 , "Name must be at least 2 characters");
                                                ;

                  form.buildIfRequired();
                                                
                    /*
  * 
            
        
  * 
  * */
            tApp.getMainApplicationPanel().add(10,10, new BorderedComponent(form));
            
            
        }).start();
        
    }
// local DTO classes to test

    static class Address {

        private String street;
        private String city;
        private String province;
        private String postalCode;

        // Constructors
        public Address() {
        }

        public Address(String street, String city, String province, String postalCode) {
            this.street = street;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
        }

        // Getters and Setters
        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        // toString
        @Override
        public String toString() {
            return "Address{" + "street='" + street + '\'' + ", city='" + city + '\'' + ", province='" + province + '\''
                    + ", postalCode='" + postalCode + '\'' + '}';
        }

        // equals and hashCode
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Address address = (Address) o;
            return Objects.equals(street, address.street) && Objects.equals(city, address.city)
                    && Objects.equals(province, address.province) && Objects.equals(postalCode, address.postalCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(street, city, province, postalCode);
        }
    }

    static class Person {

        private String firstName;
        private String lastName;
        private String email;
        private int age;
        private String phoneNumber;
        private Address address;
        private boolean isEmployed;

        // Constructors
        public Person() {
        }

        public Person(String firstName, String lastName, String email, int age, String phoneNumber, Address address, boolean isEmployed) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.age = age;
            this.phoneNumber = phoneNumber;
            this.address = address;
            this.isEmployed = isEmployed;
        }

        // Getters and Setters
        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public boolean isEmployed() {
            return isEmployed;
        }

        public void setEmployed(boolean employed) {
            isEmployed = employed;
        }

        // toString
        @Override
        public String toString() {
            return "Person{" +
                    "firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", email='" + email + '\'' +
                    ", age=" + age +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    ", address=" + address +
                    ", isEmployed=" + isEmployed +
                    '}';
        }

        // equals and hashCode
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age &&
                    isEmployed == person.isEmployed &&
                    Objects.equals(firstName, person.firstName) &&
                    Objects.equals(lastName, person.lastName) &&
                    Objects.equals(email, person.email) &&
                    Objects.equals(phoneNumber, person.phoneNumber) &&
                    Objects.equals(address, person.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, lastName, email, age, phoneNumber, address, isEmployed);
        }
    }    
    
}
