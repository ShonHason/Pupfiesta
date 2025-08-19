//  PostAuthView.swift
//  iosApp

import SwiftUI
import Shared

struct PostAuthView: View {
    // KMM VM
    let viewModel: PostAuthViewModel
    /// Called when the post-auth flow finishes (success or error)
    var onDone: () -> Void = {}

    // State
    @State private var state: PostAuthState
    @State private var observeTask: Task<Void, Never>? = nil

    init(viewModel: PostAuthViewModel, onDone: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.onDone = onDone
        _state = State(initialValue: viewModel.state.value)
    }

    // Fallback center (Tel Aviv) — same as Android example
    private let fallback = Location(latitude: 32.0853, longitude: 34.7818)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 18) {
                // Lottie — keep it clean and frameless
                LottieView(name: "pet_lovers", loopMode: .loop)
                    .frame(width: 180, height: 180)
                    .scaleEffect(0.95)
                    .accessibilityHidden(true)

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
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6)
            .padding(.horizontal, 24)
        }
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            // Observe VM state
            observeTask?.cancel()
            observeTask = Task {
                for await s in viewModel.state {
                    await MainActor.run {
                        state = s
                        if s is PostAuthState.Success || s is PostAuthState.Error {
                            // mirror Android: short pause, then exit
                            Task {
                                try? await Task.sleep(nanoseconds: 700_000_000)
                                finish()
                            }
                        }
                    }
                }
            }

            // Kick off the workflow (once)
            // NOTE: If your Swift bridge requires KotlinLong for the timeout, use:
            // let timeout = KotlinLong(value: 6_000)
            // viewModel.run(radiusMeters: 11_000, locationTimeoutMs: timeout, language: "he", fallbackCenter: fallback)
            viewModel.run(
                radiusMeters: 11_000,
                locationTimeoutMs: 6_000,
                language: "he",
                fallbackCenter: fallback
            )
        }
        .onDisappear { observeTask?.cancel(); observeTask = nil }
    }

    private func finish() { onDone() }

    // MARK: - Text helpers
    private var primaryLine: String {
        switch state {
        case let r as PostAuthState.Running: return r.message
        case let s as PostAuthState.Success: return "Saved \(s.savedCount) dog parks!"
        case let e as PostAuthState.Error:   return "Setup error"
        default: return "Preparing…"
        }
    }
    private var secondaryLine: String? {
        switch state {
        case is PostAuthState.Running: return "This will only take a moment…"
        case let e as PostAuthState.Error: return e.message
        default: return nil
        }
    }
}
