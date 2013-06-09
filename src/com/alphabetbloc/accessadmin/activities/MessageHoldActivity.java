/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.alphabetbloc.accessadmin.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.receivers.AirplaneOffReceiver;

public class MessageHoldActivity extends Activity implements OnTouchListener {

	private static final String TAG = MessageHoldActivity.class.getSimpleName();

	// View type
	public static final String STOP_HOLD = "stop_hold";
	public static final String MESSAGE = "message";
	public static final String SUBMESSAGE = "submessage";
	public static final String ADDITIONAL_INFO = "additional_info";
	public static int sHoldType = 0;
	public static String sMessage = "";
	public static String sSubMessage = "";
	public static String sAdditionalInfo = "";
	public static boolean sPermanentHold = false;

	// Alarm
	private boolean mSoundAlarm = false;
	private MediaPlayer mPlayer;
	private boolean mVolumeUp = false;

	// Network and life cycle
	public static boolean sMessageHoldActive = false;
	private boolean mNetworkOnMode = false;
	private AirplaneOffReceiver mNetworkOnReceiver;

	// Options Menu
	private static final int DEVICE_ADMIN = 1;
	private static final int ADMIN_OPTIONS = 0;
	private static final int RETURN_PERM_HOLD = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// start, stop, or change the message of the activity
		boolean stopHold = getIntent().getBooleanExtra(STOP_HOLD, false);
		if (stopHold) {
			unregisterAirplaneOffReceiver();
			sPermanentHold = false;
			finish();
		} else {
			refreshView();
		}
	}


	private void refreshView() {
		
		// create the correct view
		sHoldType = getIntent().getIntExtra(Constants.HOLD_TYPE, 0);
		switch (sHoldType) {
		case Constants.SIM_ERROR:
		case Constants.DEVICE_LOCKED:
			sPermanentHold = true;
			mSoundAlarm = true;
			setContentView(R.layout.hold_alarm);
			break;
		case Constants.ADMIN_MESSAGE:
		default:
			sPermanentHold = false;
			mSoundAlarm = false;
			setContentView(R.layout.admin_message);
			break;
		}

		// Set the text content
		// Message
		sMessage = getIntent().getStringExtra(MESSAGE);
		TextView message = (TextView) findViewById(R.id.message);
		if (sMessage != null)
			message.setText(sMessage);

		// Sub-Message
		sSubMessage = getIntent().getStringExtra(SUBMESSAGE);
		TextView submessage = (TextView) findViewById(R.id.submessage);
		if (sSubMessage != null)
			submessage.setText(sSubMessage);

		// Additional Info
		sAdditionalInfo = getIntent().getStringExtra(ADDITIONAL_INFO);
		RelativeLayout additionalInfo = (RelativeLayout) findViewById(R.id.additional_info);
		TextView additionalMessage = (TextView) findViewById(R.id.additional_message);
		if (sAdditionalInfo != null) {
			additionalInfo.setVisibility(View.VISIBLE);
			additionalMessage.setText(sAdditionalInfo);
		} else if (additionalInfo != null) {
			additionalInfo.setVisibility(View.GONE);
		}
	}

	private void startAlarm() {
		if (mPlayer == null) {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			AudioManager aM = (AudioManager) getSystemService(AUDIO_SERVICE);
			aM.setStreamVolume(AudioManager.STREAM_MUSIC, aM.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
			mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.alarm);
			mPlayer.setLooping(true);
		}
		if (mPlayer.isPlaying())
			return;
		else
			mPlayer.start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Constants.DEBUG) Log.v(TAG, "OnResume Called");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (Constants.DEBUG) Log.v(TAG, "OnPause Called");
	}

	
	
	@Override
	protected void onStart() {
		super.onStart();
		if (Constants.DEBUG) Log.v(TAG, "OnStart Called");
		sMessageHoldActive = true;
		registerAirplaneOffReceiver();
		if (mSoundAlarm)
			startAlarm();	
	}

	@Override
	protected void onStop() {
		super.onDestroy();
		sMessageHoldActive = false;
		unregisterAirplaneOffReceiver();
		if (sPermanentHold) {
			Intent i = new Intent(getApplicationContext(), MessageHoldActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(Constants.HOLD_TYPE, sHoldType);
			i.putExtra(MessageHoldActivity.MESSAGE, sMessage);
			i.putExtra(MessageHoldActivity.SUBMESSAGE, sSubMessage);
			i.putExtra(MessageHoldActivity.ADDITIONAL_INFO, sAdditionalInfo);
			getApplicationContext().startActivity(i);
		}
		stopAlarm();
	}

	private void stopAlarm() {
		if (mPlayer != null) {
			if (mPlayer.isPlaying()) {
				try {
					mPlayer.stop();
				} catch (Exception e) {
					Log.e(TAG, "MediaPlayer already stopped.");
				}
			}

			mPlayer.release();
			mPlayer = null;
		}
	}

	private void registerAirplaneOffReceiver() {
		if (mNetworkOnMode)
			return;

		mNetworkOnMode = true;
		mNetworkOnReceiver = new AirplaneOffReceiver();
		IntentFilter airplaneFilter = new IntentFilter(Constants.AIRPLANE_MODE);
		registerReceiver(mNetworkOnReceiver, airplaneFilter);
		if (Constants.DEBUG)
			Log.v(TAG, "registering airplane receiver");

		boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (enabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
			Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			i.putExtra("state", 0);
			sendBroadcast(i);
		}

	}

	private void unregisterAirplaneOffReceiver() {
		if (mNetworkOnMode) {
			mNetworkOnMode = false;
			unregisterReceiver(mNetworkOnReceiver);
			Log.v(TAG, "unregistering airplane receiver");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, DEVICE_ADMIN, 0, getString(R.string.device_admin)).setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showMenu = prefs.getBoolean(Constants.SHOW_MENU, false);
		Policy policy = new Policy(this);

		if (!policy.isAdminActive() || !showMenu || !mVolumeUp)
			return false;
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case DEVICE_ADMIN:
			Intent i = new Intent(getApplicationContext(), AdminLoginActivity.class);
			if (sPermanentHold) {
				sPermanentHold = false;
				startActivityForResult(i, RETURN_PERM_HOLD);
			} else
				startActivityForResult(i, ADMIN_OPTIONS);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RETURN_PERM_HOLD)
			sPermanentHold = true;
	}

	// Consume all UI events except Menu, Exit and Pwd Btns
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// NB: Override this in all child activities
		return false;
	}

	@Override
	public void onBackPressed() {
		return;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			event.startTracking();
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			mVolumeUp = true;
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			openOptionsMenu();
			return true;
		}

		if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
			mVolumeUp = false;
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_HOME || event.getKeyCode() == KeyEvent.KEYCODE_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent ev) {
		return true;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return true;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		return true;
	}

}
