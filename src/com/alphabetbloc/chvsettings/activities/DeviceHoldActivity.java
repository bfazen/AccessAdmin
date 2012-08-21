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

package com.alphabetbloc.chvsettings.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.Policy;
import com.alphabetbloc.chvsettings.receivers.AirplaneReceiver;

public class DeviceHoldActivity extends Activity implements OnTouchListener {

	protected static final int DEVICE_ADMIN = 1;
	private static final String TAG = "DeviceHoldActivity";
	Policy mPolicy;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPolicy = new Policy(this);
	}

	private static AirplaneReceiver mAirplaneReceiver;
	// slightly added security, prevent exiting activity via calling into device
	protected void startAirplaneMode() {
		mAirplaneReceiver = new AirplaneReceiver();
		IntentFilter airplaneFilter = new IntentFilter(Constants.AIRPLANE_MODE);
		registerReceiver(mAirplaneReceiver, airplaneFilter);
		Log.e(TAG, "registering airplane receiver");
		boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
		if (!enabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
			Intent i = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
			i.putExtra("state", 1);
			sendBroadcast(i);
		}
	}

	protected void stopAirplaneMode() {
		unregisterReceiver(mAirplaneReceiver);
		Log.e(TAG, "registering airplane receiver");
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

		if (!mPolicy.isAdminActive())
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
