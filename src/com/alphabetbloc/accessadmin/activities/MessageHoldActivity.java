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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.receivers.AirplaneOffReceiver;

public class MessageHoldActivity extends Activity implements OnTouchListener {

	protected static final int DEVICE_ADMIN = 1;
	public static final String STOP_HOLD = "stop_hold";
	private String mToast = "";
	public static boolean sMessageHoldActive = false;
	private static final String TAG = MessageHoldActivity.class.getSimpleName();
	private boolean mNetworkOnMode = false;
	private AirplaneOffReceiver mNetworkOnReceiver;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_hold);
		boolean stopHold = getIntent().getBooleanExtra(STOP_HOLD, false);
		if(stopHold){
			if (mNetworkOnMode) {
				unregisterReceiver(mNetworkOnReceiver);
				mNetworkOnMode = false;
			}
			finish();
		} else {
			mNetworkOnMode = true;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (mNetworkOnMode)
			registerAirplaneOffReceiver();
		sMessageHoldActive = true;
		mToast = getIntent().getStringExtra(Constants.TOAST_MESSAGE);
		TextView tv = (TextView) findViewById(R.id.message);
		tv.setText(mToast);
	}

	
	@Override
	protected void onPause() {
		super.onPause();
		sMessageHoldActive = false;
		if (mNetworkOnMode){
			unregisterReceiver(mNetworkOnReceiver);
			Log.v(TAG, "unregistering airplane receiver");
		}
	}

	private void registerAirplaneOffReceiver() {
		mNetworkOnReceiver = new AirplaneOffReceiver();
		IntentFilter airplaneFilter = new IntentFilter(Constants.AIRPLANE_MODE);
		registerReceiver(mNetworkOnReceiver, airplaneFilter);
		if(Constants.DEBUG) Log.v(TAG, "registering airplane receiver");
		boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (enabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
			Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			i.putExtra("state", 0);
			sendBroadcast(i);
		}
	}
	
	
	// ////// CONSUMES ALL UI EVENTS ////////
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
			if (!policy.isAdminActive() || !showMenu)
				return false;
			return super.onPrepareOptionsMenu(menu);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case DEVICE_ADMIN:
				Intent ip = new Intent(getApplicationContext(), AdminLoginActivity.class);
				startActivity(ip);
				return true;
			default:
				return super.onOptionsItemSelected(item);
			}
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
			if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_BACK) {
				return true;
			}
			return false;
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN || event.getAction() == KeyEvent.ACTION_UP) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_HOME || event.getKeyCode() == KeyEvent.KEYCODE_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
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


