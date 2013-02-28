package at.fhooe.mcm.smc.math.window;

import at.fhooe.mcm.smc.sample.FrameUtil;

/**
 * Hamming window is defined as:
 * 
 * <pre>
 * W(n) = (1 - a) - (a * cos((2 * Math.PI * n) / (N - 1)))
 * </pre>
 * 
 * with <code>a = 0.46</code>
 * 
 * @author thomaskaiser
 * 
 */
public class HammingWindow implements WindowFunction {

	private static final double a = 0.46d;
	
	private double[] window;
	
	double windowSum;

	public HammingWindow(int length) {
		// init the window
		window = new double[length];
		int N = window.length;
		windowSum = 0;
		for (int i = 0; i < window.length; i++) {
			window[i] = (1 - a) - (a * Math.cos((2 * Math.PI * i) / (N - 1)));
			windowSum += window[i];
		}
	}

	/* (non-Javadoc)
	 * @see at.fhooe.mcm.smc.math.WindowFunction#applyWindow(double[])
	 */
	public void applyWindow(double[] frame) {
		double[] temp = FrameUtil.multiply(frame, window);
		System.arraycopy(temp, 0, frame, 0, frame.length);
	}

	/* (non-Javadoc)
	 * @see at.fhooe.mcm.smc.math.WindowFunction#getWindowedFrame(double[])
	 */
	public double[] getWindowedFrame(double[] frame) {
		return FrameUtil.multiply(frame, window);
	}
	
	/**
	 * Returns the sum of the window function.
	 * @return
	 */
	public double getWindowSum() {
		return windowSum;
	}
}
