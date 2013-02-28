package at.fhooe.mcm.smc;

import java.io.File;

/** Contains constants that are used in multiple other places. */
public final class Constants {

	public static final int SAMPLERATE = 8000;
	public static final String SD_SUBDIRNAME = "SCM";

	public static final int WINDOWSIZE = 512;
	public static final int MINFREQ = 1;
	public static final int MAXFREQ = SAMPLERATE / 2;
	public static final int FILTERS = 15;
	public static final int COEFFICIENTS = FILTERS - 1;
	public static final String SAMPLE_NAME = "sample.wav";
	public static final int TRAINING_SAMPLE_DURATION = 45 * 1000;
	public static final String OUTPUTDIR = File.separator + "sdcard"
			+ File.separator + SD_SUBDIRNAME;

	public static final int CLUSTER_MAX_ITERATIONS = 10;
	public static final int CLUSTER_COUNT = 64;

	public static final int LONG_SAMPLE_DURATION = 15 * 1000;
	public static final int SHORT_SAMPLE_DURATION = 5 * 1000;
	public static final int VERIFICATION_SAMPLE_DURATION = 5 * 1000;

	public static final String SHORT_SAMPLE_NAME = "sample_short.wav";
	public static final String LONG_SAMPLE_NAME = "sample_long.wav";

	public static final String MFCC_FILE_NAME = "mfccs.fv";
	public static final String CODEBOOK_FILE_NAME = "codebook.cb";

	public static final String MIC_VERIFY_FILE_NAME = "verify.wav";

}
