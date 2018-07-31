package dtb.terminalIO.geom;

import java.util.Arrays;
import java.util.Comparator;

public interface Dimension {

	public int getWidth();

	public int getHeight();
	
	public default boolean contains(Dimension other) {
		return this.getWidth()  >= other.getWidth() &&
			   this.getHeight() >= other.getHeight();
	}
	

	
	/**
	 * Simple implementation of {@link Dimension}
	 */
	public static class Simple implements Dimension {

		
		private int width;
		private int height;
		
		public Simple() {}
		
		public Simple(int height, int width) {
			super();
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		@Override
		public String toString() {
			return "Simple [width=" + width + ", height=" + height + "]";
		}
		
		
	}
	
	
        public static final Dimension ZERO = new Simple(0, 0);

	
	// UTILITIES .. 
	
	public static class UTIL {
	    
	    
	    public static Dimension stringToDimension(String s) {
	        
	        if (s==null || s.length()==0) return ZERO;
	        
	        String[] lines = s.split("\n");
	        
	        int height = lines.length;
	        int width = Arrays.stream(lines).max(Comparator.comparing(String::length)).get().length();
	        
	        return new Dimension.Simple(height, width);
	    }
	    
	    
	}
	
	
	
}
