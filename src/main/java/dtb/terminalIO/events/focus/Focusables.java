package dtb.terminalIO.events.focus;

public interface Focusables {

    public void insertFocusable(int index, Focusable focusable);

    public void addFocusable(Focusable focusable);

    public void removeFocusable(Focusable focusable);

}
