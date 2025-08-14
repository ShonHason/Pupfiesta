import SwiftUI
import MapKit
import CoreLocation
import Shared

struct YardView: View {
    // KMM VM
    let viewModel: DogGardensViewModel

    // Bottom bar selection (if you use a tab bar outside)
    @State private var selectedTab: BottomBarContainer.Tab = .yard

    // Observed VM state
    @State private var gardensCount: Int = 0
    @State private var radiusMeters: Int32 = 1_000

    // Map state
    private let telAviv = CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818)
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818),
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var tracking: MapUserTrackingMode = .follow // native blue-dot follow

    // Flows & permission helper
    @State private var observeTasks: [Task<Void, Never>] = []
    @StateObject private var authProxy = LocationAuthProxy()

    // Navigation to profile
    @State private var goToProfile = false

    // 🔵 Your requested gradient background
    private let fullPageGradient = LinearGradient(
        colors: [
            Color(red: 252/255, green: 241/255, blue: 196/255),
            Color(red: 176/255, green: 212/255, blue: 248/255)
        ],
        startPoint: .top, endPoint: .bottom
    )

    init(viewModel: DogGardensViewModel) {
        self.viewModel = viewModel
    }

    var body: some View {
        ZStack {
            // Background gradient fills everything
            fullPageGradient.ignoresSafeArea()

            VStack(spacing: 0) {
                infoStrip
                Divider().opacity(0.1)

                ZStack(alignment: .bottomTrailing) {
                    // Native blue dot
                    Map(
                        coordinateRegion: $region,
                        interactionModes: .all,
                        showsUserLocation: true,
                        userTrackingMode: $tracking
                    )
                    .onAppear {
                        // Start centered on Tel Aviv; MapKit follows user when available
                        region.center = telAviv
                    }

                    // Zoom controls
                    zoomControls
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Scan button
                Button(action: {
                    viewModel.refreshGardensFromGoogle()
                    viewModel.saveGardens()
                }) {
                    Text("Scan")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(16)
            }

            // Hidden programmatic nav to EditProfileView
            NavigationLink(
                destination: EditProfileView(userViewModel: UserViewModel(firebaseRepo: RemoteFirebaseRepository()))
                    .navigationBarBackButtonHidden(true)
                    .toolbar(.hidden, for: .navigationBar),
                isActive: $goToProfile
            ) { EmptyView() }
            .hidden()
        }
        // Hide nav bar on the Yard (root) screen too
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            authProxy.onAuthorized = { viewModel.loadLocation() }
            authProxy.requestOnce()
            startObserving()
        }
        .onChange(of: radiusMeters) { _ in
            viewModel.setRadius(meters: Int32(radiusMeters))
            viewModel.refreshGardensFromGoogle()
            viewModel.saveGardens()
        }
        .onDisappear { cancelObservers() }
    }

    // MARK: UI Pieces

    private var infoStrip: some View {
        let radiusKm = max(1, Int(Double(radiusMeters) / 1000.0))
        return Surface {
            VStack(alignment: .center, spacing: 12) {
                Text("Dog Parks Found: \(gardensCount)")
                    .font(.headline)

                Text("Radius: \(radiusKm) km")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                HStack(alignment: .center) {
                    Text("1 km").font(.caption).frame(width: 36, alignment: .leading)

                    Slider(
                        value: Binding(
                            get: { max(1.0, Double(radiusMeters) / 1000.0) },
                            set: { newKm in
                                let km = max(1, min(10, Int(newKm.rounded())))
                                let meters = Int32(km * 1000)
                                guard meters != radiusMeters else { return }
                                radiusMeters = meters
                                viewModel.setRadius(meters: meters)
                            }
                        ),
                        in: 1...10,
                        step: 1
                    )
                    .padding(.horizontal, 8)

                    Text("10 km").font(.caption).frame(width: 48, alignment: .trailing)
                }
            }
            .padding(16)
        }
    }

    private var zoomControls: some View {
        VStack(spacing: 8) {
            Button(action: zoomIn) {
                Image(systemName: "plus")
                    .font(.headline)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel("Zoom in")

            Button(action: zoomOut) {
                Image(systemName: "minus")
                    .font(.headline)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel("Zoom out")
        }
        .padding(8)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 3)
        .padding()
    }

    private func zoomIn() {
        let minDelta: CLLocationDegrees = 0.002
        let factor: Double = 0.6
        region.span = MKCoordinateSpan(
            latitudeDelta: max(minDelta, region.span.latitudeDelta * factor),
            longitudeDelta: max(minDelta, region.span.longitudeDelta * factor)
        )
    }

    private func zoomOut() {
        let maxDelta: CLLocationDegrees = 2.0
        let factor: Double = 0.6
        region.span = MKCoordinateSpan(
            latitudeDelta: min(maxDelta, region.span.latitudeDelta / factor),
            longitudeDelta: min(maxDelta, region.span.longitudeDelta / factor)
        )
    }

    // MARK: Observe KMM flows

    private func startObserving() {
        cancelObservers()

        // gardens list
        let t1 = Task {
            for await g in viewModel.gardens {
                await MainActor.run {
                    if let arr = g as? KotlinArray<AnyObject> {
                        gardensCount = Int(arr.size)
                    } else if let swiftArr = g as? [Any] {
                        gardensCount = swiftArr.count
                    } else {
                        gardensCount = 0
                    }
                }
            }
        }

        // radius
        let t2 = Task {
            for await r in viewModel.radiusMeters {
                await MainActor.run {
                    if let r32 = r as? Int32 { radiusMeters = max(1000, r32) }
                    else if let rInt = r as? Int { radiusMeters = max(1000, Int32(rInt)) }
                }
            }
        }

        // recenter when VM publishes user location
        let t3 = Task {
            for await loc in viewModel.userLocation {
                if let loc {
                    await MainActor.run {
                        region.center = CLLocationCoordinate2D(latitude: loc.latitude, longitude: loc.longitude)
                        tracking = .follow
                    }
                }
            }
        }

        observeTasks = [t1, t2, t3]
    }

    private func cancelObservers() {
        observeTasks.forEach { $0.cancel() }
        observeTasks.removeAll()
    }
}

// Simple surface
private struct Surface<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        content
            .frame(maxWidth: .infinity)
            // Keep this if you like a card look on top of the gradient;
            // set to .clear if you want the gradient to fully show through.
            .background(Color(.secondarySystemBackground).opacity(0.9))
    }
}

/// Permission helper — keep your Info.plist keys:
/// NSLocationWhenInUseUsageDescription (and/or Always if needed)
final class LocationAuthProxy: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var asked = false
    var onAuthorized: (() -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func requestOnce() {
        switch manager.authorizationStatus {
        case .notDetermined:
            if !asked {
                asked = true
                manager.requestWhenInUseAuthorization()
            }
        case .authorizedAlways, .authorizedWhenInUse:
            onAuthorized?()
        default:
            break
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            onAuthorized?()
        default:
            break
        }
    }
}
