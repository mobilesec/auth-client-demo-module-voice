package at.fhooe.mcm.smc.math;

import junit.framework.TestCase;
import at.fhooe.mcm.smc.math.window.HammingWindow;
import at.fhooe.mcm.smc.math.window.WindowFunction;

public class HammingWindowTest extends TestCase {

	public void testSimpleWindow1() {
		int size = 100;
		double[] testin = new double[size];
		for (int i = 0; i < size; i++) {
			testin[i] = 1;
		}
		WindowFunction hw = new HammingWindow(size);
		hw.applyWindow(testin);
		// check a single value
		double f_6 = (1 - 0.46d) - (0.46d * Math.cos( (2*Math.PI*5) / (size-1)));
		assertEquals(f_6, testin[5]);
	}
	
	public void testSimpleWindow2() {
		int size = 100;
		double[] testin = new double[size];
		for (int i = 0; i < size; i++) {
			testin[i] = 1;
		}
		WindowFunction hw = new HammingWindow(size);
		double[] out = hw.getWindowedFrame(testin);
		// check a single value
		double f_6 = (1 - 0.46d) - (0.46d * Math.cos( (2*Math.PI*5) / (size-1)));
		
		assertEquals(f_6, out[5]);
	}
}
