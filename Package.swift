// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorEstimote",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorEstimote",
            targets: ["BeaconTrackerPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "BeaconTrackerPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/BeaconTrackerPlugin"),
        .testTarget(
            name: "BeaconTrackerPluginTests",
            dependencies: ["BeaconTrackerPlugin"],
            path: "ios/Tests/BeaconTrackerPluginTests")
    ]
)