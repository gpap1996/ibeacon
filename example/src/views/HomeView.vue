<template>
  <div class="beacon-scanner">
    <h2>iBeacon Scanner</h2>

    <!-- Status and Controls -->
    <div class="controls">
      <button @click="startScanning" :disabled="isScanning" class="start-btn">Start Scanning</button>
      <button @click="stopScanning" :disabled="!isScanning" class="stop-btn">Stop Scanning</button>
    </div>

    <!-- Permissions Status -->
    <div class="permissions" v-if="permissions">
      <p>Location Permission: {{ permissions.location }}</p>
      <p>Bluetooth Permission: {{ permissions.bluetooth }}</p>
      <button @click="requestPermissions" v-if="needsPermissions">Request Permissions</button>
    </div>

    <!-- Beacons List -->
    <div class="beacons-list" v-if="beacons.length">
      <h3>Detected Beacons</h3>
      <div
        v-for="beacon in beacons"
        :key="`${beacon.uuid}-${beacon.major}-${beacon.minor}`"
        class="beacon-item"
        :class="beacon.proximity"
      >
        <h4>Beacon {{ beacon.major }}.{{ beacon.minor }}</h4>
        <div class="beacon-details">
          <p>UUID: {{ beacon.uuid }}</p>
          <p>Distance: {{ beacon.distance }}m</p>
          <p>RSSI: {{ beacon.rssi }}dBm</p>
          <p>Proximity: {{ beacon.proximity }}</p>
        </div>
      </div>
    </div>

    <p v-else-if="isScanning" class="no-beacons">Scanning for beacons...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { BeaconTracker } from 'ibeacon-tracker';

// State
const isScanning = ref(false);
const beacons = ref([]);
const permissions = ref(null);

// Computed
const needsPermissions = computed(() => {
  if (!permissions.value) return true;
  return permissions.value.location !== 'granted' || permissions.value.bluetooth !== 'granted';
});

// Your Estimote beacon configuration
const BEACON_REGION = {
  uuid: 'b9407f30-f5f8-466e-aff9-25556b57fe6d',
  identifier: 'b4a6d2890fa6a8bf3825adff5fcf8b35',
  // major: 19,
  // minor: 1625,
};

// Methods
async function checkPermissions() {
  try {
    const status = await BeaconTracker.checkPermissions();
    console.log('Permission status:', status); // This will show in browser console

    if (status.location !== 'granted') {
      console.log('Location permission is not granted:', status.location);
    }
    if (status.bluetooth !== 'granted') {
      console.log('Bluetooth permission is not granted:', status.bluetooth);
    }

    return status;
  } catch (error) {
    console.error('Error checking permissions:', error);
    throw error;
  }
}

async function requestPermissions() {
  try {
    const status = await BeaconTracker.requestPermissions();
    console.log('New permission status after request:', JSON.stringify(status));
    return status;
  } catch (error) {
    console.error('Error requesting permissions:', error);
    throw error;
  }
}

async function startScanning() {
  try {
    const permissions = await BeaconTracker.checkPermissions();
    console.log('Current permissions:', permissions);

    if (permissions.location !== 'granted' || permissions.bluetooth !== 'granted') {
      console.log('Requesting permissions...');
      const newPermissions = await BeaconTracker.requestPermissions();
      console.log('New permissions:', newPermissions);
    }

    // Check Bluetooth status
    if (navigator && navigator.bluetooth) {
      console.log('Checking if Bluetooth is available...');
      const available = await navigator.bluetooth.getAvailability();
      console.log('Bluetooth available:', available);
    }

    console.log('Starting beacon ranging...');
    isScanning.value = true;
    await BeaconTracker.startRanging(BEACON_REGION);
    console.log('Ranging started successfully');
  } catch (error) {
    console.error('Error in startScanning:', error);
    alert(`Failed to start scanning: ${error.message}`);
  }
}

const stopScanning = async () => {
  try {
    await BeaconTracker.stopRanging(BEACON_REGION);
    isScanning.value = false;
    beacons.value = [];
  } catch (error) {
    console.error('Failed to stop scanning:', error);
  }
};

// Event Listeners
const setupBeaconListener = () => {
  BeaconTracker.addListener('beaconsRanged', (data) => {
    console.log('Beacons ranged:', JSON.stringify(data));
    beacons.value = data.beacons;
  });
};

// Lifecycle
onMounted(async () => {
  await checkPermissions();
  setupBeaconListener();
});

onUnmounted(() => {
  if (isScanning.value) {
    stopScanning();
  }
});
</script>

<style scoped>
.beacon-scanner {
  padding: 1rem;
}

.controls {
  margin: 1rem 0;
  display: flex;
  gap: 1rem;
}

button {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.start-btn {
  background-color: #4caf50;
  color: white;
}

.stop-btn {
  background-color: #f44336;
  color: white;
}

.permissions {
  margin: 1rem 0;
  padding: 1rem;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.beacons-list {
  margin-top: 1rem;
}

.beacon-item {
  margin: 1rem 0;
  padding: 1rem;
  border-radius: 4px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.beacon-item.immediate {
  border-left: 4px solid #4caf50;
}

.beacon-item.near {
  border-left: 4px solid #ffc107;
}

.beacon-item.far {
  border-left: 4px solid #f44336;
}

.beacon-details {
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

.beacon-details p {
  margin: 0.25rem 0;
}

.no-beacons {
  text-align: center;
  color: #666;
  margin-top: 2rem;
}
</style>
