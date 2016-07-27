package com.example.macbook.autocontrol.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.macbook.autocontrol.MainActivity;
import com.example.macbook.autocontrol.R;
import com.example.macbook.autocontrol.util.Util;

import java.util.List;

/**
 * Servicio de busqueda de semáforos compatibles
 */
public class FindSemService extends IntentService implements

		WifiScanResultHandler{

	WifiManager wifi = null;

	WifiReceiver receiver;

	// Constructor del servicio
	public FindSemService() {
		super("FindSemService");
	}

	/**
	 * Devuelve START_STICKY para no desligar el servicio */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.i(" #FSEM# SERVICE", "onStartCommand FindSemService");
		return START_STICKY;
	}

	/**
	 * onCreate - Se ejecuta solo si no está creado, obtiene el WifiManager,
	 * crea el lock e inicia el hilo de recepción de resultados
	 * de SSID WiFi disponibles */
	@Override

	public void onCreate() {
		super.onCreate();
		Log.i("#FSEM#  SERVICE", "onCreate FindSemService");

		if(SemComunication.staticState != SemComunication.StaticState.CONECTADO) {

			// Instancio wifi manager
			wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

			// Creo el receiver que va a procesar los resultados.
				receiver = new WifiReceiver(this, this.getApplicationContext());
			// Bloqueo wifi.
			wifi.createWifiLock(Util.WIFI_LOCK);


			// Muestro la notificación de ejecucion
			Util.notify(getApplicationContext(), R.drawable.running, getResources()
							.getString(R.string.app_name),
					getResources().getString(R.string.notify_running),
					MainActivity.class, Util.SEMACC_NOTIFY, false);
			Log.i("CREE NUEVO SOCKET", "------------------------");
		} else {

			Log.i("#FSEM#  SERVICE", "onCreate FindSemService: ya esta conectado, no se ejecuta");
		}
	}

	/**
	 * onResult - Se ejecuta cuando llega un listado de SSID
	 * wifi disponibles. */
	@Override
	public void onResult(List<ScanResult> results, int size) {

       // if(SemComunication.staticState == SemComunication.StaticState.NO_CONECTADO) {
			Log.i("#FSEM# RESULTADOS: ", String.valueOf(size));
            for (ScanResult scanResult : results) {
                Log.i("#FSEM#  WIFI - SSID: ", scanResult.SSID);

                if (scanResult.SSID.contains(Util.WIFI_IDENTIFIER)
						&& SemComunication.staticState == SemComunication.StaticState.NO_CONECTADO) {
                    SemComunication.create(scanResult.SSID, getApplicationContext());

                }
            }
      //  }

		this.stopSelf();

	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("#FSEM# SERVICE", "onHandleIntent FindSignService");
	}

	@Override
	public void onDestroy() {
		Log.i("#FSEM# SERVICE", "onDestroy FindSignService");
		super.onDestroy();
	}

}