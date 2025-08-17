import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        print("WhenInUse:", Bundle.main.object(
                 forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription"
               ) as? String ?? "MISSING")
        FirebaseApp.configure()       }

    var body: some Scene {
        WindowGroup {
            LandingView()
        }
    }
}
