//  DogPickerSheet.swift
//  iosApp

import SwiftUI
import Shared

/// A simple multi-select sheet for choosing dogs to check-in.
/// Now fetches the **latest** dogs from the DB via DogsViewModel.
struct DogPickerSheet: View {
    // KMM VM to pull fresh user dogs
    let dogsViewModel: DogsViewModel

    // initial selection coming from caller
    let initiallySelected: Set<String>
    let onConfirm: (Set<String>) -> Void
    let onCancel: () -> Void

    @Environment(\.dismiss) private var dismiss

    // live data fetched from DB
    @State private var dogs: [DogDto] = []

    // selection state
    @State private var selected: Set<String> = []

    // ui state
    @State private var isLoading = false
    @State private var errorMessage: String?

    init(
        dogsViewModel: DogsViewModel,
        initiallySelected: Set<String>,
        onConfirm: @escaping (Set<String>) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.dogsViewModel = dogsViewModel
        self.initiallySelected = initiallySelected
        self.onConfirm = onConfirm
        self.onCancel = onCancel
        _selected = State(initialValue: initiallySelected)
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading && dogs.isEmpty {
                    // full-screen loader
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("Loading your dogs…")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        if dogs.isEmpty {
                            Section {
                                VStack(spacing: 8) {
                                    Text("No dogs found")
                                        .font(.headline)
                                    Text("Add a dog in your profile to check in.")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                        .multilineTextAlignment(.center)
                                }
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.vertical, 24)
                            }
                        } else {
                            Section("Choose dog(s)") {
                                ForEach(dogs, id: \.id) { dog in
                                    DogRow(
                                        dog: dog,
                                        isSelected: selected.contains(dog.id),
                                        toggle: { toggle(dog.id) }
                                    )
                                }
                            }
                        }

                        if let err = errorMessage {
                            Section {
                                Text(err)
                                    .font(.footnote)
                                    .foregroundColor(.red)
                            }
                        }
                    }
                    // Pull-to-refresh to pick up just-added changes/images
                    .refreshable { await reload() }
                }
            }
            .navigationTitle("Pick Dogs")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                        dismiss()
                    }
                    .disabled(isLoading)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        onConfirm(selected)
                        dismiss()
                    }
                    .disabled(dogs.isEmpty)
                }
            }
            .task { await reload() } // initial load
        }
    }

    // MARK: - Data
    private func reload() async {
        await MainActor.run {
            if dogs.isEmpty { isLoading = true } // avoid flicker on refresh
            errorMessage = nil
        }
        do {
            let user = try await dogsViewModel.getUserOrThrow()
            let fresh: [DogDto]
            if let list = user.dogList as? [DogDto] {
                // If you later add timestamps, sort by most-recent here.
                fresh = list
            } else {
                fresh = []
            }
            await MainActor.run {
                dogs = fresh
                // keep previous selections that still exist
                selected = selected.intersection(Set(fresh.map { $0.id }))
                isLoading = false
            }
        } catch {
            await MainActor.run {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    // MARK: - Selection
    private func toggle(_ id: String) {
        if selected.contains(id) {
            selected.remove(id)
        } else {
            selected.insert(id)
        }
    }
}

// MARK: - Row
private struct DogRow: View {
    let dog: DogDto
    let isSelected: Bool
    let toggle: () -> Void

    // Prefer dogPictureUrl, fall back to photoUrl (your DTO uses both in places)
    private var imageUrlString: String? {
        if !dog.dogPictureUrl.isEmpty { return dog.dogPictureUrl }
        if !dog.photoUrl.isEmpty { return dog.photoUrl }
        return nil
    }

    var body: some View {
        Button(action: toggle) {
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(Color(.systemGray5))
                        .frame(width: 36, height: 36)

                    if let urlStr = imageUrlString, let url = URL(string: urlStr) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .empty:
                                ProgressView().scaleEffect(0.7)
                            case .success(let image):
                                image.resizable().scaledToFill()
                            case .failure:
                                Text(initials(from: dog.name))
                                    .font(.subheadline).bold()
                                    .foregroundColor(.primary)
                            @unknown default:
                                Text(initials(from: dog.name))
                                    .font(.subheadline).bold()
                                    .foregroundColor(.primary)
                            }
                        }
                        .frame(width: 36, height: 36)
                        .clipShape(Circle())
                    } else {
                        Text(initials(from: dog.name))
                            .font(.subheadline).bold()
                            .foregroundColor(.primary)
                    }
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(dog.name.isEmpty ? "Unnamed Dog" : dog.name).font(.body)
                    Text(formatBreed(dog.breed))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .imageScale(.large)
                    .foregroundStyle(isSelected ? Color.accentColor : .secondary)
            }
        }
    }

    // MARK: helpers
    private func initials(from name: String) -> String {
        let n = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if n.isEmpty { return "DG" } // Dog generic
        let parts = n.split(separator: " ")
        let first = parts.first?.first.map(String.init) ?? ""
        let second = parts.dropFirst().first?.first.map(String.init) ?? ""
        return (first + second).uppercased()
    }

    private func formatBreed(_ breed: Breed) -> String {
        breed.name.replacingOccurrences(of: "_", with: " ").capitalized
    }
}
