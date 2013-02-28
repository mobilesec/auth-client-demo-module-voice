package at.fhooe.mcm.smc.sample;

public class FrameUtil {
	private FrameUtil() {}
	
	/**
	 * Gets the number of samples in a frame of the given length in milliseconds and the given sample rate.
	 * @param msPerFrame
	 * @param sampleRate
	 * @return
	 */
	public static int getFrameSize(int msPerFrame, int sampleRate) {
		float seconds = (float) msPerFrame / 1000;
		return (int) (seconds * sampleRate);
	}

	/**
	 * Calculates the power in a sample as sum of the squares of the absolute values of all samples.
	 * @param sample
	 * @return
	 */
	public static double getPower(double[] sample) {
		double power = 0;
		for (int i = 0; i < sample.length; i++) {
			power += Math.pow(Math.abs(sample[i]), 2);
		}
		return power;
	}
	
	/**
	 * Returns the average power per sample.
	 * @param sample
	 * @return
	 */
	public static double getAveragePower(double[] sample) {
		return getPower(sample) / sample.length;
	}
	
	/**
	 * Normalizes the input array to double values between -1.0 and 1.0.
	 * @param input
	 * @return
	 */
	public static double[] normalize(short[] input) {
		double[] normalizedSamples = new double[input.length];
		
		int max = maxValue(input);
		
		for (int i = 0; i < input.length; i++) {
			normalizedSamples[i] = ((double)input[i]) / max;
		}
		
		return normalizedSamples;
	}
	
	/**
	 * Normalizes the input array to double values between -1.0 and 1.0.
	 * @param input
	 * @return
	 */
	public static double[] normalize(double[] input) {
		double[] normalizedSamples = new double[input.length];
		
		double max = maxValue(input);
		
		for (int i = 0; i < input.length; i++) {
			normalizedSamples[i] = input[i] / max;
		}
		
		return normalizedSamples;
	}

	/**
	 * Calculates max absolute value.
	 * @param input
	 * @return
	 */
	public static short maxValue(short[] input) {
		short max = Short.MIN_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (Math.abs(input[i]) > max) {
				max = (short) Math.abs(input[i]);
			}
		}
		return max;
	}

	/**
	 * Calculates max absolute value.
	 * @param input
	 * @return
	 */
	public static double maxValue(double[] input) {
		double max = Double.MIN_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (Math.abs(input[i]) > max) {
				max = Math.abs(input[i]);
			}
		}
		return max;
	}

	/**
	 * Gets the maximum value in the array.
	 * @param input
	 * @return
	 */
	public static short max(short[] input) {
		short max = Short.MIN_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] > max) {
				max = input[i];
			}
		}
		return max;
	}

	/**
	 * Gets the maximum value in the array.
	 * @param input
	 * @return
	 */
	public static double max(double[] input) {
		double max = - Double.MAX_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] > max) {
				max = input[i];
			}
		}
		return max;
	}

	/**
	 * Gets the minimum value in the array.
	 * @param input
	 * @return
	 */
	public static double min(double[] input) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < input.length; i++) {
			if (input[i] < min) {
				min = input[i];
			}
		}
		return min;
	}
	
	/**
	 * Multiplies the array by a constant factor in place.
	 * @param input
	 * @param factor
	 * @return
	 */
	public static void multiply(double[] input, double factor) {
		for (int i = 0; i < input.length; i++) {
			input[i] = input[i] * factor;
		}
	}
	
	/**
	 * Multiplies each value of the first array with the corresponding
	 * value of the second one.
	 */
	public static double[] multiply(double[] first, double[] second) {
		if (first.length != second.length) {
			throw new IllegalArgumentException("Dimensions don't match! " + first.length + " != " + second.length);
		}
		double[] result = new double[first.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = first[i] * second[i];
		}
		return result;
	}
	
	/**
	 * Adds each value of the first array with the corresponding
	 * value of the second one.
	 */
	public static double[] add(double[] first, double[] second) {
		if (first.length != second.length) {
			throw new IllegalArgumentException("Dimensions don't match! " + first.length + " != " + second.length);
		}
		double[] result = new double[first.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = first[i] + second[i];
		}
		return result;
	}

	
	public static double sum(double[] input, int offset, int len) {
		double sum = 0;
		for (int i = offset; i < offset + len; i++) {
			sum += input[i];
		}
		return sum;
	}
	
	/**
	 * Converts the input array to dB (10*log10())
	 * @param input
	 */
	public static void toDb(double[] input) {
		for (int i = 0; i < input.length; i++) {
			input[i] = 10 * Math.log10(input[i]);
		}
	}
}
