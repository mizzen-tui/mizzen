package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.StringJoiner;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.util.Logging;

/**
 * Demonstrates the nested FocusManager traversal bug (see docs/ARCHITECTURE.md §4).
 *
 * <pre>
 *   layout:   A, [ B, C ], D      ([..] = ComponentWithFocusManager group)
 *
 *   ./runDBG.sh dtb.terminalIO.examples.NestedFocusBugTest          # default panel
 *   ./runDBG.sh dtb.terminalIO.examples.NestedFocusBugTest outer    # all inside an outer ComponentWithFocusManager
 * </pre>
 *
 * Click field A first (TUIApp never gives the root FocusManager initial focus, so TAB
 * alone focuses nothing), then press TAB or SHIFT-TAB repeatedly and watch the trace.
 *
 * <pre>
 * Observed (Sep 2026):
 *   default  TAB        A B C D A C D A C D ...   B skipped after first lap
 *   default  SHIFT-TAB  A D B A D B ...           C never reached
 *   outer    TAB        A B D  (then stuck)       C never reached, TAB dead after D
 * </pre>
 */
public class NestedFocusBugTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        boolean wrapInOuterGroup = args.length > 0 && args[0].equalsIgnoreCase("outer");

        new TUIApp(args)
           .setDefaultMainApplicationPanel()
           .setOnReadyStart((tApp) -> {

               // 20-wide editable fields pre-filled with their name (TextInput(color,String) would
               // cap the length at the initial text, leaving no room to type)
               TextInput a = field("A");
               TextInput b = field("B");
               TextInput c = field("C");
               TextInput d = field("D");
               TextInput[] fields = { a, b, c, d };

               // nested group: its own FocusManager (ComponentWithFocusManager default = NAVKEYS_FIRST)
               Composite groupPanel = new VerticalPanel().setName("groupPanel");
               groupPanel.add(b);
               groupPanel.add(c);
               ComponentWithFocusManager<?> group = new ComponentWithFocusManager<>(groupPanel)
                                                          .setBorder(true)
                                                          .setTitle("nested group [B,C]");
               group.setName("group");

               Composite form = new VerticalPanel().setName("form");
               form.add(a);
               form.add(group);
               form.add(d);

               String scenario;
               if (wrapInOuterGroup) {
                   ComponentWithFocusManager<?> outer = new ComponentWithFocusManager<>(form)
                                                              .setBorder(true)
                                                              .setTitle("outer group");
                   outer.setName("outer");
                   tApp.getMainApplicationPanel().add(9, 2, outer);
                   scenario = "SCENARIO: outer - A,[B,C],D inside an outer ComponentWithFocusManager (NAVKEYS_FIRST)";
               } else {
                   tApp.getMainApplicationPanel().add(9, 2, form);
                   scenario = "SCENARIO: default - A,[B,C],D on TUIApp's default panel (NAVKEYS_LAST)";
               }

               TextComponent header = new TextComponent(Colors.IWhi,
                       "PURPOSE : show that TAB / SHIFT-TAB focus order breaks when focus managers are nested.\n" +
                       "LAYOUT  : four editable text inputs A-D; B and C sit inside a nested group (own FocusManager).\n" +
                       scenario + "\n" +
                       "TRY     : click field A, then press TAB (expect A B C D A B C D ...) or SHIFT-TAB (expect A D C B ...)\n" +
                       "FAILURE : the 'actual' line differs from expected - a field is skipped, or focus gets stuck.");
               TextComponent trace = new TextComponent(Colors.IGre, "actual  : ");

               tApp.getMainApplicationPanel().add(1, 2, header);
               tApp.getMainApplicationPanel().add(7, 2, trace);

               // poll which field(s) report focus; append to the trace whenever it changes.
               // (more than one name joined with '+' would mean inconsistent focus state)
               StringBuilder history = new StringBuilder();
               String[] last = { "" };
               tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(100, (te) -> {
                   StringJoiner focused = new StringJoiner("+");
                   for (TextInput f : fields) {
                       Focusable focusable = ToFocusable.UTIL.toFocusable(f);
                       if (focusable != null && focusable.hasFocus()) {
                           focused.add(f.getName());
                       }
                   }
                   String now = focused.length() == 0 ? "-" : focused.toString();
                   if (now.equals(last[0])) {
                       return;
                   }
                   last[0] = now;
                   Logging.info("NestedFocusBugTest focus: " + now);
                   history.append(now).append(' ');
                   // keep the tail so the line fits on screen
                   String shown = history.length() > 70 ? "..." + history.substring(history.length() - 70) : history.toString();
                   trace.setTextAndResize("actual  : " + shown);
                   te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(trace));
               }));

           }).start();
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 20);
        field.setText(name);
        field.setName(name);
        return field;
    }
}
