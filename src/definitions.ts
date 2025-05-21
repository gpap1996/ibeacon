export interface BeaconTrackerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
