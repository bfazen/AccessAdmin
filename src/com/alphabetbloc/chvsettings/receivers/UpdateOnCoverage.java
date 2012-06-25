/**
 * 
 */
package com.alphabetbloc.chvsettings.receivers;

import java.util.Calendar;

import com.alphabetbloc.chvsettings.activities.SetPrefs;
import com.alphabetbloc.chvsettings.services.UpdateClockService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Louis Fazen (louis.fazen@gmail.com)
 *
 */
public class UpdateOnCoverage extends BroadcastReceiver {

	private static final String TAG = "UpdateOnCoverage";
	Context mContext;
	
	public UpdateOnCoverage() {
		// TODO Auto-generated constructor stub
	}

	



	 
	 
	@Override
	public void onReceive(Context context, Intent intent) {      
		
		try { 
//		Bundle bundle = intent.getExtras();
//	     String message = bundle.getString("alarm_message");
//	     
//	     Intent newIntent = new Intent(context, SetPrefs.class);
//	     newIntent.putExtra("alarm_message", message);
//	     newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//	     context.startActivity(newIntent);
	    } catch (Exception e) {
	     Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show();
	     e.printStackTrace();
	 
	    }
		   
		
//        if( "android.intent.action.CHANGE_NETWORK_STATE".equals(intent.getAction())) {
//        	Log.e(TAG, "Double-check: Received intent name: " + intent.toString()); 
// 
//        	 
//         ComponentName comp = new ComponentName(context.getPackageName(), UpdateClockService.class.getName());
//         ComponentName service = context.startService(new Intent().setComponent(comp));
//         if (null == service){
//          Log.e(TAG, "Could not start service: " + comp.toString());
//         }
//        } else {
//         Log.e(TAG, "Received unexpected intent: " + intent.toString());   
//        }
        
	}


}
