package dtb.terminalIO.components;

import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Rectangle;

/**
 *  Used by many components to decide how to perform a clear (erase) of its LineSegments buffer 
 */
public interface FillOnClearPolicy {

    char getEmptyChar();

    void fillEmptyIfConfigured();

    void fillWithEmptyChar();

    IScreenLineSegmentsBuffer getSegmentsBuffer();

    Rectangle getRectangleForClearAndFill();

    boolean isFillOnClear();

    FGBGColors getEmptyColor();

    void setEmptyChar(char emptyChar);

    void setEmptyColor(FGBGColors emptyColor);

    void setFillOnClear(boolean fillOnClear);
    
    public static interface Provider {
        FillOnClearPolicy getFillOnClearPolicy();
        void setFillOnClearPolicy(FillOnClearPolicy fillerPolicy);
    }

    public static abstract class AbstractImpl implements FillOnClearPolicy {

        private boolean          fillOnClear      = true;
        private char             emptyChar        = ' ';
        private FGBGColors       emptyColor       = FGBGColors.NO_COLOR_SPECIFIED;
        private static final int EMPTY_SEGMENT_ID = -1234;
        
        private FGBGColors.Provider parentColorProvider;

        protected AbstractImpl(FGBGColors.Provider parentColorProvider) {
            this.parentColorProvider = parentColorProvider;
        }
        
        @Override
        public char getEmptyChar() {
            return emptyChar;
        }

        public void setEmptyChar(char emptyChar) {
            this.emptyChar = emptyChar;
        }
        
        public FGBGColors getParentColor() {
            if (parentColorProvider == null || parentColorProvider.getColor() == null) {
                return FGBGColors.NO_COLOR_SPECIFIED;
            }
            return parentColorProvider.getColor();
        }

        @Override
        public FGBGColors getEmptyColor() {
            return emptyColor.mergeWith(getParentColor());
        }

        public void setEmptyColor(FGBGColors emptyColor) {
            this.emptyColor = emptyColor==null? FGBGColors.NO_COLOR_SPECIFIED : emptyColor;
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
            FGBGColors emptyColor = getEmptyColor();
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
