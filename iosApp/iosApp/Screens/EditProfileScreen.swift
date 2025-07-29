// EditProfileScreen.swift

import SwiftUI
import PhotosUI

struct EditProfileScreen: View {
    // MARK: – Profile fields
    @State private var username: String
    @State private var ownerName: String
    @State private var dogName: String
    @State private var isMale: Bool
    @State private var isNeutered: Bool
    @State private var isFriendly: Bool
    @State private var weight: Int

    // MARK: – Photo picker state
    @State private var showingPhotoPicker = false
    @State private var pickedUIImage: UIImage?
    @State private var dogImage: Image

    // MARK: – Callbacks
    var onSave: (
        _ ownerName: String,
        _ dogName: String,
        _ isMale: Bool,
        _ isNeutered: Bool,
        _ isFriendly: Bool,
        _ weight: Int,
        _ dogPhoto: UIImage?
    ) -> Void

    // MARK: – Bottom-bar state & callbacks
    @State private var selectedTab: BottomBarContainer.Tab = .profile
    var onYardTap: () -> Void
    var onProfileTap: () -> Void

    // MARK: – Background gradient
    private let fullPageGradient = LinearGradient(
        colors: [
            Color(red: 252/255, green: 241/255, blue: 196/255),
            Color(red: 176/255, green: 212/255, blue: 248/255)
        ],
        startPoint: .top,
        endPoint: .bottom
    )

    // MARK: – Init
    init(
        username: String,
        ownerName: String,
        dogName: String,
        isMale: Bool,
        isNeutered: Bool,
        isFriendly: Bool,
        weight: Int,
        dogPhoto: UIImage? = nil,
        onSave: @escaping (
            _ ownerName: String,
            _ dogName: String,
            _ isMale: Bool,
            _ isNeutered: Bool,
            _ isFriendly: Bool,
            _ weight: Int,
            _ dogPhoto: UIImage?
        ) -> Void = { _,_,_,_,_,_,_ in },
        onYardTap: @escaping () -> Void = {},
        onProfileTap: @escaping () -> Void = {}
    ) {
        // Profile fields
        self._username   = State(initialValue: username)
        self._ownerName  = State(initialValue: ownerName)
        self._dogName    = State(initialValue: dogName)
        self._isMale     = State(initialValue: isMale)
        self._isNeutered = State(initialValue: isNeutered)
        self._isFriendly = State(initialValue: isFriendly)
        self._weight     = State(initialValue: weight)

        // Bottom-bar defaults
        self._selectedTab = State(initialValue: .yard)
        self.onYardTap    = onYardTap
        self.onProfileTap = onProfileTap

        // Dog image
        let img = dogPhoto.map { Image(uiImage: $0) } ?? Image(systemName: "dog.fill")
        self._dogImage = State(initialValue: img)

        // Save callback
        self.onSave = onSave
    }

    var body: some View {
        ZStack {
            // 1) Full-screen gradient
            fullPageGradient
                .ignoresSafeArea()

            // 2) Scrollable content
            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 40)

                    // Dog Avatar
                    dogImage
                        .resizable()
                        .scaledToFill()
                        .frame(width: 120, height: 120)
                        .clipShape(Circle())
                        .background(Circle().fill(Color.white).frame(width: 128, height: 128))
                        .overlay(Circle().stroke(Color.blue, lineWidth: 4))
                        .shadow(radius: 5)
                        .onTapGesture { showingPhotoPicker = true }

                    // Text Fields
                    VStack(spacing: 16) {
                        HStack {
                            Text("Username:")
                                .frame(width: 100, alignment: .leading)
                            Text(username)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(8)
                                .background(Color.white.opacity(0.6))
                                .cornerRadius(8)
                        }
                        labeledField("Owner Name:", text: $ownerName)
                        labeledField("Dog Name:",   text: $dogName)
                    }
                    .padding(.horizontal)

                    // Gender Toggle (stuck to right)
                    HStack {
                        Text("Gender:")
                            .frame(width: 100, alignment: .leading)

                        Spacer() // pushes button to the right

                        Button {
                            isMale.toggle()
                        } label: {
                            Text(isMale ? "Male" : "Female")
                                .font(.subheadline).fontWeight(.semibold)
                                .frame(width: 80, height: 32)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(isMale
                                              ? Color.blue.opacity(0.2)
                                              : Color.pink.opacity(0.2))
                                )
                                .foregroundColor(isMale ? .blue : .pink)
                        }
                    }
                    .padding(.horizontal)

                    // Toggles
                    VStack(spacing: 12) {
                        Toggle("Neutered:", isOn: $isNeutered)
                        Toggle("Friendly:", isOn: $isFriendly)
                    }
                    .padding(.horizontal)

                    // Weight Wheel
                    VStack(spacing: 0) {
                        HStack {
                            Text("Weight:")
                            Text("\(weight) kg").bold()
                        }
                        Text("Scroll To Change Weight")
                            .font(.caption.monospaced())
                            .foregroundColor(.secondary)

                        Picker("", selection: $weight) {
                            ForEach(1...200, id: \.self) {
                                Text("\($0) kg").tag($0)
                            }
                        }
                        .pickerStyle(.wheel)
                        .frame(height: 60)
                        .clipped()
                    }
                    .padding(.horizontal)

                    // Save Changes Button
                    Button("Apply Changes") {
                        onSave(
                            ownerName,
                            dogName,
                            isMale,
                            isNeutered,
                            isFriendly,
                            weight,
                            pickedUIImage
                        )
                    }
                    .font(.headline.monospaced())
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(Color.blue)
                    .cornerRadius(12)
                    .padding(.horizontal)

                    Spacer().frame(height: 20) // extra breathing room
                }
            }
        }
        // 3) Pin the bottom bar and reserve space for it
        .safeAreaInset(edge: .bottom) {
            BottomBarContainer(
                selectedTab:    $selectedTab,
                onYardTap:       onYardTap,
                onProfileTap:    onProfileTap,
                topPadding:      30,
                bottomPadding:  2,
                backgroundColor: Color(red: 176/255, green: 212/255, blue: 248/255)
            )
        }
        // 4) Photo picker sheet
        .sheet(isPresented: $showingPhotoPicker, onDismiss: applyPickedImage) {
            PhotoPicker(image: $pickedUIImage)
        }
    }

    // MARK: – Helpers
    @ViewBuilder
    private func labeledField(_ label: String, text: Binding<String>) -> some View {
        HStack {
            Text(label)
                .frame(width: 100, alignment: .leading)
            TextField("", text: text)
                .textFieldStyle(.roundedBorder)
                .autocapitalization(.none)
        }
    }

    private func applyPickedImage() {
        if let ui = pickedUIImage {
            dogImage = Image(uiImage: ui)
        }
    }
}

struct EditProfileScreen_Previews: PreviewProvider {
    static var previews: some View {
        EditProfileScreen(
            username:    "doglover123",
            ownerName:   "Alice Smith",
            dogName:     "Rufus",
            isMale:      true,
            isNeutered:  false,
            isFriendly:  true,
            weight:      25,
            dogPhoto:    nil,
            onSave:      { _,_,_,_,_,_,_ in },
            onYardTap:   { print("🏠 tapped") },
            onProfileTap:{ print("👤 tapped") }
        )
        .previewDevice("iPhone 14 Pro")
    }
}
