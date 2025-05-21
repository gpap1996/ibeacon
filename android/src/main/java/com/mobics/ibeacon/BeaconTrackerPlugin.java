package com.mobics.ibeacon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.getcapacitor.PermissionState;

import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@CapacitorPlugin(
    name = "BeaconTracker",
    permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }),
        @Permission(strings = { Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT }, alias = "bluetooth")
    }
)
public class BeaconTrackerPlugin extends Plugin {
    private static final String TAG = "BeaconTracker";
    private BeaconManager beaconManager;
    private Map<String, Region> activeRegions = new HashMap<>();

    @Override
    public void load() {
        beaconManager = BeaconManager.getInstanceForApplication(getContext());
        // Enable auto-binding (new approach)
        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(0);
        beaconManager.setBackgroundScanPeriod(1100);

        // Set up to detect iBeacon format
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                JSObject data = new JSObject();
                
                try {
                    // Create a JSON array to store beacons
                    JSArray beaconJSArray = new JSArray();
                    
                    for (Beacon beacon : beacons) {
                        JSObject beaconData = new JSObject();
                        beaconData.put("uuid", beacon.getId1().toString());
                        beaconData.put("major", beacon.getId2().toInt());
                        beaconData.put("minor", beacon.getId3().toInt());
                        beaconData.put("rssi", beacon.getRssi());
                        beaconData.put("distance", beacon.getDistance());
                        beaconData.put("proximity", getProximityString(beacon.getDistance()));
                        beaconJSArray.put(beaconData);
                    }
                    
                    data.put("beacons", beaconJSArray);
                    Log.d(TAG, "Sending beacon data: " + data.toString());
                    notifyListeners("beaconsRanged", data);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing beacon data", e);
                }
            }
        });
    }

    private String getProximityString(double distance) {
        if (distance < 0.5) return "immediate";
        if (distance < 3.0) return "near";
        return "far";
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject result = new JSObject();
        
        // Check location permissions using Android's ActivityCompat
        boolean fineLocation = ActivityCompat.checkSelfPermission(getContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ActivityCompat.checkSelfPermission(getContext(), 
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        
        // For Android 12+ also check Bluetooth permissions
        boolean bluetoothGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean bluetoothScan = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean bluetoothConnect = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            bluetoothGranted = bluetoothScan && bluetoothConnect;
        }

        // Log the actual permission states for debugging
        Log.d(TAG, String.format("Permission states - Fine Location: %b, Coarse Location: %b, Bluetooth: %b",
            fineLocation, coarseLocation, bluetoothGranted));
        
        // We need at least one location permission
        boolean locationGranted = fineLocation || coarseLocation;
        
        result.put("location", locationGranted ? "granted" : "denied");
        result.put("bluetooth", bluetoothGranted ? "granted" : "denied");
        
        call.resolve(result);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        // Save the call for later use in the permission callback
        saveCall(call);
        
        // Request both location and bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ request bluetooth permissions
            requestAllPermissions(call, "permissionsCallback");
        } else {
            // For older Android versions, only request location
            requestPermissionForAlias("location", call, "permissionsCallback");
        }
    }

    @PermissionCallback
    private void permissionsCallback(PluginCall call) {
        if (call == null) {
            return;
        }

        // Check permissions directly using Android's APIs
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

        // Log the states for debugging
        Log.d(TAG, String.format("Permission callback - Location: %b, Bluetooth: %b", 
            locationGranted, bluetoothGranted));

        JSObject result = new JSObject();
        result.put("location", locationGranted ? "granted" : "denied");
        result.put("bluetooth", bluetoothGranted ? "granted" : "denied");

        if (locationGranted && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || bluetoothGranted)) {
            call.resolve(result);
        } else {
            String message = !locationGranted ? "Location permission denied" :
                           !bluetoothGranted ? "Bluetooth permission denied" :
                           "Required permissions not granted";
            Log.e(TAG, "Permission error: " + message);
            call.reject(message);
        }
    }

    @PluginMethod
    public void startRanging(PluginCall call) {
        // First check if we have all required permissions
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
            Log.e(TAG, String.format("Missing permissions - Location: %b, Bluetooth: %b", 
                locationGranted, bluetoothGranted));
            call.reject("Missing required permissions. Please grant all permissions first.");
            return;
        }

        // Check if Bluetooth is enabled
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth");
            call.reject("Device doesn't support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            call.reject("Please enable Bluetooth to start scanning");
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
            // The new approach doesn't require binding
            startRangingRegion(call, uuid, identifier, major, minor);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ranging", e);
            call.reject("Failed to start ranging: " + e.getMessage());
        }
    }

    private void startRangingRegion(PluginCall call, String uuid, String identifier, Integer major, Integer minor) {
        try {
            Region region = new Region(
                identifier,
                Identifier.parse(uuid),
                major != null ? Identifier.fromInt(major) : null,
                minor != null ? Identifier.fromInt(minor) : null
            );

            activeRegions.put(identifier, region);
            beaconManager.startRangingBeacons(region);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ranging region", e);
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

        Region region = activeRegions.get(identifier);
        if (region != null) {
            try {
                beaconManager.stopRangingBeacons(region);
                activeRegions.remove(identifier);
                call.resolve();
            } catch (Exception e) {
                call.reject("Failed to stop ranging: " + e.getMessage());
            }
        } else {
            call.reject("Region not found");
        }
    }
}
