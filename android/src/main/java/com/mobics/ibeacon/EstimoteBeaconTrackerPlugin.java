package com.mobics.ibeacon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;
import com.estimote.coresdk.observation.region.RegionUtils;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CapacitorPlugin(
    name = "EstimoteBeaconTracker",
    permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }),
        @Permission(strings = { Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT }, alias = "bluetooth")
    }
)
public class EstimoteBeaconTrackerPlugin extends Plugin {
    private static final String TAG = "EstimoteBeaconTracker";
    private BeaconManager beaconManager;
    private Map<String, BeaconRegion> activeRegions = new HashMap<>();
    
    // Add running average for distance smoothing
    private Map<String, RunningAverage> distanceAverages = new HashMap<>();

    private static class RunningAverage {
        private static final int WINDOW_SIZE = 4;
        private final double[] values = new double[WINDOW_SIZE];
        private int index = 0;
        private int count = 0;

        public void add(double value) {
            values[index] = value;
            index = (index + 1) % WINDOW_SIZE;
            if (count < WINDOW_SIZE) count++;
        }

        public double getAverage() {
            if (count == 0) return 0;
            double sum = 0;
            for (int i = 0; i < count; i++) {
                sum += values[i];
            }
            return sum / count;
        }
    }

    @Override
    public void load() {
        beaconManager = new BeaconManager(getContext());
        
        // Configure scanning intervals
        beaconManager.setForegroundScanPeriod(150, 50); // 150ms scanning, 50ms wait
        beaconManager.setBackgroundScanPeriod(1100, 0); // 1.1s scanning, no waiting in background

        // Set up ranging listener
        beaconManager.setRangingListener((region, beacons) -> {
            JSObject data = new JSObject();
            
            try {
                JSArray beaconJSArray = new JSArray();
                
                for (Beacon beacon : beacons) {
                    String beaconId = beacon.getProximityUUID().toString() + ":" + 
                                    beacon.getMajor() + ":" + 
                                    beacon.getMinor();
                    
                    // Get or create running average for this beacon
                    RunningAverage avg = distanceAverages.computeIfAbsent(
                        beaconId, k -> new RunningAverage()
                    );
                    
                    // Calculate distance using RSSI and txPower
                    double distance = calculateDistance(beacon.getRssi(), beacon.getMeasuredPower());
                    avg.add(distance);
                    double smoothedDistance = avg.getAverage();
                    
                    JSObject beaconData = new JSObject();
                    beaconData.put("uuid", beacon.getProximityUUID().toString());
                    beaconData.put("major", beacon.getMajor());
                    beaconData.put("minor", beacon.getMinor());
                    beaconData.put("rssi", beacon.getRssi());
                    beaconData.put("distance", smoothedDistance);
                    beaconData.put("rawDistance", distance);
                    beaconData.put("proximity", getProximityString(smoothedDistance));
                    beaconJSArray.put(beaconData);
                }
                
                data.put("beacons", beaconJSArray);
                notifyListeners("beaconsRanged", data);
            } catch (Exception e) {
                Log.e(TAG, "Error processing beacon data", e);
            }
        });
    }

    private double calculateDistance(int rssi, int txPower) {
        if (rssi == 0) return -1.0;
        
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

    private String getProximityString(double distance) {
        if (distance < 0.5) return "immediate";
        if (distance < 3.0) return "near";
        return "far";
    }

    @PluginMethod
    public void startRanging(PluginCall call) {
        // First check permissions
        if (!checkPermissions(call)) {
            return;
        }

        String uuid = call.getString("uuid");
        String identifier = call.getString("identifier");
        Integer major = call.getInt("major");
        Integer minor = call.getInt("minor");

        if (uuid == null || identifier == null) {
            call.reject("UUID and identifier are required");
            return;
        }

        try {
            BeaconRegion region = new BeaconRegion(
                identifier,
                UUID.fromString(uuid),
                major != null ? major : null,
                minor != null ? minor : null
            );

            activeRegions.put(identifier, region);
            beaconManager.connect(() -> {
                beaconManager.startRanging(region);
                getActivity().runOnUiThread(() -> call.resolve());
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ranging", e);
            call.reject("Failed to start ranging: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopRanging(PluginCall call) {
        String identifier = call.getString("identifier");
        
        if (identifier == null) {
            call.reject("Identifier is required");
            return;
        }

        BeaconRegion region = activeRegions.get(identifier);
        if (region != null) {
            try {
                beaconManager.stopRanging(region);
                activeRegions.remove(identifier);
                call.resolve();
            } catch (Exception e) {
                call.reject("Failed to stop ranging: " + e.getMessage());
            }
        } else {
            call.reject("Region not found");
        }
    }

    private boolean checkPermissions(PluginCall call) {
        boolean fineLocation = ActivityCompat.checkSelfPermission(getContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ActivityCompat.checkSelfPermission(getContext(), 
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = fineLocation || coarseLocation;

        boolean bluetoothGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothGranted = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        if (!locationGranted || !bluetoothGranted) {
            call.reject("Missing required permissions. Please grant all permissions first.");
            return false;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            call.reject("Device doesn't support Bluetooth");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            call.reject("Please enable Bluetooth to start scanning");
            return false;
        }

        return true;
    }
}
