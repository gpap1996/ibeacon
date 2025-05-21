import { WebPlugin } from '@capacitor/core';

import type { BeaconTrackerPlugin } from './definitions';

export class BeaconTrackerWeb extends WebPlugin implements BeaconTrackerPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
