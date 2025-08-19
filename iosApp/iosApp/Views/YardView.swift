//
//  YardView.swift
//  iosApp
//

import SwiftUI
import MapKit
import CoreLocation
import Shared

private let TEL_AVIV = CLLocationCoordinate2D(latitude: 32.0853, longitude: 34.7818)
private let CHECKIN_MAX_DISTANCE_M: CLLocationDistance = 450

struct YardView: View {
    let viewModel: DogGardensViewModel
    let userViewModel: UserViewModel

    @State private var radiusMeters: Int32 = 1_000

    @State private var region = MKCoordinateRegion(
        center: TEL_AVIV,
        span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05)
    )
    @State private var userCoord: CLLocationCoordinate2D? = nil
    @State private var camera: MapCameraPosition = .automatic

    @State private var dogGardens: [DogGarden] = []
    @State private var selectedGarden: DogGarden? = nil

    // Sheet state
    @State private var presentDogs: [DogDto] = []
    @State private var gardenPhotoUrl: String = ""

    // Dog picker
    @State private var showDogPicker = false
    @State private var myDogs: [DogDto] = []
    @State private var selectedDogIds: Set<String> = []

    // Observers / loader / bootstrap
    @State private var observeTasks: [Task<Void, Never>] = []
    @State private var sheetTasks: [Task<Void, Never>] = []
    @StateObject private var authProxy = LocationAuthProxy()
    @State private var didBootstrap = false
    @State private var isFetching = false
    @State private var lastFetchAt: Date = .distantPast
    @State private var watchdogWork: DispatchWorkItem?

    // Check-in state (single garden at a time)
    @State private var activeCheckinGardenId: String? = nil

    // Small popup for dog details
    @State private var showDogCard = false
    @State private var quickDog: DogDto? = nil

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            VStack(spacing: 0) {
                header(dbCount: dogGardens.count)
                Divider().opacity(0.1)

                ZStack(alignment: .top) {
                    MapContainer(
                        region: $region,
                        camera: $camera,
                        userCoord: userCoord,
                        radiusMeters: radiusMeters,
                        gardens: dogGardens,
                        onSelect: { g in openGardenSheet(g) }
                    )

                    if let g = selectedGarden {
                        let flags = checkinFlags(for: g)

                        GardenTopSheet(
                            garden: g,
                            photoUrl: gardenPhotoUrl,
                            presentDogs: presentDogs,
                            canCheckIn: flags.allowed,
                            isCheckedIn: (activeCheckinGardenId == g.id),
                            disabledReason: flags.reason,
                            onCheckIn: {
                                let fresh = checkinFlags(for: g)
                                if fresh.allowed { showDogPicker = true }
                            },
                            onCheckOut: { checkOutDogs(for: g) },
                            onClose: { closeGardenSheet() },
                            onDogTap: { d in
                                quickDog = d
                                withAnimation(.spring(response: 0.25, dampingFraction: 0.9)) {
                                    showDogCard = true
                                }
                            }
                        )
                        .transition(AnyTransition.move(edge: .top).combined(with: .opacity))
                        .zIndex(10)
                        .padding(.top, 8)
                        .frame(maxWidth: .infinity, alignment: .top)
                        .allowsHitTesting(true)
                    }

                    if isFetching {
                        VStack(spacing: 8) {
                            ProgressView()
                            Text("Loading dog parks…")
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                        .padding(12)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 12))
                        .shadow(radius: 4)
                        .padding(.top, 12)
                    }
                }
                .animation(.spring(response: 0.35, dampingFraction: 0.85), value: selectedGarden != nil)
            }
        }
        // Dog picker (multi-select)
        .sheet(isPresented: $showDogPicker) {
            if #available(iOS 16.0, *) {
                DogPickerSheet(
                    allDogs: myDogs,
                    initiallySelected: selectedDogIds,
                    onConfirm: { ids in
                        selectedDogIds = ids
                        if let g = selectedGarden { checkInDogs(for: g, ids: ids) }
                        showDogPicker = false
                    },
                    onCancel: { showDogPicker = false }
                )
                .presentationDetents([.medium]) // smaller than large
            } else {
                DogPickerSheet(
                    allDogs: myDogs,
                    initiallySelected: selectedDogIds,
                    onConfirm: { ids in
                        selectedDogIds = ids
                        if let g = selectedGarden { checkInDogs(for: g, ids: ids) }
                        showDogPicker = false
                    },
                    onCancel: { showDogPicker = false }
                )
            }
        }
        // Small dog info popup (very compact)
        .sheet(isPresented: $showDogCard) {
            if let d = quickDog {
                if #available(iOS 16.0, *) {
                    DogQuickCard(dog: d)
                        .presentationDetents([.height(240)])
                        .presentationDragIndicator(.hidden)
                } else {
                    DogQuickCard(dog: d)
                }
            } else {
                Text("No dog selected").padding()
            }
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            // 1) collect flows first
            startObserving()

            // 2) request permission -> loadLocation in VM
            authProxy.onAuthorized = { viewModel.loadLocation() }
            authProxy.requestOnce()

            // 3) user/dogs (suspend)
            Task {
                do { try await userViewModel.loadUserIfNeeded() }
                catch { platformLogger(tag: "PUP", message: "loadUserIfNeeded error \(error.localizedDescription)") }
            }

            // 4) bootstrap: set radius + scan (centers) + fetch
            if !didBootstrap {
                didBootstrap = true
                platformLogger(tag: "PUP", message: "Bootstrap: setRadius(\(radiusMeters)) + onScanClick()")
                viewModel.setRadius(meters: radiusMeters)
                fetchGardens(reason: "bootstrap_onScan", recenter: true)
            }
        }
        .onDisappear { cancelObservers() }
    }

    // MARK: Header + Slider

    private func header(dbCount: Int) -> some View {
        let radiusKm = max(1, Int(Double(radiusMeters) / 1000.0))
        return Surface {
            VStack(spacing: 12) {
                Text("Dog Parks : \(dbCount)").font(.headline)
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
                                platformLogger(tag: "PUP", message: "Slider → setRadius(\(m))")
                                viewModel.setRadius(meters: m)

                                // fetch around same center (no recenter)
                                fetchGardens(reason: "slider_radius_change", recenter: false)
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

    // MARK: Garden sheet + presence

    private func openGardenSheet(_ g: DogGarden) {
        selectedGarden = g
        startWatchingPresence(for: g)
        loadGardenPhoto(for: g)
    }

    private func closeGardenSheet() {
        selectedGarden = nil
        stopWatchingPresence()
    }

    private func startWatchingPresence(for garden: DogGarden) {
        stopWatchingPresence()
        viewModel.startWatchingPresence(gardenId: garden.id)

        let t1 = Task {
            for await arr in viewModel.presentDogs {
                await MainActor.run {
                    let list = decodeDogs(arr)
                    presentDogs = list
                    platformLogger(tag: "PUP", message: "presentDogs EMIT for garden=\(garden.id) count=\(list.count)")
                }
            }
        }
        sheetTasks.append(t1)
    }

    private func stopWatchingPresence() {
        viewModel.stopWatchingPresence()
        sheetTasks.forEach { $0.cancel() }
        sheetTasks.removeAll()
        presentDogs = []
        gardenPhotoUrl = ""
    }

    private func loadGardenPhoto(for garden: DogGarden) {
        Task {
            do { try await viewModel.loadGardenPhoto(placeId: garden.id, maxWidth: 900) }
            catch { platformLogger(tag: "PUP", message: "loadGardenPhoto error \(error.localizedDescription)") }
        }
        let t = Task {
            for await urlAny in viewModel.gardenPhotoUrl {
                await MainActor.run {
                    gardenPhotoUrl = urlAny ?? ""
                }
            }
        }
        sheetTasks.append(t)
    }

    // MARK: Check-in/out

    private func checkInDogs(for garden: DogGarden, ids: Set<String>) {
        let picked = myDogs.filter { ids.contains($0.id) }
        Task {
            do {
                try await viewModel.checkInDogs(gardenId: garden.id, dogs: picked)
                activeCheckinGardenId = garden.id
            } catch {
                platformLogger(tag: "PUP", message: "checkInDogs error \(error.localizedDescription)")
            }
        }
    }

    private func checkOutDogs(for garden: DogGarden) {
        let ids = Set(presentDogs.map { $0.id }).intersection(selectedDogIds)
        Task {
            do {
                try await viewModel.checkOutDogs(gardenId: garden.id, dogIds: Array(ids))
                if activeCheckinGardenId == garden.id { activeCheckinGardenId = nil }
            } catch {
                platformLogger(tag: "PUP", message: "checkOutDogs error \(error.localizedDescription)")
            }
        }
    }

    // MARK: Fetch orchestration (no async/await here)

    private func fetchGardens(reason: String, recenter: Bool) {
        let now = Date()
        if now.timeIntervalSince(lastFetchAt) < 0.2 {
            platformLogger(tag: "PUP", message: "fetch SKIP (throttled) reason=\(reason)")
            return
        }
        lastFetchAt = now

        isFetching = true
        platformLogger(tag: "PUP", message: "fetch START reason=\(reason) recenter=\(recenter) radius=\(radiusMeters)")

        armWatchdog(after: 2.5)

        if recenter {
            viewModel.onScanClick()  // centers + then getGoogleGardens()
        } else {
            viewModel.getGoogleGardens()
        }
    }

    private func armWatchdog(after seconds: Double) {
        watchdogWork?.cancel()
        let work = DispatchWorkItem { [weak viewModel] in
            if isFetching {
                platformLogger(tag: "PUP", message: "Watchdog → retry getGoogleGardens() (still fetching)")
                viewModel?.getGoogleGardens()
            }
        }
        watchdogWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: work)
    }

    // MARK: Observers

    private func startObserving() {
        cancelObservers()

        // gardens → UI
        let t1 = Task {
            for await g in viewModel.gardens {
                await MainActor.run {
                    let list = decodeGardens(g)
                    dogGardens = list
                    isFetching = false

                    let c = userCoord ?? region.center
                    platformLogger(tag: "PUP", message: "VM.gardens EMIT count=\(list.count) (loader OFF) radius=\(radiusMeters) center=(\(c.latitude), \(c.longitude))")
                }
            }
        }

        // radius mirror
        let t2 = Task {
            for await r in viewModel.radiusMeters {
                await MainActor.run {
                    let newR: Int32
                    if let r32 = r as? Int32 { newR = max(1000, r32) }
                    else if let rInt = r as? Int { newR = max(1000, Int32(rInt)) }
                    else { return }
                    guard newR != radiusMeters else { return }
                    radiusMeters = newR
                    platformLogger(tag: "PUP", message: "VM.radiusMeters MIRROR → \(newR)")
                }
            }
        }

        // search center → move camera (fetch is triggered by onScanClick)
        let t3 = Task {
            for await loc in viewModel.searchCenter {
                await MainActor.run {
                    let c = CLLocationCoordinate2D(latitude: loc.latitude, longitude: loc.longitude)
                    userCoord = c
                    region.center = c
                    if #available(iOS 17.0, *) { camera = .region(region) }
                    platformLogger(tag: "PUP", message: "VM.searchCenter EMIT → (\(c.latitude), \(c.longitude))")
                }
            }
        }

        // user/dogs
        let t4 = Task {
            for await u in userViewModel.currentUser {
                await MainActor.run {
                    let dogs = (u)?.dogList ?? []
                    myDogs = dogs
                    selectedDogIds = []
                    platformLogger(tag: "PUP", message: "User currentUser EMIT → dogs=\(dogs.count)")
                }
            }
        }

        observeTasks = [t1, t2, t3, t4]
    }

    private func cancelObservers() {
        observeTasks.forEach { $0.cancel() }
        observeTasks.removeAll()
        sheetTasks.forEach { $0.cancel() }
        sheetTasks.removeAll()
        watchdogWork?.cancel()
        watchdogWork = nil
    }

    // MARK: decode helpers

    private func decodeGardens(_ g: Any?) -> [DogGarden] {
        if let arr = g as? KotlinArray<AnyObject> {
            var tmp: [DogGarden] = []
            tmp.reserveCapacity(Int(arr.size))
            for i in 0..<Int(arr.size) {
                if let dg = arr.get(index: Int32(i)) as? DogGarden { tmp.append(dg) }
            }
            return tmp
        } else if let swiftArr = g as? [DogGarden] {
            return swiftArr
        } else if let anyArr = g as? [Any] {
            return anyArr.compactMap { $0 as? DogGarden }
        } else {
            return []
        }
    }

    private func decodeDogs(_ any: Any?) -> [DogDto] {
        if let arr = any as? KotlinArray<AnyObject> {
            var tmp: [DogDto] = []
            for i in 0..<Int(arr.size) {
                if let d = arr.get(index: Int32(i)) as? DogDto { tmp.append(d) }
            }
            return tmp
        } else if let swiftArr = any as? [DogDto] {
            return swiftArr
        } else if let anyArr = any as? [Any] {
            return anyArr.compactMap { $0 as? DogDto }
        } else {
            return []
        }
    }

    // MARK: distance + flags

    private func distanceToGarden(_ g: DogGarden) -> CLLocationDistance? {
        guard let c = userCoord else { return nil }
        let me = CLLocation(latitude: c.latitude, longitude: c.longitude)
        let gg = CLLocation(latitude: g.location.latitude, longitude: g.location.longitude)
        return me.distance(from: gg)
    }

    private struct CheckinFlags {
        let allowed: Bool
        let reason: String?
    }

    private func checkinFlags(for g: DogGarden) -> CheckinFlags {
        if let d = distanceToGarden(g), d > CHECKIN_MAX_DISTANCE_M {
            return .init(allowed: false, reason: "You must be within 450m to check in.")
        }
        if let active = activeCheckinGardenId, active != g.id {
            return .init(allowed: false, reason: "You’re already checked in at another garden.")
        }
        if myDogs.isEmpty {
            return .init(allowed: false, reason: "Add a dog to your profile first.")
        }
        return .init(allowed: true, reason: nil)
    }
}

// MARK: - Small surface
private struct Surface<Content: View>: View {
    @ViewBuilder let content: Content
    var body: some View {
        content
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground).opacity(0.9))
    }
}
