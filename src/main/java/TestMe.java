
public class TestMe {

    static void doAction(Runnable myRunner) {
        myRunner.run();
        System.out.println("-->"+ myRunner.getClass().getName());
        // what would happen for the print of class name.. there is a class right?
    }

    public static void main(String[] args) {

        String someName = "GeorgeOfTheJungle"; // closure var

        doAction(() -> System.out.println("Hello Lambda world! says " + someName));

        doAction(() -> System.out.println("Good bye Lambda world! says " + someName));

        doAction(new Runnable() {
            public void run() {
                System.out.println("Hello anonymous inner class " + someName);
            }
        });

    }
}
