/**
 * 
 */
package com.alphabetbloc.chvsettings.receivers;

import com.alphabetbloc.chvsettings.services.UpdateClockService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Louis Fazen (louis.fazen@gmail.com)
 *
 */
public class UpdateOnBoot extends BroadcastReceiver {

	private static final String TAG = "UpdateOnBoot";

	public UpdateOnBoot() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {      
		Log.e(TAG, "Broadcast BOOT_COMPLETED received an intent initially");
		
        if( "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	Log.e(TAG, "Received intent name: " + intent.toString()); 
        	Log.e(TAG, "Broadcast BOOT_COMPLETED received and double-checked");
        	
//        	ACTIVITY ATTEMPT
//          ComponentName comp = new ComponentName("com.alphabetbloc.chvsettings.activities", UpdateClockTask.class.getName());
//          ComponentName asynctask = context.startActivity(new Intent().setComponent(comp));
//          Intent i = new Intent(context, UpdateClockTask.class);  
//          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//          context.startActivity(i);
        	
//        	SERVICE ATTEMPTS        	
//          Intent serviceIntent = new Intent("com.alphabetbloc.chvsettings.services.UpdateClockService");
//          Intent serviceIntent = new Intent(context, UpdateTimeService.class); 
//          context.startService(serviceIntent);
        	
         ComponentName comp = new ComponentName(context.getPackageName(), UpdateClockService.class.getName());
         ComponentName service = context.startService(new Intent().setComponent(comp));
         if (null == service){

          Log.e(TAG, "Could not start service: " + comp.toString());
         }
        } else {
         Log.e(TAG, "Received unexpected intent: " + intent.toString());   
        }
        
	}


}
