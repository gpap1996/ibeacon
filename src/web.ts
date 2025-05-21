import { WebPlugin } from '@capacitor/core';

import type { BeaconTrackerPlugin, BeaconRegion, PermissionStatus } from './definitions';

export class BeaconTrackerWeb extends WebPlugin implements BeaconTrackerPlugin {
  async checkPermissions(): Promise<PermissionStatus> {
    return {
      location: 'prompt',
      bluetooth: 'prompt',
    };
  }

  async requestPermissions(): Promise<PermissionStatus> {
    return {
      location: 'prompt',
      bluetooth: 'prompt',
    };
  }

  async startRanging(_options: BeaconRegion): Promise<void> {
    console.log('Starting ranging for region:', _options);
  }

  async stopRanging(_options: BeaconRegion): Promise<void> {
    console.log('Stopping ranging for region:', _options);
  }
}
