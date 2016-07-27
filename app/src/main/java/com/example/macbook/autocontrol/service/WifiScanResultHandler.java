package com.example.macbook.autocontrol.service;

import android.net.wifi.ScanResult;

import java.util.List;

public interface WifiScanResultHandler {

	void onResult(List<ScanResult> results, int size);

}
