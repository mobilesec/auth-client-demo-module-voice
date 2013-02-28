package at.fhooe.mcm.smc.math;

import java.util.Random;

import at.fhooe.mcm.smc.math.mfcc.MFCC;
import at.fhooe.mcm.smc.sample.FrameUtil;

public class MFCCTest {
	private static final int numberFilters = 9;
	private static final int numberCoefficients = numberFilters;
	private static final int sampleRate = 10 * 1024;
	private static final int windowSize = 1024;
	private double[] sin1;
	private double[] sin2;
	private double[] sin3;

	private double[] gleich;

	private double[] signal;

	protected void setUp() {
		int len = 1 * sampleRate;

		int constant = 1;
		gleich = new double[len];
		for (int i = 0; i < len; i++) {
			gleich[i] = constant;
		}

		int f1 = 50;
		int f2 = 500;
		int f3 = 2000;

		double amp1 = 1;
		double amp2 = 1;
		double amp3 = 1;

		sin1 = new double[len];
		sin2 = new double[len];
		sin3 = new double[len];

		signal = new double[len];

		for (int i = 0; i < len; i++) {
			sin1[i] = amp1 * Math.sin(2 * Math.PI * f1 / len * i);
			sin2[i] = amp2 * Math.sin(2 * Math.PI * f2 / len * i);
			sin3[i] = amp3 * Math.sin(2 * Math.PI * f3 / len * i);

			signal[i] = sin1[i] + sin2[i] + sin3[i];
		}
	}

	public void testWindow() {
		MFCC mfcc = new MFCC(sampleRate, windowSize,
				numberCoefficients, false, 0, sampleRate / 2, numberFilters);

		signal = FrameUtil.normalize(signal);

		double[] frame = new double[windowSize];
		System.arraycopy(signal, 0, frame, 0, windowSize);

		GraphUtil.showXYChart(frame, "combined frame (single window)");

		double[] window = mfcc.processWindow(frame, 0);

		GraphUtil.showXYChart(window, "MFCCs for first window");
	}

	public void testCompareMFCCs() {
		MFCC mfcc = new MFCC(sampleRate, windowSize, numberCoefficients, false, 0,
				sampleRate / 2, numberFilters);
		MFCC mfcc2 = new MFCC(sampleRate, windowSize,
				numberCoefficients - 1, true, 1, sampleRate / 2, numberFilters);
		
		// WindowFunction w = new HammingWindow(windowSize);
		// mfcc.setWindowFunction(w);

		double[] signal2 = new double[signal.length];
		System.arraycopy(signal, 0, signal2, 0, signal.length);
		signal = FrameUtil.normalize(signal);
		
		double[] frame = new double[windowSize];
		double[] frame2 = new double[windowSize];
		System.arraycopy(signal, 0, frame, 0, windowSize);
		System.arraycopy(signal2, 0, frame2, 0, windowSize);
		
		GraphUtil.showXYChart(frame, "combined frame (single window)");
		GraphUtil.showXYChart(frame2, "combined frame2 (single window)");
		
		double[] window = mfcc.processWindow(frame, 0);
		double[] window2 = mfcc2.processWindow(frame2, 0);

		GraphUtil.showXYChart(window, "MFCCs for first window");
		GraphUtil.showXYChart(window2, "MFCCs_comirva for first window");

	}

	public void testWholeSample() {
		MFCC mfcc = new MFCC(sampleRate, windowSize, numberCoefficients, false, 0,
				sampleRate / 2, numberFilters);

		signal = FrameUtil.normalize(signal);
		GraphUtil.showXYChart(signal, "combined signal");

		double[][] processAll = mfcc.process(signal);

		for (int i = 0; i < processAll.length; i++) {
			GraphUtil.showXYChart(processAll[i], "MFCC for frame " + i);
		}

		// double[] xValues = new double[processAll[0].length];
		// for (int i = 0; i < xValues.length; i++) {
		// xValues[i] = i * sampleRate / windowSize;
		// }
		//
		// MelFilterBank filterBank = mfcc.getFilterBank();
		// for (int i = 0; i < 1; i++) {
		// double[] fftSpectrum = processAll[i];
		// GraphUtil.showXYChart(xValues, fftSpectrum, "FFT spectrum at " + i);
		//			
		// double[] melSpectrum = filterBank.multiplyCompressed(fftSpectrum);
		// GraphUtil.showXYChart(xValues, melSpectrum, "mel-spectrum at " + i);
		//
		// double[] melFilterOuputs = filterBank.filter(fftSpectrum);
		// GraphUtil.showXYChart(melFilterOuputs, "sum of mel-spectrum at " +
		// i);
		//			
		// FrameUtil.toDb(melFilterOuputs);
		// GraphUtil.showXYChart(melFilterOuputs, "Log-mel-spectrum at " + i);
		//			
		// }

	}
	
	public void testPerformance() {
		MFCC mfcc = new MFCC(sampleRate, windowSize, numberCoefficients, false, 0,
				sampleRate / 2, numberFilters);
		MFCC mfcc2 = new MFCC(sampleRate, windowSize, numberCoefficients, false, 0,
				sampleRate / 2, numberFilters);
		
		Random r = new Random();
		double[] inDouble = new double[5000 * windowSize];
		double[] inInt = new double[5000 * windowSize];
		for (int i = 0; i < inInt.length; i++) {
			inDouble[i] = r.nextDouble();
			inInt[i] = r.nextInt();
		}
		
		long start = System.currentTimeMillis();
		mfcc.process(inDouble);
		System.out.println("MFCC took " + (System.currentTimeMillis() - start) + "ms");
		
		start = System.currentTimeMillis();
		mfcc2.process(inInt);
		System.out.println("MFCC_comirva took " + (System.currentTimeMillis() - start) + "ms");
	}

	public void testGleich() {
		GraphUtil.showXYChart(gleich, "gleichanteil");
		
		MFCC mfcc = new MFCC(sampleRate, windowSize, numberCoefficients, false, 0,
				sampleRate / 2, numberFilters);

		gleich = FrameUtil.normalize(gleich);
		double[][] processAll = mfcc.process(gleich);

		double[] xValues = new double[processAll[0].length];
		for (int i = 0; i < xValues.length; i++) {
			xValues[i] = i * sampleRate / windowSize;
		}

		for (int i = 0; i < processAll.length; i++) {
			GraphUtil.showXYChart(xValues, processAll[i], "FFT at " + i);
		}
	}

	public static void main(String args[]) {
		MFCCTest test = new MFCCTest();
		test.setUp();
		test.testPerformance();
//		test.testCompareMFCCs();
		// test.testWindow();
//		test.testWholeSample();
	}
}
