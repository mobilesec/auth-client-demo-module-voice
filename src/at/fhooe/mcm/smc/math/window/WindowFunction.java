package at.fhooe.mcm.smc.math.window;

public interface WindowFunction {

	/**
	 * Applies a Hamming window to the given frame.
	 * @param frame
	 */
	public void applyWindow(double[] frame);

	/**
	 * Returns a new copied array of the input frame multiplied with the Hamming
	 * window.
	 * 
	 * @param frame
	 * @return
	 */
	public double[] getWindowedFrame(double[] frame);

	/**
	 * Returns the sum of the window function.
	 * @return
	 */
	public double getWindowSum();

}