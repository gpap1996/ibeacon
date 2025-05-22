export interface BeaconData {
  tag: string;
  rssi: number;
  distance: number;
  proximity: string;
  attachments?: { [key: string]: string };
}

export type RangeType = 'near' | 'far' | 'custom';

export interface BeaconRangeOptions {
  type: RangeType;
  customDistance?: number; // in meters, only used when type is 'custom'
}

export interface BeaconRegion {
  identifier: string;
  tags: string[];
  range?: BeaconRangeOptions; // If not provided, defaults to 'near'
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
