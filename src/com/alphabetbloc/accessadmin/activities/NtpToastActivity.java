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
import com.alphabetbloc.accessadmin.data.Constants;

public class NtpToastActivity extends Activity {

	private static final String TAG = NtpToastActivity.class.getSimpleName();
	public static final String NTP_MESSAGE_BODY = "ntp_message_body";
	public static final String NTP_MESSAGE_DATE = "ntp_message_date";
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		String message = getIntent().getStringExtra(NTP_MESSAGE_BODY);
		String date = getIntent().getStringExtra(NTP_MESSAGE_BODY);
		AlertDialog d = createNtpDialog(message, date);
		d.show();

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
				if(Constants.DEBUG) Log.e(TAG, "finishing activity");
			}
		});

		return builder.create();
	}

}