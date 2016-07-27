package com.example.macbook.autocontrol.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

public class WifiReceiver extends BroadcastReceiver implements Runnable {

	private int size = 0;
	private List<ScanResult> results;
	private WifiManager wifi;
	private WifiScanResultHandler handler;
	private boolean bContinue = true;

	public WifiReceiver(WifiScanResultHandler handler, Context context) {
		super();
		this.handler = handler;

		context.registerReceiver(this, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		// Solicito la instancia del WiFiManager
		this.wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		// Si WIFI esta apagado, lo enciendo.
		if (wifi.isWifiEnabled() == false) {
			wifi.setWifiEnabled(true);
		}
		wifi.startScan();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		results = wifi.getScanResults();
		size = results.size();
		handler.onResult(results, size);
		this.bContinue = false;
		
		context.unregisterReceiver(this);
	}

	@Override
	public void run() {

		long waitTime = 1500L;

		while (bContinue) {
			synchronized (this) {
				try {
					wait(waitTime);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		//context.unregisterReceiver(this);

	}

}
