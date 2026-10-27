package mizzen.components.plot.dotmatrix;

import java.util.HashMap;
import java.util.Map;

import mizzen.components.plot.PointPlotter;

public class DotMatrixRenderer {

    // Full 5x7 Dot Matrix Patterns for uppercase, lowercase letters, and punctuation
    private static final Map<Character, String[]> dotMatrixMap = new HashMap<>();

    static {
        // Uppercase letters (A-Z)
        dotMatrixMap.put('A', new String[]{
            "01110", "10001", "10001", "11111", "10001", "10001", "10001"
        });
        dotMatrixMap.put('B', new String[]{
            "11110", "10001", "10001", "11110", "10001", "10001", "11110"
        });
        dotMatrixMap.put('C', new String[]{
            "01110", "10001", "10000", "10000", "10000", "10001", "01110"
        });
        dotMatrixMap.put('D', new String[]{
            "11110", "10001", "10001", "10001", "10001", "10001", "11110"
        });
        dotMatrixMap.put('E', new String[]{
            "11111", "10000", "10000", "11110", "10000", "10000", "11111"
        });
        dotMatrixMap.put('F', new String[]{
            "11111", "10000", "10000", "11110", "10000", "10000", "10000"
        });
        dotMatrixMap.put('G', new String[]{
            "01110", "10001", "10000", "10111", "10001", "10001", "01111"
        });
        dotMatrixMap.put('H', new String[]{
            "10001", "10001", "10001", "11111", "10001", "10001", "10001"
        });
        dotMatrixMap.put('I', new String[]{
            "01110", "00100", "00100", "00100", "00100", "00100", "01110"
        });
        dotMatrixMap.put('J', new String[]{
            "00111", "00001", "00001", "00001", "00001", "10001", "01110"
        });
        dotMatrixMap.put('K', new String[]{
            "10001", "10010", "10100", "11000", "10100", "10010", "10001"
        });
        dotMatrixMap.put('L', new String[]{
            "10000", "10000", "10000", "10000", "10000", "10000", "11111"
        });
        dotMatrixMap.put('M', new String[]{
            "10001", "11011", "10101", "10101", "10001", "10001", "10001"
        });
        dotMatrixMap.put('N', new String[]{
            "10001", "11001", "10101", "10011", "10001", "10001", "10001"
        });
        dotMatrixMap.put('O', new String[]{
            "01110", "10001", "10001", "10001", "10001", "10001", "01110"
        });
        dotMatrixMap.put('P', new String[]{
            "11110", "10001", "10001", "11110", "10000", "10000", "10000"
        });
        dotMatrixMap.put('Q', new String[]{
            "01110", "10001", "10001", "10001", "10101", "10010", "01101"
        });
        dotMatrixMap.put('R', new String[]{
            "11110", "10001", "10001", "11110", "10100", "10010", "10001"
        });
        dotMatrixMap.put('S', new String[]{
            "01111", "10000", "10000", "01110", "00001", "00001", "11110"
        });
        dotMatrixMap.put('T', new String[]{
            "11111", "00100", "00100", "00100", "00100", "00100", "00100"
        });
        dotMatrixMap.put('U', new String[]{
            "10001", "10001", "10001", "10001", "10001", "10001", "01110"
        });
        dotMatrixMap.put('V', new String[]{
            "10001", "10001", "10001", "10001", "10001", "01010", "00100"
        });
        dotMatrixMap.put('W', new String[]{
            "10001", "10001", "10001", "10101", "10101", "10101", "01010"
        });
        dotMatrixMap.put('X', new String[]{
            "10001", "10001", "01010", "00100", "01010", "10001", "10001"
        });
        dotMatrixMap.put('Y', new String[]{
            "10001", "10001", "01010", "00100", "00100", "00100", "00100"
        });
        dotMatrixMap.put('Z', new String[]{
            "11111", "00001", "00010", "00100", "01000", "10000", "11111"
        });

        // Lowercase letters (a-z)
        dotMatrixMap.put('a', new String[]{
            "00000", "01110", "00001", "01111", "10001", "10001", "01111"
        });
        dotMatrixMap.put('b', new String[]{
            "10000", "10000", "11110", "10001", "10001", "10001", "11110"
        });
        dotMatrixMap.put('c', new String[]{
            "00000", "01110", "10001", "10000", "10000", "10001", "01110"
        });
        dotMatrixMap.put('d', new String[]{
            "00001", "00001", "01111", "10001", "10001", "10001", "01111"
        });
        dotMatrixMap.put('e', new String[]{
            "00000", "01110", "10001", "11111", "10000", "10001", "01110"
        });
        dotMatrixMap.put('f', new String[]{
            "00011", "00100", "01111", "00100", "00100", "00100", "00100"
        });
        dotMatrixMap.put('g', new String[]{
            "00000", "01111", "10001", "10001", "01111", "00001", "01110"
        });
        dotMatrixMap.put('h', new String[]{
            "10000", "10000", "11110", "10001", "10001", "10001", "10001"
        });
        dotMatrixMap.put('i', new String[]{
            "00100", "00000", "01100", "00100", "00100", "00100", "01110"
        });
        dotMatrixMap.put('j', new String[]{
            "00010", "00000", "00110", "00010", "00010", "10010", "01100"
        });
        dotMatrixMap.put('k', new String[]{
            "10000", "10010", "10100", "11000", "10100", "10010", "10001"
        });
        dotMatrixMap.put('l', new String[]{
            "01100", "00100", "00100", "00100", "00100", "00100", "01110"
        });
        dotMatrixMap.put('m', new String[]{
            "00000", "10110", "11001", "10101", "10101", "10101", "10101"
        });
        dotMatrixMap.put('n', new String[]{
            "00000", "11110", "10001", "10001", "10001", "10001", "10001"
        });
        dotMatrixMap.put('o', new String[]{
            "00000", "01110", "10001", "10001", "10001", "10001", "01110"
        });
        dotMatrixMap.put('p', new String[]{
            "00000", "11110", "10001", "10001", "11110", "10000", "10000"
        });
        dotMatrixMap.put('q', new String[]{
            "00000", "01111", "10001", "10001", "01111", "00001", "00001"
        });
        dotMatrixMap.put('r', new String[]{
            "00000", "11110", "10001", "10000", "10000", "10000", "10000"
        });
        dotMatrixMap.put('s', new String[]{
            "00000", "01111", "10000", "01110", "00001", "10001", "01110"
        });
        dotMatrixMap.put('t', new String[]{
            "00100", "00100", "01111", "00100", "00100", "00100", "00011"
        });
        dotMatrixMap.put('u', new String[]{
            "00000", "10001", "10001", "10001", "10001", "10001", "01111"
        });
        dotMatrixMap.put('v', new String[]{
            "00000", "10001", "10001", "10001", "10001", "01010", "00100"
        });
        dotMatrixMap.put('w', new String[]{
            "00000", "10001", "10101", "10101", "10101", "10101", "01010"
        });
        dotMatrixMap.put('x', new String[]{
            "00000", "10001", "01010", "00100", "01010", "10001", "10001"
        });
        dotMatrixMap.put('y', new String[]{
            "00000", "10001", "10001", "01111", "00001", "10001", "01110"
        });
        dotMatrixMap.put('z', new String[]{
            "00000", "11111", "00010", "00100", "01000", "10000", "11111"
        });

        // Punctuation and symbols
        dotMatrixMap.put('!', new String[]{
            "00100", "00100", "00100", "00100", "00100", "00000", "00100"
        });
        dotMatrixMap.put('@', new String[]{
            "01110", "10001", "10111", "10101", "10111", "10000", "01111"
        });
        dotMatrixMap.put(',', new String[]{
            "00000", "00000", "00000", "00000", "00000", "00100", "01000"
        });
        dotMatrixMap.put('.', new String[]{
            "00000", "00000", "00000", "00000", "00000", "00100", "00000"
        });
        dotMatrixMap.put('?', new String[]{
            "01110", "10001", "00001", "00110", "00100", "00000", "00100"
        });
        dotMatrixMap.put('-', new String[]{
            "00000", "00000", "00000", "11111", "00000", "00000", "00000"
        });
        dotMatrixMap.put(' ', new String[]{
            "00000", "00000", "00000", "00000", "00000", "00000", "00000"
        });
    }

    static {
        // Existing letters (A-Z), lowercase (a-z), and punctuation (like !, ., etc.)
        // ... Already defined above ...
        
        // Numbers (0-9)
        dotMatrixMap.put('0', new String[]{
            "01110", "10001", "10011", "10101", "11001", "10001", "01110"
        });
        dotMatrixMap.put('1', new String[]{
            "00100", "01100", "00100", "00100", "00100", "00100", "01110"
        });
        dotMatrixMap.put('2', new String[]{
            "01110", "10001", "00001", "00110", "01000", "10000", "11111"
        });
        dotMatrixMap.put('3', new String[]{
            "01110", "10001", "00001", "00110", "00001", "10001", "01110"
        });
        dotMatrixMap.put('4', new String[]{
            "00010", "00110", "01010", "10010", "11111", "00010", "00010"
        });
        dotMatrixMap.put('5', new String[]{
            "11111", "10000", "11110", "00001", "00001", "10001", "01110"
        });
        dotMatrixMap.put('6', new String[]{
            "00110", "01000", "10000", "11110", "10001", "10001", "01110"
        });
        dotMatrixMap.put('7', new String[]{
            "11111", "10001", "00010", "00100", "01000", "10000", "10000"
        });
        dotMatrixMap.put('8', new String[]{
            "01110", "10001", "10001", "01110", "10001", "10001", "01110"
        });
        dotMatrixMap.put('9', new String[]{
            "01110", "10001", "10001", "01111", "00001", "00010", "01100"
        });

        // Punctuation and symbols
        dotMatrixMap.put('!', new String[]{
            "00100", "00100", "00100", "00100", "00100", "00000", "00100"
        });
        dotMatrixMap.put('@', new String[]{
            "01110", "10001", "10111", "10101", "10111", "10000", "01111"
        });
        dotMatrixMap.put('#', new String[]{
            "01010", "11111", "01010", "01010", "11111", "01010", "01010"
        });
        dotMatrixMap.put('$', new String[]{
            "00100", "01111", "10000", "01110", "00001", "11110", "00100"
        });
        dotMatrixMap.put('%', new String[]{
            "11001", "11010", "00010", "00100", "01000", "10110", "10011"
        });
        dotMatrixMap.put('^', new String[]{
            "00100", "01010", "10001", "00000", "00000", "00000", "00000"
        });
        dotMatrixMap.put('*', new String[]{
            "00000", "01010", "00100", "11111", "00100", "01010", "00000"
        });
        dotMatrixMap.put('(', new String[]{
            "00010", "00100", "01000", "01000", "01000", "00100", "00010"
        });
        dotMatrixMap.put(')', new String[]{
            "01000", "00100", "00010", "00010", "00010", "00100", "01000"
        });
        dotMatrixMap.put('-', new String[]{
            "00000", "00000", "00000", "11111", "00000", "00000", "00000"
        });
        dotMatrixMap.put('_', new String[]{
            "00000", "00000", "00000", "00000", "00000", "00000", "11111"
        });
        dotMatrixMap.put('+', new String[]{
            "00000", "00100", "00100", "11111", "00100", "00100", "00000"
        });
        dotMatrixMap.put('=', new String[]{
            "00000", "11111", "00000", "00000", "11111", "00000", "00000"
        });
        dotMatrixMap.put('[', new String[]{
            "01110", "01000", "01000", "01000", "01000", "01000", "01110"
        });
        dotMatrixMap.put(']', new String[]{
            "01110", "00010", "00010", "00010", "00010", "00010", "01110"
        });
        dotMatrixMap.put('{', new String[]{
            "00110", "01000", "01000", "10000", "01000", "01000", "00110"
        });
        dotMatrixMap.put('}', new String[]{
            "01100", "00010", "00010", "00001", "00010", "00010", "01100"
        });
        dotMatrixMap.put('|', new String[]{
            "00100", "00100", "00100", "00100", "00100", "00100", "00100"
        });
        dotMatrixMap.put('\\', new String[]{
            "10000", "01000", "00100", "00010", "00001", "00000", "00000"
        });
        dotMatrixMap.put('/', new String[]{
            "00001", "00010", "00100", "01000", "10000", "00000", "00000"
        });
        dotMatrixMap.put('\'', new String[]{
            "00100", "00100", "00100", "00000", "00000", "00000", "00000"
        });
        dotMatrixMap.put('"', new String[]{
            "01010", "01010", "00000", "00000", "00000", "00000", "00000"
        });
        dotMatrixMap.put(',', new String[]{
            "00000", "00000", "00000", "00000", "00100", "00100", "01000"
        });
        dotMatrixMap.put('.', new String[]{
            "00000", "00000", "00000", "00000", "00000", "00100", "00100"
        });
    }
    
    
    
    // Method to render a character
    public static void renderCharacter(char c) {
        if (dotMatrixMap.containsKey(c)) {
            for (String row : dotMatrixMap.get(c)) {
                // Print the row with "#" for "1" and "." for "0"
                System.out.println(row.replace('1', '#').replace('0', '.'));
            }
        } else {
            System.out.println("Character not defined.");
        }
    }

    // Main method to render an entire string
    public static void renderString(String text) {
        for (char c : text.toCharArray()) {
            renderCharacter(c);
            System.out.println();  // Space between characters
        }
    }

    public static void mainOLD(String[] args) {
        // Test rendering a string
        renderString("Hello, World!  Water is wet");
    }
    
 // Main method to render an entire string with proper spacing between characters
    public static void renderStringWithSpacing(String text, int spacing) {
        // Initialize an array to hold rows of the full string
        StringBuilder[] rows = new StringBuilder[7]; // Each character is 7 rows high
        for (int i = 0; i < 7; i++) {
            rows[i] = new StringBuilder(); // Initialize each row
        }

        // Loop over each character in the text
        for (int charIndex = 0; charIndex < text.length(); charIndex++) {
            char c = text.charAt(charIndex);
            if (dotMatrixMap.containsKey(c)) {
                // For each character, append its 5x7 matrix to the rows
                String[] charMatrix = dotMatrixMap.get(c);
                for (int i = 0; i < 7; i++) {
                    rows[i].append(charMatrix[i].replace('1', '#').replace('0', '.'));
                    // Add spacing (dots) after each character except the last one
                    if (charIndex < text.length() - 1) {
                        for (int s = 0; s < spacing; s++) {
                            rows[i].append('.'); // Append empty dots for spacing
                        }
                    }
                }
            }
        }

        // Print out the rows to form the full string
        for (StringBuilder row : rows) {
            System.out.println(row.toString());
        }
    }
    public static void plotStringWithSpacing(PointPlotter plotter, String text, int spacing, float startX, float startY) {
        int charWidth = 5; // Each character in the dot matrix is 5 dots wide
        int charHeight = 7; // Each character in the dot matrix is 7 dots high
        
        float x = startX; // Initialize the x-coordinate for the first character

        // Loop over each character in the text
        for (char charIndex : text.toCharArray()) {
            if (dotMatrixMap.containsKey(charIndex)) {
                String[] charMatrix = dotMatrixMap.get(charIndex); // Get the matrix for the character
                
                // Loop over the matrix rows (y-axis)
                for (int row = 0; row < charHeight; row++) {
                    String rowPattern = charMatrix[row]; // Get the row's pattern

                    // Loop over the matrix columns (x-axis)
                    for (int col = 0; col < charWidth; col++) {
                        if (rowPattern.charAt(col) == '1') {
                            // If we encounter a "1", plot the point
                            plotter.plot(x + col, startY + row);
                        }
                    }
                }

                // After each character, shift the x-coordinate by the character width + spacing
                x += charWidth + spacing;
            }
        }
    }

    public static void main(String[] args) {
        // Example: Render the string "HELLO" with 2-dot spacing between letters
        renderStringWithSpacing("HELLO WORLD!  Water is wet?", 2);
    }
    
    
}
