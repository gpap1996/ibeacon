<template>
  <div class="beacon-page">
    <header class="header">
      <h1>Estimote Beacons</h1>
    </header>

    <main class="content">
      <div v-if="!permissionsGranted || !bluetoothEnabled" class="setup-required">
        <div class="warning-box">
          <h2>Setup Required</h2>
          <template v-if="!permissionsGranted">
            <p>This app needs location and bluetooth permissions to scan for beacons.</p>
            <button class="btn primary" @click="requestPermissions">Grant Permissions</button>
          </template>
          <template v-if="permissionsGranted && !bluetoothEnabled">
            <p>Please enable Bluetooth to scan for beacons.</p>
            <button class="btn primary" @click="checkPermissions">Check Again</button>
          </template>
        </div>
      </div>

      <div v-else>
        <div class="controls">
          <div class="range-selector">
            <label for="rangeType">Range Type:</label>
            <select id="rangeType" v-model="selectedRange" :disabled="isScanning" class="select-input">
              <option v-for="option in RANGE_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>

          <div v-if="selectedRange === 'custom'" class="custom-range">
            <label for="customDistance">Custom Distance (meters):</label>
            <input
              type="number"
              id="customDistance"
              v-model="customDistance"
              :disabled="isScanning"
              min="0.1"
              max="10"
              step="0.1"
              class="number-input"
            />
          </div>

          <div class="current-range">
            <p>Current range: {{ getCurrentRangeDisplay() }}</p>
          </div>

          <button
            @click="isScanning ? stopScanning() : startScanning()"
            :class="['btn', isScanning ? 'danger' : 'primary']"
          >
            {{ isScanning ? 'Stop Scanning' : 'Start Scanning' }}
          </button>
        </div>

        <div class="settings">
          <div class="setting-group">
            <label for="range-select">Scan Range:</label>
            <select id="range-select" v-model="selectedRange" :disabled="isScanning" class="select-input">
              <option v-for="option in RANGE_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <div v-if="selectedRange === 'custom'" class="custom-range">
              <label for="custom-distance">Distance (meters):</label>
              <input
                id="custom-distance"
                type="number"
                v-model="customDistance"
                min="0.5"
                max="70"
                step="0.5"
                :disabled="isScanning"
                class="number-input"
              />
            </div>
          </div>

          <div class="setting-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="enableBackgroundScanning" :disabled="isScanning" />
              Enable Background Scanning
            </label>
            <div class="hint">When enabled, scanning will continue even when app is in background</div>
          </div>
        </div>

        <div class="beacon-list">
          <div v-if="beacons.length > 0" class="beacon-items">
            <div v-for="beacon in beacons" :key="beacon.identifier" class="beacon-item">
              <div class="beacon-header">
                <h2>{{ beacon.tag }}</h2>
                <span class="badge" :class="beacon.proximity">{{ beacon.proximity }}</span>
              </div>
              <p class="beacon-info">
                <span class="distance-icon">📍</span>
                Distance: {{ beacon.distance }}m
              </p>
              <p v-if="beacon.lastSeen" class="beacon-info">
                <span class="time-icon">⏱️</span>
                Last seen: {{ formatLastSeen(beacon.lastSeen) }}
              </p>
            </div>
          </div>

          <div v-else class="beacon-empty">
            <p>{{ isScanning ? 'Searching for beacons...' : 'No beacons found. Press Start Scanning to begin.' }}</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { BeaconTracker } from 'capacitor-estimote';

const RANGE_OPTIONS = [
  { label: 'Near (0-3m)', value: 'near' },
  { label: 'Far (3-7m)', value: 'far' },
  { label: 'Custom', value: 'custom' },
];

const BEACON_CONFIG = {
  tags: ['fridge', 'desk'], // Example tags to monitor
  identifier: 'test-zone', // A unique name for this scanning zone
};

const isScanning = ref(false);
const beacons = ref([]);
const selectedRange = ref('near');
const customDistance = ref(1.0);
const enableBackgroundScanning = ref(false); // New background scanning toggle
const permissionsState = ref({
  location: 'denied',
  bluetooth: 'denied',
  bluetoothEnabled: false,
});

// Computed
const permissionsGranted = computed(
  () => permissionsState.value.location === 'granted' && permissionsState.value.bluetooth === 'granted',
);

const bluetoothEnabled = computed(() => permissionsState.value.bluetoothEnabled);

function getCurrentRangeDisplay() {
  if (selectedRange.value === 'custom') {
    return `Custom: ${customDistance.value}m`;
  }
  return selectedRange.value === 'near' ? 'Near (0-3m)' : 'Far (3-7m)';
}

async function checkPermissions() {
  try {
    const perms = await BeaconTracker.checkPermissions();
    permissionsState.value = perms;
    return perms;
  } catch (error) {
    console.error('Error checking permissions:', error);
    return null;
  }
}

async function requestPermissions() {
  try {
    await BeaconTracker.requestPermissions();
    await checkPermissions();
  } catch (error) {
    console.error('Error requesting permissions:', error);
  }
}

async function startScanning() {
  try {
    // Double-check permissions and Bluetooth state before starting
    const perms = await checkPermissions();
    if (!perms || !perms.bluetoothEnabled) {
      alert('Please make sure Bluetooth is enabled and permissions are granted');
      return;
    }

    const rangingConfig = {
      identifier: BEACON_CONFIG.identifier,
      tags: BEACON_CONFIG.tags,
      range: {
        type: selectedRange.value,
        ...(selectedRange.value === 'custom' ? { customDistance: parseFloat(customDistance.value) } : {}),
      },
      enableBackgroundScanning: enableBackgroundScanning.value, // Add background scanning option
    };

    console.log('Starting ranging with config:', rangingConfig);
    await BeaconTracker.startRanging(rangingConfig);
    console.log('Started ranging beacons');
    isScanning.value = true;
  } catch (error) {
    console.error('Error starting ranging:', error);
    if (error.message.includes('Bluetooth must be enabled')) {
      alert('Please enable Bluetooth to scan for beacons');
    } else if (error.message.includes('permission')) {
      alert('Required permissions are not granted. Please check your settings.');
    } else {
      alert('Failed to start beacon scanning. Please try again.');
    }
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

function formatLastSeen(timestamp) {
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes}m ago`;
}

// Event Listeners
BeaconTracker.addListener('beaconDidEnter', (data) => {
  console.log('beaconDidEnter:', data);
  if (data?.beacons) {
    data.beacons.forEach((beacon) => {
      const existingIndex = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (existingIndex === -1) {
        beacons.value.push({
          ...beacon,
          lastSeen: Date.now(),
        });
      }
    });
  }
});

BeaconTracker.addListener('beaconDidExit', (data) => {
  console.log('beaconDidExit:', data);
  if (data?.beacons) {
    data.beacons.forEach((beacon) => {
      const index = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (index !== -1) {
        beacons.value.splice(index, 1);
      }
    });
  }
});

BeaconTracker.addListener('beaconsDidRangeEvent', (data) => {
  console.log('beaconsDidRangeEvent:', data);
  if (data?.beacons) {
    data.beacons.forEach((beacon) => {
      const existingIndex = beacons.value.findIndex((b) => b.tag === beacon.tag);
      if (existingIndex !== -1) {
        beacons.value[existingIndex] = {
          ...beacon,
          lastSeen: Date.now(),
        };
      }
    });
  }
});

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
.beacon-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  background-color: #f8f9fa;
  padding: 1rem;
  margin-bottom: 2rem;
  border-radius: 8px;
}

.header h1 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.5rem;
}

.content {
  margin-top: 30px;
  padding: 0 1rem;
}

.warning-box {
  background-color: #fff3cd;
  border: 1px solid #ffeeba;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.warning-box h2 {
  color: #856404;
  margin-top: 0;
}

.controls {
  background-color: #fff;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.range-selector,
.custom-range {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  color: #2c3e50;
  font-weight: 500;
}

.select-input,
.number-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  margin-bottom: 1rem;
  font-size: 1rem;
}

.current-range {
  margin: 1rem 0;
  padding: 0.5rem;
  background-color: #e9ecef;
  border-radius: 4px;
}

.btn {
  display: inline-block;
  padding: 0.5rem 1rem;
  font-size: 1rem;
  font-weight: 500;
  text-align: center;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
  width: 100%;
}

.btn.primary {
  background-color: #007bff;
  color: white;
}

.btn.danger {
  background-color: #dc3545;
  color: white;
}

.btn:hover {
  opacity: 0.9;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.beacon-list {
  margin-top: 2rem;
}

.beacon-items {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.beacon-item {
  background-color: white;
  border-radius: 8px;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.beacon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.beacon-header h2 {
  margin: 0;
  font-size: 1.2rem;
  color: #2c3e50;
}

.badge {
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
}

.badge.near {
  background-color: #28a745;
  color: white;
}

.badge.far {
  background-color: #ffc107;
  color: #212529;
}

.beacon-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.5rem 0;
  color: #6c757d;
}

.beacon-empty {
  text-align: center;
  color: #6c757d;
  padding: 2rem;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.setting-group {
  margin-bottom: 1rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
}

.checkbox-label input[type='checkbox'] {
  width: 1.2rem;
  height: 1.2rem;
}

.hint {
  font-size: 0.8rem;
  color: #666;
  margin-top: 0.25rem;
}
</style>
