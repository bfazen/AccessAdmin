package com.alphabetbloc.accessadmin.activities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
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
	private Context mContext;
	private ScheduledExecutorService mExecutor = Executors.newScheduledThreadPool(5);
	// system time computed from NTP server response
	private long mNtpTime = -1;
	// value of SystemClock.elapsedRealtime() corresponding to mNtpTime
	private long mNtpTimeReference = -1;
	private AlertDialog mNtpDialog;
	private boolean mDialogActive = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		mNtpTime = getIntent().getLongExtra(NTP_TIME, 0);
		mNtpTimeReference = getIntent().getLongExtra(NTP_TIME_REFERENCE, 0);

		if (mNtpTime > 0 && mNtpTimeReference > 0)
			showDynamicTimeDialog();
		else
			showStaticTimeDialog();
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

							AlertDialog resetDialog = createNtpDialog(mContext.getString(R.string.set_datetime), getCurrentNtpTime());
							resetDialog.show();
							mNtpDialog.cancel();
							mNtpDialog = resetDialog;
							resetDialog = null;
							
						}
					});
				}
			}

		}, 120, TimeUnit.SECONDS);

		mNtpDialog = createNtpDialog(mContext.getString(R.string.set_datetime), getCurrentNtpTime());
		mNtpDialog.show();
	}

	private String getCurrentNtpTime() {
		long now = mNtpTime + (SystemClock.elapsedRealtime() - mNtpTimeReference);
		Date date = new Date();
		date.setTime(now);
		String ntpString = new SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a ' ('HH:mm')'").format(date);
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
		TextView bodyView = (TextView) view.findViewById(R.id.message_body);
		TextView dateView = (TextView) view.findViewById(R.id.message_date);
		bodyView.setText(body);
		dateView.setText(date);
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