package mizzen.app;

import junit.framework.TestCase;

/**
 * Tests TUIApp.argumentsReadAt: which of a program's arguments are TUIApp's own, so an app that checks its own
 * arguments can skip them instead of calling theme=Dark an unknown option.
 */
public class TUIAppArgumentsTest extends TestCase {

    private static final String[] ARGS = { "IMAGE", "a.gif", "theme=Retro Phosphor Rounded", "NOGRAPHICS",
                                           "--LoggingEnabled", "true", "IMAGE", "b.gif", "LoggingEnabled" };

    public void testItsOwnAndHowManyWords() {
        assertEquals("theme=NAME, one word", 1, TUIApp.argumentsReadAt(ARGS, 2));
        assertEquals("NOGRAPHICS", 1, TUIApp.argumentsReadAt(ARGS, 3));
        assertEquals("LoggingEnabled and its value", 2, TUIApp.argumentsReadAt(ARGS, 4));
        assertEquals("LoggingEnabled last, no value after it", 1, TUIApp.argumentsReadAt(ARGS, 8));
    }

    public void testTheAppsOwnAreNotItsOwn() {
        assertEquals(0, TUIApp.argumentsReadAt(ARGS, 0));
        assertEquals(0, TUIApp.argumentsReadAt(ARGS, 1));
        assertEquals(0, TUIApp.argumentsReadAt(ARGS, 6));
        assertEquals("past the end", 0, TUIApp.argumentsReadAt(ARGS, 9));
        assertEquals(0, TUIApp.argumentsReadAt(null, 0));
    }
}
