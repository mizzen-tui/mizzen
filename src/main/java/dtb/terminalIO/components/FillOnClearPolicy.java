package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 *  Used by many components to decide how to perform a clear (erase) of its LineSegments buffer 
 */
public interface FillOnClearPolicy<T> extends EmptyAttributes<T> {

    void fillEmptyIfConfigured();

    void fillWithEmptyChar();

    IScreenLineSegmentsBuffer getSegmentsBuffer();

    Rectangle getRectangleForClearAndFill();

    boolean isFillOnClear();

    void setFillOnClear(boolean fillOnClear);
    
    public static interface Provider {
        FillOnClearPolicy getFillOnClearPolicy();
        // void setFillOnClearPolicy(FillOnClearPolicy fillerPolicy);
    }

    public static abstract class AbstractImpl<T extends ControlCode.ColorProvider> implements FillOnClearPolicy<T> {

        private boolean          fillOnClear      = true;
        private char             emptyChar        = ' ';
        private Color            emptyColor       = FGBGColors.NO_COLOR_SPECIFIED;
        private static final int EMPTY_SEGMENT_ID = -1234;
        
        private ControlCode.ColorProvider parentColorProvider;

        protected AbstractImpl(ControlCode.ColorProvider parentColorProvider) {
            this.parentColorProvider = parentColorProvider;
        }
        
        @Override
        public char getEmptyChar() {
            return emptyChar;
        }

        public T setEmptyChar(char emptyChar) {
            this.emptyChar = emptyChar;
            return (T) parentColorProvider;
        }
        
        public Color getParentColor() {
            if (parentColorProvider == null || parentColorProvider.getColor() == null) {
                return FGBGColors.NO_COLOR_SPECIFIED;
            }
            return parentColorProvider.getColor();
        }

        @Override
        public Color getEmptyColor() {
            return emptyColor.mergeWith(getParentColor());
        }

        public T setEmptyColor(Color emptyColor) {
            this.emptyColor = emptyColor==null? FGBGColors.NO_COLOR_SPECIFIED : emptyColor;
            return (T) parentColorProvider;
        }

        @Override
        public boolean isFillOnClear() {
            return fillOnClear;
        }

        public void setFillOnClear(boolean fillOnClear) {
            this.fillOnClear = fillOnClear;
        }

        @Override
        public abstract Rectangle getRectangleForClearAndFill();

        @Override
        public abstract IScreenLineSegmentsBuffer getSegmentsBuffer();

        @Override
        public void fillWithEmptyChar() {
            Color emptyColor = getEmptyColor();
            char emptyChar = getEmptyChar();
            Rectangle bounds = this.getRectangleForClearAndFill();
            this.getSegmentsBuffer().fillRectWithChar(emptyColor, emptyChar, bounds);
        }

        @Override
        public void fillEmptyIfConfigured() {
            if (isFillOnClear()) {
                fillWithEmptyChar();
            }
        }

        @Override
        public String toString() {
            return "FillOnClearPolicy.AbstractImpl [fillOnClear=" + fillOnClear + ", emptyChar='" + emptyChar + "', emptyColor=" + emptyColor + "]";
        }

    }

}
