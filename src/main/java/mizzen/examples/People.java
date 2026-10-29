package mizzen.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.table.ObjectTableModel;

/**
 * People for the table examples (Table01Test, TableResizeTest): ten with short columns, or a hundred
 * with every column and a bio long enough to wrap. Both are tables of Person - so one table can show
 * either.
 */
public final class People {

    private People() {
    }

    /** one person; public fields, so a table column (or a form) reads them by name */
    public static class Person {
        public final int id;
        public final String firstName;
        public final String lastName;
        public final int age;
        public final String occupation;
        public final String city;
        public final String country;
        public final String email;
        public final String phone;
        public final String bio;

        Person(int id, String firstName, String lastName, int age, String occupation, String city, String country,
               String email, String phone, String bio) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
            this.occupation = occupation;
            this.city = city;
            this.country = country;
            this.email = email;
            this.phone = phone;
            this.bio = bio;
        }

        @Override
        public String toString() {
            return firstName + " " + lastName;
        }
    }

    // the ten, one to a line - id, name, age, occupation, what they're like
    private static Person person(int id, String firstName, String lastName, int age, String occupation, String bio) {
        String email = (firstName + "." + lastName).toLowerCase() + "@example.com";
        return new Person(id, firstName, lastName, age, occupation, "Toronto", "Canada", email, "416-555-01" + (10 + id), bio);
    }

    public static final List<Person> TEN = Arrays.asList(
            person( 1, "Alice",   "Moreau",   30, "Engineer",     "Enjoys hiking and outdoor activities."),
            person( 2, "Bob",     "Okafor",   25, "Designer",     "Loves painting and digital art."),
            person( 3, "Charlie", "Nguyen",   35, "Teacher",      "Passionate about education and learning."),
            person( 4, "Diana",   "Lindqvist",28, "Doctor",       "Works in a busy hospital."),
            person( 5, "Eve",     "Rossi",    40, "Chef",         "Specializes in Italian cuisine."),
            person( 6, "Frank",   "Haddad",   22, "Student",      "Studying computer science."),
            person( 7, "Grace",   "Kowalski", 33, "Writer",       "Writes novels and short stories."),
            person( 8, "Hank",    "Tanaka",   29, "Musician",     "Plays the guitar and piano."),
            person( 9, "Ivy",     "Mensah",   31, "Photographer", "Enjoys capturing nature and wildlife."),
            person(10, "Jack",    "Silva",    27, "Architect",    "Designs modern buildings and structures."));

    private static final String[] FIRST_NAMES = { "Amara", "Ben", "Chloe", "Dev", "Elena", "Femi", "Gus", "Hana", "Ilya", "Jun", "Kofi", "Lena" };
    private static final String[] LAST_NAMES  = { "Abara", "Brennan", "Castillo", "Dubois", "Eriksen", "Fischer", "Gupta", "Horvat", "Ito" };
    private static final String[] OCCUPATIONS = { "Nurse", "Pilot", "Baker", "Welder", "Librarian", "Analyst", "Carpenter" };
    private static final String[][] PLACES    = { { "Halifax", "Canada" }, { "Lyon", "France" }, { "Osaka", "Japan" },
                                                  { "Accra", "Ghana" }, { "Porto", "Portugal" }, { "Denver", "USA" } };

    /** n people, made up from the lists above - each with a bio long enough to wrap */
    public static List<Person> many(int n) {
        List<Person> people = new ArrayList<Person>();
        for (int i = 1; i <= n; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length], last = LAST_NAMES[i % LAST_NAMES.length];
            String[] place = PLACES[i % PLACES.length];
            String occupation = OCCUPATIONS[i % OCCUPATIONS.length];
            people.add(new Person(i, first, last, 20 + i % 30, occupation, place[0], place[1],
                    (first + "." + last + i).toLowerCase() + "@example.com", "555-01" + String.format("%02d", i % 100),
                    first + " is a " + occupation.toLowerCase() + " in " + place[0] + ", " + place[1]
                            + ". This bio runs long on purpose, so a narrow column has to wrap it over several lines."));
        }
        return people;
    }

    /** the ten, a few columns */
    public static ObjectTableModel<Person> fewColumns(List<Person> people) {
        return new ObjectTableModel<Person>()
                .column("ID", (p) -> p.id)
                .column("Name", (p) -> p.firstName)
                .column("Age", (p) -> p.age)
                .column("Occupation", (p) -> p.occupation)
                .column("Description", (p) -> p.bio)
                .setRows(people);
    }

    /** every column - in a narrow table, the bio is dropped first, then the email */
    public static ObjectTableModel<Person> allColumns(List<Person> people) {
        return new ObjectTableModel<Person>()
                .column("ID", (p) -> p.id)
                .column("FirstName", (p) -> p.firstName)
                .column("LastName", (p) -> p.lastName)
                .column("Age", (p) -> p.age)
                .column("Occupation", (p) -> p.occupation)
                .column("City", (p) -> p.city)
                .column("Country", (p) -> p.country)
                .column("Email", (p) -> p.email).droppable(2)
                .column("Phone", (p) -> p.phone)
                .column("Bio", (p) -> p.bio).droppable(1)
                .setRows(people);
    }
}
