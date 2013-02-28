package at.fhooe.mcm.smc.math;

import java.io.IOException;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.others.KMeansClustering;
import at.fhooe.mcm.smc.math.others.MFCC_comirva;
import at.fhooe.mcm.smc.math.others.PointList;
import at.fhooe.mcm.smc.math.others.gmm.CovarianceSingularityException;
import at.fhooe.mcm.smc.math.others.gmm.GaussianMixture;
import at.fhooe.mcm.smc.wav.WavReader;

public class GmmTest {
	public static final int SAMPLERATE = 8000;
	public static final int WINDOWSIZE = 256;
	public static final int MINFREQ = 1;
	public static final int MAXFREQ = SAMPLERATE / 2;
	public static final int FILTERS = 20;
	public static final int COEFFICIENTS = FILTERS - 1;
	public static final int CLUSTER_MAX_ITERATIONS = 20;
	public static final int CLUSTER_COUNT = 96;

	public static void main(String args[]) throws IOException,
			CovarianceSingularityException {
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/samples";
		String tk = dir + "/Tk.wav";
		String tk1 = dir + "/Tk1.wav";
		String you = dir + "/You.wav";
		String maddow = dir + "/maddow.wav";
		String olbermann = dir + "/olbermann.wav";
		String chomsky = dir + "/chomsky.wav";
		String finkelstein = dir + "/finkelstein.wav";

		GmmTest test = new GmmTest();
		System.out.printf("Starting to create GMM for %s\n", tk);
		long start = System.currentTimeMillis();
		GaussianMixture tkGmm = test.createGmm(tk);
		long elapsed = System.currentTimeMillis() - start;
		System.out.printf("Created GMM in %d ms\n", elapsed);
		
		System.out.printf("Starting to create GMM for %s\n", maddow);
		start = System.currentTimeMillis();
		GaussianMixture maddowGmm = test.createGmm(maddow);
		elapsed = System.currentTimeMillis() - start;
		System.out.printf("Created GMM in %d ms\n",  elapsed);

		PointList tkPoints = test.getFeatureVector(tk);
		PointList youPoints = test.getFeatureVector(you);
		PointList maddowPoints = test.getFeatureVector(maddow);

		double p_tk_tk = tkGmm.getLogLikelihood(tkPoints);
		double p_tk_you = tkGmm.getLogLikelihood(youPoints);
		double p_tk_maddow = tkGmm.getLogLikelihood(maddowPoints);

		System.out.printf("Log likelyhood for tk from tk: %.3f\n", p_tk_tk);
		System.out.printf("Log likelyhood for you from tk: %.3f\n", p_tk_you);
		System.out.printf("Log likelyhood for maddow from tk: %.3f\n", p_tk_maddow);
	}

	public GaussianMixture createGmm(String wavFilePath) throws IOException {
		PointList pl = getFeatureVector(wavFilePath);

		KMeansClustering kmeans = new KMeansClustering(CLUSTER_COUNT, pl,
				false, CLUSTER_MAX_ITERATIONS);
		kmeans.run();

		double[] weights = kmeans.getClusterWeights();
		Matrix[] means = kmeans.getMeans();
		Matrix[] covariances = kmeans.getFullCovariances();

		GaussianMixture gmm = new GaussianMixture(weights, means, covariances);
		int attempt = 0;
		int MAX_ATTEMPTS = 3;
//		while (attempt++ < MAX_ATTEMPTS) {
			try {
				gmm.runEM(pl);
//				System.out.printf("GMM training succeeded!\n");
				// break out of loop
//				break;
			} catch (CovarianceSingularityException e) {
//				System.err.printf("Exception occurred in GMM attempt %d, retrying %d times\n", attempt, (MAX_ATTEMPTS - attempt));
//				pl = e.getCorrectedPointList();
			}
//		}

//		// create pointlist from clusters
//		int numberClusters = kmeans.getNumberClusters();
//		Matrix[] centers = new Matrix[numberClusters];
//		double[][] centerCoords = new double[numberClusters][];
//
//		for (int i = 0; i < numberClusters; i++) {
//			centers[i] = kmeans.getCluster(i).getCenter();
//			centerCoords[i] = centers[i].getColumnPackedCopy();
//		}
//		PointList clusterList = new PointList(centers[0].getRowDimension(),
//				numberClusters);
//		for (int i = 0; i < numberClusters; i++) {
//			clusterList.add(centerCoords[i]);
//		}
//		int attempt = 0;
//		while (attempt++ < 3) {
//			try {
//				gmm.runEM(clusterList);
//				System.out.printf("GMM succeeded!\n");
//				// break out of loop
//				break;
//			} catch (CovarianceSingularityException e) {
//				System.out.printf("Exception occurred in GMM attempt %d, retrying\n", attempt);
//				clusterList = e.getCorrectedPointList();
//			}
//		}
		return gmm;
	}

	public PointList getFeatureVector(String wavFilePath) throws IOException {
		double[] samples = readSamples(wavFilePath);
		double[][] mfccs = calculateMFCCs(samples);

		PointList pl = createPointList(mfccs);
		return pl;
	}

	private PointList createPointList(double[][] mfccs) {
		int vectorSize = mfccs[0].length;
		int vectorCount = mfccs.length;
		PointList pl = new PointList(vectorSize, vectorCount);
		for (int i = 0; i < vectorCount; i++) {
			pl.add(mfccs[i]);
		}
		return pl;
	}

	private double[][] calculateMFCCs(double[] sampleValues) {
		MFCC_comirva mfcc2 = new MFCC_comirva(SAMPLERATE, WINDOWSIZE,
				COEFFICIENTS, false, MINFREQ + 1, MAXFREQ, FILTERS);

		int hopSize = WINDOWSIZE / 2;
		int mfccCount = (sampleValues.length / hopSize) - 1;
		double[][] mfcc = new double[mfccCount][COEFFICIENTS];

		for (int i = 0, pos = 0; pos < sampleValues.length - hopSize; i++, pos += hopSize) {
			mfcc[i] = mfcc2.processWindow(sampleValues, pos);
		}

		return mfcc;
	}

	private double[] readSamples(String wavFileName) throws IOException {
		WavReader reader = new WavReader(wavFileName);
		int sampleSize = reader.getFrameSize();
		int sampleCount = reader.getPayloadLength() / sampleSize;
		byte[] buffer = new byte[sampleSize];

		int windowCount = (int) Math.floor(sampleCount / WINDOWSIZE);

		double[] samples = new double[windowCount * WINDOWSIZE];
		for (int i = 0; i < samples.length; i++) {
			reader.read(buffer, 0, sampleSize);
			short sample = 0;
			// hardcoded two bytes here
			short b1 = buffer[0];
			short b2 = buffer[1];
			b2 <<= 8;
			sample = (short) (b1 | b2);
			samples[i] = sample;
		}
		return samples;
	}
}
