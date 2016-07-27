package com.example.macbook.autocontrol.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.macbook.autocontrol.util.Util;

public class LaunchReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

			try {

				Log.i("INFOSIGN", "LAUNCHER");

				// Cuando se prende el smartphone tambien tengo que
				// instalar el evento cada SEGS_POS segundos.

				Intent alarmIntent = new Intent(context, FindSemService.class);
				PendingIntent pintent = PendingIntent.getService(context, 0,
						alarmIntent, 0);
				AlarmManager alarm = (AlarmManager) context
						.getSystemService(Context.ALARM_SERVICE);
				alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				 Util.SEGS_POS,
				 Util.SEGS_POS, pintent);

			} catch (Exception e) {
				Toast.makeText(context,
						"Info Sign no esta corriendo. Error: " + e.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}
}