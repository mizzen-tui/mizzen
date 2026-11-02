package mizzen.examples;

import java.io.IOException;

import mizzen.app.TUIApp;
import mizzen.components.TextComponent;
import mizzen.components.chart.LineChart;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.components.modal.Dialog;
import mizzen.controlcodes.color.Colors;
import mizzen.events.keyboard.KeyStroke;
import mizzen.geom.Dimension;

/**
 * Dialogs, one per button. Each is built step by step and shown; the line at the bottom says how it
 * ended.
 * <p/>
 * A button closes its dialog once its action has run - unless the action calls showError (the dialog
 * stays open to show it), a submit finds the form invalid, or the button staysOpen(). Enter in a field
 * presses the first button; Esc presses Cancel.
 * <pre>
 *     ./runDBG.sh DialogDemo
 * </pre>
 */
public class DialogDemo {

    /** what the login form fills in */
    public static class Login {
        public String user = "";
        public String password = "";
    }

    private final TUIApp app;
    private final TextComponent outcome = new TextComponent(Colors.IGre, "Nothing yet");

    public static void main(String[] args) throws IOException {
        new DialogDemo(new TUIApp(args)).start();
    }

    DialogDemo(TUIApp app) {
        this.app = app;
    }

    void start() {
        app.setDefaultMainApplicationPanel().confirmOnExit().setOnReadyStart((tApp) -> {
            VerticalPanel page = new VerticalPanel().setVerticalSpacer(1);
            page.add(new TextComponent(Colors.BYel, "Dialogs - pick one (TAB or arrows, then Enter)"));
            VerticalPanel choices = new VerticalPanel();
            choices.add(button("1  Alert", this::alert));
            choices.add(button("2  Error", this::error));
            choices.add(button("3  Yes / No", this::yesNo));
            choices.add(button("4  Your name", this::yourName));
            choices.add(button("5  Type a word to confirm", this::typeToConfirm));
            choices.add(button("6  Login form", this::login));
            choices.add(button("7  Reset keeps it open", this::withReset));
            choices.add(button("8  A chart with its settings", this::chartTool));
            page.add(choices);
            page.add(outcome);
            tApp.getMainApplicationPanel().add(1, 2, page);
        }).start();
    }

    // ---- the dialogs

    /** a message and OK */
    private void alert() {
        show(Dialog.info("Saved 3 files.")
                .ok()
                .onClose(() -> say("Alert closed")));
    }

    /** the same, as an error */
    private void error() {
        show(Dialog.error("Can't read /var/backups/ig-2026-11-02.tar.gz:\npermission denied")
                .ok());
    }

    /** two buttons: Delete (also the D key) and Cancel (also Esc) */
    private void yesNo() {
        show(Dialog.question("Delete 3 backups?\nThis can't be undone.").title("Delete")
                .button("delete", "Delete", (d) -> say("Deleted 3 backups")).key(KeyStroke.caseInsensitive('d'))
                .cancel((d) -> say("Kept them")));
    }

    /** a field in the middle: focus starts there, and Enter in it presses OK */
    private void yourName() {
        TextInput name = new TextInput(Colors.IYel, 20);
        show(Dialog.question("What's your name?").title("Welcome")
                .add("Name: ", name)
                .button("ok", "OK", (d) -> say("Hello " + name.getText()))
                .cancel((d) -> say("No name given")));
    }

    /** Run can't be pressed until the word is typed */
    private void typeToConfirm() {
        TextInput word = new TextInput(Colors.IYel, 10);
        String command = "sudo -n /opt/wp-ops/bin/wp_backup_server.sh FORCE --progress";
        show(Dialog.warning("Runs on the live server and takes about 3 minutes.\nIt can't be cancelled once started.\n\n  " + command)
                .title("Back up production")
                .add("Type BACKUP to continue: ", word)
                .button("run", "Run", (d) -> say("Backup started"))
                    .enabledWhen(() -> "BACKUP".equals(word.getText()))
                .cancel((d) -> say("Backup not started")));
    }

    /** a form in the middle, in the dialog's colours: submit checks it first, and showError keeps the dialog open */
    private void login() {
        Login login = new Login();
        show(Dialog.question("Login to the server (the password is: secret)").title("Sign in")
                .form(login, (f) -> f.inputWidth(24)
                        .text("User", "user").required()
                        .password("Password", "password").required())
                .submit("login", "Login", (d) -> {
                    if (!"secret".equals(login.password)) {
                        d.showError("Login failed - wrong password");
                        return;
                    }
                    say("Logged in as " + login.user);
                })
                .cancel((d) -> say("Not logged in")));
    }

    /** Reset clears the field and the dialog stays open */
    private void withReset() {
        TextInput note = new TextInput(Colors.IYel, 30);
        note.setText("type here, then Reset");
        show(Dialog.question("A note for the log").title("Note")
                .add("Note: ", note)
                .button("ok", "OK", (d) -> say("Note: " + note.getText()))
                .button("reset", "Reset", (d) -> note.setText("")).staysOpen()
                .cancel());
    }

    /** any component in the middle - here a chart, with buttons that change it and leave it open */
    private void chartTool() {
        LineChart chart = new LineChart(Colors.Cya, new Dimension.Simple(10, 56)).series("size", Colors.BCya).legend(false);
        for (int day = 0; day < 30; day++) {
            chart.add("size", day, 1.9 + day * day * 0.0001);
        }
        chart.redraw();                                    // fitted to the points just added
        show(Dialog.question("Backup size, last 30 days (GB)").title("Disk")
                .add(chart)
                .button("zero", "From 0", (d) -> chart.yRange(0.0, null)).staysOpen()
                .button("fit", "Fit", (d) -> chart.yRange(null, null)).staysOpen()
                .button("done", "Done"));
    }

    // ---- helpers

    private Button button(String label, Runnable opens) {
        Button button = new Button(Colors.BWhi, label);
        button.onChange((te) -> opens.run());
        return button;
    }

    private void show(Dialog dialog) {
        dialog.show(app);
    }

    private void say(String text) {
        outcome.setTextAndResize(text);
    }
}
