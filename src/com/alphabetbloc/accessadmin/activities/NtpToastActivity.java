package com.alphabetbloc.accessadmin.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.alphabetbloc.accessadmin.R;

public class NtpToastActivity extends Activity {

	private static final String TAG = NtpToastActivity.class.getSimpleName();
	public static final String NTP_MESSAGE = "ntp_message";
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		String message = getIntent().getStringExtra(NTP_MESSAGE);
		AlertDialog d = createNtpDialog(message);
		d.show();

	}

	private AlertDialog createNtpDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		

		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.toast_ntp_update, null);
		TextView messageView = (TextView) view.findViewById(R.id.message);
		messageView.setText(message);
//		builder.setMessage(message);
//		builder.setTitle(R.string.wrong_datetime);
		builder.setView(view);
//		builder.setIcon(R.drawable.priority);

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
				Log.e(TAG, "finishing activity");
			}
		});

		return builder.create();
	}

}