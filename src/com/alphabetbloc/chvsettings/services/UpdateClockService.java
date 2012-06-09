package com.alphabetbloc.chvsettings.services;

/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Paint.Join;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.alphabetbloc.chvsettings.R;
import com.alphabetbloc.chvsettings.activities.PrefsPassword;
import com.alphabetbloc.chvsettings.services.UpdateClockService.ExecShell.SHELL_CMD;

import android.app.AlarmManager;
import android.bluetooth.BluetoothClass.Device;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Config;
import android.util.Log;
import android.widget.Toast;

public class UpdateClockService extends IntentService {

	private NotificationManager mNM;
	private int NOTIFICATION = R.string.clock_service_start;

	private boolean needNtpComparison;
	private boolean clockUpdated;

	private Exception exception;
	private static final String TAG = "UpdateClockService";

	private static final int REFERENCE_TIME_OFFSET = 16;
	private static final int ORIGINATE_TIME_OFFSET = 24;
	private static final int RECEIVE_TIME_OFFSET = 32;
	private static final int TRANSMIT_TIME_OFFSET = 40;
	private static final int NTP_PACKET_SIZE = 48;

	private static final int NTP_PORT = 123;
	private static final int NTP_MODE_CLIENT = 3;
	private static final int NTP_VERSION = 3;

	// Number of seconds between Jan 1, 1900 and Jan 1, 1970 : 70 years plus 17
	// leap days
	private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;
	private static final long LAST_TIME_CHECK = 1337960568210L;

	// system time computed from NTP server response
	private long mNtpTime = 0;

	// Current system time of Android wall clock
	private long mSystemTime;

	// value of SystemClock.elapsedRealtime() corresponding to mNtpTime
	private long mNtpTimeReference;

	// round trip time in milliseconds
	private long mRoundTripTime;

	private int timeout = 10000;
	private String host = "pool.ntp.org";

	private Handler mToastHandler;
	private Handler mNtpHandler;

	/**
	 * A constructor is required, and must call the super IntentService(String)
	 * constructor with a name for the worker thread.
	 */
	public UpdateClockService() {
		super("UpdateClockService");
		mToastHandler = new Handler();
		mNtpHandler = new Handler();
	}

	/**
	 * The IntentService calls this method from the default worker thread with
	 * the intent that started the service. When this method returns,
	 * IntentService stops the service, as appropriate.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.e("UpdateClockTask", "UpdateClockTask is running!");
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();
		needNtpComparison = false;
		clockUpdated = false;

		try {
			checkSystemTime();
			if (needNtpComparison) {
				Log.e(TAG, "We are about to requestNtpTime() :  needNtpComparison = " + needNtpComparison);
				synchronized (this) {
					try {
//						NB: can't use AsyncTask for this because AsyncTask can only be initiation from the Main UI thread, and here we are inside of IntentService Thread.
//						TODO: use ScheduledExecutorService to run this more than once...
//						TODO: take this away from the submit password and just run on boot... need to test that as well
//						TODO: also need to test all of this on a rooted device, and specifically on CM7.2!!!
//						TODO: remove the isDeviceRooted function and just make it public! ... this could also be used to test to see if there is 3G vs. 2G and change it automatically
//						TODO: also would need to receive intent callers to see if the network is very good, or what networks are available prior to switching on... I dont know if this is a broadcast receiver already?
//						TODO: updateSystemClock will probably throw exceptions because it gets called even if NTPComparison is true... though maybe NOT?! based on the logic below?
						
						 Thread requestNtp = new Thread(mRequestNtpTime, "RequestNtpTimeThread");
						 requestNtp.start();
						 try{
							 requestNtp.join();			 
						 }
						 catch(Exception exception){
							 Log.e(TAG, "failed to join to requestNtp");
						 }
					} catch (Exception e) {
					}
				}
				Log.e(TAG, "Finished requesting requestNtpTime() :  needNtpComparison = " + needNtpComparison);
				if (clockNeedsUpdate()) {
					if (isDeviceRooted()) {
						Log.e(TAG, "rooted!");
						updateSystemClock();
						if (clockUpdated) {

							mToastHandler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(UpdateClockService.this, R.string.clock_updated_success, Toast.LENGTH_LONG).show();
								}
							});
							clockUpdated = false;
						}

					} else {
						Log.e(TAG, "This device is not rooted.");
						mToastHandler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(UpdateClockService.this, R.string.clock_needs_update, Toast.LENGTH_LONG).show();
							}
						});

					}

				}
			} else {
				Log.e(TAG, "System Clock Does not require an update");
			}
		} catch (Exception e) {
			if (Config.LOGD)
				Log.e(TAG, "HandleIntent try failed: " + e);

		}
		mNM.cancel(NOTIFICATION);
	}

	private void showNotification() {
		CharSequence tickerText = getText(R.string.clock_service_ticker);
		CharSequence dropdownText = getText(R.string.clock_service_text);
		Notification notification = new Notification(R.drawable.clock_update, tickerText, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, PrefsPassword.class), 0);
		notification.setLatestEventInfo(this, getText(R.string.clock_service_label), dropdownText, contentIntent);
		mNM.notify(NOTIFICATION, notification);
	}

	private void checkSystemTime() {
		Log.e(TAG, "CheckSystemTime is called!");

		mSystemTime = Calendar.getInstance().getTimeInMillis();
		long timeDifference = mSystemTime - LAST_TIME_CHECK;

		Log.e(TAG, "SystemClock.ElapsedRealtTime check was    : " + getDuration(SystemClock.elapsedRealtime()));
		Log.e(TAG, "last time check was    : " + getDuration(LAST_TIME_CHECK));
		Log.e(TAG, "current system time is: " + getDuration(mSystemTime));
		Log.e(TAG, "timeDifference between them is: " + getDuration(timeDifference));

		// check to see if the time is prior to the last time or if it is >14
		// days since last check
		long days = 1000 * 60 * 60 * 24;
		Log.e(TAG, "timeDifference: " + (timeDifference) + " days: " + days);
		Log.e(TAG, "timeDifference / days: " + (timeDifference / days));
		if (((timeDifference / days) > 14) || timeDifference < 0) {
			// we need to update the system time!
			needNtpComparison = true;
			Log.e(TAG, "We need an NtpComparison!  needNtpComparison = " + needNtpComparison);
		}

	}

	/**
	 * Convert a millisecond duration to a string format
	 * 
	 * @param millis
	 *            A duration to convert to a string form
	 * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
	 */
	public static String getDuration(long millis) {
		if (millis < 0) {
			// throw new
			// IllegalArgumentException("Duration must be greater than zero!");
			return "requested time is negative";
		}

		int years = (int) (millis / (1000 * 60 * 60 * 24 * 365.25));
		millis -= (years * (1000 * 60 * 60 * 24 * 365.25));
		int days = (int) ((millis / (1000 * 60 * 60 * 24)) % 365.25);
		millis -= (days * (1000 * 60 * 60 * 24));
		int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
		millis -= (hours * (1000 * 60 * 60));
		int minutes = (int) ((millis / (1000 * 60)) % 60);
		millis -= (minutes * (1000 * 60));
		int seconds = (int) (millis / 1000) % 60;

		StringBuilder sb = new StringBuilder(64);
		sb.append(years);
		sb.append(" Years ");
		sb.append(days);
		sb.append(" Days ");
		sb.append(hours);
		sb.append(" Hours ");
		sb.append(minutes);
		sb.append(" Minutes ");
		sb.append(seconds);
		sb.append(" Seconds");

		return (sb.toString());
	}

	private Runnable mRequestNtpTime = new Runnable() {
		int i = 1;
		
		public void run() {
			try {
				Log.e(TAG, "RequestNtpTime is called and is being tried!");

				DatagramSocket socket = new DatagramSocket();
				socket.setSoTimeout(timeout);
				Log.e(TAG, "RequestNtpTime is called and is about to test the internet connection...");
				InetAddress address = InetAddress.getByName(host);

				Log.e(TAG, "RequestNtpTime has internet connection... inetaddress of: " + address);

				byte[] buffer = new byte[NTP_PACKET_SIZE];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

				// set mode = 3 (client) and version = 3
				// mode is in low 3 bits of first byte
				// version is in bits 3-5 of first byte
				buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

				// get current time and write it to the request packet
				long requestTime = System.currentTimeMillis();
				long requestTicks = SystemClock.elapsedRealtime();
				writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

				socket.send(request);

				// read the response
				DatagramPacket response = new DatagramPacket(buffer, buffer.length);
				socket.receive(response);
				long responseTicks = SystemClock.elapsedRealtime();
				long responseTime = requestTime + (responseTicks - requestTicks);
				socket.close();

				// extract the results
				long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
				long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
				long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
				long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);
				// receiveTime = originateTime + transit + skew
				// responseTime = transmitTime + transit - skew
				// clockOffset = ((receiveTime - originateTime) + (transmitTime
				// -
				// responseTime))/2
				// = ((originateTime + transit + skew - originateTime) +
				// (transmitTime - (transmitTime + transit - skew)))/2
				// = ((transit + skew) + (transmitTime - transmitTime - transit
				// +
				// skew))/2
				// = (transit + skew - transit + skew)/2
				// = (2 * skew)/2 = skew
				long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime)) / 2;
				// if (Config.LOGD) Log.d(TAG, "round trip: " + roundTripTime +
				// " ms");
				// if (Config.LOGD) Log.d(TAG, "clock offset: " + clockOffset +
				// " ms");

				// save our results - use the times on this side of the network
				// latency
				// (response rather than request time)
				mNtpTime = responseTime + clockOffset;
				mNtpTimeReference = responseTicks;
				mRoundTripTime = roundTripTime;

				needNtpComparison = false;
				i++;
				
			} catch (UnknownHostException e) {
				needNtpComparison = true;
				Log.d(TAG, "RequestNtpTime failed... caught UnknownHostException: " + e);
				e.printStackTrace();
			} catch (SocketException e) {
				needNtpComparison = true;
				Log.d(TAG, "RequestNtpTime failed... caught SocketException: " + e);
				e.printStackTrace();
			} catch (IOException e) {
				needNtpComparison = true;
				Log.d(TAG, "RequestNtpTime failed... caught IOException: " + e);
				e.printStackTrace();
			}

			finally{
				Log.d(TAG, "RequestNtpTime failed... beyond catch now. needNtpComparison: " + needNtpComparison + " i: " + i);
//				if (needNtpComparison && i <= 5) {
//					 postDelayed(this, i * 10 * 1000);
//					 this.join();
//					 Log.d(TAG, "RequestNtpTime failed... reposting 5 times every 10s, current time: " + i );
//					 }
//				Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				//
//								@Override
//								public void uncaughtException(Thread thread, Throwable ex) {
//									// TODO Auto-generated method stub
//									System.out.println("Caught " + ex);
//									
//								}
//							});
			}
			
		}
		
	};

	// deleting old catch:
	// } catch (Exception e) {
	// Log.e(TAG, "RequestNtpTime is called but no internet connectivity!");
	// return false;
	// }

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.e(TAG, "onDestroy has been called!");
		super.onDestroy();
	}

	/**
	 * Compares the NTPTime with the SystemTime. Returns true if the difference
	 * is greater than 200 seconds.
	 */
	private boolean clockNeedsUpdate() {
		Log.e(TAG, "clockNeedsUpdate Called... Asking if Clock Needs Update?");
		long now = mNtpTime + SystemClock.elapsedRealtime() - mNtpTimeReference;
		mSystemTime = Calendar.getInstance().getTimeInMillis();
		long sys2 = System.currentTimeMillis();

		long delta = Math.abs(now - mSystemTime);

		if (delta > 10) {
			Log.e(TAG, "clockNeedsUpdate is true b/c delta is: " + delta);
			return true;

		} else {
			Log.e(TAG, "Delta is less than 60000 already... no need to reset time and is: " + delta);
			return false;
		}
	}

	private void updateSystemClock() {
		SystemClock.setCurrentTimeMillis(mNtpTime);

		// Let's double-check!
		long newSys = System.currentTimeMillis();
		long newDelta = Math.abs(mNtpTime - newSys);
		if (newDelta == 0) {
			Log.e(TAG, "System clock has been updated to NTP time");
			clockUpdated = true;
		} else {
			Log.e(TAG, "Something went wrong! The Ntp-System Delta is: " + newDelta);
			clockUpdated = false;
		}

	}

	// Alt code for setting the system time:
	// AlarmManager alarm = (AlarmManager)
	// getSystemService(ALARM_SERVICE);
	// alarm.setTime(1330082817000);

	/**
	 * Reads an unsigned 32 bit big endian number from the given offset in the
	 * buffer.
	 */
	private long read32(byte[] buffer, int offset) {
		byte b0 = buffer[offset];
		byte b1 = buffer[offset + 1];
		byte b2 = buffer[offset + 2];
		byte b3 = buffer[offset + 3];

		// convert signed bytes to unsigned values
		int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
		int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
		int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
		int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

		return ((long) i0 << 24) + ((long) i1 << 16) + ((long) i2 << 8) + (long) i3;
	}

	/**
	 * Reads the NTP time stamp at the given offset in the buffer and returns it
	 * as a system time (milliseconds since January 1, 1970).
	 */
	private long readTimeStamp(byte[] buffer, int offset) {
		long seconds = read32(buffer, offset);
		long fraction = read32(buffer, offset + 4);
		return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
	}

	/**
	 * Writes system time (milliseconds since January 1, 1970) as an NTP time
	 * stamp at the given offset in the buffer.
	 */
	private void writeTimeStamp(byte[] buffer, int offset, long time) {
		long seconds = time / 1000L;
		long milliseconds = time - seconds * 1000L;
		seconds += OFFSET_1900_TO_1970;

		// write seconds in big endian format
		buffer[offset++] = (byte) (seconds >> 24);
		buffer[offset++] = (byte) (seconds >> 16);
		buffer[offset++] = (byte) (seconds >> 8);
		buffer[offset++] = (byte) (seconds >> 0);

		long fraction = milliseconds * 0x100000000L / 1000L;
		// write fraction in big endian format
		buffer[offset++] = (byte) (fraction >> 24);
		buffer[offset++] = (byte) (fraction >> 16);
		buffer[offset++] = (byte) (fraction >> 8);
		// low order bits should be random data
		buffer[offset++] = (byte) (Math.random() * 255.0);
	}

	/**
	 * @author Kevin Kowalewski
	 * 
	 */

	// private static String LOG_TAG = Root.class.getName();

	public boolean isDeviceRooted() {
		if (checkRootMethod1()) {
			return true;
		}
		if (checkRootMethod2()) {
			return true;
		}
		if (checkRootMethod3()) {
			return true;
		}
		return false;
	}

	public boolean checkRootMethod1() {
		String buildTags = android.os.Build.TAGS;

		// if (buildTags != null && buildTags.contains("test-keys")) {
		// return true;
		// }
		return false;
	}

	public boolean checkRootMethod2() {
		try {
			File file = new File("/system/app/Superuser.apk");
			if (file.exists()) {
				return true;
			}
		} catch (Exception e) {
		}

		return false;
	}

	public boolean checkRootMethod3() {
		if (new ExecShell().executeCommand(SHELL_CMD.check_su_binary) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @author Kevin Kowalewski
	 * 
	 */
	public static class ExecShell {

		private String LOG_TAG = ExecShell.class.getName();

		public static enum SHELL_CMD {
			check_su_binary(new String[] { "/system/xbin/which", "su" }), ;

			String[] command;

			SHELL_CMD(String[] command) {
				this.command = command;
			}
		}

		public ArrayList<String> executeCommand(SHELL_CMD shellCmd) {
			String line = null;
			ArrayList<String> fullResponse = new ArrayList<String>();
			Process localProcess = null;

			try {
				localProcess = Runtime.getRuntime().exec(shellCmd.command);
			} catch (Exception e) {
				return null;
				// e.printStackTrace();
			}

			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(localProcess.getOutputStream()));
			BufferedReader in = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));

			try {
				while ((line = in.readLine()) != null) {
					Log.d(LOG_TAG, "--> Line received: " + line);
					fullResponse.add(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			Log.d(LOG_TAG, "--> Full response was: " + fullResponse);

			return fullResponse;
		}

	}

}

//Logic for this function:
	// if clock has not been checked in 2 weeks, then it may require an
	// update but we don't yet know
	// if/while NTP has internet access,
	// then let us check it against the Device Time
	// if there is a difference between the NTP and the SystemTime, then we
	// should try to fix it
	// if device is rooted, let's go ahead and fix it
	// else lets tell the user to fix it.
	// else, let's cycle through again until we get internet access
	//

// waiting function...

//long endTime = System.currentTimeMillis() + 30 * 1000;
//while (System.currentTimeMillis() < endTime) {
//	synchronized (this) {
//		try {
//			wait(endTime - System.currentTimeMillis());
//		} catch (Exception e) {
//		}
//	}
//}
//
//Log.e(TAG, "RequestNtpTime has finished waiting...");

// old time zone code...
/*
 * Long longDate = Long.valueOf(date);
 * 
 * Calendar cal = Calendar.getInstance(); int offset =
 * cal.getTimeZone().getOffset(cal.getTimeInMillis()); Date da = new Date(); da
 * = new Date(longDate-(long)offset); cal.setTime(da);
 * 
 * String time =cal.getTime().toLocaleString(); //this is full string
 * 
 * time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(da); //this is
 * only time
 * 
 * time = DateFormat.getDateInstance(DateFormat.MEDIUM).format(da); //this is
 * only date
 * 
 * DateFormat formatter = new SimpleDateFormat(dateFormat);
 */

// long endTime = System.currentTimeMillis() + 5 * 1000;
// wait(endTime - System.currentTimeMillis());

// Calendar c = Calendar.getInstance();
// return c.getTimeInMillis();

// Calendar c = Calendar.getInstance();
// TimeZone z = c.getTimeZone();
//
// int offset = z.getRawOffset();
// if(z.inDaylightTime(new Date())){
// offset = offset + z.getDSTSavings();
// }
// int offsetHrs = offset / 1000 / 60 / 60;
// int offsetMins = offset / 1000 / 60 % 60;
//
// c.add(Calendar.HOUR_OF_DAY, (-offsetHrs));
// c.add(Calendar.MINUTE, (-offsetMins));
//
// System.out.println("Current Android GMT time: "+c.getTime());
// // Log.e(TAG, "sys is:" + c.getTime());
//
//
// // Africa/Nairobi
// TimeZone nyctz = TimeZone.getTimeZone("America/New_York");
// TimeZone nbotz = TimeZone.getTimeZone("Africa/Nairobi");
// TimeZone defaulttz = TimeZone.getDefault();
//
// // Log.e(TAG, "nyctz is: " + nyctz.getID());
// // Log.e(TAG, "nbotz is: " + nbotz.getID());
// // Log.e(TAG, "defaulttz is: " + defaulttz.getID());
//
// Calendar newC = null;
// newC.setTimeZone(TimeZone.getTimeZone("Africa/Nairobi"));
