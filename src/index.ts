import { registerPlugin } from '@capacitor/core';

import type { BeaconTrackerPlugin } from './definitions';

const BeaconTracker = registerPlugin<BeaconTrackerPlugin>('BeaconTracker', {
  web: () => import('./web').then((m) => new m.BeaconTrackerWeb()),
});

export * from './definitions';
export { BeaconTracker };
