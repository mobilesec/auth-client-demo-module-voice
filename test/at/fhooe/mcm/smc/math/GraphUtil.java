package at.fhooe.mcm.smc.math;

import javax.swing.JFrame;

import org.apache.commons.math.complex.Complex;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class GraphUtil {

	public static void showXYChart(double[] values, String title) {
		double[] xValues = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			xValues[i] = i;
		}
		showXYChart(xValues, values, title);
	}
	
	public static void showXYChart(Complex[] values, String title) {
		double[] newValues = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			newValues[i] = values[i].abs();
		}
		showXYChart(newValues, title);
	}
	
	public static void showXYChart(double[] xValues, double[] yValues, String title) {
		XYSeries series = new XYSeries(title);
		for (int i = 0; i < xValues.length; i++) {
			series.add(xValues[i], yValues[i]);
		}
		
		XYSeriesCollection dataset = new XYSeriesCollection();

		dataset.addSeries(series);
		
		JFreeChart chart = ChartFactory.createXYLineChart(title, // Title
				"x-axis", // x-axis Label
				"y-axis", // y-axis Label
				dataset, // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true, // Show Legend
				true, // Use tooltips
				false // Configure chart to generate URLs?
				);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		JFrame frame = new JFrame();
		frame.setSize(600, 400);
		frame.setContentPane(chartPanel);
		frame.setVisible(true);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
