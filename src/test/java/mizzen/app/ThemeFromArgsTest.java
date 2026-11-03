package mizzen.app;

import mizzen.components.theme.Theme;
import junit.framework.TestCase;

/** theme=NAME on the command line picks a theme by name, case aside; other arguments are the app's; a wrong name fails */
public class ThemeFromArgsTest extends TestCase {

    public void testFoundByNameCaseAside() {
        assertSame(Theme.DARK, TUIApp.themeFromArgs(new String[] { "NOGRAPHICS", "theme=dark" }));
        assertSame(Theme.CLASSIC, TUIApp.themeFromArgs(new String[] { "THEME=Classic" }));
    }

    public void testNoneGiven() {
        assertNull(TUIApp.themeFromArgs(new String[] { "--port", "8080" }));
        assertNull(TUIApp.themeFromArgs(null));
    }

    public void testAWrongNameSaysWhichThereAre() {
        try {
            TUIApp.themeFromArgs(new String[] { "theme=Pink" });
            fail("no theme Pink");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Pink") && expected.getMessage().contains("Dark"));
        }
    }
}
