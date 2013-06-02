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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.Policy;
import com.alphabetbloc.accessadmin.receivers.AirplaneOnReceiver;

public class DeviceHoldActivity extends Activity implements OnTouchListener {

	protected static final int DEVICE_ADMIN = 1;
	private static final String TAG = "DeviceHoldActivity";
	private boolean mAirplaneMode = false;
	private AirplaneOnReceiver mAirplaneOnReceiver;

	// slightly added security, prevent exiting activity via calling into device
	protected void startAirplaneMode() {
		mAirplaneMode = true;
	}

	private void registerAirplaneOnReceiver() {
		mAirplaneOnReceiver = new AirplaneOnReceiver();
		IntentFilter airplaneFilter = new IntentFilter(Constants.AIRPLANE_MODE);
		registerReceiver(mAirplaneOnReceiver, airplaneFilter);
		if(Constants.DEBUG) Log.v(TAG, "registering airplane receiver");
		boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (!enabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
			Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			i.putExtra("state", 1);
			sendBroadcast(i);
		}
	}

	protected void stopAirplaneMode() {
		if (mAirplaneMode) {
			mAirplaneMode = false;

			unregisterReceiver(mAirplaneOnReceiver);
			if(Constants.DEBUG) Log.v(TAG, "unregistering airplane receiver");
			boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
			if (enabled) {
				Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
				Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				i.putExtra("state", 0);
				sendBroadcast(i);
			}
		}
		// else we must have already stopped airplane mode
	}

	private void unregisterAirplaneReceiver() {
		unregisterReceiver(mAirplaneOnReceiver);
		Log.v(TAG, "unregistering airplane receiver");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAirplaneMode)
			unregisterAirplaneReceiver();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mAirplaneMode)
			registerAirplaneOnReceiver();
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
