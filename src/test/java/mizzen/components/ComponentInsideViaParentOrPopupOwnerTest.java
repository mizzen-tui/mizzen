package mizzen.components;

import mizzen.components.composite.Composite;
import junit.framework.TestCase;

/**
 * Tests Component.UTIL.isPartOf, which answers "is this component inside that one?" by walking up from the
 * component through its parents.
 * <p/>
 * A popup, such as a dropdown's open list, is drawn on the root panel rather than inside the dropdown; for a
 * popup the walk goes to its owner (the dropdown) instead of its parent, so the list counts as inside whatever
 * holds the dropdown. A cycle in those links is a broken tree - the walk would never end - so it must fail,
 * naming the components in the walk.
 */
public class ComponentInsideViaParentOrPopupOwnerTest extends TestCase {

    private final Composite dialog = new Composite();
    private final Composite form = new Composite();
    private final TextComponent field = new TextComponent("field");
    private final Composite root = new Composite();
    private final Composite popup = new Composite();

    @Override
    protected void setUp() throws Exception {
        root.setName("root");
        popup.setName("popup");
        dialog.setName("dialog");
        form.setName("form");
        root.add(0, 0, dialog);
        dialog.add(0, 0, form);
        form.add(0, 0, field);
        root.add(5, 5, popup);                               // drawn on the root, as a dropdown's list is
    }

    public void testInsideIsPartOf() {
        assertTrue(Component.UTIL.isPartOf(field, dialog));
        assertTrue(Component.UTIL.isPartOf(dialog, dialog));
        assertFalse(Component.UTIL.isPartOf(root, dialog));
    }

    public void testAPopupIsPartOfWhatOwnsIt() {
        assertFalse(Component.UTIL.isPartOf(popup, dialog));
        Component.UTIL.setPopupOwner(popup, field);
        assertTrue(Component.UTIL.isPartOf(popup, dialog));
    }

    public void testOwningAnAncestorFailsWhenTheLinkIsMade() {
        try {
            Component.UTIL.setPopupOwner(form, field);       // form owned by its own child: field -> form -> field ...
            fail("a popup owned by something inside it must fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("cannot own"));
            assertTrue(expected.getMessage(), expected.getMessage().contains("field"));
        }
        assertNull("the link was not made", form.getPopupOwner());
    }

    public void testOwningItselfFails() {
        try {
            Component.UTIL.setPopupOwner(popup, popup);
            fail("a popup owning itself must fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("cannot own"));
        }
    }

    /** a cycle made without setPopupOwner's check (the field set directly) still fails the walk, listing it */
    public void testACycleFoundOnTheWalkFailsAndShowsTheWalk() {
        ((AbstractComponent<?>) form).popupOwner = field;
        try {
            Component.UTIL.isPartOf(field, root);
            fail("a cycle must fail");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("cycle"));
            assertTrue(expected.getMessage(), expected.getMessage().contains("form"));
        }
    }
}
