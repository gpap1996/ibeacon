# ibeacon-tracker

capacitor ibeacon tracking

## Install

```bash
npm install ibeacon-tracker
npx cap sync
```

## API

<docgen-index>

* [`checkPermissions()`](#checkpermissions)
* [`requestPermissions()`](#requestpermissions)
* [`startRanging(...)`](#startranging)
* [`stopRanging(...)`](#stopranging)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### checkPermissions()

```typescript
checkPermissions() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### requestPermissions()

```typescript
requestPermissions() => Promise<PermissionStatus>
```

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### startRanging(...)

```typescript
startRanging(options: BeaconRegion) => Promise<void>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#beaconregion">BeaconRegion</a></code> |

--------------------


### stopRanging(...)

```typescript
stopRanging(options: BeaconRegion) => Promise<void>
```

| Param         | Type                                                  |
| ------------- | ----------------------------------------------------- |
| **`options`** | <code><a href="#beaconregion">BeaconRegion</a></code> |

--------------------


### Interfaces


#### PermissionStatus

| Prop            | Type                                           |
| --------------- | ---------------------------------------------- |
| **`location`**  | <code>'granted' \| 'denied' \| 'prompt'</code> |
| **`bluetooth`** | <code>'granted' \| 'denied' \| 'prompt'</code> |


#### BeaconRegion

| Prop             | Type                |
| ---------------- | ------------------- |
| **`uuid`**       | <code>string</code> |
| **`identifier`** | <code>string</code> |
| **`major`**      | <code>number</code> |
| **`minor`**      | <code>number</code> |

</docgen-api>
