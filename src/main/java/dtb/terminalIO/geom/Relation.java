package dtb.terminalIO.geom;

import java.util.function.BiPredicate;

/**
 * Utility class to represent relationship  between things ( ie rectangles)
 * 
 * A relationship can be be r1 is left of r2
 * 
 *   or ..
 *   
 * r1 is above r2  
 *   
 */
public class Relation<T, U extends T, V extends T> {
	protected T r1;
	protected T r2;

	protected Relation(U r1, V r2, BiPredicate<T,T> pred) {
		this.r1 = r1;
		this.r2 = r2;
		if (! pred.test(r1,r2)) {
			swap();
		}
		if (! pred.test(this.r1,this.r2)) {
			String className = this.getClass().getSimpleName();
			throw new IllegalArgumentException("Cannot create "
			                                   + className 
			                                   + " for objects "
			                                   + String.valueOf(r1) 
			                                   + " and " 
			                                   + String.valueOf(r2));
		}
	}
	
	
	protected void swap()
	{
		T tmp = r1;
		r1 = r2;
		r2 = tmp;
	}
	
	protected String asString(String r1Label, String r2Label) {
		return String.format("%s=[%s] %s=[%d]", r1Label, String.valueOf(r1),r2Label,String.valueOf(r2));
	}

	/**
	 * A relationship that determines if object 'left' is left of object 'right' and if not
	 * then to a swap so that 'right' is referred to as left and former left is referred to as right
	 * Say that 10 times!
	 */
	public static class LeftRight<T> extends Relation<T,T,T> {

		public LeftRight(T left, T right, BiPredicate<T,T> pred) {
			super(left, right, pred);
		}

		public T getLeft() {
			return r1;
		}

		public T getRight() {
			return r2;
		}
		
		public String toString() { return asString("left", "right"); }
	}

	/**
	 * A relationship that determines if object 'over' is on top of object 'under' and if not
	 * then to a swap so that 'top' is referred to as under and former under is referred to as top
	 * 
	 * D'oh ..
	 * 
	 * .. Here's another tongue twister ..
	 *     Silly Sally sold sea shells at the seashore..  Say that 10 times!
	 * 
	 */
	public static class OverUnder<T> extends Relation<T,T,T> {

		public OverUnder(T over, T under, BiPredicate<T,T> pred) {
			super(over, under, pred);
		}

		public T getOver() {
			return r1;
		}

		public T getUnder() {
			return r2;
		}
		public String toString() { return asString("over", "under"); }
	}

}
