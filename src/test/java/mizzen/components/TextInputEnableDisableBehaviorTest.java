package mizzen.components;

import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import junit.framework.TestCase;

public class TextInputEnableDisableBehaviorTest  extends TestCase {

    
    public void testTextInputSetEnabledPreservesOriginalColor() {
        
        EventBus mockEventBus = new EventBus.STUBBED();
        
        // Create a TextInput with a specific color
        Color originalColor = Colors.Red;
        TestableTextInput textInput = new TestableTextInput(originalColor, "Test");

        // Verify initial state
        assertEquals(originalColor, textInput.getColor());
        assertEquals(originalColor, textInput.getInnerTextComponent().getColor());

        // Disable the TextInput and check the color change
        textInput.setEnabled(false, mockEventBus);
        assertEquals(Colors.DEFAULT_DISABLED_COLOR, textInput.getColor());
        assertEquals(Colors.DEFAULT_DISABLED_COLOR, textInput.getInnerTextComponent().getColor());

        // Re-enable the TextInput and verify the original color is restored
        textInput.setEnabled(true, mockEventBus);
        assertEquals(originalColor, textInput.getColor());
        assertEquals(originalColor, textInput.getInnerTextComponent().getColor());
    }    
    
    static 
    public class TestableTextInput extends TextInput {

        public TestableTextInput(Color color, String initialText) {
            super(color, initialText.length());
            this.setText(initialText);
        }

        // Expose the inner TextComponent for testing
        public TextComponent getInnerTextComponent() {
            return this.getRenderer();
        }
    }

    
}
