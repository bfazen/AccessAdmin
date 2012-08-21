package com.alphabetbloc.chvsettings.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Policy;

public class SetProviderId extends DeviceHoldActivity {

	public static final String TAG = "InitialSetUpActivity";
	private Button mConfirmBtn;
	private Button mSubmitBtn;
	private TextView mInstructionText;
	private EditText mProviderId;
	private Long mFirstId;
	private Policy mPolicy;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startAirplaneMode();
		mPolicy = new Policy(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshView();
	}
	
	private void refreshView() {
		setContentView(R.layout.setup_provider);

		// Setup Provider ID
		mProviderId = (EditText) findViewById(R.id.provider_id);
		mInstructionText = (TextView) findViewById(R.id.instruction);
		mInstructionText.setText(R.string.provider_id);

		// Buttons
		mSubmitBtn = (Button) findViewById(R.id.submit_button);
		mSubmitBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mProviderId.getText().toString().equals("")) {
					Long providerId = Long.valueOf(mProviderId.getText().toString());
					if (providerId < 0)
						Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "is not a valid Provider ID.  ID can only contain numeric digits.", Toast.LENGTH_SHORT).show();
					else if (providerId > Integer.MAX_VALUE)
						Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "is not a valid Provider ID.  Please enter an ID with less than 10 digits", Toast.LENGTH_SHORT).show();
					else {
						mFirstId = providerId;
						mProviderId.setText("");
						mInstructionText.setText(R.string.confirm_id);
						mConfirmBtn.setVisibility(View.VISIBLE);
						mSubmitBtn.setVisibility(View.GONE);
					}
				} else {
					Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "Please Enter a Numeric Provider ID.", Toast.LENGTH_SHORT).show();
				}

			}

		});
		
		mConfirmBtn = (Button) findViewById(R.id.confirm_button);
		mConfirmBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mProviderId.getText().toString().equals("")) {
					Long providerId = Long.valueOf(mProviderId.getText().toString());
					if (providerId < 0)
						Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "is not a valid Provider ID.  ID can only contain numeric digits.", Toast.LENGTH_SHORT).show();
					else if (providerId > Integer.MAX_VALUE)
						Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "is not a valid Provider ID.  Please enter an ID with less than 10 digits", Toast.LENGTH_SHORT).show();
					else {
						if (mFirstId.equals(providerId)) {
							Toast.makeText(SetProviderId.this, "Success. Provider ID has been set to:" + mProviderId.getText().toString(), Toast.LENGTH_SHORT).show();
							mPolicy.setProviderId(safeLongToInt(providerId));
							Intent data = new Intent();
						    setResult(RESULT_OK, data);
							finish();
						} else {
							mProviderId.setText("");
							mInstructionText.setText(R.string.provider_id);
							mConfirmBtn.setVisibility(View.GONE);
							mSubmitBtn.setVisibility(View.VISIBLE);
							Toast.makeText(SetProviderId.this, "Provider IDs do not match. Please enter a new Provider ID.", Toast.LENGTH_SHORT).show();
						}
					}
				} else {
					Toast.makeText(SetProviderId.this, mProviderId.getText().toString() + "Please Click on White Box to Enter a Provider ID.", Toast.LENGTH_SHORT).show();
				}

			}

		});

		mConfirmBtn.setVisibility(View.GONE);
	}
	
	public static int safeLongToInt(long l) {
		if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(l + "is outside the bounds of int");
		}
		return (int) l;
	}
	///Override which buttons to allow through DeviceHold by not consuming TouchEvent
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.equals(mConfirmBtn) || v.equals(mSubmitBtn) || v.equals(mProviderId)) {
			return false;
		}
		return true;

	}
}