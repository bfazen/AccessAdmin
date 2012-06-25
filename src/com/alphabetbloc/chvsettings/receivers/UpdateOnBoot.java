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
	
		
        if( "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	
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
