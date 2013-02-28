package at.fhooe.mcm.smc.math;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;
import at.fhooe.mcm.smc.math.mfcc.MFCC;
import at.fhooe.mcm.smc.math.vq.ClusterUtil;
import at.fhooe.mcm.smc.math.vq.Codebook;
import at.fhooe.mcm.smc.math.vq.KMeans;
import at.fhooe.mcm.smc.wav.WavReader;

public class ClusterTest {

	public static final int SAMPLERATE = 8000;
	public static final int WINDOWSIZE = 512;
	public static final int MINFREQ = 1;
	public static final int MAXFREQ = SAMPLERATE / 2;
	public static final int FILTERS = 15;
	public static final int COEFFICIENTS = FILTERS - 1;
	public static final int CLUSTER_MAX_ITERATIONS = 20;
	public static final int CLUSTER_COUNT = 96;

	public static void main(String args[]) throws IOException {
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/samples";
		String tk = dir + "/Tk.wav";
		String tk1 = dir + "/Tk1.wav";
		String tk2 = dir + "/Tk2.wav";
		String you = dir + "/You.wav";
		String maddow = dir + "/maddow.wav";
		String olbermann = dir + "/olbermann.wav";
		String chomsky = dir + "/chomsky.wav";
		String finkelstein = dir + "/finkelstein.wav";

		ClusterTest ct = new ClusterTest();

		// testDistortions(tk, tk1, you, ct);

		long start = System.currentTimeMillis();
		Matrix crossComparison = ct.createCrossComparison(new String[] { tk, /*tk1, tk2,*/
				you, olbermann, maddow, chomsky, finkelstein });
		long elapsed = System.currentTimeMillis() - start;
		
		int speakerCount = crossComparison.getColumnDimension();
		double sumSameSpeakers = crossComparison.trace();
		double avgSameSpeakers = sumSameSpeakers / speakerCount;
		
		double sumCrossSpeakers = crossComparison.sum() - sumSameSpeakers;
		double avgCrossSpeakers = sumCrossSpeakers / (speakerCount*speakerCount - speakerCount);
		System.out.println();
		System.out.printf("Total speakers: %d\n", speakerCount);
		System.out.printf("Total time %d ms for cluster size= %d\n", elapsed, CLUSTER_COUNT);
		System.out.printf("Avg. Distortion for same speaker to self=%.2f\n", avgSameSpeakers);
		System.out.printf("Avg. Distortion for speaker to different speaker=%.2f\n",avgCrossSpeakers);
		System.out.printf("same/different=%.2f\n", (avgSameSpeakers/avgCrossSpeakers));
		System.out.println();
		crossComparison.print(4, 1);
	}
	
	public void saveCodebook(String dir, String name, Codebook codebook) {
		File codebookFile = new File(dir + File.separator + name);
		try {
//			System.out.println("Writing codebook to " + codebookFile);
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(codebookFile));
			oos.writeObject(codebook);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Matrix createCrossComparison(String[] wavFiles) throws IOException {
		int count = wavFiles.length;
		Matrix results = new Matrix(count, count);
		double[][][] mfccs = new double[count][][];
		Codebook[] codebooks = new Codebook[count];

		for (int i = 0; i < count; i++) {
			mfccs[i] = calculateMFCCs(wavFiles[i]);
			codebooks[i] = createCodebook(wavFiles[i]);
			saveCodebook(wavFiles[i], codebooks[i]);
		}

		double distortion = 0;
		double sumDistortion = 0;
		double avgDistortion = 0;
		
		for (int i = 0; i < count; i++) {
			double minDist = Double.MAX_VALUE;
			int minDistIndex = -1;
			for (int j = 0; j < count; j++) {
				distortion = ClusterUtil.calculateAverageDistortion(mfccs[i],
						codebooks[j]);
				sumDistortion += distortion;
				results.set(i, j, distortion);
				if (distortion < minDist) {
					minDistIndex = j;
					minDist = distortion;
				}
			}
			avgDistortion = sumDistortion / count;
			System.out.println(String.format("Min. Distortion for %d is: %7.2f. Avg. Distortion to all: %7.2f. Ratio: %5.3f", 
					i, minDist, avgDistortion, (minDist / avgDistortion)));
		}

		return results;
	}

	public void saveCodebook(String wavFilePath, Codebook codebook) {
		File wavFile = new File(wavFilePath);
		File directory = wavFile.getParentFile();
		int start = 0;
		String fileNameOnly = wavFile.getName();
		int extensionStartIndex = fileNameOnly.lastIndexOf(".");
		int end = extensionStartIndex;
		
		String fileName = fileNameOnly.substring(start, end);
		fileName = fileName.concat(".codebook");
		saveCodebook(directory.getAbsolutePath(), fileName, codebook);
	}

	public double[][] calculateMFCCs(String wavFilePath) throws IOException {
		double[] samples = readSamples(wavFilePath);
		double[][] mfccs = calculateMFCCs(samples);
		return mfccs;
	}

	public Codebook createCodebook(String wavFilePath) throws IOException {
		double[] samples = readSamples(wavFilePath);
		double[][] mfccs = calculateMFCCs(samples);

		FeatureVector pl = createPointList(mfccs);

		KMeans kmeans = new KMeans(CLUSTER_COUNT, pl, CLUSTER_MAX_ITERATIONS);
//		System.out.println("Starting to create clustering for " + wavFilePath + "... ");
		kmeans.run();

		int numberClusters = kmeans.getNumberClusters();
		Matrix[] centers = new Matrix[numberClusters];
		for (int i = 0; i < numberClusters; i++) {
			centers[i] = kmeans.getCluster(i).getCenter();
		}
		Codebook cb = new Codebook();
		cb.setLength(numberClusters);
		cb.setCentroids(centers);

		return cb;
	}

	public static void testDistortions(String tk, String tk1, String you,
			ClusterTest ct) throws IOException {
		Codebook tkCodebook = ct.createCodebook(tk);
		double[][] tkMfccs = ct.calculateMFCCs(tk);
		double distTk_Tk = ClusterUtil.calculateAverageDistortion(tkMfccs,
				tkCodebook);
	
		Codebook tkCodebook1 = ct.createCodebook(tk1);
		double[][] tkMfccs1 = ct.calculateMFCCs(tk1);
		double distTk1_Tk1 = ClusterUtil.calculateAverageDistortion(tkMfccs1,
				tkCodebook1);
	
		Codebook youCodebook = ct.createCodebook(you);
		double[][] youMfccs = ct.calculateMFCCs(you);
		double distYou_you = ClusterUtil.calculateAverageDistortion(youMfccs,
				youCodebook);
	
		double distTk_you = ClusterUtil.calculateAverageDistortion(tkMfccs,
				youCodebook);
		double distTk_tk1 = ClusterUtil.calculateAverageDistortion(tkMfccs,
				tkCodebook1);
	
		double distYou_tk = ClusterUtil.calculateAverageDistortion(youMfccs,
				tkCodebook);
		double distYou_tk1 = ClusterUtil.calculateAverageDistortion(youMfccs,
				tkCodebook1);
	
		double distTk1_tk = ClusterUtil.calculateAverageDistortion(tkMfccs1,
				tkCodebook);
		double distTk1_you = ClusterUtil.calculateAverageDistortion(tkMfccs1,
				youCodebook);
	
		System.out.println("--- SAME TO SAME ---");
		System.out.println("AvgDistortion tk/tk=" + distTk_Tk);
		System.out.println("AvgDistortion you/you=" + distYou_you);
		System.out.println("AvgDistortion tk1/tk1=" + distTk1_Tk1);
	
		System.out.println(" --- CROSS COMPARISONS ---");
		System.out.println("AvgDistortion tk/you=" + distTk_you);
		System.out.println("AvgDistortion tk/tk1=" + distTk_tk1);
	
		System.out.println();
		System.out.println("AvgDistortion you/tk=" + distYou_tk);
		System.out.println("AvgDistortion you/tk1=" + distYou_tk1);
	
		System.out.println();
		System.out.println("AvgDistortion tk1/tk=" + distTk1_tk);
		System.out.println("AvgDistortion tk1/you=" + distTk1_you);
	}

	private FeatureVector createPointList(double[][] mfccs) {
		int vectorSize = mfccs[0].length;
		int vectorCount = mfccs.length;
		FeatureVector pl = new FeatureVector(vectorSize, vectorCount);
		for (int i = 0; i < vectorCount; i++) {
			pl.add(mfccs[i]);
		}
		return pl;
	}

	private double[][] calculateMFCCs(double[] sampleValues) {
		MFCC mfcc2 = new MFCC(SAMPLERATE, WINDOWSIZE,
				COEFFICIENTS, false, MINFREQ + 1, MAXFREQ, FILTERS);
		
		int hopSize = WINDOWSIZE / 2;
		int mfccCount = (sampleValues.length / hopSize) - 1;
		double[][] mfcc = new double[mfccCount][COEFFICIENTS];
		double[] currentWindow = new double[WINDOWSIZE];
		for (int i = 0, pos = 0; pos < sampleValues.length - hopSize; i++, pos += hopSize) {
			System.arraycopy(sampleValues, pos, currentWindow, 0, WINDOWSIZE);
			mfcc[i] = mfcc2.processWindow(currentWindow, 0);
//			mfcc[i] = mfcc2.processWindow(sampleValues, pos);
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
