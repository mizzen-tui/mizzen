package dtb.terminalIO.events.keyboard;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.TerminalScreen;

/**
 * Central point for instantiating and aligning terminal input parser and mouse mode.
 * Used by both InputEventsProducer and MouseEventOrchestrator.
 */
public class InputParserManager {

    // Resolved once at class load from -D or ENV
    private static final boolean USE_LEGACY_MODE = resolveLegacyMode();

    /**
     * System property: -Ddtb.legacyMouseMode=true|false
     * Env var: DTB_LEGACY_MOUSE_MODE=true|false
     * System property wins if present.
     */
    private static boolean resolveLegacyMode() {
        String prop = System.getProperty("dtb.legacyMouseMode");
        if (prop != null) {
            return parseBoolean(prop);
        }

        String env = System.getenv("DTB_LEGACY_MOUSE_MODE");
        if (env != null) {
            return parseBoolean(env);
        }

        // Default: choose what you want as baseline
        // true  => legacy parser + legacy mouse
        // false => SGR parser + SGR mouse
        return true;   // I’d default to legacy for now; flip later once SGR is fully trusted.
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v);
    }

    /**
     * Expose current mode (handy for logging / debugging).
     */
    public static boolean isLegacyMode() {
        return USE_LEGACY_MODE;
    }

    /**
     * Gets the active parser instance for the configured mode.
     */
    public static InputBytesEventParserInterface getParser() {
        if (USE_LEGACY_MODE) {
            // Legacy parser (old behaviour)
            return new InputBytesEventParser();
        } else {
            // New SGR-aware parser (previously InputBytesEventParserNewNotWorking)
            return new SGRInputBytesEventParser();
        }
    }

    /**
     * Enables the appropriate mouse mode.
     */
    public static void enableMouseTracking(TerminalScreen screen) {
        disableMouseTracking(screen);

        if (USE_LEGACY_MODE) {
            screen.add(ControlCode.SET_VT200_MOUSE); // 1000h
            screen.add(ControlCode.ENABLE_MOUSE_TRACKING);   // 1002h
            // Logging.info("InputParserManager: Legacy mouse mode enabled");
        } else {
        	
            // === SGR mode: 1002h + 1006h, no 1000h ===
            screen.add(ControlCode.SET_BTN_MOTION_TRACKING); // ?1002h
            screen.add(ControlCode.SET_SGR_EXT_MODE_MOUSE);  // ?1006h
            

            // If you want any-motion instead of button events, use 1003h:
            // screen.add(ControlCode.ENABLE_MOUSE_DRAG_TRACKING); // ?1003h
            // screen.add(ControlCode.SET_SGR_EXT_MODE_MOUSE);     // ?1006h            
            
            // DEL BELOW
            // SGR mode: VT200 + drag tracking + SGR extended mouse
//        	
//        	
//            screen.add(ControlCode.SET_VT200_MOUSE);         // 1000h
//            screen.add(ControlCode.ENABLE_MOUSE_DRAG_TRACKING); // 1003h
//            screen.add(ControlCode.SET_SGR_EXT_MODE_MOUSE);  // 1006h
            // Logging.info("InputParserManager: SGR mouse mode enabled");
        }
    }

    /**
     * Disables all known mouse modes.
     */
    public static void disableMouseTracking(TerminalScreen screen) {
        screen.add(ControlCode.DISABLE_MOUSE_DRAG_TRACKING);
        screen.add(ControlCode.DISABLE_BTN_MOTION_TRACKING);
        screen.add(ControlCode.DISABLE_MOUSE_TRACKING);
        screen.add(ControlCode.DISABLE_VT200_MOUSE);
        screen.add(ControlCode.DISABLE_SGR_EXT_MODE_MOUSE);
        screen.add(ControlCode.DISABLE_EXT_MODE_MOUSE);
        screen.add(ControlCode.DISABLE_URXVT_EXT_MODE_MOUSE);
        screen.add(ControlCode.DISABLE_PIXEL_POSITION_MOUSE);
        screen.add(ControlCode.DISABLE_X10_MOUSE);
        screen.add(ControlCode.DISABLE_VT200_HIGHLIGHT_MOUSE);
        screen.add(ControlCode.DISABLE_BTN_EVENT_MOUSE);
        screen.add(ControlCode.DISABLE_ANY_EVENT_MOUSE);
        screen.add(ControlCode.DISABLE_FOCUS_EVENT_MOUSE);
        screen.add(ControlCode.DISABLE_ALTERNATE_SCROLL);
        screen.refresh();
        // Logging.info("InputParserManager: All mouse modes disabled");
    }
}
