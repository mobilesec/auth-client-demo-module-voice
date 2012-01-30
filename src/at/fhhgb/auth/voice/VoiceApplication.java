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

import android.app.Application;
import android.content.ContentUris;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import at.fhhgb.auth.provider.AuthDb.Mode;

/**
 * This is basically just used to initialize the database on first startup and
 * to provide the ID of the mode when it was inserted.
 * @author thomaskaiser
 *
 */
public class VoiceApplication extends Application {
	private static final String TAG = "VoiceAuth";
	
	private static final String PREF_KEY_MODE_ID = "modeId";

	@Override
	public void onCreate() {
		super.onCreate();
		initAuthMethod();
	}
	
	/**
	 * Returns the unique ID of this authentication mode that was returned when 
	 * this authentication mode was first inserted into the database.
	 * @return
	 */
	public long getModeId() {
		return PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getLong(PREF_KEY_MODE_ID, -1);
	}
	
	private void initAuthMethod() {
		if (!voiceAuthMethodExists()) {
			createAuthMethod();
		}
	}

	private void createAuthMethod() {
		Log.d(TAG, "Creating auth method with values: " + ContentValueDefaults.DEFAULT_CONTENT_VALUES.toString());
		Uri insertedUri = getContentResolver().insert(Mode.CONTENT_URI, ContentValueDefaults.DEFAULT_CONTENT_VALUES);
		long modeId = ContentUris.parseId(insertedUri);
		Log.v(TAG, "Storing modeId in preferences: " + modeId);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.edit().putLong(PREF_KEY_MODE_ID, modeId).commit();
	}

	private boolean voiceAuthMethodExists() {
		String[] projection = {
				Mode.NAME
		};
		String selection = Mode.NAME + " = ?";
		String[] selectionArgs = {
				ContentValueDefaults.UNIQUE_NAME
		};
		Cursor c = getContentResolver().query(Mode.CONTENT_URI, projection, selection, selectionArgs, null);
		
		if (!c.moveToFirst()) 
			return false;
		
		String existingName = c.getString(c.getColumnIndexOrThrow(Mode.NAME));
		
		return ContentValueDefaults.UNIQUE_NAME.equals(existingName);
	}

}
