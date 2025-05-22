<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-title>Estimote Beacons</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">
      <div v-if="needsPermissions">
        <ion-text color="warning">
          <h2>Permissions Required</h2>
          <p>This app needs location and bluetooth permissions to scan for beacons.</p>
        </ion-text>
        <ion-button @click="checkPermissions">Check Permissions</ion-button>
      </div>

      <div v-else>
        <ion-button @click="isScanning ? stopScanning() : startScanning()" :color="isScanning ? 'danger' : 'primary'">
          {{ isScanning ? 'Stop Scanning' : 'Start Scanning' }}
        </ion-button>
        <div class="beacon-list">
          <div v-if="beacons.length > 0" class="beacon-items">
            <div v-for="beacon in beacons" :key="beacon.identifier" class="beacon-item">
              <div class="beacon-header">
                <h2>{{ beacon.tag }}</h2>
                <span class="badge" :class="beacon.proximity">{{ beacon.proximity }}</span>
              </div>
              <p class="beacon-info">
                <span class="icon">📍</span>
                Distance: {{ beacon.distance }}m
              </p>
              <p v-if="beacon.lastSeen" class="beacon-info">
                <span class="icon">⏱️</span>
                Last seen: {{ formatLastSeen(beacon.lastSeen) }}
              </p>
            </div>
          </div>

          <div v-else class="beacon-empty">
            <p>{{ isScanning ? 'Searching for beacons...' : 'No beacons found. Press Start Scanning to begin.' }}</p>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { BeaconTracker } from 'capacitor-estimote';

// State
const isScanning = ref(false);
const beacons = ref([]);
const permissions = ref(null);

// Computed
const needsPermissions = computed(() => {
  if (!permissions.value) return true;
  return permissions.value.location !== 'granted' || permissions.value.bluetooth !== 'granted';
});

// Estimote Proximity SDK configuration
const BEACON_CONFIG = {
  tags: ['fridge', 'desk'], // Example tags to monitor
  identifier: 'test-zone', // A unique name for this scanning zone
};

// Methods
async function checkPermissions() {
  try {
    const perms = await BeaconTracker.checkPermissions();
    permissions.value = perms;
  } catch (error) {
    console.error('Error checking permissions:', error);
  }
}

async function startScanning() {
  try {
    const rangingConfig = {
      identifier: BEACON_CONFIG.identifier,
      tags: BEACON_CONFIG.tags,
    };
    console.log('Starting ranging with config:', JSON.stringify(rangingConfig));
    await BeaconTracker.startRanging(rangingConfig);
    console.log('Started ranging beacons');
    isScanning.value = true;
  } catch (error) {
    console.error('Error starting ranging:', error);
    alert('Failed to start ranging: ' + error.message);
  }
}

const stopScanning = async () => {
  try {
    await BeaconTracker.stopRanging({
      identifier: BEACON_CONFIG.identifier,
    });
    console.log('Stopped ranging');
    isScanning.value = false;
    beacons.value = [];
  } catch (error) {
    console.error('Error stopping ranging:', error);
  }
};

// Utility functions
function formatLastSeen(timestamp) {
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes}m ago`;
}

// Event Listeners
BeaconTracker.addListener('beaconDidEnter', (data) => {
  console.log('beaconDidEnter:', JSON.stringify(data));
  if (data && data.beacons) {
    data.beacons.forEach((beacon) => {
      const existingIndex = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (existingIndex === -1) {
        beacons.value.push({
          ...beacon,
          lastSeen: Date.now(),
          distance: beacon.proximity === 'near' ? 2 : 5,
        });
      }
    });
  }
});

BeaconTracker.addListener('beaconDidExit', (data) => {
  console.log('beaconDidExit:', JSON.stringify(data));
  if (data && data.beacons) {
    data.beacons.forEach((beacon) => {
      const index = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (index !== -1) {
        beacons.value.splice(index, 1);
      }
    });
  }
});

BeaconTracker.addListener('beaconsDidRangeEvent', (data) => {
  console.log('beaconsDidRangeEvent:', JSON.stringify(data));
  if (data && data.beacons) {
    data.beacons.forEach((beacon) => {
      const existingIndex = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (existingIndex !== -1) {
        beacons.value[existingIndex] = {
          ...beacon,
          lastSeen: Date.now(),
          distance: beacon.proximity === 'near' ? 2 : 5,
        };
      }
    });
  }
});

// Lifecycle
onMounted(async () => {
  await checkPermissions();
});

onUnmounted(() => {
  if (isScanning.value) {
    stopScanning();
  }
});
</script>

<style scoped>
.beacon-list {
  margin-top: 20px;
  padding: 0 15px;
}

.beacon-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.beacon-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.beacon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.beacon-header h2 {
  margin: 0;
  font-size: 1.2em;
  color: #333;
}

.badge {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.9em;
  font-weight: 500;
}

.badge.near {
  background-color: #4caf50;
  color: white;
}

.badge.far {
  background-color: #ff9800;
  color: white;
}

.beacon-info {
  margin: 8px 0;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  font-size: 1.2em;
}

.beacon-empty {
  text-align: center;
  color: #666;
  padding: 20px;
}
</style>
