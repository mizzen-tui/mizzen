package mizzen.examples;

import java.util.Arrays;

import mizzen.app.TUIApp;
import mizzen.components.Badge;
import mizzen.components.TextComponent;
import mizzen.components.card.Card;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.DropdownListBox;
import mizzen.components.progressmeter.Counter;
import mizzen.components.table.ObjectTableModel;
import mizzen.components.table.TableComponent;
import mizzen.components.theme.Theme;
import mizzen.geom.Dimension;

/**
 * Cards and badges: a status page as cards on the background, a badge for the state. Pick Rounded in the theme list for
 * rounded cards on a raised surface; in Legacy a card is a bordered panel.
 * <pre>
 *     ● M I Z Z E N  status                               ● OK
 *
 *     ╭ Site ─────────╮  ╭ Disk ──────────╮  ╭ Backups ──────╮
 *     │ HTTP 200      │  │ 52% used       │  │ 7 kept        │
 *     │ ● up 41 days  │  │ ⣿⣿⣿⣿⣿⣀⣀⣀⣀⣀    │  │ newest 9 h    │
 *     ╰───────────────╯  ╰────────────────╯  ╰───────────────╯
 * </pre>
 */
public class CardsDemo {

    public static void main(String[] args) {
        new TUIApp(args)
                .setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((tApp) -> {
                    Composite screen = (Composite) tApp.getMainApplicationPanel();

                    Badge health = new Badge(Theme::successColor, "OK");
                    Button flip = new Button("Flip state");
                    flip.onChange((te) -> {
                        boolean ok = "ALERT".equals(health.getText());
                        health.setText(ok ? "OK" : "ALERT").setDotThemeColor(ok ? Theme::successColor : Theme::errorColor);
                    });
                    DropdownListBox<Theme> theme = ThemeList.of(tApp);
                    HorizontalPanel heading = new HorizontalPanel().setHorizontalSpacer(4)
                            .add(new Badge("M I Z Z E N   status").setThemeColor(Theme::strongColor))
                            .add(health).add(flip).add(theme);

                    Card site = new Card("Site")
                            .add(new TextComponent("HTTP 200").setThemeColor(Theme::strongColor))
                            .add(new Badge(Theme::successColor, "up 41 days"));
                    Card disk = new Card("Disk")
                            .add(new TextComponent("52% used").setThemeColor(Theme::strongColor))
                            .add(new Counter(10).set(52, 100));
                    Card backups = new Card("Backups")
                            .add(new TextComponent("7 kept").setThemeColor(Theme::strongColor))
                            .add(new TextComponent("newest 9 h").setThemeColor(Theme::dimColor));
                    HorizontalPanel cards = new HorizontalPanel().setHorizontalSpacer(2).add(site).add(disk).add(backups);

                    // buttons: filled (the theme's button colour) and outlined (a border, no fill); in Rounded, focus
                    // shows by colour, not [brackets] - TAB between them
                    Button refresh = new Button("Refresh");
                    Button details = new Button("Details").setThemeColor(Theme::textColor);
                    details.setBorder(true);
                    HorizontalPanel buttons = new HorizontalPanel().setHorizontalSpacer(2).add(refresh).add(details);

                    // a light table: a rule under the heading, every other row striped (in themes with a stripe colour)
                    TableComponent<String[]> table = new TableComponent<String[]>(new ObjectTableModel<String[]>()
                            .column("Backup", (r) -> r[0]).column("Size", (r) -> r[1]).column("Age", (r) -> r[2])
                            .setRows(Arrays.asList(new String[] { "wp_backup_20261101.tar.gz.gpg", "1904 MB", "9 h" },
                                                   new String[] { "wp_backup_20261031.tar.gz.gpg", "1901 MB", "33 h" },
                                                   new String[] { "wp_backup_20261030.tar.gz.gpg", "1899 MB", "57 h" })));
                    table.setLinesStyle(TableComponent.TableLinesStyle.HEADER_RULE);
                    table.setSpacedRows(false);
                    table.setStripedRows(true);
                    table.resize(new Dimension.Simple(table.heightAt(table.widthNeeded()), table.widthNeeded()));
                    Card recent = new Card("Recent backups").add(table);

                    screen.add(new VerticalPanel().setVerticalSpacer(1).add(heading).add(cards).add(recent).add(buttons));
                })
                .start();
    }
}
