package cipm.stats.pelt;

/**
 * Represents a segment defined by [start, end) indices.
 * 
 * @author ezgiyircali
 *
 */
public class PeltSegment {

	private final int start;
	private final int end;

	public PeltSegment(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Returns the starting index of the segment (inclusive).
	 * 
	 * @return the start index
	 */
	public int getStart() {
		return start;
	}

	/**
	 * Returns the ending index of the segment (exclusive).
	 * 
	 * @return the end index
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * Compares this segment to the specified object for equality. Returns true if
	 * and only if the object is also a PeltSegment with the same start and end
	 * indices.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PeltSegment))
			return false;
		PeltSegment that = (PeltSegment) o;
		return start == that.start && end == that.end;
	}

	/**
	 * Returns a hash code value for this segment based on its start and end indices.
	 */
	@Override
	public int hashCode() {
		return java.util.Objects.hash(start, end);
	}

}
