export interface BeaconData {
  tag: string;
  rssi: number;
  distance: number;
  proximity: string;
  attachments?: { [key: string]: string };
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
