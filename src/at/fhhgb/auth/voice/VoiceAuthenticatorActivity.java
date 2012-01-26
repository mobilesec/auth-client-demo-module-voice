package at.fhhgb.auth.voice;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import at.fhhgb.auth.lib.intent.IntentIntegrator.Extras;
import at.fhhgb.auth.lib.util.UIUtils;
import at.fhhgb.auth.provider.AuthDb.Feature;
import at.fhhgb.auth.provider.AuthDb.Subject;
import at.fhooe.mcm.smc.Constants;
import at.fhooe.mcm.smc.math.mfcc.FeatureVector;
import at.fhooe.mcm.smc.math.mfcc.MFCC;
import at.fhooe.mcm.smc.math.vq.ClusterUtil;
import at.fhooe.mcm.smc.math.vq.Codebook;
import at.fhooe.mcm.smc.wav.WavReader;
import at.fhooe.mcm.smc.wav.WaveRecorder;

import com.google.gson.Gson;

public class VoiceAuthenticatorActivity extends Activity implements OnClickListener {
	private static final int RECORDING_DURATION = 2500;
	private static final int UI_REFRESH_TIME = 250;
	public static final String TAG = "VoiceAuth";
	private static final double THRESHOLD = 10000;
	
	private Uri userUri;

    private ProgressBar progressBar;
    private Button btnStartRecording;
    private Button btnCheck;
	
	private WaveRecorder waveRecorder;
	private long lastRecordStartTime;
	
	public FeatureVector userFeatureVector;
    
    private static File outputFile = 
			new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), 
					"recording.wav");
	
	private Handler updateUiHandler = new Handler() {
		public void handleMessage(Message msg) {
			int elapsed = (int) (System.currentTimeMillis() - lastRecordStartTime);
			if (elapsed < RECORDING_DURATION) {
				progressBar.setProgress(elapsed);
				sendEmptyMessageDelayed(0, UI_REFRESH_TIME);
			} else {
				waveRecorder.stop();
				progressBar.setProgress(progressBar.getMax());
				new MfccTask(VoiceAuthenticatorActivity.this).execute(outputFile.getAbsolutePath());
			}
		}
	};

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voice_auth); 
        
        userUri = getIntent().getData();
        
        // this just for testing
        if (userUri == null) {
        	finish();
			return;
		}
		if (!checkUserStillExists()) {
			UIUtils.showErrorDialog(
					this,
					"User deleted",
					"The requested user does not exist anymore, authentication will not be possible!",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			return;
		}
        checkFeaturesExist();
    }

	private boolean checkUserStillExists() {
		Cursor c = managedQuery(userUri, null, null, null, null);
		return c.getCount() > 0;
	}

	private void checkFeaturesExist() {
		long userId = ContentUris.parseId(userUri);
		long modeId = ((VoiceApplication) getApplication()).getModeId();
		Uri featuresUri = Feature.buildFeaturesForSubjectAndMode(userId, modeId);
		Cursor c = managedQuery(featuresUri, null, null, null, null);
		if (c.getCount() == 0) {
			askToCreateFeatures();
		} else {
			setupUi();
		}
	}

	private void askToCreateFeatures() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setTitle("Unknown user");
		builder.setMessage("No voice features for this user yet, do you want to record features now?");
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(VoiceAuthenticatorActivity.this, CreateVoiceSample.class);
				startActivity(intent);
				finish();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		builder.show();
	}

	/**
	 * Loads user info from the content resolver and sets up button listener.
	 */
	private void setupUi() {
		TextView claimedName = (TextView) findViewById(R.id.txt_claim);
		fillNameFields(claimedName);
		btnStartRecording = (Button) findViewById(R.id.start_recording);
		btnCheck = (Button) findViewById(R.id.btn_check_results);
		progressBar = (ProgressBar) findViewById(R.id.progress);
		
		progressBar.setMax(RECORDING_DURATION);
	}

	private void fillNameFields(TextView claimedName) {
		Cursor cursor = managedQuery(userUri, null, null, null, null);
		if (cursor.moveToFirst()) {
			String firstName = cursor.getString(cursor.getColumnIndex(Subject.FIRST_NAME));
			String lastName = cursor.getString(cursor.getColumnIndex(Subject.LAST_NAME));
			
			claimedName.setText(lastName + ", " + firstName);
		} else {
			claimedName.setText("Error: Could not load user!");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_recording:
			startRecording();
			break;
		case R.id.btn_check_results:
			checkResults();
			break;
		default:
			break;
		}
	}

	private void startRecording() {
		if (outputFile.exists()) outputFile.delete();
		btnStartRecording.setEnabled(false);
		
		waveRecorder = new WaveRecorder(8000);
		waveRecorder.setOutputFile(outputFile.getAbsolutePath());
		waveRecorder.prepare();
		waveRecorder.start();
		lastRecordStartTime = System.currentTimeMillis();
		updateUiHandler.sendEmptyMessage(0);
	}

	private void checkResults() {
		Cursor allCodebooksCursor = queryAllCodebooks();
		long claimedUserId = ContentUris.parseId(userUri);
		long bestUserId = -1;
		Log.i(TAG, "Starting to check voice features for userId=" + claimedUserId);
		
		double minAverageDistortion = Double.MAX_VALUE;
		if (allCodebooksCursor.moveToFirst()) {
			do {
				long currentUserId = allCodebooksCursor.getLong(allCodebooksCursor.getColumnIndexOrThrow(Feature._ID));
				Codebook codebook = getCodebookForUser(allCodebooksCursor);
				double averageDistortion = ClusterUtil.calculateAverageDistortion(
						userFeatureVector, codebook);
				
				Log.d(TAG, "Calculated avg distortion for userId " + currentUserId + " =" + averageDistortion);
				if (averageDistortion < minAverageDistortion) {
					minAverageDistortion = averageDistortion;
					bestUserId = currentUserId;
				}
			} while (allCodebooksCursor.moveToNext());
		}
		
		if (minAverageDistortion <= THRESHOLD && claimedUserId == bestUserId) {
			returnSuccess(minAverageDistortion);
		} else {
			returnFailure();
		}
	}

	private Codebook getCodebookForUser(Cursor cursor) {
		Gson gson = new Gson();
		String representation = cursor.getString(cursor
				.getColumnIndexOrThrow(Feature.REPRESENTATION));
		Codebook codebook = gson.fromJson(representation,
				Codebook.class);
		return codebook;
	}

	private Cursor queryAllCodebooks() {
		long modeId = ((VoiceApplication) getApplication()).getModeId();
		Uri uri = Feature.buildFeaturesForMode(modeId);
		String[] columns = {
				Feature.SUBJECT_ID,
				Feature.REPRESENTATION
		};
		Cursor cursor = managedQuery(uri, columns, null, null, null);
		return cursor;
	}

	private void returnFailure() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra(Extras.EXTRA_RESULT, false);
		resultIntent.putExtra(Extras.EXTRA_RESULT_CONFIDENCE, 0.0d);
		setResult(RESULT_OK, resultIntent);
		finish();
	}

	private void returnSuccess(double minAverageDistortion) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra(Extras.EXTRA_RESULT, true);
		resultIntent.putExtra(Extras.EXTRA_RESULT_CONFIDENCE, minAverageDistortion);
		setResult(RESULT_OK, resultIntent);
		finish();
	}
	
	class MfccTask extends AsyncTask<String, Object, FeatureVector> {

		private ProgressDialog progressDialog;
		private final Activity parentActivity;
		
		public MfccTask(Activity parentActivity) {
			this.parentActivity = parentActivity;
		}

		@Override
		protected FeatureVector doInBackground(String... params) {
			String filename = params[0];
			WavReader wavReader = new WavReader(filename);
			
			Log.i(TAG, "Starting to read from file " + filename);
			double[] samples = readSamples(wavReader);

			Log.i(TAG, "Starting to calculate MFCC");
			double[][] mfcc = calculateMfcc(samples);
			
			FeatureVector pl = createFeatureVector(mfcc);
			
			return pl;
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
		protected void onPostExecute(FeatureVector result) {
			progressDialog.dismiss();
			userFeatureVector = result;
			btnCheck.setEnabled(true);
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