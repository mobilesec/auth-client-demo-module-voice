package at.fhooe.mcm.smc;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.genetics.CrossoverPolicy;

import com.sun.org.apache.bcel.internal.generic.NEW;

import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;
import at.fhooe.mcm.smc.math.mfcc.MFCC;
import at.fhooe.mcm.smc.math.vq.ClusterUtil;
import at.fhooe.mcm.smc.math.vq.Codebook;
import at.fhooe.mcm.smc.math.vq.KMeans;
import at.fhooe.mcm.smc.wav.WavReader;

/** Main testing tool. */
public class MainTest {

	public static void main(String args[]) throws IOException,
			ClassNotFoundException {
		MainTest mt = new MainTest();
	
		mt.testCrossComparisonMatrix();
		// mt.recalculateCodebooks();
		// mt.testSampleCrossComparison(Constants.SHORT_SAMPLE_NAME);
	}

	public Matrix createCrossComparison(String trainingSample, String verifySample)
			throws IOException, ClassNotFoundException {
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/mobile phone folder structure";
		HashMap<String, String> sampleDirectories = readSampleDirectories(dir);
		Set<String> keySet = sampleDirectories.keySet();

		int count = keySet.size();
		Matrix results = new Matrix(count, count);

		List<String> speakerIdList = new ArrayList<String>(keySet);
		double[][][] trainingMfccs = new double[count][][];
		double[][][] verifyMfccs = new double[count][][];
		Codebook[] codebooks = new Codebook[count];

		for (int i = 0; i < count; i++) {
			String currentDir = sampleDirectories.get(speakerIdList.get(i));
			
			double[] samples = readSamples(currentDir, trainingSample);
			trainingMfccs[i] = calculateMFCCs(samples);
			codebooks[i] = trainCodebook(trainingMfccs[i]);
			writeCodebook(currentDir, codebooks[i]);
			
			samples = readSamples(currentDir, verifySample);
			verifyMfccs[i] = calculateMFCCs(samples);
			// codebooks[i] = readCodebook(currentDir);
		}

		double distortion = 0;
		double sumDistortion = 0;
		double avgDistortion = 0;

		for (int i = 0; i < count; i++) {

			double minDist = Double.MAX_VALUE;
			int minDistIndex = -1;
			for (int j = 0; j < count; j++) {
				distortion = ClusterUtil.calculateAverageDistortion(verifyMfccs[i],
						codebooks[j]);

				sumDistortion += distortion;
				results.set(i, j, distortion);
				if (distortion < minDist) {
					minDistIndex = j;
					minDist = distortion;
				}
			}
			avgDistortion = sumDistortion / count;
			sumDistortion = 0;
			Log
					.d(
							"Min. Distortion for %d is: %7.2f. Avg. Distortion to all: %7.2f. Ratio: %5.3f",
							i, minDist, avgDistortion,
							(minDist / avgDistortion));
		}

		return results;
	}

	public void testCrossComparisonMatrix() throws IOException,
			ClassNotFoundException {
		long start = System.currentTimeMillis();
		Matrix crossComparison = createCrossComparison(Constants.LONG_SAMPLE_NAME, Constants.SHORT_SAMPLE_NAME);
		long elapsed = System.currentTimeMillis() - start;
		int speakerCount = crossComparison.getColumnDimension();
		double sumSameSpeakers = crossComparison.trace();
		double avgSameSpeakers = sumSameSpeakers / speakerCount;

		double sumCrossSpeakers = crossComparison.sum() - sumSameSpeakers;
		double avgCrossSpeakers = sumCrossSpeakers
				/ (speakerCount * speakerCount - speakerCount);
		System.out.println();
		System.out.printf("Total speakers: %d\n", speakerCount);
		System.out.printf("Total time %d ms for cluster size=%d and MFCC=%d \n", elapsed,
				Constants.CLUSTER_COUNT, Constants.COEFFICIENTS);
		System.out.printf("Avg. Distortion for same speaker to self=%.2f\n",
				avgSameSpeakers);
		System.out.printf(
				"Avg. Distortion for speaker to different speaker=%.2f\n",
				avgCrossSpeakers);
		System.out.printf("same/different=%.2f\n",
				(avgSameSpeakers / avgCrossSpeakers));
		// System.out.println();
		crossComparison.print(4, 1);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String dateString = sdf.format(new Date());
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/mobile phone folder structure";
		String fileName = dir + File.separator + "result_" + dateString + ".csv";
		FileWriter fw = new FileWriter(fileName);
		
		for (int i = 0; i < crossComparison.getRowDimension(); i++) {
			for (int j = 0; j < crossComparison.getColumnDimension(); j++) {
				fw.append("" + crossComparison.get(i, j));
				fw.append(",");
			}
			fw.append("\n");
		}
		fw.flush();
		fw.close();
	}

	private void writeCodebook(String directory, Codebook cb) {

		File codebookFile = new File(directory + File.separator
				+ Constants.CODEBOOK_FILE_NAME);
		try {
			Log.d("Writing codebook to "
					+ shortenPath(codebookFile.getAbsolutePath()));
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(codebookFile));
			oos.writeObject(cb);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Codebook makeCodebook(KMeans kmeans) {
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

	private Codebook trainCodebook(double[][] mfccs) {
		FeatureVector pl = createPointList(mfccs);

		KMeans kmeans = new KMeans(Constants.CLUSTER_COUNT, pl,
				Constants.CLUSTER_MAX_ITERATIONS);
		// System.out.println("Starting to create clustering for " + wavFilePath
		// + "... ");
		kmeans.run();
		Codebook cb = makeCodebook(kmeans);
		return cb;
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

	private FeatureVector readFeatureVectorFile(String directory)
			throws FileNotFoundException, IOException, ClassNotFoundException {
		File file = new File(directory + File.separator
				+ Constants.MFCC_FILE_NAME);
		// Log.d("Trying to read feature vector from " +
		// shortenPath(file.getAbsolutePath()));
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
		FeatureVector fv = (FeatureVector) ois.readObject();
		return fv;
	}

	public void testSampleCrossComparison(String sampleFileName)
			throws IOException {
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/mobile phone folder structure";

		HashMap<String, String> sampleDirectories = readSampleDirectories(dir);
		Set<String> keySet = sampleDirectories.keySet();

		int count = keySet.size();
		Matrix results = new Matrix(count, count);

		for (String speakerId : keySet) {
			double[] sampleValues = readSamples(sampleDirectories
					.get(speakerId), sampleFileName);
			double[][] mfcc = calculateMFCCs(sampleValues);

			String minDistanceSampleId = null;
			double minAverageDistortion = Double.MAX_VALUE;
			double sumDistortion = 0;
			double avgDistortion = 0;

			int codebookCount = keySet.size();

			for (String sampleId : keySet) {
				Codebook cb = readCodebook(sampleDirectories.get(sampleId));
				if (cb != null) {
					double averageDistortion = ClusterUtil
							.calculateAverageDistortion(mfcc, cb);
					sumDistortion += averageDistortion;
					Log.d("Calculated avg distortion=%f", averageDistortion);
					if (averageDistortion < minAverageDistortion) {
						minAverageDistortion = averageDistortion;
						minDistanceSampleId = sampleId;
					}
				}
			}
			avgDistortion = (sumDistortion) / (codebookCount);
			Log.d("******\nFound minimum codebook distance for speaker: %s",
					speakerId);
			Log
					.d(
							"Least codebook distance for: %s\n  Distance: %.2f\n  Average Distance %.2f",
							minDistanceSampleId, minAverageDistortion,
							avgDistortion);
		}
	}

	private double[][] calculateMFCCs(double[] sampleValues) {
		MFCC mfcc2 = new MFCC(Constants.SAMPLERATE, Constants.WINDOWSIZE,
				Constants.COEFFICIENTS, false, Constants.MINFREQ + 1,
				Constants.MAXFREQ, Constants.FILTERS);

		// Log.d("Starting to calculate MFCC");

		int hopSize = Constants.WINDOWSIZE / 2;
		int mfccCount = (sampleValues.length / hopSize) - 1;
		double[][] mfcc = new double[mfccCount][Constants.COEFFICIENTS];

		long start = System.currentTimeMillis();

		for (int i = 0, pos = 0; pos < sampleValues.length - hopSize; i++, pos += hopSize) {
			mfcc[i] = mfcc2.processWindow(sampleValues, pos);
		}

		// Log.d("Calculated " + mfcc.length + " vectors of MFCCs in "
		// + (System.currentTimeMillis() - start) + "ms");
		return mfcc;
	}

	private Codebook readCodebook(String speakerDirPath) {
		Codebook codebook = null;
		try {
			speakerDirPath += File.separator + Constants.CODEBOOK_FILE_NAME;
			// Log.d("Trying to read codebook from %s",
			// shortenPath(speakerDirPath));
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					speakerDirPath));
			codebook = (Codebook) ois.readObject();
		} catch (StreamCorruptedException e) {
			Log.e("Error reading codebook from path %s", speakerDirPath);
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			Log.e("Error reading codebook from path %s", speakerDirPath);
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("Error reading codebook from path %s", speakerDirPath);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Log.e("Error reading codebook from path %s", speakerDirPath);
			e.printStackTrace();
		}
		return codebook;
	}

	private HashMap<String, String> readSampleDirectories(String parentDir) {
		File outputDirectory = new File(parentDir);
		boolean exists = outputDirectory.exists();
		if (!exists) {
			Log.e("Output dir %s doesn't exist, can't verify anything",
					outputDirectory);
			return null;
		}
		File[] subDirs = outputDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory()
						&& !pathname.getName().startsWith(".");
			}
		});
		HashMap<String, String> result = new HashMap<String, String>();
		for (File dir : subDirs) {
			String name = dir.getName();
			String path = dir.getAbsolutePath();
			// Log.d("Found sample directory for name %s at %s", name, path);
			result.put(name, path);
		}
		return result;
	}

	private double[] readSamples(String speakerDirPath, String sampleFileName)
			throws IOException {
		String sampleFilePath = speakerDirPath + File.separator
				+ sampleFileName;
		WavReader reader = new WavReader(sampleFilePath);
		int sampleSize = reader.getFrameSize();
		int sampleCount = reader.getPayloadLength() / sampleSize;
		byte[] buffer = new byte[sampleSize];

		int windowCount = (int) Math.floor(sampleCount / Constants.WINDOWSIZE);

		// Log.d("Starting to read from file " + shortenPath(sampleFilePath));
		double[] sampleValues = new double[windowCount * Constants.WINDOWSIZE];
		for (int i = 0; i < sampleValues.length; i++) {
			reader.read(buffer, 0, sampleSize);
			short sample = 0;
			// hardcoded two bytes here
			short b1 = buffer[0];
			short b2 = buffer[1];
			b2 <<= 8;
			sample = (short) (b1 | b2);
			sampleValues[i] = sample;
			if (i % 1000 == 0) {
			}
		}
		return sampleValues;
	}

	/** Shortens a given file path to the last 2 elements */
	private String shortenPath(String path) {
		String sep = File.separator;
		int lastSepIndex = path.lastIndexOf(sep);
		String newPath = path.substring(0, lastSepIndex);
		lastSepIndex = newPath.lastIndexOf(sep);
		newPath = path.substring(lastSepIndex, path.length());
		return newPath;
	}

	public void recalculateCodebooks() throws IOException {
		String dir = "/Users/thomaskaiser/Documents/MCM/MC480_Project_I/SVN/trunk/Implementation/ProjectFiles/mobile phone folder structure";
		HashMap<String, String> sampleDirectories = readSampleDirectories(dir);
		Set<String> keySet = sampleDirectories.keySet();

		for (String speakerId : keySet) {
			Log.d("Recalculating codebook for %s", speakerId);
			FeatureVector pl = null;
			try {
				pl = readFeatureVectorFile(sampleDirectories.get(speakerId));
			} catch (Exception e) {
				Log.e("Error reading feature vector", e);
			}

			KMeans kmeans = new KMeans(Constants.CLUSTER_COUNT, pl,
					Constants.CLUSTER_MAX_ITERATIONS);
			Log.d("Prepared k means clustering");
			long start = System.currentTimeMillis();
			kmeans.run();
			Log.d("Clustering finished, total time = "
					+ (System.currentTimeMillis() - start) + "ms");

			Codebook cb = makeCodebook(kmeans);

			writeCodebook(sampleDirectories.get(speakerId), cb);
		}
	}
}
