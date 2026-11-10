package mizzen.components.theme;

import junit.framework.TestCase;

/**
 * Tests choosing a theme by name, as on the command line: theme=NAME among a program's arguments (TUIApp passes
 * them to Theme.setDefaultFromArgs). The name is a theme's, case aside, or - as the theme picker shows them - a
 * theme's name with its border style after it ("Retro Phosphor Rounded"). Other arguments are the app's; a wrong
 * name fails, listing the names there are.
 */
public class ThemeFromArgsTest extends TestCase {

    public void testFoundByNameCaseAside() {
        assertSame(Theme.DARK, Theme.fromArgs(new String[] { "NOGRAPHICS", "theme=dark" }));
        assertSame(Theme.CLASSIC, Theme.fromArgs(new String[] { "THEME=Classic" }));
    }

    public void testNoneGiven() {
        assertNull(Theme.fromArgs(new String[] { "--port", "8080" }));
        assertNull(Theme.fromArgs(null));
    }

    public void testAWrongNameSaysWhichThereAre() {
        try {
            Theme.fromArgs(new String[] { "theme=Pink" });
            fail("no theme Pink");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Pink") && expected.getMessage().contains("Dark"));
        }
    }

    /** the names the theme picker gives: a theme, then its border style - after a space or a hyphen */
    public void testANameWithItsBorderStyle() {
        Theme rounded = Theme.fromArgs(new String[] { "theme=Retro Phosphor Rounded" });
        assertTrue(rounded.isRounded());
        assertEquals("Retro Phosphor Rounded", rounded.name());
        assertEquals("the picker's own name for it", Theme.named("Retro Phosphor").rounded().name(), rounded.name());

        assertTrue(Theme.named("retro phosphor - rounded").isRounded());
        assertTrue(Theme.named("Dark-Rounded").isRounded());
        assertTrue(Theme.named("Rounded Traditional").isTraditional());
    }

    public void testAThemeCalledRoundedIsStillItself() {
        assertSame(Theme.ROUNDED, Theme.named("Rounded"));
    }

    public void testAStyleOnAnUnknownThemeIsNoTheme() {
        assertNull(Theme.named("Pink Rounded"));
        assertNull(Theme.named(null));
    }
}
