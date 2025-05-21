<template>
  <div class="beacon-scanner">
    <h2>Estimote Scanner</h2>

    <!-- Status and Controls -->
    <div class="controls">
      <button @click="startScanning" :disabled="isScanning" class="start-btn">Start Estimote Scanning</button>
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
      <h3>Detected Estimote Beacons</h3>
      <div
        v-for="beacon in beacons"
        :key="`${beacon.uuid}-${beacon.major}-${beacon.minor}`"
        class="beacon-item"
        :class="beacon.proximity"
      >
        <h4>Estimote Beacon {{ beacon.major }}.{{ beacon.minor }}</h4>
        <div class="beacon-details">
          <p>UUID: {{ beacon.uuid }}</p>
          <p>Distance: {{ beacon.distance }}m</p>
          <p>Raw Distance: {{ beacon.rawDistance }}m</p>
          <p>RSSI: {{ beacon.rssi }}dBm</p>
          <p>Proximity: {{ beacon.proximity }}</p>
        </div>
      </div>
    </div>

    <p v-else-if="isScanning" class="no-beacons">Scanning for Estimote beacons...</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { EstimoteTracker } from 'ibeacon-tracker';

// State
const isScanning = ref(false);
const beacons = ref([]);
const permissions = ref(null);

// Computed
const needsPermissions = computed(() => {
  if (!permissions.value) return true;
  return permissions.value.location !== 'granted' || permissions.value.bluetooth !== 'granted';
});

// Default Estimote beacon configuration
const BEACON_REGION = {
  uuid: 'B9407F30-F5F8-466E-AFF9-25556B57FE6D', // Estimote's default UUID
  identifier: 'b4a6d2890fa6a8bf3825adff5fcf8b35',
  // Not specifying major/minor to detect all Estimote beacons
};

// Methods
async function checkPermissions() {
  try {
    const status = await EstimoteTracker.checkPermissions();
    console.log('Estimote Permission status:', status);
    permissions.value = status;

    if (status.location !== 'granted') {
      console.log('Location permission is not granted:', status.location);
    }
    if (status.bluetooth !== 'granted') {
      console.log('Bluetooth permission is not granted:', status.bluetooth);
    }

    return status;
  } catch (error) {
    console.error('Error checking Estimote permissions:', error);
    throw error;
  }
}

async function requestPermissions() {
  try {
    const status = await EstimoteTracker.requestPermissions();
    console.log('New Estimote permission status:', JSON.stringify(status));
    permissions.value = status;
    return status;
  } catch (error) {
    console.error('Error requesting Estimote permissions:', error);
    throw error;
  }
}

async function startScanning() {
  try {
    const perms = await EstimoteTracker.checkPermissions();
    console.log('Current Estimote permissions:', perms);
    permissions.value = perms;

    if (perms.location !== 'granted' || perms.bluetooth !== 'granted') {
      console.log('Requesting Estimote permissions...');
      const newPerms = await EstimoteTracker.requestPermissions();
      permissions.value = newPerms;
      console.log('New Estimote permissions:', newPerms);
    }

    // Check Bluetooth status
    if (navigator && navigator.bluetooth) {
      console.log('Checking if Bluetooth is available...');
      const available = await navigator.bluetooth.getAvailability();
      console.log('Bluetooth available:', available);
    }

    console.log('Starting Estimote beacon ranging...');
    isScanning.value = true;
    await EstimoteTracker.startRanging(BEACON_REGION);
    console.log('Estimote ranging started successfully');
  } catch (error) {
    console.error('Error in Estimote startScanning:', error);
    alert(`Failed to start Estimote scanning: ${error.message}`);
    isScanning.value = false;
  }
}

const stopScanning = async () => {
  try {
    await EstimoteTracker.stopRanging(BEACON_REGION);
    isScanning.value = false;
    beacons.value = [];
  } catch (error) {
    console.error('Failed to stop Estimote scanning:', error);
  }
};

// Event Listeners
const setupBeaconListener = () => {
  EstimoteTracker.addListener('beaconsRanged', (data) => {
    console.log('Estimote Beacons ranged:', JSON.stringify(data));
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
  background-color: #00b8d4; /* Estimote blue */
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
  border-left: 4px solid #00b8d4; /* Estimote blue */
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
