package at.fhooe.mcm.smc.math;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;

import at.fhooe.mcm.smc.sample.FrameUtil;

import junit.framework.TestCase;

public class FrameUtilTest extends TestCase {
	
	public void testMultiply1() {
		double[] testin = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double factor = -2.0d;
		FrameUtil.multiply(testin, factor);
		assertEquals(-12.0d, testin[1]);
	}
	
	public void testMultiplyEach() {
		double[] testin1 = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double[] testin2 = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double[] out = FrameUtil.multiply(testin1, testin2);
		
		assertEquals(36.0d, out[1]);
	}
	
	public void testAdd() {
		double[] testin1 = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double[] testin2 = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double[] out = FrameUtil.add(testin1, testin2);
		
		assertEquals(12.0d, out[1]);
		assertEquals(-20.0d, out[4]);
	}

	public void testMaxValueDouble() {
		double[] testin = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double maxValue = FrameUtil.maxValue(testin);
		assertEquals(10.d, maxValue);
		
		double[] testin2 = { 1.0d, 6.0d, -1.0d, -5.0d, 10.0d };
		maxValue = FrameUtil.maxValue(testin2);
		assertEquals(10.d, maxValue);
	}

	public void testMaxValueShort() {
		short[] testin = { 1, 6, -1, -5, -10 };
		short maxValue = FrameUtil.maxValue(testin);
		assertEquals(10, maxValue);

		short[] testin2 = { 1, 6, -1, -5, -10 };
		maxValue = FrameUtil.maxValue(testin2);
		assertEquals(10, maxValue);
	}

	public void testMaxDouble() {
		double[] testin = { 1.0d, 6.0d, -1.0d, -5.0d, -10.0d };
		double max = FrameUtil.max(testin);
		assertEquals(6.0d, max);
		
		double[] testin2 = { -1.0d, -6.0d, -1.0d, -5.0d, -10.0d };
		max = FrameUtil.max(testin2);
		assertEquals(-1.0d, max);
	}
	
	public void testMaxShort() {
		short[] testin = { 1, 6, -1, -5, -10 };
		short max = FrameUtil.max(testin);
		assertEquals(6, max);

		short[] testin2 = { -1, -6, -1, -5, -10 };
		max = FrameUtil.max(testin2);
		assertEquals(-1, max);
	}

	public void testNormalizer() {
		RandomData r = new RandomDataImpl();
		int size = 10000;
		short[] test = new short[size];

		for (int i = 0; i < size; i++) {
			test[i] = (short) r.nextInt(Short.MIN_VALUE, Short.MAX_VALUE);
		}

		double[] res = FrameUtil.normalize(test);

		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		for (int i = 0; i < res.length; i++) {
			max = res[i] > max ? max = res[i] : max;
			min = res[i] < min ? min = res[i] : min;
		}
		assertTrue(max <= 1.0d);
		assertTrue(min >= -1.0d);
	}

	public void testSampleCalc() {
		int rate = 1000;
		int ms = 1000;
		int frameSize = FrameUtil.getFrameSize(ms, rate);
		assertEquals(1000, frameSize);

		rate = 8000;
		ms = 30;
		frameSize = FrameUtil.getFrameSize(ms, rate);
		assertEquals(240, frameSize);
	}

	public void testPowerZero() {
		int size = 1000;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = 0;
		}
		double result = FrameUtil.getPower(test);
		assertEquals(0d, result);
	}

	public void testPowerOne() {
		int size = 1000;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = 1;
		}
		double result = FrameUtil.getPower(test);
		assertEquals((double) size, result);
	}

	public void testPowerAlternate() {
		int size = 1000;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = i % 2 == 0 ? 1d : -1d;
		}
		double result = FrameUtil.getPower(test);
		assertEquals((double) size, result);
	}

	public void testPowerTwo() {
		int size = 10;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = 2;
		}
		double result = FrameUtil.getPower(test);
		assertEquals(40d, result);
	}

	public void testPowerTwoAlternate() {
		int size = 100;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = i % 2 == 0 ? 2d : -2d;
		}
		double result = FrameUtil.getPower(test);
		assertEquals(400d, result);
	}

	public void testPowerQuarterAlternate() {
		int size = 100;
		double[] test = new double[size];
		double val = 0.5d;
		for (int i = 0; i < size; i++) {
			test[i] = i % 2 == 0 ? val : -val;
		}
		double result = FrameUtil.getPower(test);
		assertEquals(25d, result);
	}

	public void testAvgPower() {
		int size = 100;
		double[] test = new double[size];
		for (int i = 0; i < size; i++) {
			test[i] = i % 2 == 0 ? 2d : -2d;
		}
		double result = FrameUtil.getAveragePower(test);
		assertEquals(4d, result);
	}
}
