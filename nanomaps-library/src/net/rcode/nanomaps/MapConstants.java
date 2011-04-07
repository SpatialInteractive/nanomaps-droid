package net.rcode.nanomaps;

/**
 * Global constants.  We implement this interface on some key classes for
 * easy access.
 * @author stella
 *
 */
public interface MapConstants {
	public static final int BIAS_MASK=0x70000000;
	public static final int BIAS_NONE=0x0;
	public static final int BIAS_LEFT=0x10000000;
	public static final int BIAS_TOP=0x10000000;
	public static final int BIAS_RIGHT=0x20000000;
	public static final int BIAS_BOTTOM=0x20000000;
	public static final int BIAS_CENTER=0x30000000;
}
