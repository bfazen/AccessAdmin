package com.alphabetbloc.accessadmin.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.alphabetbloc.accessadmin.R;
import com.alphabetbloc.accessadmin.data.Constants;
import com.alphabetbloc.accessadmin.data.EncryptedPreferences;
import com.alphabetbloc.accessadmin.data.StringGenerator;

public class ViewSmsSettings extends SherlockActivity {

	private String mAdminId = "";
	private String mReportingLine = "";
	private Context mContext;
	private final static int SMS_ADMIN = 0;
	private final static int SMS_RESET = 1;

	public enum SmsCode {

		SMS_CODE_LOCK, SMS_CODE_GPS, SMS_CODE_HOLD, SMS_CODE_STOP_HOLD, SMS_CODE_RESET_PWD_DEFAULT, SMS_CODE_WIPE_ODK, SMS_CODE_WIPE_DATA, SMS_CODE_CANCEL_ALARM, SMS_CODE_RESET_ADMIN_ID, SMS_CODE_RESET_PWD_SECRET

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshView();
	}

	private void refreshView() {
		setContentView(R.layout.sms_settings);
		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		mContext = this;

		final SharedPreferences prefs = new EncryptedPreferences(this, this.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		ViewGroup smsGroup = (ViewGroup) findViewById(R.id.sms_list);
		mAdminId = prefs.getString(Constants.UNIQUE_DEVICE_ID, "");
		mReportingLine = prefs.getString(Constants.SMS_REPLY_LINE, "");
		// Require Device Admin ID
		smsGroup.addView(getDividerView(SMS_ADMIN));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_LOCK));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_GPS));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_HOLD));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_STOP_HOLD));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_RESET_PWD_DEFAULT));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_WIPE_ODK));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_WIPE_DATA));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_CANCEL_ALARM));

		// Resets: Do not require Device Admin ID
		smsGroup.addView(getDividerView(SMS_RESET));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_RESET_ADMIN_ID));
		smsGroup.addView(getItemView(SmsCode.SMS_CODE_RESET_PWD_SECRET));
	}

	private View getDividerView(int divider) {
		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View smsDivider = vi.inflate(R.layout.sms_divider_item, null);
		TextView titleView = (TextView) smsDivider.findViewById(R.id.title);
		TextView descrView = (TextView) smsDivider.findViewById(R.id.description);
		TextView codeView = (TextView) smsDivider.findViewById(R.id.code);
		ImageView image = (ImageView) smsDivider.findViewById(R.id.image);

		String title = "";
		String description = "";
		String code = "";

		image.setBackgroundResource(R.drawable.id_icon_inverse);

		switch (divider) {
		case SMS_ADMIN:
			title = getString(R.string.sms_admin_divider_title);
			description = getString(R.string.sms_admin_divider_description);
			code = mAdminId;
			image.setBackgroundResource(R.drawable.id_icon_inverse);
			break;
		case SMS_RESET:
			title = getString(R.string.sms_reset_divider_title);
			description = getString(R.string.sms_reset_divider_description);
			code = mReportingLine;
			image.setBackgroundResource(R.drawable.phone);
			break;
		default:
			break;
		}

		titleView.setText(title);
		descrView.setText(description);
		codeView.setText(code);

		return smsDivider;
	}

	private View getItemView(SmsCode smscode) {

		String title = "";
		String description = "";
		String code = "";
		String example = "";

		switch (smscode) {
		case SMS_CODE_LOCK:
			title = getString(R.string.sms_title_lock);
			description = getString(R.string.sms_description_lock);
			code = Constants.SMS_CODE_LOCK;
			example = mAdminId + code;
			break;
		case SMS_CODE_GPS:
			title = getString(R.string.sms_title_gps);
			description = getString(R.string.sms_description_gps);
			code = Constants.SMS_CODE_GPS;
			example = mAdminId + code;
			break;
		case SMS_CODE_HOLD:
			title = getString(R.string.sms_title_hold);
			description = getString(R.string.sms_description_hold);
			code = Constants.SMS_CODE_HOLD;
			example = mAdminId + code;
			break;
		case SMS_CODE_STOP_HOLD:
			title = getString(R.string.sms_title_stop_hold);
			description = getString(R.string.sms_description_stop_hold);
			code = Constants.SMS_CODE_STOP_HOLD;
			example = mAdminId + code;
			break;
		case SMS_CODE_RESET_PWD_DEFAULT:
			title = getString(R.string.sms_title_pwd_default);
			description = getString(R.string.sms_description_pwd_default);
			code = Constants.SMS_CODE_RESET_PWD_DEFAULT;
			example = mAdminId + code;
			break;
		case SMS_CODE_WIPE_ODK:
			title = getString(R.string.sms_title_wipe_odk);
			description = getString(R.string.sms_description_wipe_odk);
			code = Constants.SMS_CODE_WIPE_ODK;
			example = mAdminId + code;
			break;
		case SMS_CODE_WIPE_DATA:
			title = getString(R.string.sms_title_wipe_data);
			description = getString(R.string.sms_description_wipe_data);
			code = Constants.SMS_CODE_WIPE_DATA;
			example = mAdminId + code;
			break;
		case SMS_CODE_CANCEL_ALARM:
			title = getString(R.string.sms_title_cancel_alarm);
			description = getString(R.string.sms_description_cancel_alarm);
			code = Constants.SMS_CODE_CANCEL_ALARM;
			example = mAdminId + code;
			break;
		case SMS_CODE_RESET_ADMIN_ID:
			title = getString(R.string.sms_title_reset_admin);
			description = getString(R.string.sms_description_reset_admin);
			code = Constants.SMS_CODE_RESET_ADMIN_ID;
			example = code;
			break;
		case SMS_CODE_RESET_PWD_SECRET:
			title = getString(R.string.sms_title_pwd_secret);
			description = getString(R.string.sms_description_pwd_secret);
			code = Constants.SMS_CODE_RESET_PWD_SECRET;
			example = code;
			break;
		default:
			break;

		}

		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View smsItem = vi.inflate(R.layout.sms_item, null);
		TextView titleView = (TextView) smsItem.findViewById(R.id.title);
		titleView.setText(title);

		TextView descrView = (TextView) smsItem.findViewById(R.id.description);
		descrView.setText(description);

		TextView codeView = (TextView) smsItem.findViewById(R.id.sms_code);
		codeView.setText(code);

		TextView exampleView = (TextView) smsItem.findViewById(R.id.example);
		exampleView.setText(example);

		return smsItem;

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuItem adminId = menu.add(0, R.string.reset_admin_id, 0, getString(R.string.reset_admin_id));
		adminId.setIcon(R.drawable.id_icon_inverse);
		adminId.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		MenuItem smsLine = menu.add(0, R.string.reset_sms_line, 0, getString(R.string.reset_sms_line));
		smsLine.setIcon(R.drawable.phone);
		smsLine.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

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
		case R.string.reset_sms_line:
			updateSMSLine();
			return true;
		case R.string.reset_admin_id:
			updateAdminId();
			return true;
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

	private void updateSMSLine() {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);

		input.setText(prefs.getString(Constants.SMS_REPLY_LINE, ""));
		alert.setTitle("Update Current SMS Line");
		alert.setIcon(R.drawable.phone);
		alert.setView(input);
		alert.setPositiveButton("Update", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				prefs.edit().putString(Constants.SMS_REPLY_LINE, input.getText().toString().trim()).commit();
				showResultDialog(Constants.SMS_REPLY_LINE);
				refreshView();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		alert.show();
	}

	private void updateAdminId() {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Update Unique Device ID");
		alert.setIcon(R.drawable.id_icon_inverse);
		alert.setMessage("The current Unique Device ID is: " + prefs.getString(Constants.UNIQUE_DEVICE_ID, ""));
		alert.setPositiveButton("Update", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String rAlphaNum = (new StringGenerator(15)).getRandomAlphaNumericString();
				prefs.edit().putString(Constants.UNIQUE_DEVICE_ID, rAlphaNum).commit();
				refreshView();
				showResultDialog(Constants.UNIQUE_DEVICE_ID);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		alert.show();
	}

	private void showResultDialog(String type) {
		final SharedPreferences prefs = new EncryptedPreferences(mContext, mContext.getSharedPreferences(Constants.ENCRYPTED_PREFS, Context.MODE_PRIVATE));
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.toast_view, (ViewGroup) findViewById(R.id.toast_layout_root));
		TextView titleTxt = (TextView) view.findViewById(R.id.title_txt2);
		TextView body = (TextView) view.findViewById(R.id.message);
		TextView updateText = (TextView) view.findViewById(R.id.updated_text);
		ImageView image = (ImageView) view.findViewById(R.id.icon);

		if (type == Constants.UNIQUE_DEVICE_ID) {
			titleTxt.setText("New Unique Device ID");
			image.setBackgroundResource(R.drawable.id_icon_inverse);
			body.setText("Your Device ID has been updated to:");
			updateText.setText(prefs.getString(Constants.UNIQUE_DEVICE_ID, ""));
		} else if (type == Constants.SMS_REPLY_LINE) {
			titleTxt.setText("New SMS Line");
			image.setBackgroundResource(R.drawable.id_icon_inverse);
			body.setText("The device will now send all admin SMS to:");
			updateText.setText(prefs.getString(Constants.SMS_REPLY_LINE, ""));

		}

		
		Toast toast = new Toast(mContext);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setView(view);
		toast.setDuration(Toast.LENGTH_LONG);
		toast.show();
	}

}
