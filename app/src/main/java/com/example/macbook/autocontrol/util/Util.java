package com.example.macbook.autocontrol.util;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class Util {
	public static final String SHAPREF = "semacc";
	public static final String PRIMERAVEZ_SP = "primera_vez";
	public static final int SEMACC_NOTIFY = 13584249;
	
	public static final long SEGS_POS = 30000L; // 30 seg
	
	public static final String P_ACC = "acc";
	public static final String ACC_REPRODUCIR = "R";
	public static final String ACC_PRUEBA = "P";
	public static final String WIFI_LOCK = "com.adox.semacc";
	
	public static final String WIFI_IDENTIFIER = "camara";
	public static final String WIFI_PASS = "adox2311";
	public static String posturl = "http://192.168.1.200:3488/";

	public static String semIp = "192.168.1.200";
	//public static String semIp = "192.168.1.101";
	//public static String semIp = "192.168.1.101";
	public static int semPort = 3488;
//	public static String posturl = "http://192.168.0.101:6500/";
	
//	public static final String WIFI_IDENTIFIER = "InfoSign-";
//	public static final String WIFI_PASS = "ISADOX2311";
//	public static String posturl = "http://192.168.0.100:6500/";

	public static void showToast(final String msj, final Activity act) {
		act.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(act.getApplicationContext(), msj,
						Toast.LENGTH_LONG).show();
			}
		});
	}

	public static int notify(Context context, int icon, String title,
			String text, Class<?> activityClass, int mId) {
		return Util.notify(context, icon, title, text, activityClass, mId, true);
	}

	public static int notify(Context context, int icon, String title,
			String text, Class<?> activityClass, int mId, boolean cancelable) {

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				context).setSmallIcon(icon).setContentTitle(title)
				.setContentText(text);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			// Creates an explicit intent for an Activity in your app
			Intent resultIntent = new Intent(context, activityClass);

			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(activityClass);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
					PendingIntent.FLAG_UPDATE_CURRENT);

			mBuilder.setContentIntent(resultPendingIntent);
		}
		
		mBuilder.setAutoCancel(cancelable);
		mBuilder.setOngoing(!cancelable);
		
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(mId, mBuilder.build());

		return mId;
	}
	
	public static void cancelNotify(Context context, int mId){
		NotificationManager mNotificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(mId);
	}
}
