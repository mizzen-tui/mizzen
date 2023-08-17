package dtb.terminalIO.geom;

import dtb.terminalIO.util.StringUtils;

public enum Align {
    LEFT,
    CENTER,
    RIGHT;
    
    public static class UTIL {
        
        /**
         * returns a string that has been aligned either LEFT, CENTER, or RIGHT
         */
        public static String formatToAlignedData( String displayString, int preferredSize, Align align) {
            return formatToAlignedData(displayString, preferredSize, align, 0, false);
        } 
        
        /**
         * returns a string that has been aligned either LEFT, CENTER, or RIGHT
         */
        public static String formatToAlignedData( String displayString, int preferredSize, int allowedDataWidth, Align align) {
            return formatToAlignedData(displayString, preferredSize, align, allowedDataWidth, false);
        } 
        
        /**
         * returns a string that has been aligned either LEFT, CENTER, or RIGHT
         */
        public static String formatToAlignedData( String displayString, int preferredSize, Align align , int allowedDataWidth, boolean truncateIfNeeded) 
        {
            //int size = columnDataItem.getPreferredWidth();
            if (allowedDataWidth<1) {
                allowedDataWidth = preferredSize;
            }
            if (truncateIfNeeded && Math.min(allowedDataWidth,preferredSize)  < displayString.length()) {
                return StringUtils.truncate(displayString, Math.min(allowedDataWidth,preferredSize));
            }
            switch (align) 
            {
                case LEFT:  if (preferredSize==allowedDataWidth)
                            {
                               return StringUtils.padRight(displayString, preferredSize);  
                            } else 
                            {
                               return StringUtils.center(StringUtils.padRight(displayString, allowedDataWidth), preferredSize);
                            }
                    
                case CENTER: return StringUtils.center(displayString, preferredSize);
                case RIGHT:  return (preferredSize==allowedDataWidth) 
                                      ? StringUtils.padLeft(displayString, preferredSize)  
                                      : StringUtils.center(StringUtils.padLeft(displayString, allowedDataWidth), preferredSize);
                default :  // can't get here
                           throw new IllegalStateException();
            }
        }
        
    }
    
}
