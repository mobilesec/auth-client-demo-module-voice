package at.fhooe.mcm.smc.math.vq;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;
/**
 * Cluster utility functions. 
 * @author thomaskaiser
 *
 */
public class ClusterUtil {

	/**
	 * Calculates average distortion between a vector of MFCCs and a codebook. 
	 * @param mfcc
	 * @param cb
	 * @return
	 */
	public static double calculateAverageDistortion(double[][] mfcc, Codebook cb) {
		int T = mfcc.length;
		double factor = 1d / (double) T;
		Matrix[] clusterCenters = cb.getCentroids();
		
		double sumDistortion = 0;
		for (int i = 0; i < T; i++) {
			sumDistortion += minDistance(mfcc[i], clusterCenters);
		}
		
		return factor * sumDistortion;
	}
	
	public static double calculateAverageDistortion(FeatureVector featureVector, Codebook cb) {
		int T = featureVector.size();
		double factor = 1d / (double) T;
		Matrix[] clusterCenters = cb.getCentroids();
		
		double sumDistortion = 0;
		for (int i = 0; i < T; i++) {
			double[] buffer = new double[featureVector.getDimension()];
			Matrix matrix = featureVector.get(i);
			buffer = matrix.getColumnPackedCopy();
			
			sumDistortion += minDistance(buffer, clusterCenters);
		}
		
		return factor * sumDistortion;
	}

	/**
	 * Multidimensional distance calculation.
	 * @param from
	 * @param to
	 * @return
	 */
	private static double minDistance(double[] from, Matrix[] to) {
		
		double temp;
		Matrix fromMatrix = new Matrix(from.length, 1);
		for (int i = 0; i < from.length; i++) {
			fromMatrix.set(i, 0, from[i]);
		}

		double minDistance = Double.MAX_VALUE;
		for (int k = 0; k < to.length; k++) {
			temp = euclideanDistance(fromMatrix, to[k]);
			if (temp < minDistance) {
				minDistance = temp;
			}
		}

		return minDistance;
	}

	/**
	 * Helper method.
	 * @param from
	 * @param to
	 * @return
	 */
	private static double euclideanDistance(Matrix from, Matrix to) {
		Matrix diff = from.minus(to);
		return diff.transpose().times(diff).get(0, 0);
	}
}
