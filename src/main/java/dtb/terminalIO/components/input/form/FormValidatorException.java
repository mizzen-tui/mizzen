package dtb.terminalIO.components.input.form;

public class FormValidatorException extends RuntimeException {

    private static final long serialVersionUID = -4971124470263358232L;

    private Object formData;

    public FormValidatorException() {
    }

    public FormValidatorException(String message) {
        super(message);
    }

    public FormValidatorException(String message, Object details) {
        super(message);
        this.formData = details;
    }
    
    // returns null if not found
    public static FormValidatorException findEmbeddedValidatorException(Exception e1) {
        if (e1 == null) {
            return null;
        }
        Throwable e = e1;
        int counter = 0;
        do {
            counter++;
            if (e instanceof FormValidatorException) {
                return (FormValidatorException) e;
            }
            e = e.getCause();
            counter++;

        } while (e != null && counter < 10);

        if (e instanceof FormValidatorException) {
            return (FormValidatorException) e;
        }

        return null;
    }

    public static String findValidatorExceptionMessage(Exception e1) {
        FormValidatorException e = findEmbeddedValidatorException(e1);
        if (e != null) {
            return e.getMessage();
        }
        return null;
    }

    public static boolean isCausedBy(Exception e1) {
        return findEmbeddedValidatorException(e1) != null;
    }

}
