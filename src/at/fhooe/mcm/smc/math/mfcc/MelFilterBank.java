package at.fhooe.mcm.smc.math.mfcc;

import at.fhooe.mcm.smc.sample.FrameUtil;

/** Represents a mel filter bank for filtering a power spectrum. */
public class MelFilterBank {

	private int windowSize;
	private float sampleRate;
	private double baseFreq;

	private double minFreq;
	private double maxFreq;
	private int numberFilters;

	private double[][] filterBank;
	private double[] compressedBank;

	/**
	 * Create a filterbank with the given number of filters according to the
	 * given parameters.
	 * 
	 */
	public MelFilterBank(int sampleRate, int windowSize, int minFreq,
			int maxFreq, int filterCount) {
		this.windowSize = windowSize;
		this.sampleRate = sampleRate;
		this.minFreq = minFreq;
		this.maxFreq = maxFreq;
		this.numberFilters = filterCount;

		// frequency resolution, one element of the
		// fft will correspond to this many hertz (?)
		baseFreq = sampleRate / windowSize;

		filterBank = createFilterBank();
	}

	/**
	 * Returns the whole filter bank.
	 * 
	 * @return
	 */
	public double[][] getFilterBank() {
		return filterBank;
	}
	
	/**
	 * Returns a specific filter.
	 * @param position
	 * @return
	 */
	public double[] getFilter(int position) {
		return filterBank[position];
	}
	
	/**
	 * This will compress the whole filter bank into a single filter of
	 * size {@link #windowSize}, so a FFT spectrum of the same size can be multiplied
	 * with the whole filter bank at once easily.
	 * @return
	 */
	public double[] compressFilters() {
		int filterSize = windowSize / 2 + 1;
		double[] compressed = new double[filterSize];
		
		for (int i = 0; i < filterSize; i++) {
			double sum = 0;
			for (int currentFilter = 0; currentFilter < numberFilters; currentFilter++) {
				sum += filterBank[currentFilter][i];
			}
			compressed[i] = sum;
		}
		return compressed;
	}
	
	/**
	 * Compresses all filters and multiplies the given input array with all filters at once.
	 * @param input
	 * @return
	 */
	public double[] multiplyCompressed(double[] input) {
		if (compressedBank == null) {
			compressedBank = compressFilters();
		}
		return FrameUtil.multiply(compressedBank, input);
	}
	
	/**
	 * Gives the output of each filter when multiplied with the given input.
	 * @param input
	 * @return The output of each filter as single double value.
	 */
	public double[] filter(double[] input) {
		double[] result = new double[numberFilters];
		for (int i = 0; i < result.length; i++) {
			// filter with current filter
			double[] filtered = FrameUtil.multiply(filterBank[i], input);
			double sum = FrameUtil.sum(filtered, 0, filtered.length);
			result[i] = sum;
		}
		return result;
	}

	/**
	 * Creates the filter bank: for each filter a triangular shape on the
	 * spectrum is generated.
	 * 
	 * @return
	 */
	private double[][] createFilterBank() {
		// nyquist freq
		double maxF = sampleRate / 2;
		// nyquist in mel scale
		double maxMelFreq = linearToMel(maxF);
		
		// frequency resolution in mel scale basically,
		// filters are spaced equally on mel scale between zero and maxMelFreq
		double melFreqIncrement = maxMelFreq / (numberFilters + 1);
		
		double[] melCenters = new double[numberFilters + 2];
		double[] linearCenters = new double[numberFilters + 2];
		
		// create (numberFilters + 2) equidistant points for the triangles
		double nextCenterMel = 0;
		for (int i = 0; i < melCenters.length; i++) {
			// transform the points back to linear scale
			melCenters[i] = nextCenterMel;
			linearCenters[i] = melToLinear(nextCenterMel);
			nextCenterMel += melFreqIncrement;
		}

		// ajust boundaries to exactly fit the given min./max. frequency
		linearCenters[0] = minFreq;
		linearCenters[numberFilters + 1] = maxFreq;
		
		// create the filter bank matrix
		double[][] matrix = new double[numberFilters][];

		for (int i = 1; i <= numberFilters; i++) {
			// the spectrum will consist of windowSize/2 elements
			// because of the redundancy of the spectrum
			double[] filter = new double[(windowSize / 2) + 1];

			// for each frequency of the fft
			for (int j = 0; j < filter.length; j++) {
				// compute the filter weight of the current triangular mel
				// filter
				double freq = baseFreq * j;
				filter[j] = getMelFilterWeight(i, freq, linearCenters);
			}
			
			// normalize the filter: area has to be 1
			double sum = FrameUtil.sum(filter, 0, filter.length);
			FrameUtil.multiply(filter, 1 / sum);

			// add the computed mel filter to the filter bank
			matrix[i - 1] = filter;
		}
		

		return matrix;
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

	public static double linearToMel(double inputFreq) {
		return (2595.0 * (Math.log(1.0 + inputFreq / 700.0) / Math.log(10.0)));
	}

	public static double melToLinear(double inputFreq) {
		return (700.0 * (Math.pow(10.0, (inputFreq / 2595.0)) - 1.0));
	}
}
