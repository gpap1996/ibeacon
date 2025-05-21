import Foundation

@objc public class BeaconTracker: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
