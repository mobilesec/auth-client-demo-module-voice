package at.fhooe.mcm.smc.math;

import at.fhooe.mcm.smc.math.mfcc.MelFilterBank;
import at.fhooe.mcm.smc.sample.FrameUtil;

public class MelFilterTest {

	int windowSize = 512;
	// int sampleRate = windowSize * 4;
	int sampleRate = 8000;
	int filterCount = 10;
	int minFreq = 20;
	int maxFreq = 4000;

	public MelFilterTest() {
	}

	public void test1() {
		MelFilterBank mfb = new MelFilterBank(sampleRate, windowSize, minFreq,
				maxFreq, filterCount);
		double[][] filterBank = mfb.getFilterBank();

		int filters = filterBank.length;

		double[] xValues = new double[filterBank[0].length];
		for (int i = 0; i < xValues.length; i++) {
			xValues[i] = i * sampleRate / windowSize;
		}

		for (int i = 0; i < filters; i++) {
			GraphUtil.showXYChart(xValues, filterBank[i], "Filter " + i);
		}
	}

	public void testCompressed() {
		MelFilterBank mfb = new MelFilterBank(sampleRate, windowSize, minFreq,
				maxFreq, filterCount);

		double[] filterBank = mfb.compressFilters();

		double[] xValues = new double[filterBank.length];
		for (int i = 0; i < xValues.length; i++) {
			xValues[i] = i * sampleRate / windowSize;
		}

		GraphUtil.showXYChart(xValues, filterBank, "Compressed Filter");

		double[] gleichSpectrum = new double[filterBank.length];

		for (int i = 0; i < gleichSpectrum.length; i++) {
			gleichSpectrum[i] = 1;
		}

		double[] outSpectrum = FrameUtil.multiply(gleichSpectrum, filterBank);

		GraphUtil.showXYChart(xValues, outSpectrum,
				"Filtered spectrum (all at once)");
	}

	public void testSpectrum() {
		MelFilterBank mfb = new MelFilterBank(sampleRate, windowSize, minFreq,
				maxFreq, filterCount);
		double[] gleichSpectrum = new double[mfb.getFilter(0).length];

		for (int i = 0; i < gleichSpectrum.length; i++) {
			gleichSpectrum[i] = 10;
		}

		GraphUtil.showXYChart(gleichSpectrum, "Gleichanteil");

		// multiply each part of the filterbanks with the spectrum
		double[][] filterBank = mfb.getFilterBank();
		double[][] outSpectrum = new double[filterBank.length][];
		double[] finalSpectrum = new double[gleichSpectrum.length];

		for (int i = 0; i < filterBank.length; i++) {
			double[] filter = filterBank[i];

			outSpectrum[i] = FrameUtil.multiply(gleichSpectrum, filter);
			// GraphUtil.showXYChart(outSpectrum[i], i +
			// " Gleichanteil (filtered)");

			finalSpectrum = FrameUtil.add(finalSpectrum, outSpectrum[i]);
		}
		GraphUtil.showXYChart(finalSpectrum,
				"Final spectrum (filtered and added)");
	}

	public static void main(String args[]) {
		MelFilterTest m = new MelFilterTest();
//		m.testSpectrum();
//		m.test1();
		m.testCompressed();
	}
}
