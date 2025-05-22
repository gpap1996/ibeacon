package com.mobics.estimote;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
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

@CapacitorPlugin(name = "EstimoteBeaconTracker", permissions = {
        @Permission(strings = { Manifest.permission.ACCESS_FINE_LOCATION }),
        @Permission(strings = { Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT }, alias = "bluetooth")
})
public class EstimoteBeaconTrackerPlugin extends Plugin {
    private static final String TAG = "EstimoteBeaconTracker";
    private ProximityObserver proximityObserver;
    private ProximityObserver.Handler observationHandler;
    private Map<String, ProximityZone> activeZones = new HashMap<>();
    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void load() {
        BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private boolean checkBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject permissionsResultObject = new JSObject();

        // Check location permission
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            permissionsResultObject.put("location", "granted");
        } else {
            permissionsResultObject.put("location", "denied");
        }

        // Check Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean bluetoothScan = ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean bluetoothConnect = ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            permissionsResultObject.put("bluetooth", (bluetoothScan && bluetoothConnect) ? "granted" : "denied");
        } else {
            permissionsResultObject.put("bluetooth", "granted");
        }

        // Add Bluetooth state
        permissionsResultObject.put("bluetoothEnabled", checkBluetoothEnabled());

        call.resolve(permissionsResultObject);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        requestAllPermissions(call, "permissionsCallback");
    }

    @PermissionCallback
    private void permissionsCallback(PluginCall call) {
        checkPermissions(call);
    }

    private boolean hasRequiredPermissions(PluginCall call) {
        // Check location permission first
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            call.reject("Location permission is required");
            return false;
        }

        // Check Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getContext(),
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                call.reject("Bluetooth permissions are required for Android 12+");
                return false;
            }
        }

        // Check if Bluetooth is enabled
        if (!checkBluetoothEnabled()) {
            call.reject("Bluetooth must be enabled");
            return false;
        }

        return true;
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
                    .build(); // Create a zone for each tag
            for (int i = 0; i < tagsArray.length(); i++) {
                try {
                    String tag = tagsArray.getString(i);
                    JSObject rangeOptions = call.getObject("range"); // Start building the zone
                    ProximityZoneBuilder zoneBuilder = new ProximityZoneBuilder();
                    ProximityZoneBuilder.CallbackBuilder callbackBuilder;

                    // Configure the range based on options and get the callback builder
                    if (rangeOptions != null) {
                        String rangeType = rangeOptions.getString("type", "near");
                        Log.i(TAG, "Setting range type: " + rangeType + " for tag: " + tag);

                        switch (rangeType) {
                            case "far":
                                // Far range is approximately 7m
                                Log.i(TAG, "Using built-in far range (approximately 7m) for tag: " + tag);
                                callbackBuilder = zoneBuilder.forTag(tag).inFarRange();
                                break;
                            case "custom":
                                Double customDistance = rangeOptions.getDouble("customDistance");
                                if (customDistance != null && customDistance > 0) {
                                    // Validate the custom range is a reasonable value
                                    if (customDistance < 0.5) {
                                        Log.w(TAG, "Custom distance too small, using 0.5m for tag: " + tag);
                                        callbackBuilder = zoneBuilder.forTag(tag).inCustomRange(0.5);
                                    } else if (customDistance > 70.0) {
                                        Log.w(TAG, "Custom distance too large, using 70m for tag: " + tag);
                                        callbackBuilder = zoneBuilder.forTag(tag).inCustomRange(70.0);
                                    } else {
                                        Log.i(TAG, "Using custom range: " + customDistance + "m for tag: " + tag);
                                        callbackBuilder = zoneBuilder.forTag(tag).inCustomRange(customDistance);
                                    }
                                } else {
                                    Log.w(TAG,
                                            "Invalid or missing custom distance, falling back to near range for tag: "
                                                    + tag);
                                    callbackBuilder = zoneBuilder.forTag(tag).inNearRange();
                                }
                                break;
                            case "near":
                            default:
                                // Near range is approximately 3m
                                Log.i(TAG, "Using built-in near range (approximately 3m) for tag: " + tag);
                                callbackBuilder = zoneBuilder.forTag(tag).inNearRange();
                                break;
                        }
                    } else {
                        Log.i(TAG, "No range options provided, using default near range (approximately 3m) for tag: "
                                + tag);
                        callbackBuilder = zoneBuilder.forTag(tag).inNearRange();
                    }

                    // Build the final zone with callbacks
                    ProximityZone zone = callbackBuilder
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
}
