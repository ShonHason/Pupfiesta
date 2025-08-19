import SwiftUI
import Shared

struct PostAuthView: View {
    let viewModel: PostAuthViewModel
    var onDone: () -> Void = {}

    @State private var state: PostAuthState
    @State private var observeTask: Task<Void, Never>? = nil
    @State private var goToTabs = false  // <-- trigger navigation

    init(viewModel: PostAuthViewModel, onDone: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.onDone = onDone
        _state = State(initialValue: viewModel.state.value)
    }

    /// Read Google Places key from Info.plist (same key you used elsewhere)
    private var placesKey: String {
        Bundle.main.object(forInfoDictionaryKey: "GOOGLE_PLACES_API_KEY") as? String ?? ""
    }

    private let fallback = Location(latitude: 32.0853, longitude: 34.7818)

    // Build the tabs destination with concrete VMs (no placeholders)
    @ViewBuilder
    private func makeTabsRoot() -> some View {
        let repo = RemoteFirebaseRepository()
        let gardensVM = DogGardensViewModel(
            firebaseRepo: repo,
            gardensRepo: GoogleGardensRepository(client: httpClient(), apiKey: placesKey),
            defaultLanguage: "he"
        )
        let dogsVM = DogsViewModel(firebaseRepo: repo)

        // Get a UserViewModel instance from Koin/ServiceLocator
        let userVM = ServiceLocator().userVM()

        TabsRootView(dogsVM: dogsVM, gardensVM: gardensVM, userVM: userVM)
    }

    var body: some View {
        ZStack {
            // Hidden link that activates when we're done
            NavigationLink(destination: makeTabsRoot(), isActive: $goToTabs) { EmptyView() }
                .hidden()

            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                LottieView(name: "pet_lovers", loopMode: .loop)
                    .frame(width: 180, height: 180)
                    .scaleEffect(0.95)

                Text(primaryLine)
                    .font(.title3.weight(.semibold))
                    .multilineTextAlignment(.center)

                if let sub = secondaryLine {
                    Text(sub)
                        .font(.footnote)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }

                if state is PostAuthState.Error {
                    Button("Continue") { finish() }
                        .buttonStyle(.borderedProminent)
                        .padding(.top, 8)
                }
            }
            .padding(24)
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6)
            .padding(.horizontal, 24)
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            observeTask?.cancel()
            observeTask = Task {
                for await s in viewModel.state {
                    await MainActor.run {
                        state = s
                        if s is PostAuthState.Success || s is PostAuthState.Error {
                            Task {
                                try? await Task.sleep(nanoseconds: 700_000_000)
                                finish()
                            }
                        }
                    }
                }
            }
            viewModel.run(
                radiusMeters: 11_000,
                locationTimeoutMs: 6_000,
                language: "he",
                fallbackCenter: fallback
            )
        }
        .onDisappear { observeTask?.cancel(); observeTask = nil }
    }

    private func finish() {
        // Navigate to tabs
        goToTabs = true
        // Still call the external hook if parent wants to react
        onDone()
    }

    private var primaryLine: String {
        switch state {
        case let r as PostAuthState.Running: return r.message
        case let s as PostAuthState.Success: return "Saved \(s.savedCount) dog parks!"
        case is PostAuthState.Error:         return "Setup error"
        default:                             return "Preparing…"
        }
    }
    private var secondaryLine: String? {
        switch state {
        case is PostAuthState.Running:       return "This will only take a moment…"
        case let e as PostAuthState.Error:   return e.message
        default: return nil
        }
    }
}
