package com.alphabetbloc.chvsettings.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.data.Constants;
import com.alphabetbloc.chvsettings.data.EncryptedPreferences;

/**
 * Resets the Admin Password...
 * 
 * @author Louis Fazen (louis.fazen@gmail.com)
 * 
 */
public class SetAdminPassword extends SherlockActivity {

	public static final String TAG = "SetAdminPassword";
	private Button mSubmitButton;
	private TextView mInstructionText;
	private EditText mPasswordField;
	private String mFirstPassword;
	private boolean mAdminVerified;
	private String mCurrentPassword;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdminVerified = false;
		mFirstPassword = "";
		createView();
	}

	private void createView() {

		setContentView(R.layout.admin_login);

		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		// Setup current admin password
		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		mCurrentPassword = prefs.getString(Constants.ADMIN_PASSWORD, null);
		if (mCurrentPassword == null)
			prefs.edit().putString(Constants.ADMIN_PASSWORD, Constants.DEFAULT_ADMIN_PASSWORD).commit();

		// Setup Admin ID
		mPasswordField = (EditText) findViewById(R.id.text_password);
		mInstructionText = (TextView) findViewById(R.id.instruction);
		mInstructionText.setText(R.string.verify_admin_password);

		// Buttons
		mSubmitButton = (Button) findViewById(R.id.submit_button);
		mSubmitButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mPasswordField.getText().toString().equals("")) {
					String userEntry = mPasswordField.getText().toString();
					mPasswordField.setText("");
					if (!mAdminVerified) {
						if (verifyAdminPassword(userEntry))
							mInstructionText.setText(R.string.update_admin_password);
					} else if (!isSecure(userEntry)) {
						mInstructionText.setText(R.string.secure_admin_password);
						Toast.makeText(SetAdminPassword.this, "Sorry, that password is insecure.", Toast.LENGTH_SHORT).show();
					} else if (mFirstPassword == "") {
						mFirstPassword = userEntry;
						userEntry = "";
						mInstructionText.setText(R.string.confirm_admin_password);
					} else if (!mFirstPassword.equals(userEntry)) {
						mInstructionText.setText(R.string.update_admin_password);
						Toast.makeText(SetAdminPassword.this, "Passwords do not match. Please enter a new Administrator Password", Toast.LENGTH_SHORT).show();
					} else if (mFirstPassword.equals(userEntry)) {
						prefs.edit().putString(Constants.ADMIN_PASSWORD, userEntry).commit();
						Toast.makeText(SetAdminPassword.this, "Password has been updated.", Toast.LENGTH_SHORT).show();
						Intent intenta = new Intent(v.getContext(), AdminSettingsActivity.class);
						intenta.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(intenta);
						finish();
					} else {

						Toast.makeText(SetAdminPassword.this, "Please Try Again.", Toast.LENGTH_SHORT).show();
					}
				} else {
					Toast.makeText(SetAdminPassword.this, "Please Click on White Box to Enter An Admin Password.", Toast.LENGTH_SHORT).show();
				}
			}
		});

	}

	private static boolean isSecure(String str) {
		boolean alpha = false;
		boolean num = false;
		boolean lower = false;
		boolean upper = false;
		int count = 0;
		for (char c : str.toCharArray()) {
			if (Character.isLetter(c))
				alpha = true;
			if (Character.isDigit(c))
				num = true;
			if (Character.isLowerCase(c))
				lower = true;
			if (Character.isUpperCase(c))
				upper = true;
			count++;
		}

		if (alpha && num && upper && lower && count > 7)
			return true;
		else
			return false;
	}

	private boolean verifyAdminPassword(String userEntry) {
		int i = userEntry.compareTo(mCurrentPassword);
		if (i == 0) {
			mAdminVerified = true;
		} else {
			Toast.makeText(SetAdminPassword.this, "Incorrect Password: " + userEntry, Toast.LENGTH_SHORT).show();
			mAdminVerified = false;
		}
		return mAdminVerified;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		SubMenu sub = menu.addSubMenu("Menu");
		sub.setIcon(R.drawable.submenu);
		sub.add(0, R.string.list_short_password, 0, getString(R.string.list_short_password));
		sub.add(0, R.string.list_short_policy, 0, getString(R.string.list_short_policy));
		sub.add(0, R.string.list_short_apps, 0, getString(R.string.list_short_apps));
		sub.add(0, R.string.list_short_sms, 0, getString(R.string.list_short_sms));
		sub.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent i = new Intent(this, AdminSettingsActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(i);
			return false;
		case R.string.list_short_policy:
			startActivity(new Intent(this, SetDevicePolicy.class));
			finish();
			return true;
		case R.string.list_short_apps:
			startActivity(new Intent(this, SetAppPreferences.class));
			finish();
			return true;
		case R.string.list_short_password:
			startActivity(new Intent(this, SetAdminPassword.class));
			finish();
			return true;
		case R.string.list_short_sms:
			startActivity(new Intent(this, ViewSmsSettings.class));
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}