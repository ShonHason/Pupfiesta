//
//  EditProfileView.swift
//  iosApp
//

import SwiftUI
import Shared
import UIKit

// MARK: - Breed helpers
private let screenGradient = LinearGradient(
    colors: [
        Color(red: 252/255, green: 241/255, blue: 196/255),
        Color(red: 176/255, green: 212/255, blue: 248/255)
    ],
    startPoint: .topLeading,
    endPoint: .bottomTrailing
)

private func allBreeds() -> [Breed] {
    let arr = SwiftBridge().allBreeds()
    var out: [Breed] = []
    let n = Int(arr.size)
    out.reserveCapacity(n)
    for i in 0..<n {
        if let b = arr.get(index: Int32(i)) as? Breed { out.append(b) }
    }
    return out
}
private func breedTitle(_ b: Breed) -> String {
    b.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
}
private func breedByName(_ name: String, from breeds: [Breed]) -> Breed? {
    breeds.first { $0.name == name }
}

// MARK: - UI model

private struct DogCardModel: Identifiable {
    var id: String            // local UI id (unique)
    var backendId: String     // Firestore id; "" for new
    var name: String
    var breed: Breed?
    var weight: Int
    var isMale: Bool
    var isNeutered: Bool
    var isFriendly: Bool
    var localImage: UIImage?
    var isTemp: Bool { backendId.isEmpty }
}

// MARK: - View

@MainActor
struct EditProfileView: View {
    let dogsViewModel: DogsViewModel

    // cached breeds
    @State private var breeds: [Breed] = allBreeds()

    @State private var ownerName = ""
    @State private var email = ""

    @State private var dogs: [DogCardModel] = []
    @State private var selectedDogId: String? = nil

    // editor state
    @State private var isEditing = false
    @State private var workingDog: DogCardModel? = nil
    @State private var selectedBreedName: String = ""
    @State private var originalDogBeforeEdit: DogCardModel? = nil   // snapshot for Cancel

    // delete confirm
    @State private var showDeleteConfirm = false

    // photo
    @State private var showImagePicker = false
    @State private var pickedUIImage: UIImage? = nil

    // ux
    @State private var busy = false
    @State private var errorMessage = ""

    private var addingTempDog: Bool { dogs.contains { $0.isTemp } }

    private func unwrap<T>(_ binding: Binding<T?>, defaultValue: @autoclosure @escaping () -> T) -> Binding<T> {
        Binding(
            get: { binding.wrappedValue ?? defaultValue() },
            set: { binding.wrappedValue = $0 }
        )
    }

    var body: some View {
        ZStack {
            screenGradient.ignoresSafeArea()   // background layer

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    OwnerHeader(ownerName: ownerName, email: email)
                        .padding(.horizontal)

                    Text("Dogs")
                        .font(.headline)
                        .padding(.horizontal)

                    DogGrid(
                        dogs: dogs,
                        selectedDogId: selectedDogId,
                        busy: busy,
                        onSelect: handleSelectDog(_:),
                        onAdd: handleAddDog
                    )
                    .padding(.horizontal)

                    if workingDog != nil {
                        DogDetailsCard(
                            working: unwrap($workingDog, defaultValue: placeholderDog()),
                            isEditing: $isEditing,
                            selectedBreedName: $selectedBreedName,
                            onTapAvatar: {
                                guard isEditing else { return }
                                pickedUIImage = nil
                                showImagePicker = true
                            },
                            onSave: { toSave in
                                Task { await handleSave(toSave) }
                            },
                            onCancel: { handleCancel() },
                            onDelete: { showDeleteConfirm = true }
                        )
                        .padding(.horizontal)
                    }
                }
                .padding(.vertical, 16)
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .task { await initialLoad() }
        .sheet(isPresented: $showImagePicker, onDismiss: {
            guard var w = workingDog, let img = pickedUIImage else { return }
            w.localImage = img
            workingDog = w
        }) {
            PhotoPicker(image: $pickedUIImage)
        }
        .confirmationDialog("Delete this dog?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Delete", role: .destructive) {
                Task { await confirmDelete() }
            }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Error", isPresented: Binding(get: { !errorMessage.isEmpty }, set: { _ in })) {
            Button("OK") { errorMessage = "" }
        } message: { Text(errorMessage) }
        .onChange(of: isEditing) { newValue in
            if newValue, let w = workingDog {
                originalDogBeforeEdit = w
            }
        }
    }

    // MARK: - load & mapping

    private func initialLoad() async {
        if selectedBreedName.isEmpty { selectedBreedName = breeds.first?.name ?? "" }
        do {
            let user = try await dogsViewModel.getUserOrThrow()
            applyUser(user)
            if selectedDogId == nil { selectFirstDog() }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func selectFirstDog() {
        guard let first = dogs.first else { return }
        selectedDogId = first.id
        workingDog = first
        selectedBreedName = first.breed?.name ?? (breeds.first?.name ?? "")
    }

    private func applyUser(_ u: UserDto) {
        email = u.email
        ownerName = u.name
        if let list = u.dogList as? [DogDto] {
            dogs = list.enumerated().map { (i, d) in toCardModel(d, uiFallback: "remote-\(i)-\(d.name)") }
        } else {
            dogs = []
        }
        if let sel = selectedDogId, let keep = dogs.first(where: { $0.id == sel }), !isEditing {
            workingDog = keep
        }
    }

    private func toCardModel(_ d: DogDto, uiFallback: String) -> DogCardModel {
        let backendId = d.id ?? ""
        let uiId = backendId.isEmpty ? uiFallback : backendId
        return DogCardModel(
            id: uiId,
            backendId: backendId,
            name: d.name,
            breed: d.breed,
            weight: Int(d.weight),
            isMale: d.isMale,
            isNeutered: d.isNeutered,
            isFriendly: d.isFriendly,
            localImage: nil
        )
    }

    private func placeholderDog() -> DogCardModel {
        DogCardModel(
            id: "placeholder",
            backendId: "",
            name: "",
            breed: breeds.first,
            weight: 10,
            isMale: true,
            isNeutered: false,
            isFriendly: true,
            localImage: nil
        )
    }

    // MARK: - Add/Select

    private func handleSelectDog(_ dog: DogCardModel) {
        if selectedDogId == dog.id {
            selectedDogId = nil
            workingDog = nil
            isEditing = false
        } else {
            selectedDogId = dog.id
            workingDog = dog
            selectedBreedName = dog.breed?.name ?? (breeds.first?.name ?? "")
            isEditing = false
        }
    }

    private func handleAddDog() {
        guard !addingTempDog, !busy else { return }
        var dog = DogCardModel(
            id: "temp-\(UUID().uuidString)",
            backendId: "",
            name: "New Dog",
            breed: breeds.first,
            weight: 10,
            isMale: true,
            isNeutered: false,
            isFriendly: true,
            localImage: nil
        )
        while dogs.contains(where: { $0.id == dog.id }) {
            dog.id = "temp-\(UUID().uuidString)"
        }
        dogs.append(dog)
        selectedDogId = dog.id
        workingDog = dog
        selectedBreedName = dog.breed?.name ?? (breeds.first?.name ?? "")
        isEditing = true
    }

    // MARK: - Save/Cancel/Delete

    private func toDtoAdd(_ c: DogCardModel) -> DogDto {
        DogDto(
            id: "",
            name: c.name,
            breed: c.breed ?? breeds.first!,
            weight: Int32(c.weight),
            imgUrl: "",
            isFriendly: c.isFriendly,
            isMale: c.isMale,
            isNeutered: c.isNeutered,
            ownerId: ""
        )
    }

    private func toDtoUpdate(_ c: DogCardModel) -> DogDto {
        DogDto(
            id: c.backendId,
            name: c.name,
            breed: c.breed ?? breeds.first!,
            weight: Int32(c.weight),
            imgUrl: "",
            isFriendly: c.isFriendly,
            isMale: c.isMale,
            isNeutered: c.isNeutered,
            ownerId: ""
        )
    }

    private func handleCancel() {
        guard let sel = selectedDogId else { return }
        if let original = originalDogBeforeEdit {
            workingDog = original
            if let idx = dogs.firstIndex(where: { $0.id == sel }) {
                dogs[idx] = original
            }
        }
        isEditing = false
    }

    private func handleSave(_ toSave: DogCardModel) async {
        busy = true
        defer { busy = false }
        do {
            if toSave.backendId.isEmpty {
                // ADD
                let saved = try await dogsViewModel.addDogAndLinkToUserOrThrow(dog: toDtoAdd(toSave))

                // patch temp chip to real
                let newId = saved.id
                if !newId.isEmpty, let idx = dogs.firstIndex(where: { $0.id == toSave.id }) {
                    var patched = toSave
                    patched.backendId = newId
                    patched.id = newId
                    dogs[idx] = patched
                    selectedDogId = patched.id
                    workingDog = patched
                }

                let user = try await dogsViewModel.getUserOrThrow()
                applyUser(user)

                if !saved.id.isEmpty, let found = dogs.first(where: { $0.backendId == saved.id }) {
                    selectedDogId = found.id
                    workingDog = found
                    selectedBreedName = found.breed?.name ?? (breeds.first?.name ?? "")
                } else {
                    selectFirstDog()
                }
                isEditing = false
            } else {
                // UPDATE
                try await dogsViewModel.updateDogAndUserOrThrow(dog: toDtoUpdate(toSave))

                // optimistic local patch
                if let idx = dogs.firstIndex(where: { $0.id == toSave.id }) {
                    dogs[idx] = toSave
                }

                let user = try await dogsViewModel.getUserOrThrow()
                applyUser(user)
                if let refreshed = dogs.first(where: { $0.backendId == toSave.backendId }) {
                    selectedDogId = refreshed.id
                    workingDog = refreshed
                    selectedBreedName = refreshed.breed?.name ?? (breeds.first?.name ?? "")
                }
                isEditing = false
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func confirmDelete() async {
        platformLogger(tag: "PUP", message: "DEL1 selectedDogId=\(selectedDogId ?? "nil")")

        guard let sel = selectedDogId,
              let d = dogs.first(where: { $0.id == sel }) else {
            platformLogger(tag: "PUP", message: "DEL2 no dog for selected id")
            return
        }

        platformLogger(tag: "PUP", message: "DEL3 candidate uiId=\(d.id) backendId=\(d.backendId) isTemp=\(d.isTemp)")
        busy = true
        defer { busy = false }

        if d.isTemp {
            platformLogger(tag: "PUP", message: "DEL4 temp → local remove only")
            dogs.removeAll { $0.id == sel }
            selectedDogId = nil
            workingDog = nil
            isEditing = false
            return
        }

        do {
            let idOrName = d.backendId.isEmpty ? d.name : d.backendId
            platformLogger(tag: "PUP", message: "DEL5 calling VM delete idOrName=\(idOrName)")
            try await dogsViewModel.deleteDogAndUserOrThrow(dogId: idOrName)
            platformLogger(tag: "PUP", message: "DEL6 VM delete finished OK")

            let user = try await dogsViewModel.getUserOrThrow()
            applyUser(user)

            selectedDogId = nil
            workingDog = nil
            isEditing = false
            platformLogger(tag: "PUP", message: "DEL7 UI refreshed (dogs.count=\(dogs.count))")
        } catch {
            platformLogger(tag: "PUP", message: "DELX error \(error.localizedDescription)")
            errorMessage = error.localizedDescription
        }
    }

    // MARK: - Owner header

    private struct OwnerHeader: View {
        let ownerName: String
        let email: String
        var body: some View {
            let displayOwnerName = ownerName.isEmpty ? "—" : ownerName
            let displayEmail = email.isEmpty ? "—" : email
            VStack(alignment: .leading, spacing: 4) {
                Text(displayOwnerName).font(.title.bold())
                Text(displayEmail).font(.subheadline).foregroundColor(.secondary)
            }
        }
    }

    // MARK: - 5-per-row grid

    private struct DogGrid: View {
        let dogs: [DogCardModel]
        let selectedDogId: String?
        let busy: Bool
        let onSelect: (DogCardModel) -> Void
        let onAdd: () -> Void

        // exactly 5 columns, nicely spaced
        private let columns = Array(
            repeating: GridItem(.flexible(minimum: 40), spacing: 12, alignment: .center),
            count: 5
        )

        var body: some View {
            LazyVGrid(columns: columns, alignment: .center, spacing: 14) {
                ForEach(dogs, id: \.id) { dog in
                    DogGridTile(
                        dog: dog,
                        isSelected: selectedDogId == dog.id,
                        disabled: busy
                    ) { onSelect(dog) }
                }
                // Always show the + tile (won't disappear after 5 items)
                AddGridTile(disabled: busy, action: onAdd)
            }
            .padding(.bottom, 8) // avoid bottom clipping
        }
    }

    private struct DogGridTile: View {
        let dog: DogCardModel
        let isSelected: Bool
        let disabled: Bool
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                DogAvatar(dog: dog)
                    .frame(width: 54, height: 54) // smaller circles
                    .overlay(Circle().stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2))
                    .contentShape(Circle()) // full hit area
            }
            .buttonStyle(.plain)
            .disabled(disabled)
        }
    }

    private struct AddGridTile: View {
        let disabled: Bool
        let action: () -> Void
        var body: some View {
            Button(action: action) {
                ZStack {
                    Circle().stroke(disabled ? Color.gray : Color.blue, lineWidth: 2)
                    Image(systemName: "plus")
                        .font(.title3.bold()) // slightly smaller icon
                        .foregroundColor(disabled ? .gray : .blue)
                }
                .frame(width: 54, height: 54)     // matches dog circles
                .contentShape(Circle())           // full hit area
            }
            .buttonStyle(.plain)
            .disabled(disabled)
        }
    }

    // MARK: - Details card

    private struct DogDetailsCard: View {
        @Binding var working: DogCardModel
        @Binding var isEditing: Bool
        @Binding var selectedBreedName: String

        var onTapAvatar: () -> Void
        var onSave: (DogCardModel) -> Void
        var onCancel: () -> Void
        var onDelete: () -> Void

        let breeds = allBreeds()

        var body: some View {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 12) {
                    Button { if isEditing { onTapAvatar() } } label: {
                        DogAvatar(dog: working)
                            .frame(width: 64, height: 64)
                            .overlay(isEditing ? Circle().stroke(Color.blue, lineWidth: 2) : nil)
                    }
                    .buttonStyle(.plain)
                    .disabled(!isEditing)

                    if isEditing {
                        TextField("Dog Name", text: $working.name)
                            .font(.title3.bold())
                            .textFieldStyle(.roundedBorder)
                    } else {
                        Text(working.name).font(.title3.bold())
                    }
                    Spacer()
                }

                if isEditing {
                    Picker("Breed", selection: $selectedBreedName) {
                        ForEach(breeds, id: \.name) { b in
                            Text(breedTitle(b)).tag(b.name)
                        }
                    }
                    .onChange(of: selectedBreedName) { new in
                        working.breed = breedByName(new, from: breeds)
                    }
                } else {
                    HStack {
                        Text("Breed").bold(); Spacer()
                        Text(working.breed.map(breedTitle) ?? "—").foregroundColor(.secondary)
                    }
                }

                if isEditing {
                    Stepper(value: $working.weight, in: 1...200) {
                        HStack { Text("Weight").bold(); Spacer(); Text("\(working.weight) kg").font(.headline) }
                    }
                } else {
                    HStack(spacing: 12) {
                        Text("Weight").bold()
                        Text(" \(working.weight) kg").font(.headline)
                        Spacer()
                    }
                }

                BinaryRow(title: "Gender",   left: "Male", right: "Female", value: $working.isMale,     enabled: isEditing)
                BinaryRow(title: "Neutered", left: "Yes",  right: "No",     value: $working.isNeutered, enabled: isEditing)
                BinaryRow(title: "Friendly", left: "Yes",  right: "No",     value: $working.isFriendly, enabled: isEditing)

                HStack(spacing: 12) {
                    Button(isEditing ? "Save" : "Edit") {
                        if isEditing {
                            if working.breed == nil,
                               let b = breedByName(selectedBreedName, from: breeds) ?? breeds.first {
                                working.breed = b
                            }
                            onSave(working)
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(isEditing ? .green : .blue)
                    .disabled(working.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)

                    if isEditing {
                        Button("Cancel") { onCancel() }
                            .buttonStyle(.bordered)

                        Button("Delete", role: .destructive) { onDelete() }
                            .buttonStyle(.bordered)
                    }
                }
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.systemBackground).opacity(0.92))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.white.opacity(0.35), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.12), radius: 14, x: 0, y: 6)
        }
    }

        private struct BinaryRow: View {
            let title: String
            let left: String
            let right: String
            @Binding var value: Bool
            let enabled: Bool
            var body: some View {
                VStack(alignment: .leading, spacing: 8) {
                    Text(title).bold()
                    HStack(spacing: 8) {
                        Seg(text: left,  selected: value,  enabled: enabled) { if enabled { value = true } }
                        Seg(text: right, selected: !value, enabled: enabled) { if enabled { value = false } }
                    }
                }
            }
            private struct Seg: View {
                let text: String
                let selected: Bool
                let enabled: Bool
                let action: () -> Void
                var body: some View {
                    Button(action: action) {
                        Text(text).font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity).padding(.vertical, 8)
                            .background(selected ? Color.white : Color.gray.opacity(0.2))
                            .foregroundColor(.primary)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .opacity(enabled ? 1 : 0.5)
                    .disabled(!enabled)
                }
            }
        }
    }

    // MARK: - Avatar

    private struct DogAvatar: View {
        let dog: DogCardModel
        var body: some View {
            ZStack {
                Circle().fill(Color(.secondarySystemBackground))
                if let img = dog.localImage {
                    Image(uiImage: img).resizable().scaledToFill()
                } else {
                    Image(systemName: "pawprint.fill").font(.largeTitle)
                }
            }
            .clipShape(Circle())
            .overlay(Circle().strokeBorder(Color.white.opacity(0.6), lineWidth: 1))
            .shadow(radius: 2)
        }
    }

