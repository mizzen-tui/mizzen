package mizzen.components.input;

import java.util.Arrays;
import java.util.List;

import mizzen.components.Component;
import mizzen.components.input.DisplayableValue.StringDisplayValue;
import junit.framework.TestCase;

/**
 * New items in a dropdown: its button stays as wide as its text - also when the new items are as wide
 * as the old, the case that left a 14-wide text in a 9-wide button.
 */
public class DropdownNewItemsTest extends TestCase {

    private static List<DisplayableValue<String>> items(String... values) {
        return Arrays.<DisplayableValue<String>>asList(StringDisplayValue.fromStringValues(values));
    }

    private static void assertButtonFitsAfter(String[] before, String[] after) {
        boolean checking = Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG;
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;       // fails a draw that falls outside its component
        try {
            DropdownListBox<String> dropdown = DropdownListBox.fromStrings(before);
            dropdown.getSegmentsBuffer();
            dropdown.setItems(items(after));
            dropdown.setValue(after[0]);
            dropdown.getSegmentsBuffer();
        } finally {
            Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = checking;
        }
    }

    public void testNewItemsAsWideAsTheOld() {
        assertButtonFitsAfter(new String[] { "Ontario", "Quebec" }, new String[] { "Maine", "Vermont" });
    }

    public void testWiderItems() {
        assertButtonFitsAfter(new String[] { "Ontario", "Quebec" }, new String[] { "Newfoundland", "Saskatchewan" });
    }

    public void testNarrowerItems() {
        assertButtonFitsAfter(new String[] { "Newfoundland", "Saskatchewan" }, new String[] { "PEI", "BC" });
    }
}
