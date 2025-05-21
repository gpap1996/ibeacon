export interface BeaconData {
  uuid: string;
  major: number;
  minor: number;
  rssi: number;
  distance: number;
  proximity: string;
}

export interface BeaconRegion {
  identifier: string;
  tag: string;
}

export interface BeaconTrackerPlugin {
  checkPermissions(): Promise<PermissionStatus>;
  requestPermissions(): Promise<PermissionStatus>;
  startRanging(options: BeaconRegion): Promise<void>;
  stopRanging(options: BeaconRegion): Promise<void>;
}

export interface PermissionStatus {
  location: 'granted' | 'denied' | 'prompt';
  bluetooth: 'granted' | 'denied' | 'prompt';
}
