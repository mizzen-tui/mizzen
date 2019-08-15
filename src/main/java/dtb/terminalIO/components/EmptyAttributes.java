package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;

/**
 * Attributes used when rendering
 * 'empty' areas of a component
 */
public interface EmptyAttributes<T> {
    char getEmptyChar();
    Color getEmptyColor();
    T setEmptyChar(char emptyChar);
    T setEmptyColor(Color emptyColor);
}
