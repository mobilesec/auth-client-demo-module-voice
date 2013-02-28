package at.fhooe.mcm.smc.math.vq;

import java.util.Random;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;

/** Clusters using the kmeans algorithm. 
 * 
 * @author thomaskaiser
 *
 */
public class KMeans {
	private static Random rnd = new Random();

	private int maxIterations;
	private int numberClusters;

	private FeatureVector featureVector;
	private Cluster[] clusters;

	private boolean clustered = false;

	public KMeans(int numberClusters, FeatureVector featureVector,
			int maxIterations) {
		if (numberClusters < 1)
			throw new IllegalArgumentException("Cluster number must be >= 1");
		if (maxIterations < 1)
			throw new IllegalArgumentException("Max iteration must be >= 1");
		if (featureVector == null || featureVector.size() == 0)
			throw new IllegalArgumentException(
					"Feature vector must not be null or empty");

		this.numberClusters = numberClusters;
		this.clusters = new Cluster[numberClusters];
		this.maxIterations = maxIterations;
		this.featureVector = featureVector;

		// select cluster center points
		int[] startPoints = new int[numberClusters];
		for (int i = 0; i < numberClusters;) {
			startPoints[i] = rnd.nextInt(featureVector.size());
			for (int j = 0; j < i; j++) {
				if (startPoints[j] == startPoints[i])
					i--;
			}
			i++;
		}

		for (int i = 0; i < numberClusters; i++)
			clusters[i] = new Cluster(this.featureVector.get(startPoints[i]));
	}

	public void run() {
		double MQE = 0.99d * Double.MAX_VALUE;
		double oldMQE = Double.MAX_VALUE;
		double minDistance;
		int clusterIndex = 0;
		Matrix curPoint;
		int i = 0;
		double diff = oldMQE - MQE;
		double percentage = diff / oldMQE * 100;

		while (MQE < oldMQE && i < maxIterations /* && percentage >= 0.5d */) {
			oldMQE = MQE;
			MQE = 0;

			// adjust cluster center except on first run
			if (i != 0) {
				for (int n = 0; n < numberClusters; n++)
					clusters[n].reset(clusters[n].getMean());
			}

			// compute the new clustering
			for (int k = 0; k < featureVector.size(); k++) {
				curPoint = featureVector.get(k);
				minDistance = Double.MAX_VALUE;

				// compare the point to each cluster center
				for (int j = 0; j < numberClusters; j++) {
					double d = clusters[j].getDistanceFromCenter(curPoint);
					if (d < minDistance) {
						minDistance = d;
						clusterIndex = j;
					}
				}

				// increase mean quantisation error
				MQE += minDistance;
				// add the point to the cluster with minimal distance
				clusters[clusterIndex].add(curPoint);
			}

			i++;
			diff = oldMQE - MQE;
			percentage = diff / oldMQE * 100;
			System.out.println("KMeansClustering: Iteration " + i + "/"
					+ maxIterations + ". diff(MQE/oldMQE) = " + (oldMQE - MQE)
					+ " (" + String.format("%.2f", percentage) + "%)");
		}

		clustered = true;
	}

	public int getNumberClusters() {
		return numberClusters;
	}

	public Matrix getMean(int cluster) {
		checkClustering();

		Matrix y = clusters[cluster].getMean();

		return y;
	}

	public Cluster getCluster(int number) {
		checkClustering();
		return clusters[number];
	}

	public Matrix[] getMeans() {
		checkClustering();

		Matrix[] m = new Matrix[numberClusters];

		for (int i = 0; i < numberClusters; i++)
			m[i] = getMean(i);

		return m;
	}

	private void checkClustering() {
		if (!clustered)
			throw new RuntimeException("there is no clustering yet;");
	}
}
