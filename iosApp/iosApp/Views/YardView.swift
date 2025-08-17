//  YardView.swift
//  iosApp
//
import SwiftUI
import MapKit
import CoreLocation
import Shared

private let TEL_AVIV = CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818)

struct YardView: View {
    let viewModel: DogGardensViewModel

    @State private var gardensCount: Int = 0
    @State private var radiusMeters: Int32 = 1_000

    @State private var region = MKCoordinateRegion(
        center: TEL_AVIV,
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var userCoord: CLLocationCoordinate2D? = nil
    @State private var camera: MapCameraPosition = .automatic

    @State private var dogGardens: [DogGarden] = []
    @State private var selectedGarden: DogGarden? = nil

    @State private var observeTasks: [Task<Void, Never>] = []
    @StateObject private var authProxy = LocationAuthProxy()
    @State private var scanDebounce: Task<Void, Never>? = nil

    var body: some View {
        let center = userCoord ?? region.center
        let filtered = filterGardens(dogGardens, center: center, radiusMeters: CLLocationDistance(radiusMeters))

        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 0) {
                infoStrip(filteredCount: filtered.count)
                Divider().opacity(0.1)

                ZStack(alignment: .top) {
                    // Map
                    MapContainer(
                        region: $region,
                        camera: $camera,
                        userCoord: userCoord,
                        radiusMeters: radiusMeters,
                        gardens: filtered,
                        onSelect: { g in selectedGarden = g }
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    // Place zoom buttons on top of the map (tappable)
                    .overlay(alignment: .bottomTrailing) {
                        ZoomControls(region: $region)
                            .padding(16)
                    }

                    // Top sheet
                    if let g = selectedGarden {
                        GardenTopSheet(
                            garden: g,
                            userCoord: userCoord ?? region.center,
                            onClose: { selectedGarden = nil }
                        )
                        .transition(.move(edge: .top).combined(with: .opacity))
                        .zIndex(10)
                        .padding(.top, 8)
                        .frame(maxWidth: .infinity, alignment: .top)
                        .allowsHitTesting(true)
                    }
                }
                .animation(.spring(response: 0.35, dampingFraction: 0.85), value: selectedGarden != nil)
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            authProxy.onAuthorized = { viewModel.loadLocation() }
            authProxy.requestOnce()
            startObserving()
            triggerScanDebounced(delayMs: 10)
        }
        .onChange(of: radiusMeters) { _ in
            viewModel.setRadius(meters: radiusMeters)
            triggerScanDebounced()
        }
        .onDisappear { cancelObservers() }
        .onChange(of: filtered.count) { newValue in gardensCount = newValue }
    }

    // MARK: UI
    private func infoStrip(filteredCount: Int) -> some View {
        let radiusKm = max(1, Int(Double(radiusMeters) / 1000.0))
        return Surface {
            VStack(spacing: 12) {
                Text("Dog Parks Found: \(filteredCount)").font(.headline)
                Text("Radius: \(radiusKm) km")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                HStack {
                    Text("1 km").font(.caption).frame(width: 36, alignment: .leading)
                    Slider(
                        value: Binding(
                            get: { max(1.0, Double(radiusMeters) / 1000.0) },
                            set: { newKm in
                                let km = max(1, min(10, Int(newKm.rounded())))
                                let m = Int32(km * 1000)
                                guard m != radiusMeters else { return }
                                radiusMeters = m
                                viewModel.setRadius(meters: m)
                            }
                        ),
                        in: 1...10, step: 1
                    )
                    .padding(.horizontal, 8)
                    Text("10 km").font(.caption).frame(width: 48, alignment: .trailing)
                }
            }
            .padding(16)
        }
    }

    // MARK: Observe KMM flows
    private func startObserving() {
        cancelObservers()

        let t1 = Task {
            for await g in viewModel.gardens {
                await MainActor.run {
                    let list: [DogGarden]
                    if let arr = g as? KotlinArray<AnyObject> {
                        var tmp: [DogGarden] = []
                        tmp.reserveCapacity(Int(arr.size))
                        for i in 0..<Int(arr.size) {
                            if let dg = arr.get(index: Int32(i)) as? DogGarden { tmp.append(dg) }
                        }
                        list = tmp
                    } else if let swiftArr = g as? [DogGarden] {
                        list = swiftArr
                    } else if let anyArr = g as? [Any] {
                        list = anyArr.compactMap { $0 as? DogGarden }
                    } else {
                        list = []
                    }
                    dogGardens = list
                }
            }
        }

        let t2 = Task {
            for await r in viewModel.radiusMeters {
                await MainActor.run {
                    if let r32 = r as? Int32 { radiusMeters = max(1000, r32) }
                    else if let rInt = r as? Int { radiusMeters = max(1000, Int32(rInt)) }
                }
            }
        }

        let t3 = Task {
            for await loc in viewModel.searchCenter {
                await MainActor.run {
                    let c = CLLocationCoordinate2D(latitude: loc.latitude, longitude: loc.longitude)
                    userCoord = c
                    region.center = c
                    if #available(iOS 17.0, *) { camera = .region(region) }
                    triggerScanDebounced()
                }
            }
        }

        observeTasks = [t1, t2, t3]
    }

    private func cancelObservers() {
        observeTasks.forEach { $0.cancel() }
        observeTasks.removeAll()
        scanDebounce?.cancel()
        scanDebounce = nil
    }

    private func triggerScanDebounced(delayMs: Int = 250) {
        scanDebounce?.cancel()
        scanDebounce = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
            viewModel.onScanClick()
        }
    }

    private func filterGardens(_ all: [DogGarden], center: CLLocationCoordinate2D, radiusMeters: CLLocationDistance) -> [DogGarden] {
        let centerLoc = CLLocation(latitude: center.latitude, longitude: center.longitude)
        return all.filter { g in
            let coord = CLLocation(latitude: g.location.latitude, longitude: g.location.longitude)
            return centerLoc.distance(from: coord) <= radiusMeters
        }
    }
}

// Small surface
private struct Surface<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        content
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground).opacity(0.9))
    }
}
