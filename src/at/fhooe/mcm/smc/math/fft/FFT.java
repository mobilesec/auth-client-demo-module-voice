package at.fhooe.mcm.smc.math.fft;

import at.fhooe.mcm.smc.math.window.HammingWindow;
import at.fhooe.mcm.smc.math.window.WindowFunction;

/** Custom normalized power FFT. */
public final class FFT {

	public static final int FFT_FORWARD = -1;

	public static final int FFT_REVERSE = 1;
	
	private static final int FFT_DIRECTION = FFT_FORWARD;

	private WindowFunction windowFunction;
	private int windowSize;
	private static final double twoPI = 2 * Math.PI;
	
	public FFT(int windowSize) {
		this(windowSize, new HammingWindow(windowSize));
	}
	
	public FFT(int windowSize, WindowFunction windowFunction) {
		this.windowSize = windowSize;
		if (windowSize != (1 << ((int) Math.rint(Math.log(windowSize)
				/ Math.log(2)))))
			throw new IllegalArgumentException("Illegal FFT window size: " + windowSize + " is not power of 2");

		if (windowFunction == null) {
			throw new IllegalArgumentException("Window function must not be null");
		}
		this.windowFunction = windowFunction;
	}

	/** Transforms in place. Imaginary part ignored for now. */
	public void transform(double[] re, double[] im) {
		if (re.length < windowSize)
			throw new IllegalArgumentException(
					"FFT input must be equal length to window size");

		if (windowFunction != null) {
			windowFunction.applyWindow(re);
		}

		normalizedPowerFFT(re);
	}

	/**
	 * The FFT method. Calculation is inline, for complex data stored in 2
	 * separate arrays. Length of input data must be a power of two.
	 * 
	 * @param re
	 *            the real part of the complex input and output data
	 * @param im
	 *            the imaginary part of the complex input and output data
	 * @param direction
	 *            the direction of the Fourier transform (FORWARD or REVERSE)
	 * @throws IllegalArgumentException
	 *             if the length of the input data is not a power of 2
	 */
	private void fft(double re[], double im[], int direction) {
		int n = re.length;
		int bits = (int) Math.rint(Math.log(n) / Math.log(2));

		if (n != (1 << bits))
			throw new IllegalArgumentException("fft data must be power of 2");

		int localN;
		int j = 0;
		for (int i = 0; i < n - 1; i++) {
			if (i < j) {
				double temp = re[j];
				re[j] = re[i];
				re[i] = temp;
				temp = im[j];
				im[j] = im[i];
				im[i] = temp;
			}

			int k = n / 2;

			while ((k >= 1) && (k - 1 < j)) {
				j = j - k;
				k = k / 2;
			}

			j = j + k;
		}

		for (int m = 1; m <= bits; m++) {
			localN = 1 << m;
			double Wjk_r = 1;
			double Wjk_i = 0;
			double theta = twoPI / localN;
			double Wj_r = Math.cos(theta);
			double Wj_i = direction * Math.sin(theta);
			int nby2 = localN / 2;
			for (j = 0; j < nby2; j++) {
				for (int k = j; k < n; k += localN) {
					int id = k + nby2;
					double tempr = Wjk_r * re[id] - Wjk_i * im[id];
					double tempi = Wjk_r * im[id] + Wjk_i * re[id];
					re[id] = re[k] - tempr;
					im[id] = im[k] - tempi;
					re[k] += tempr;
					im[k] += tempi;
				}
				double wtemp = Wjk_r;
				Wjk_r = Wj_r * Wjk_r - Wj_i * Wjk_i;
				Wjk_i = Wj_r * Wjk_i + Wj_i * wtemp;
			}
		}
	}


	private void normalizedPowerFFT(double[] re) {
		double[] im = new double[re.length];
		double r, i;

		fft(re, im, FFT_FORWARD);

		double windowSum = windowFunction.getWindowSum();
		for (int j = 0; j < re.length; j++) {
			r = re[j] / windowSum * 2;
			i = im[j] / windowSum * 2;
			re[j] = r * r + i * i;
		}
	}

	public void setWindowFunction(WindowFunction windowFunction) {
		this.windowFunction = windowFunction;
	}
}