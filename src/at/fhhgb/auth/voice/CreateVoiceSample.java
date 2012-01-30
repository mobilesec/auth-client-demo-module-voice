/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.fhhgb.auth.voice;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import at.fhhgb.auth.lib.intent.IntentIntegrator.Extras;
import at.fhhgb.auth.provider.AuthDb.Feature;
import at.fhooe.mcm.smc.Constants;
import at.fhooe.mcm.smc.math.matrix.Matrix;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;
import at.fhooe.mcm.smc.math.mfcc.MFCC;
import at.fhooe.mcm.smc.math.vq.Codebook;
import at.fhooe.mcm.smc.math.vq.KMeans;
import at.fhooe.mcm.smc.wav.WavReader;
import at.fhooe.mcm.smc.wav.WaveRecorder;

import com.google.gson.Gson;

/**
 * Lets the user create a codebook (voice feature), if none exists yet. 
 * @author thomaskaiser
 *
 */
public class CreateVoiceSample extends Activity {
	static final String TAG = "VoiceAuth";

	private static final int RECORDING_DURATION = 5000;
	private static final int UI_REFRESH_TIME = 300;
	
	private long userId;
	
	private Button btnRecord;
	private Button btnCancel;
	private Button btnReset;
	private Button btnCalculate;
	private Button btnSave;
	private ProgressBar progressBar;
	
	private WaveRecorder waveRecorder;
	private long lastRecordStartTime;
	
	/** The recording output file. */
	private static File outputFile = 
			new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), 
					"recording.wav");

	public String codebookString;
	
	/** This is actually used to stop the record after the recording time is up as well. */
	private Handler updateUiHandler = new Handler() {
		public void handleMessage(Message msg) {
			int elapsed = (int) (System.currentTimeMillis() - lastRecordStartTime);
			if (elapsed < RECORDING_DURATION) {
				progressBar.setProgress(elapsed);
				sendEmptyMessageDelayed(0, UI_REFRESH_TIME);
			} else {
				waveRecorder.stop();
				progressBar.setProgress(progressBar.getMax());
				setButtonsEnabled(false, true, false, true, false);
			}
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_voice_sample);
		
		userId = getIntent().getLongExtra(Extras.EXTRA_USER_ID, -1);
		setupUi();
	}
	
	private void setupUi() {
		btnRecord = (Button) findViewById(R.id.btn_record);
		btnCalculate = (Button) findViewById(R.id.btn_calculate);
		btnCancel = (Button) findViewById(R.id.btn_cancel);
		btnReset = (Button) findViewById(R.id.btn_reset);
		btnSave = (Button) findViewById(R.id.btn_save);
		progressBar = (ProgressBar) findViewById(R.id.progress);
		
		progressBar.setMax(RECORDING_DURATION);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_record:
			record();
			break;
		case R.id.btn_calculate:
			calculateMfccs();
			break;
		case R.id.btn_cancel:
			cancelRecording();
			reset();
			break;
		case R.id.btn_reset:
			reset();
			break;
		case R.id.btn_save:
			save();
			break;

		default:
			break;
		}
	}

	private void save() {
		insertFeature(codebookString);
	}

	private void calculateMfccs() {
		setButtonsEnabled(false, false, false, false, false);
		new MfccTask(this).execute(outputFile.getAbsolutePath());
	}

	private void cancelRecording() {
		waveRecorder.stop();
		waveRecorder.release();
		waveRecorder.reset();
		updateUiHandler.removeMessages(0);
	}

	private void record() {
		setButtonsEnabled(false, false, true, false, false);
		
		if (outputFile.exists()) outputFile.delete();
		
		waveRecorder = new WaveRecorder(8000);
		waveRecorder.setOutputFile(outputFile.getAbsolutePath());
		waveRecorder.prepare();
		waveRecorder.start();
		lastRecordStartTime = System.currentTimeMillis();
		updateUiHandler.sendEmptyMessage(0);
	}

	private void reset() {
		setButtonsEnabled(true, false, false, false, false);
		progressBar.setProgress(0);
	}
	
	private void setButtonsEnabled(boolean recEnabled, boolean calcEnabled, 
			boolean cancelEnabled, boolean resetEnabled, boolean saveEnabled) {
		btnRecord.setEnabled(recEnabled);
		btnCalculate.setEnabled(calcEnabled);
		btnCancel.setEnabled(cancelEnabled);
		btnReset.setEnabled(resetEnabled);
		btnSave.setEnabled(saveEnabled);
	}

	private void insertFeature(String password) {
		long modeId = ((VoiceApplication)getApplication()).getModeId();
		
		ContentValues cv = new ContentValues();
		cv.put(Feature.SUBJECT_ID, userId);
		cv.put(Feature.MODE_ID, modeId);
		cv.put(Feature.REPRESENTATION, password);
		Uri insert = getContentResolver().insert(Feature.CONTENT_URI, cv);
		Log.i(TAG, "Inserted voice features to URI: " + insert);
		
		if (ContentUris.parseId(insert) != -1) {
			finish();
		} else {
			Toast.makeText(this, "Could not save features!", Toast.LENGTH_LONG).show();
		}
	}
	
	/**
	 * This is used to get from a filename of a .wav file to
	 * MFCCS, to a cluster of MFCC, to String representation of a 
	 * codebook. 
	 */
	class MfccTask extends AsyncTask<String, Object, String> {

		private ProgressDialog progressDialog;
		private final Activity parentActivity;
		
		public MfccTask(Activity parentActivity) {
			this.parentActivity = parentActivity;
		}

		@Override
		protected String doInBackground(String... params) {
			String filename = params[0];
			WavReader wavReader = new WavReader(filename);
			
			Log.i(TAG, "Starting to read from file " + filename);
			double[] samples = readSamples(wavReader);

			Log.i(TAG, "Starting to calculate MFCC");
			double[][] mfcc = calculateMfcc(samples);
			
			FeatureVector pl = createFeatureVector(mfcc);
			
			KMeans kmeans = doClustering(pl);
			
			Codebook cb = createCodebook(kmeans);
			
			Gson gson = new Gson();
			String codebookJsonString = gson.toJson(cb, Codebook.class);
			
			return codebookJsonString;
		}

		private Codebook createCodebook(KMeans kmeans) {
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

		private KMeans doClustering(FeatureVector pl) {
			long start;
			KMeans kmeans = new KMeans(Constants.CLUSTER_COUNT, pl, Constants.CLUSTER_MAX_ITERATIONS);
			Log.i(TAG, "Prepared k means clustering");
			start = System.currentTimeMillis();
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			kmeans.run();
			Log.i(TAG, "Clustering finished, total time = " + (System.currentTimeMillis() - start) + "ms");
			return kmeans;
		}

		private FeatureVector createFeatureVector(double[][] mfcc) {
			int vectorSize = mfcc[0].length;
			int vectorCount = mfcc.length;
			Log.i(TAG, "Creating pointlist with dimension=" + vectorSize + ", count=" + vectorCount);
			FeatureVector pl = new FeatureVector(vectorSize, vectorCount);
			for (int i = 0; i < vectorCount; i++) {
				pl.add(mfcc[i]);
			}
			Log.d(CreateVoiceSample.TAG, "Added all MFCC vectors to pointlist");
			return pl;
		}

		private short createSample(byte[] buffer) {
			short sample = 0;
			// hardcoded two bytes here
			short b1 = buffer[0];
			short b2 = buffer[1];
			b2 <<= 8;
			sample = (short) (b1 | b2);
			return sample;
		}

		private double[][] calculateMfcc(double[] samples) {
			MFCC mfccCalculator = new MFCC(Constants.SAMPLERATE, Constants.WINDOWSIZE,
					Constants.COEFFICIENTS, false, Constants.MINFREQ + 1, Constants.MAXFREQ, Constants.FILTERS);
			
			int hopSize = Constants.WINDOWSIZE / 2;
			int mfccCount = (samples.length / hopSize) - 1;
			double[][] mfcc = new double[mfccCount][Constants.COEFFICIENTS];
			long start = System.currentTimeMillis();
			for (int i = 0, pos = 0; pos < samples.length - hopSize; i++, pos += hopSize) {
				mfcc[i] = mfccCalculator.processWindow(samples, pos);
				if (i % 20 == 0) {
					publishProgress("Calculating features...", i, mfccCount);
				}
			}
			publishProgress("Calculating features...", mfccCount, mfccCount);

			Log.i(TAG, "Calculated " + mfcc.length + " vectors of MFCCs in "
					+ (System.currentTimeMillis() - start) + "ms");
			return mfcc;
		}

		private double[] readSamples(WavReader wavReader) {
			int sampleSize = wavReader.getFrameSize();
			int sampleCount = wavReader.getPayloadLength() / sampleSize;
			int windowCount = (int) Math.floor(sampleCount / Constants.WINDOWSIZE);
			byte[] buffer = new byte[sampleSize];
			double[] samples = new double[windowCount
			                              * Constants.WINDOWSIZE];
			
			try {
				for (int i = 0; i < samples.length; i++) {
					wavReader.read(buffer, 0, sampleSize);
					samples[i] = createSample(buffer);
					
					if (i % 1000 == 0) {
						publishProgress("Reading samples...", i, samples.length);
					}
				}
			} catch (IOException e) {
				Log.e(CreateVoiceSample.TAG, "Exception in reading samples", e);
			}
			return samples;
		}
		
		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			codebookString = result;
			setButtonsEnabled(false, false, false, true, true);
		}
		
		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(parentActivity);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle("Working...");
			progressDialog.setMessage("Working...");
			progressDialog.setProgress(0);
			progressDialog.setMax(10000);
			progressDialog.show();
		}
		
		@Override
		protected void onProgressUpdate(Object... values) {
			String msg = (String) values[0];
			Integer current = (Integer) values[1];
			Integer max = (Integer) values[2];
			
			progressDialog.setMessage(msg);
			progressDialog.setProgress(current);
			progressDialog.setMax(max);
		}
		
	}
}
