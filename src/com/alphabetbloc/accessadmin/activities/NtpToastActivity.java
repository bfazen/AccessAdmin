/*
 * Copyright (C) 2012 Louis Fazen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.alphabetbloc.accessadmin.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;

public class NtpToastActivity extends Activity {

	private static final String TAG = NtpToastActivity.class.getSimpleName();
	public static final String NTP_TIME_REFERENCE = "ntp_time_reference";
	public static final String NTP_TIME = "ntp_time";
	public static final String CHANGE_TIMEZONE = "change_timezone";
	public static final String CHANGE_TIME = "change_time";

	private Context mContext;
	private ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(5);
	// system time computed from NTP server response
	private long mNtpTime = -1;
	// value of SystemClock.elapsedRealtime() corresponding to mNtpTime
	private long mNtpTimeReference = -1;
	private AlertDialog mNtpDialog;
	private boolean mDialogActive = false;
	private boolean mChangeTimeZone = false;
	private boolean mChangeTime = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mNtpTime = getIntent().getLongExtra(NTP_TIME, 0);
		mNtpTimeReference = getIntent().getLongExtra(NTP_TIME_REFERENCE, 0);
		mChangeTimeZone = getIntent().getBooleanExtra(CHANGE_TIMEZONE, true);
		mChangeTime = getIntent().getBooleanExtra(CHANGE_TIME, true);

		if (mNtpTime > 0 && mNtpTimeReference > 0)
			showDynamicTimeDialog();
		else
			showStaticTimeDialog();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDialogActive && mNtpTime > 0 && mNtpTimeReference > 0) {
			updateNtpTime();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (Constants.DEBUG)
			Log.v(TAG, "OnStart Called");
		mDialogActive = true;
	}

	@Override
	protected void onStop() {
		super.onDestroy();
		if (Constants.DEBUG)
			Log.v(TAG, "OnStop Called");
		mDialogActive = false;
	}

	private void showDynamicTimeDialog() {

		mExecutor.schedule(new Runnable() {

			public void run() {
				if (mDialogActive && mNtpTime > 0 && mNtpTimeReference > 0) {

					mExecutor.schedule(this, 120, TimeUnit.SECONDS);
					NtpToastActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							updateNtpTime();
						}
					});
				}
			}

		}, 120, TimeUnit.SECONDS);

		mNtpDialog = createNtpDialog(mContext.getString(R.string.set_datetime), getCurrentNtpTime());
		mNtpDialog.show();
	}

	private void updateNtpTime() {
		AlertDialog resetDialog = createNtpDialog(mContext.getString(R.string.set_datetime), getCurrentNtpTime());
		resetDialog.show();
		if (mNtpDialog != null)
			mNtpDialog.cancel();
		mNtpDialog = resetDialog;
		resetDialog = null;
	}

	private String getCurrentNtpTime() {
		long now = mNtpTime + (SystemClock.elapsedRealtime() - mNtpTimeReference);
		Date date = new Date();
		date.setTime(now);
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a ' ('HH:mm')'");
		sdf.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
		String ntpString = sdf.format(date);
		if (Constants.DEBUG)
			Log.v(TAG, "Current NTP Time= \n\t" + ntpString + "\n\t NTP TIME=" + mNtpTime + "\n\t NTP REFERENCE=" + mNtpTimeReference);
		return ntpString;
	}

	private void showStaticTimeDialog() {
		mNtpDialog = createNtpDialog(mContext.getString(R.string.set_datetime_unknown), "");
		mNtpDialog.show();
	}

	private AlertDialog createNtpDialog(String body, String date) {

		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.toast_ntp_update, null);

		// timezone
		TextView tzMessage = (TextView) view.findViewById(R.id.timezone_message);
		TextView tzValue = (TextView) view.findViewById(R.id.timezone_value);
		if (mChangeTimeZone) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
			String timeZone = settings.getString(Constants.DEFAULT_TIME_ZONE_UI_STRING, "Nairobi");
			tzMessage.setVisibility(View.VISIBLE);
			tzValue.setText(timeZone);
			tzValue.setVisibility(View.VISIBLE);
		} else {
			tzMessage.setVisibility(View.GONE);
			tzValue.setVisibility(View.GONE);
		}

		// time
		TextView tMessage = (TextView) view.findViewById(R.id.time_message);
		TextView tValue = (TextView) view.findViewById(R.id.time_value);
		if (mChangeTime) {
			tMessage.setVisibility(View.VISIBLE);
			tMessage.setText(body);
			tValue.setText(date);
		} else {
			tMessage.setVisibility(View.GONE);
			tValue.setText("");  // dont get rid, as it retains proper spacing due to Android Alert Dialog Bug
		}
		
		if (!mChangeTime && !mChangeTimeZone){
			//Should never happen!
			tMessage.setVisibility(View.VISIBLE);
			tMessage.setText(body);
			tValue.setText(date);
		}

		builder.setView(view);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		});

		return builder.create();
	}

}