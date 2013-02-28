package at.fhooe.mcm.smc.math.mfcc;

import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastCosineTransformer;
import org.apache.commons.math.transform.FastFourierTransformer;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.window.WindowFunction;
import at.fhooe.mcm.smc.sample.FrameUtil;

public class MelFrequencyCC {
	private int windowSize;
	private int hopSize;
	private int sampleRate;
	private double baseFreq;
	
	private static double[] buffer;

	// fields concerning the mel filter banks
	private double minFreq;
	private double maxFreq;
	private int numberFilters;

	// fields concerning the MFCCs settings
	private int numberCoefficients;
	private boolean useFirstCoefficient = false;

	private MelFilterBank filterBank;
	private FastFourierTransformer fft;
	private FastCosineTransformer dct;
	private WindowFunction windowFunction;

	private Matrix dctMatrix;

	/**
	 * Default constructor. Leaves object ready for processing whole audio
	 * samples with {@link #processAll(double)} or single windows with
	 * {@link #processWindow(double)}. Window size must be larger than 32 and a
	 * power of two.
	 * 
	 * @param sampleRate
	 * @param windowSize
	 * @param numberCoefficients
	 * @param minFreq
	 * @param maxFreq
	 * @param numberFilters
	 */
	public MelFrequencyCC(int sampleRate, int windowSize,
			int numberCoefficients, int minFreq, int maxFreq, int numberFilters) {
		this.sampleRate = sampleRate;
		this.windowSize = windowSize;
		this.numberCoefficients = numberCoefficients;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.numberFilters = numberFilters;

		if (numberCoefficients != numberFilters) {
			throw new IllegalArgumentException(
					"number of mel coefficients must be equal to number of mel filters");
		}

		// derived stuff
		hopSize = windowSize / 2;
		// frequency resolution, one element of the
		// fft will correspond to this many hertz (?)
		baseFreq = sampleRate / windowSize;
		
		buffer = new double[windowSize];

		filterBank = new MelFilterBank(sampleRate, windowSize, minFreq,
				maxFreq, numberFilters);

		fft = new FastFourierTransformer();
		dct = new FastCosineTransformer();

		// experimental: use matrix for DCT operation
		dctMatrix = createDCTMatrix();
	}

	private Matrix createDCTMatrix() {
		// compute constants
		double k = Math.PI / numberFilters;
		double w1 = 1.0 / (Math.sqrt(numberFilters));// 1.0/(Math.sqrt(numberFilters/2));
		double w2 = Math.sqrt(2.0 / numberFilters);// Math.sqrt(2.0/numberFilters)*(Math.sqrt(2.0)/2.0);

		// create new matrix
		Matrix matrix = new Matrix(
				numberCoefficients, numberFilters);

		// generate dct matrix
		for (int i = 0; i < numberCoefficients; i++) {
			for (int j = 0; j < numberFilters; j++) {
				if (i == 0) {
					matrix.set(i, j, w1 * Math.cos(k * i * (j + 0.5d)));
				} else {
					matrix.set(i, j, w2 * Math.cos(k * i * (j + 0.5d)));
				}
			}
		}

		// just index if we are using first coefficient
		if (!useFirstCoefficient) {
			matrix = matrix.getMatrix(1,
					numberCoefficients - 1, 0, numberFilters - 1);
		}

		return matrix;
	}

	public MelFilterBank getFilterBank() {
		return filterBank;
	}

	/**
	 * Processes the whole input array and returns an array of MFCCs for each
	 * window inside the input array. The values in the array have to be
	 * normalized, meaning they have to be in the range [-1.0,1.0].
	 * 
	 * @param input
	 * @return
	 */
	public double[][] processAll(double[] input) {
		double max = FrameUtil.max(input);
		double min = FrameUtil.min(input);
		if (max > 1.0d || min < -1.0d) {
			throw new IllegalArgumentException(
					"input array must be normalized to [-1.0,1.0]");
		}

		if (input.length % hopSize != 0) {
			throw new IllegalArgumentException(
					"input array size must be multiple of (window size/2)");
		}

		int hops = (input.length / hopSize) - 1;
		double[][] result = new double[hops][numberCoefficients];

		for (int i = 0, offset = 0; offset <= input.length - windowSize; i++, offset += hopSize) {
			result[i] = processWindow(input, offset);
		}

		return result;
	}

	/**
	 * Processes a single window of normalized values, starting from offset.
	 * 
	 * @param window
	 * @return
	 */
	public double[] processWindow(double[] window, int offset) {
		if (window.length - offset < windowSize) {
			throw new IllegalArgumentException(
					"window length must be equal window size (" + windowSize
							+ ")");
		}

		// input array
//		double[] buffer = new double[windowSize];
		System.arraycopy(window, offset, buffer, 0, windowSize);

		// apply window if existing
		if (windowFunction != null) {
			windowFunction.applyWindow(buffer);
		}

		// only first half of spectrum is interesting
		int fftLength = (windowSize / 2) + 1;

		// FFT_comirva fft_c = new FFT_comirva(FFT_comirva.FFT_NORMALIZED_POWER,
		// windowSize, FFT_comirva.WND_HAMMING);
		//		
		// double[] buffer2 = new double[windowSize];
		// System.arraycopy(buffer, 0, buffer2, 0, windowSize);
		// fft_c.transform(buffer2, null);

		Complex[] transform = fft.transform(buffer);
		

		// get magnitude spectrum only of first half,
		// ignore redundant spectrum
		double[] magnitude = new double[fftLength];

		for (int i = 0; i < fftLength; i++) {
			// get power spectrum
			magnitude[i] = Math.pow(transform[i].abs() / (1), 2);
		}

		// apply filters
		double[] melFilterOuputs = filterBank.filter(magnitude);

		// logarithmize
		FrameUtil.toDb(melFilterOuputs);

		// DCT
		double[] mfccs = dct.transform(melFilterOuputs);
		

		return mfccs;
	}

	public void setWindowFunction(WindowFunction windowFunction) {
		this.windowFunction = windowFunction;
	}

	public WindowFunction getWindowFunction() {
		return windowFunction;
	}
}
