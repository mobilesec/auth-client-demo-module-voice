package at.fhooe.mcm.smc.math.mfcc;

import at.fhooe.mcm.smc.math.fft.FFT;
import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.window.HammingWindow;

/**
 * Calculates mel frequency cepstral coefficients. 
 * @author thomaskaiser
 *
 */
public class MFCC {
	private int windowSize;
	private int hopSize;
	private float sampleRate;
	private double baseFreq;

	// fields concerning the mel filter banks
	private double minFreq;
	private double maxFreq;
	private int numberFilters;

	// fields concerning the MFCCs settings
	private int numberCoefficients;
	private boolean useFirstCoefficient;

	private double[] buffer;
	private Matrix dctMatrix;
	private Matrix melFilterBanks;
	private FFT fft;

	public MFCC(float sampleRate, int windowSize, int numberCoefficients,
			boolean useFirstCoefficient, double minFreq, double maxFreq,
			int numberFilters) {
		// check for correct window size
		if (windowSize < 32) {
			throw new IllegalArgumentException(
					"window size must be at least 32");
		} else {
			int i = 32;
			while (i < windowSize && i < Integer.MAX_VALUE)
				i = i << 1;

			if (i != windowSize)
				throw new IllegalArgumentException("window size must be 2^n");
		}

		// check sample rate
		sampleRate = Math.round(sampleRate);
		if (sampleRate < 1)
			throw new IllegalArgumentException("sample rate must be at least 1");

		// check numberFilters
		if (numberFilters < 2 || numberFilters > (windowSize / 2) + 1)
			throw new IllegalArgumentException(
					"number filters must be at least 2 and smaller than the nyquist frequency");

		// check numberCoefficients
		if (numberCoefficients < 1 || numberCoefficients >= numberFilters)
			throw new IllegalArgumentException(
					"the number of coefficients must be greater or equal to 1 and samller than the number of filters");

		// check minFreq/maxFreq
		if (minFreq <= 0 || minFreq > maxFreq || maxFreq > 88200.0f)
			throw new IllegalArgumentException(
					"the min. frequency must be greater 0 smaller than the max. frequency, which must be smaller than 88200.0");
		;

		this.sampleRate = sampleRate;
		this.windowSize = windowSize;
		this.hopSize = windowSize / 2; // 50% Overleap
		this.baseFreq = sampleRate / windowSize;

		this.numberCoefficients = numberCoefficients;
		this.useFirstCoefficient = useFirstCoefficient;

		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.numberFilters = numberFilters;

		// create buffers
		buffer = new double[windowSize];

		// store filter weights and DCT matrix due to performance reason
		melFilterBanks = getMelFilterBanks();
		dctMatrix = createDCTMatrix();

		// create power fft object
		fft = new FFT(windowSize, new HammingWindow(windowSize));
	}

	private double[] getMelFilterBankBoundaries(double minFreq, double maxFreq,
			int numberFilters) {
		// create return array
		double[] centers = new double[numberFilters + 2];
		double maxFreqMel, minFreqMel, deltaFreqMel, nextCenterMel;

		// compute mel min./max. frequency
		maxFreqMel = linToMelFreq(maxFreq);
		minFreqMel = linToMelFreq(minFreq);
		deltaFreqMel = (maxFreqMel - minFreqMel) / (numberFilters + 1);

		// create (numberFilters + 2) equidistant points for the triangles
		nextCenterMel = minFreqMel;
		for (int i = 0; i < centers.length; i++) {
			// transform the points back to linear scale
			centers[i] = melToLinFreq(nextCenterMel);
			nextCenterMel += deltaFreqMel;
		}

		// ajust boundaries to exactly fit the given min./max. frequency
		centers[0] = minFreq;
		centers[numberFilters + 1] = maxFreq;

		return centers;
	}

	private Matrix getMelFilterBanks() {
		// get boundaries of the different filters
		double[] boundaries = getMelFilterBankBoundaries(minFreq, maxFreq,
				numberFilters);

		// ignore filters outside of spectrum
		for (int i = 1; i < boundaries.length - 1; i++) {
			if (boundaries[i] > sampleRate / 2) {
				numberFilters = i - 1;
				break;
			}
		}

		// create the filter bank matrix
		double[][] matrix = new double[numberFilters][];

		// fill each row of the filter bank matrix with one triangular mel
		// filter
		for (int i = 1; i <= numberFilters; i++) {
			double[] filter = new double[(windowSize / 2) + 1];

			// for each frequency of the fft
			for (int j = 0; j < filter.length; j++) {
				// compute the filter weight of the current triangular mel
				// filter
				double freq = baseFreq * j;
				filter[j] = getMelFilterWeight(i, freq, boundaries);
			}

			// add the computed mel filter to the filter bank
			matrix[i - 1] = filter;
		}

		// return the filter bank
		return new Matrix(matrix, numberFilters, (windowSize / 2) + 1);
	}

	private double getMelFilterWeight(int filterBank, double freq,
			double[] boundaries) {
		// for most frequencies the filter weight is 0
		double result = 0;

		// compute start- , center- and endpoint as well as the height of the
		// filter
		double start = boundaries[filterBank - 1];
		double center = boundaries[filterBank];
		double end = boundaries[filterBank + 1];
		double height = 2.0d / (end - start);

		// is the frequency within the triangular part of the filter
		if (freq >= start && freq <= end) {
			// depending on frequencys position within the triangle
			if (freq < center) {
				// ...use a ascending linear function
				result = (freq - start) * (height / (center - start));
			} else {
				// ..use a descending linear function
				result = height
						+ ((freq - center) * (-height / (end - center)));
			}
		}

		return result;
	}

	private double linToMelFreq(double inputFreq) {
		return (2595.0 * (Math.log(1.0 + inputFreq / 700.0) / Math.log(10.0)));
	}

	private double melToLinFreq(double inputFreq) {
		return (700.0 * (Math.pow(10.0, (inputFreq / 2595.0)) - 1.0));
	}

	private Matrix createDCTMatrix() {
		// compute constants
		double k = Math.PI / numberFilters;
		double w1 = 1.0 / (Math.sqrt(numberFilters));// 1.0/(Math.sqrt(numberFilters/2));
		double w2 = Math.sqrt(2.0 / numberFilters);// Math.sqrt(2.0/numberFilters)*(Math.sqrt(2.0)/2.0);

		// create new matrix
		Matrix matrix = new Matrix(numberCoefficients, numberFilters);

		// generate dct matrix
		for (int i = 0; i < numberCoefficients; i++) {
			for (int j = 0; j < numberFilters; j++) {
				if (i == 0)
					matrix.set(i, j, w1 * Math.cos(k * i * (j + 0.5d)));
				else
					matrix.set(i, j, w2 * Math.cos(k * i * (j + 0.5d)));
			}
		}

		// ajust index if we are using first coefficient
		if (!useFirstCoefficient)
			matrix = matrix.getMatrix(1, numberCoefficients - 1, 0,
					numberFilters - 1);

		return matrix;
	}

	public double[][] process(double[] input) {
		// check for null
		if (input == null)
			throw new IllegalArgumentException(
					"input data must not be a null value");

		// check for correct array length
		if ((input.length % hopSize) != 0)
			throw new IllegalArgumentException(
					"Input data must be multiple of hop size (windowSize/2).");

		// create return array with appropriate size
		double[][] mfcc = new double[(input.length / hopSize) - 1][numberCoefficients];

		// process each window of this audio segment
		for (int i = 0, pos = 0; pos < input.length - hopSize; i++, pos += hopSize)
			mfcc[i] = processWindow(input, pos);

		return mfcc;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public double[] processWindow(double[] window, int start)
			throws IllegalArgumentException {
		// number of unique coefficients, and the rest are symmetrically
		// redundant
		int fftSize = (windowSize / 2) + 1;

		// check start
		if (start < 0)
			throw new IllegalArgumentException("start must be a positve value");

		// check window size
		if (window == null || window.length - start < windowSize)
			throw new IllegalArgumentException(
					"the given data array must not be a null value and must contain data for one window");

		// just copy to buffer
		for (int j = 0; j < windowSize; j++)
			buffer[j] = window[j + start];

		// perform power fft
		fft.transform(buffer, null);

		// use all coefficient up to the nequist frequency (ceil((fftSize+1)/2))
		Matrix x = new Matrix(buffer, windowSize);
		x = x.getMatrix(0, fftSize - 1, 0, 0); // fftSize-1 is the index of the
												// nyquist frequency

		// apply mel filter banks
		x = melFilterBanks.times(x);

		// to db
		double log10 = 10 * (1 / Math.log(10)); // log for base 10 and scale by
												// factor 10
		x.thrunkAtLowerBoundary(1);
		x.logEquals();
		x.timesEquals(log10);

		// compute DCT
		x = dctMatrix.times(x);

		return x.getColumnPackedCopy();
	}
}