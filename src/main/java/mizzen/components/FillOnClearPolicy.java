package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.ColorProvider;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.controlcodes.color.Colors.FgColor;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

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

    public static abstract class AbstractImpl<T extends ColorProvider> implements FillOnClearPolicy<T> {

        private boolean          fillOnClear      = true;
        private char             emptyChar        = ' ';
        private Color            emptyColor       = Colors.FGBGColors.NO_COLOR_SPECIFIED;
        private static final int EMPTY_SEGMENT_ID = -1234;
        
        private ColorProvider parentColorProvider;

        protected AbstractImpl(ColorProvider parentColorProvider) {
            this.parentColorProvider = parentColorProvider;
        }
        
        @Override
        public char getEmptyChar() {
            return emptyChar;
        }

        public T setEmptyChar(char emptyChar) {
            if (this.emptyChar != emptyChar) {
                this.emptyChar = emptyChar;
                changed();
            }
            return (T) parentColorProvider;
        }

        /** a setting changed: the component it belongs to redraws (if it is one) */
        private void changed() {
            if (parentColorProvider instanceof DirtyFlag) {
                DirtyFlag owner = (DirtyFlag) parentColorProvider;
                if (!owner.isDirty()) {
                    owner.markAsDirty();
                }
            }
        }
        
        public Color getParentColor() {
            if (parentColorProvider == null || parentColorProvider.getColor() == null) {
                return Colors.FGBGColors.NO_COLOR_SPECIFIED;
            }
            return parentColorProvider.getColor();
        }

        @Override
        public Color getEmptyColor() {
            Color parentColor = getParentColor();
            if (parentColor instanceof FgColor) {
                return emptyColor;
            }
            if (parentColor instanceof FGBGColors && ((FGBGColors)parentColor).getBgc()==null) {
                return emptyColor;
            }
            
            return emptyColor.mergeWith(getParentColor());
        }

        public T setEmptyColor(Color emptyColor) {
            Color newColor = emptyColor == null ? Colors.FGBGColors.NO_COLOR_SPECIFIED : emptyColor;
            if (!newColor.equals(this.emptyColor)) {
                this.emptyColor = newColor;
                changed();
            }
            return (T) parentColorProvider;
        }

        @Override
        public boolean isFillOnClear() {
            return fillOnClear;
        }

        public void setFillOnClear(boolean fillOnClear) {
            if (this.fillOnClear != fillOnClear) {
                this.fillOnClear = fillOnClear;
                changed();
            }
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
            
            // TODO:  why is this commented out?  -- figure out why then remove
            
            //this.getSegmentsBuffer().fillRectWithChar(emptyColor, emptyChar, bounds);
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
