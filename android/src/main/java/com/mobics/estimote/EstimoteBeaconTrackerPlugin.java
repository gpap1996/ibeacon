package com.mobics.estimote;

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

import com.estimote.proximity_sdk.api.ProximityObserver;
import com.estimote.proximity_sdk.api.ProximityObserverBuilder;
import com.estimote.proximity_sdk.api.EstimoteCloudCredentials;
import com.estimote.proximity_sdk.api.ProximityZone;
import com.estimote.proximity_sdk.api.ProximityZoneBuilder;
import com.estimote.proximity_sdk.api.ProximityZoneContext;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CapacitorPlugin(name = "EstimoteBeaconTracker", permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }),
        @Permission(strings = { Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT }, alias = "bluetooth")
})
public class EstimoteBeaconTrackerPlugin extends Plugin {
    private static final String TAG = "EstimoteBeaconTracker";
    private ProximityObserver proximityObserver;
    private ProximityObserver.Handler observationHandler;
    private Map<String, ProximityZone> activeZones = new HashMap<>();

    @Override
    public void load() {
        // We'll initialize the observer in startRanging
    }

    @PluginMethod
    public void startRanging(PluginCall call) {
        if (!hasRequiredPermissions(call)) {
            return;
        }

        String identifier = call.getString("identifier");
        JSArray tagsArray = call.getArray("tags");

        if (tagsArray == null || tagsArray.length() == 0) {
            Log.e(TAG, "Tags parameter is missing or empty");
            call.reject("Tags are required");
            return;
        }

        try {
            EstimoteCloudCredentials cloudCredentials = new EstimoteCloudCredentials("g-papapanos1996-gmail-com--7am",
                    "c2b96f6129d426e0592767a5010f7483");
            Log.d(TAG, "Starting ranging with configuration - identifier: " + identifier + ", tags: "
                    + tagsArray.toString());

            proximityObserver = new ProximityObserverBuilder(getContext(), cloudCredentials)
                    .withBalancedPowerMode()
                    .withTelemetryReportingDisabled()
                    .withAnalyticsReportingDisabled()
                    .withEstimoteSecureMonitoringDisabled()
                    .build();

            // Create a zone for each tag
            for (int i = 0; i < tagsArray.length(); i++) {
                try {
                    String tag = tagsArray.getString(i);
                    ProximityZone zone = new ProximityZoneBuilder()
                            .forTag(tag)
                            .inNearRange()
                            // .inNearRange(): //approximately 0-3 meters
                            // .inFarRange(): //approximately 3-7 meters
                            // .inCustomRange(double)//lets you specify a custom range, but it's still an
                            // approximation
                            .onEnter(context -> {
                                Log.d(TAG, "Beacon entered range - Tag: " + tag);
                                notifyBeaconEvent("beaconDidEnter", context, tag, "near");
                                return kotlin.Unit.INSTANCE;
                            })
                            .onExit(context -> {
                                Log.d(TAG, "Beacon exited range - Tag: " + tag);
                                notifyBeaconEvent("beaconDidExit", context, tag, "far");
                                return kotlin.Unit.INSTANCE;
                            })
                            .build();

                    // Start observing this zone
                    String zoneIdentifier = identifier + "_" + tag;
                    activeZones.put(zoneIdentifier, zone);
                    observationHandler = proximityObserver.startObserving(zone);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up zone for tag: " + i, e);
                }
            }

            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start ranging", e);
            call.reject("Failed to start ranging: " + e.getMessage());
        }
    }

    private void notifyBeaconEvent(String eventName, ProximityZoneContext context, String tag, String proximity) {
        JSObject data = new JSObject();
        JSArray beaconsArray = new JSArray();
        JSObject beaconData = new JSObject();

        // Basic beacon information
        beaconData.put("tag", tag);
        beaconData.put("proximity", proximity);

        // Add attachments if available
        Map<String, String> attachments = context.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            JSObject attachmentsObj = new JSObject();
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                attachmentsObj.put(entry.getKey(), entry.getValue());
            }
            beaconData.put("attachments", attachmentsObj);
        }

        beaconsArray.put(beaconData);
        data.put("beacons", beaconsArray);

        // Notify the specific event type and the generic range event
        notifyListeners(eventName, data);
        if (eventName.equals("beaconDidEnter") || eventName.equals("beaconDidExit")) {
            notifyListeners("beaconsDidRangeEvent", data);
        }
    }

    @PluginMethod
    public void stopRanging(PluginCall call) {
        if (observationHandler != null) {
            observationHandler.stop();
            observationHandler = null;
        }
        activeZones.clear();
        call.resolve();
    }

    private boolean hasRequiredPermissions(PluginCall call) {
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

    @PluginMethod
    @Override
    public void checkPermissions(PluginCall call) {
        if (hasRequiredPermissions(null)) {
            JSObject result = new JSObject();
            result.put("location", "granted");
            result.put("bluetooth", "granted");
            call.resolve(result);
        } else {
            // Save the call for later
            bridge.saveCall(call);

            // Request permissions
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                };
            } else {
                permissions = new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };
            }

            pluginRequestPermissions(permissions, 1);
        }
    }

    @PermissionCallback
    private void checkPermissionsCallback(PluginCall call) {
        if (hasRequiredPermissions(null)) {
            JSObject result = new JSObject();
            result.put("location", "granted");
            result.put("bluetooth", "granted");
            call.resolve(result);
        } else {
            JSObject result = new JSObject();
            result.put("location", "denied");
            result.put("bluetooth", "denied");
            call.reject("Permissions were not granted");
        }
    }
}
