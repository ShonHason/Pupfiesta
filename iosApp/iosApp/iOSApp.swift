import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinBridge.shared.start()   // ← most common export for Kotlin object
    }

    var body: some Scene {
        WindowGroup { LandingView() }
    }
}
