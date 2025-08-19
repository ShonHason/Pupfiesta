//
//  GardenTopSheet.swift
//  iosApp
//

import SwiftUI
import Shared
import UIKit

// MARK: - Top sheet (selected garden)
struct GardenTopSheet: View {
    let garden: DogGarden
    let photoUrl: String
    let presentDogs: [DogDto]

    // Flags from parent
    let canCheckIn: Bool
    let isCheckedIn: Bool
    let disabledReason: String?

    // Actions
    let onCheckIn: () -> Void
    let onCheckOut: () -> Void
    let onClose: () -> Void
    let onDogTap: (DogDto) -> Void

    // Tooltip (phone: long-press)
    @State private var tooltipDogId: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            // Header
            HStack(spacing: 10) {
                GardenCircleImage(urlString: photoUrl, diameter: 44)
                VStack(alignment: .leading, spacing: 2) {
                    Text(garden.name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    HStack(spacing: 6) {
                        Image(systemName: "pawprint.circle.fill")
                            .imageScale(.small)
                            .foregroundColor(.secondary)
                        Text("\(presentDogs.count) here now")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Close")
            }

            // Dogs row (button for reliable taps in a horizontal scroll)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(presentDogs, id: \.id) { d in
                        Button {
                            onDogTap(d)
                        } label: {
                            GardenDogChip(dog: d, showLabel: tooltipDogId == d.id)
                        }
                        .buttonStyle(.plain)
                        .contentShape(Rectangle())
                        .onLongPressGesture(minimumDuration: 0.2) {
                            tooltipDogId = d.id
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                if tooltipDogId == d.id { tooltipDogId = nil }
                            }
                        }
                    }
                }
                .padding(.vertical, 2)
            }

            // Reason line (if check-in is disabled)
            if let reason = disabledReason, !isCheckedIn, !canCheckIn {
                Text(reason)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.top, 2)
            }

            // Actions
            HStack(spacing: 8) {
                if isCheckedIn {
                    Button(role: .destructive, action: onCheckOut) {
                        Label("Check out", systemImage: "rectangle.portrait.and.arrow.right")
                            .labelStyle(.titleAndIcon)
                            .font(.caption)
                    }
                    .buttonStyle(.bordered)
                }

                Spacer()

                Button(action: onCheckIn) {
                    Label(isCheckedIn ? "Checked-in" : "Check in", systemImage: "checkmark.circle.fill")
                        .labelStyle(.titleAndIcon)
                        .font(.caption)
                }
                .buttonStyle(.borderedProminent)
                .disabled(isCheckedIn || !canCheckIn)
                .opacity((isCheckedIn || !canCheckIn) ? 0.5 : 1)
            }
        }
        .padding(10)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.ultraThinMaterial)
                .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [Color.white.opacity(0.55), Color.white.opacity(0.18)],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ),
                    lineWidth: 0.8
                )
        )
        .padding(.horizontal, 10)
    }
}

// MARK: - Tiny circular garden image
private struct GardenCircleImage: View {
    let urlString: String
    let diameter: CGFloat

    var body: some View {
        ZStack {
            Circle().fill(Color(.secondarySystemBackground))
            if !urlString.isEmpty, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty: ProgressView().scaleEffect(0.6)
                    case .failure: Image(systemName: "photo").font(.title3).foregroundColor(.secondary)
                    case .success(let img): img.resizable().scaledToFill()
                    @unknown default: EmptyView()
                    }
                }
                .clipShape(Circle())
            } else {
                Image(systemName: "leaf")
                    .font(.title3)
                    .foregroundColor(.secondary)
            }
        }
        .frame(width: diameter, height: diameter)
        .overlay(Circle().stroke(Color.white.opacity(0.85), lineWidth: 0.8))
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

// MARK: - Dog chip (avatar + optional tiny name label)
private struct GardenDogChip: View {
    let dog: DogDto
    let showLabel: Bool

    var body: some View {
        ZStack(alignment: .bottom) {
            GardenDogAvatar(dog: dog, diameter: 30)
            if showLabel {
                Text(dog.name.isEmpty ? "Dog" : dog.name)
                    .font(.caption2.weight(.medium))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(.thinMaterial, in: Capsule())
                    .overlay(Capsule().stroke(Color.white.opacity(0.5), lineWidth: 0.5))
                    .offset(y: 18)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
        }
    }
}

// MARK: - Small round dog avatar (DogDto version)
public struct GardenDogAvatar: View {
    let dog: DogDto
    var diameter: CGFloat = 36

    public init(dog: DogDto, diameter: CGFloat = 36) {
        self.dog = dog
        self.diameter = diameter
    }

    public var body: some View {
        ZStack {
            if !dog.dogPictureUrl.isEmpty, let url = URL(string: dog.dogPictureUrl) {
                AsyncImage(url: url) { p in
                    switch p {
                    case .empty: ProgressView().scaleEffect(0.8)
                    case .failure: Circle().fill(Color.gray.opacity(0.2)).overlay(Text("🐶"))
                    case .success(let img): img.resizable().scaledToFill()
                    @unknown default: EmptyView()
                    }
                }
            } else {
                Circle().fill(Color.gray.opacity(0.15))
                    .overlay(Text("🐶"))
            }
        }
        .frame(width: diameter, height: diameter)
        .clipShape(Circle())
        .overlay(Circle().stroke(Color.white.opacity(0.9), lineWidth: 0.8))
        .shadow(color: .black.opacity(0.06), radius: 3, x: 0, y: 1)
        .contentShape(Circle())
    }
}

// MARK: - Tiny, elegant card for dog details (modern)
public struct DogQuickCard: View {
    let dog: DogDto
    @Environment(\.dismiss) private var dismiss

    public init(dog: DogDto) { self.dog = dog }

    // Subtle accent based on friendliness / gender
    private var accentGradient: LinearGradient {
        let colors: [Color]
        if dog.isFriendly {
            colors = dog.isMale
                ? [Color.blue.opacity(0.85), Color.mint.opacity(0.85)]
                : [Color.pink.opacity(0.85), Color.purple.opacity(0.85)]
        } else {
            colors = [Color.orange.opacity(0.85), Color.red.opacity(0.85)]
        }
        return LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Tiny grabber so the sheet feels native even if drag indicator is hidden
            Capsule()
                .fill(Color.primary.opacity(0.15))
                .frame(width: 28, height: 4)
                .padding(.top, 8)
                .padding(.bottom, 8)

            VStack(spacing: 12) {
                // Header
                HStack(spacing: 12) {
                    RingAvatar(urlString: dog.dogPictureUrl, size: 56, ringWidth: 2, gradient: accentGradient)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(dog.name.isEmpty ? "Dog" : dog.name)
                            .font(.headline.weight(.semibold))
                            .lineLimit(1)
                        Text(dog.breed.name.replacingOccurrences(of: "_", with: " ").capitalized)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    Spacer()
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill").font(.title3)
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close")
                }

                // Chips
                HStack(spacing: 8) {
                    InfoChip(icon: "scalemass", text: "\(Int(dog.weight)) kg")
                    InfoChip(icon: dog.isMale ? "person.fill" : "person", text: dog.isMale ? "Male" : "Female")
                    InfoChip(icon: dog.isNeutered ? "scissors" : "scissors", text: dog.isNeutered ? "Neutered" : "—")
                    InfoChip(icon: dog.isFriendly ? "hand.thumbsup.fill" : "hand.thumbsup", text: dog.isFriendly ? "Friendly" : "Shy")
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Soft divider
                Rectangle()
                    .fill(Color.primary.opacity(0.08))
                    .frame(height: 1)
                    .overlay(
                        accentGradient.opacity(0.25)
                            .mask(Rectangle().frame(height: 1))
                    )

                // Mini bio line (optional feel-good touch)
                HStack(spacing: 8) {
                    Image(systemName: "sparkles")
                        .imageScale(.small)
                        .foregroundColor(.secondary)
                    Text(summaryLine(for: dog))
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(14)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .overlay(
                        // Chic gradient hairline
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(accentGradient.opacity(0.45), lineWidth: 0.8)
                    )
                    .shadow(color: .black.opacity(0.10), radius: 16, x: 0, y: 8)
            )
            .padding(.horizontal, 12)
            .padding(.bottom, 10)
        }
        .background(Color.clear)
    }

    private func summaryLine(for d: DogDto) -> String {
        let sex = d.isMale ? "Male" : "Female"
        let mood = d.isFriendly ? "friendly" : "shy"
        let neuter = d.isNeutered ? "neutered" : "not neutered"
        return "\(sex), \(neuter), \(Int(d.weight))kg • very \(mood)"
    }
}

// MARK: - Pieces

private struct RingAvatar: View {
    let urlString: String
    let size: CGFloat
    let ringWidth: CGFloat
    let gradient: LinearGradient

    var body: some View {
        ZStack {
            Circle().fill(Color(.secondarySystemBackground))

            if !urlString.isEmpty, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .empty: ProgressView().scaleEffect(0.8)
                    case .failure: Image(systemName: "pawprint.fill").font(.title2).foregroundColor(.secondary)
                    case .success(let img): img.resizable().scaledToFill()
                    @unknown default: EmptyView()
                    }
                }
                .clipShape(Circle())
            } else {
                Image(systemName: "pawprint.fill").font(.title2).foregroundColor(.secondary)
            }
        }
        .frame(width: size, height: size)
        .overlay(
            Circle()
                .strokeBorder(gradient, lineWidth: ringWidth)
                .blur(radius: 0.2)
        )
        .shadow(color: .black.opacity(0.06), radius: 4, x: 0, y: 2)
    }
}

private struct InfoChip: View {
    let icon: String
    let text: String

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon).imageScale(.small)
            Text(text).font(.caption.weight(.medium))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            Capsule(style: .continuous)
                .fill(.thinMaterial)
        )
        .overlay(
            Capsule(style: .continuous)
                .stroke(Color.white.opacity(0.45), lineWidth: 0.6)
        )
    }
}

    private struct InfoPill: View {
        let icon: String
        let text: String
        var body: some View {
            HStack(spacing: 4) {
                Image(systemName: icon).imageScale(.small)
                Text(text).font(.caption2)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.thinMaterial, in: Capsule())
            .overlay(Capsule().stroke(Color.white.opacity(0.5), lineWidth: 0.5))
        }
    }


// MARK: - Dog picker sheet (multi-select)
public struct DogPickerSheet: View {
    let allDogs: [DogDto]
    let initiallySelected: Set<String>
    let onConfirm: (Set<String>) -> Void
    let onCancel: () -> Void

    @State private var selected: Set<String> = []

    public init(allDogs: [DogDto], initiallySelected: Set<String>, onConfirm: @escaping (Set<String>) -> Void, onCancel: @escaping () -> Void) {
        self.allDogs = allDogs
        self.initiallySelected = initiallySelected
        self.onConfirm = onConfirm
        self.onCancel = onCancel
    }

    public var body: some View {
        NavigationView {
            List {
                ForEach(allDogs, id: \.id) { d in
                    HStack(spacing: 12) {
                        GardenDogAvatar(dog: d, diameter: 32)
                        Text(d.name.isEmpty ? "Dog" : d.name)
                            .font(.subheadline)
                        Spacer()
                        Image(systemName: selected.contains(d.id) ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(selected.contains(d.id) ? .accentColor : .secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if selected.contains(d.id) { selected.remove(d.id) } else { selected.insert(d.id) }
                    }
                }
            }
            .navigationBarTitle("Select dogs", displayMode: .inline)
            .navigationBarItems(
                leading: Button("Cancel", action: onCancel),
                trailing: Button("Confirm") { onConfirm(selected) }.disabled(allDogs.isEmpty)
            )
            .onAppear { selected = initiallySelected }
        }
    }
}
