    //
    //  EditProfileView.swift
    //  iosApp
    //

    import SwiftUI
    import PhotosUI
    import Shared

    struct EditProfileView: View {
        // Injected KMM VM (use the SAME instance you created after login)
        let userViewModel: UserViewModel

        // View-model state (from your KMM StateFlow)
        @State private var vmState: UserState?

        // Editable fields (local UI copy)
        @State private var username: String = ""   // derived: ownerName or email
        @State private var email: String = ""
        @State private var ownerName: String = ""
        @State private var dogName: String = ""
        @State private var isMale: Bool = true
        @State private var isNeutered: Bool = true
        @State private var isFriendly: Bool = true
        @State private var weight: Int = 25
        
        // Photo picker
        @State private var showingPhotoPicker = false
        @State private var pickedUIImage: UIImage?
        @State private var dogImage: Image = Image(systemName: "dog.fill")

        // Observation + errors
        @State private var observeTask: Task<Void, Never>? = nil
        @State private var errorMessage = ""

        // Background
        private let fullPageGradient = LinearGradient(
            colors: [
                Color(red: 252/255, green: 241/255, blue: 196/255),
                Color(red: 176/255, green: 212/255, blue: 248/255)
            ],
            startPoint: .top, endPoint: .bottom
        )

        var body: some View {
            ZStack {
                fullPageGradient.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        Spacer().frame(height: 40)

                        // Avatar
                        dogImage
                            .resizable()
                            .scaledToFill()
                            .frame(width: 120, height: 120)
                            .clipShape(Circle())
                            .background(Circle().fill(Color.white).frame(width: 128, height: 128))
                            .overlay(Circle().stroke(Color.blue, lineWidth: 4))
                            .shadow(radius: 5)
                            .onTapGesture { showingPhotoPicker = true }

                        // Fields
                        VStack(spacing: 16) {
                            HStack {
                                Text("email:")
                                    .frame(width: 110, alignment: .leading)
                                Text(email)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(8)
                                    .background(Color.white.opacity(0.6))
                                    .cornerRadius(8)
                            }

                            labeledField("Owner Name:", text: $ownerName)
                                .onChange(of: ownerName) { _ in userViewModel.setOwnerName(v: ownerName) }

                            labeledField("Dog Name:", text: $dogName)
                                .onChange(of: dogName) { _ in userViewModel.setDogName(v: dogName) }
                        }
                        .padding(.horizontal)

                        // Gender
                        HStack {
                            Text("Gender:")
                                .frame(width: 110, alignment: .leading)
                            Spacer()
                            Button {
                                isMale.toggle()
                                userViewModel.setDogGender(gender: isMale ? .male : .female)
                            } label: {
                                Text(isMale ? "Male" : "Female")
                                    .font(.subheadline).fontWeight(.semibold)
                                    .frame(width: 80, height: 32)
                                    .background(
                                        RoundedRectangle(cornerRadius: 16)
                                            .fill(isMale ? Color.blue.opacity(0.2) : Color.pink.opacity(0.2))
                                    )
                                    .foregroundColor(isMale ? .blue : .pink)
                            }
                        }
                        .padding(.horizontal)

                        // Toggles
                        VStack(spacing: 12) {
                            Toggle("Neutered:", isOn: $isNeutered)
                                .onChange(of: isNeutered) { _ in userViewModel.setIsNeutered(v: isNeutered) }

                            Toggle("Friendly:", isOn: $isFriendly)
                                .onChange(of: isFriendly) { _ in userViewModel.setIsFriendly(v: isFriendly) }
                        }
                        .padding(.horizontal)

                        // Weight
                        VStack(spacing: 0) {
                            HStack {
                                Text("Weight:")
                                Text("\(weight) kg").bold()
                            }
                            Text("Scroll To Change Weight")
                                .font(.caption.monospaced())
                                .foregroundColor(.secondary)

                            Picker("", selection: $weight) {
                                ForEach(1...200, id: \.self) { Text("\($0) kg").tag($0) }
                            }
                            .pickerStyle(.wheel)
                            .frame(height: 60)
                            .clipped()
                            .onChange(of: weight) { _ in userViewModel.setDogWeight(kg: Int32(weight)) }
                        }
                        .padding(.horizontal)

                        // Apply
                        Button("Apply Changes") {
                            userViewModel.setOwnerName(v: ownerName)
                            userViewModel.setDogName(v: dogName)
                            userViewModel.setDogGender(gender: isMale ? .male : .female)
                            userViewModel.setIsNeutered(v: isNeutered)
                            userViewModel.setIsFriendly(v: isFriendly)
                            userViewModel.setDogWeight(kg: Int32(weight))
                        }
                        .font(.headline.monospaced())
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(Color.blue)
                        .cornerRadius(12)
                        .padding(.horizontal)

                        stateBanner
                        Spacer().frame(height: 20)
                    }
                }
            }
            // Photo picker
            .sheet(isPresented: $showingPhotoPicker, onDismiss: applyPickedImage) {
                PhotoPicker(image: $pickedUIImage)
            }
            // Hide default nav bar (optional)
            .navigationBarBackButtonHidden(true)
            .toolbar(.hidden, for: .navigationBar)

            .task {
                do {
                    let dto = try await userViewModel.getUserOrThrow()
                    applyUser(dto)
                } catch {
                    errorMessage = error.localizedDescription
                }
            }



            // (Optional) observe KMM state flow if you still emit Initial/Loaded/Error
            .onAppear {
                vmState = userViewModel.userState.value

                observeTask?.cancel()
                observeTask = Task {
                    for await s in userViewModel.userState {
                        vmState = s
                        if let i = s as? UserState.Initial { applyForm(i.data) }
                        if let e = s as? UserState.Error { errorMessage = e.message }
                    }
                }

                if let i = vmState as? UserState.Initial { applyForm(i.data) }
            }
            .onDisappear { observeTask?.cancel(); observeTask = nil }
            .alert("Error", isPresented: Binding(
                get: { !errorMessage.isEmpty },
                set: { _ in }
            )) {
                Button("OK") { errorMessage = "" }
            } message: { Text(errorMessage) }
        }

        // MARK: - Helpers

        /// Map a `UserDto` (Firestore document) into local SwiftUI state
        private func applyUser(_ u: UserDto) {
            email     = u.email
            ownerName = u.name
            
            if let dogs = u.dogList as? [DogDto], let first = dogs.first {
                dogName    = first.name
                isMale     = first.isMale
                isNeutered = first.isNeutered
                isFriendly = first.isFriendly
                weight     = Int(first.weight)
            }
        }

        /// Map your existing Initial/UserFormData to local UI (keeps old flow working)
        private func applyForm(_ f: UserFormData) {
            email      = f.email
            ownerName  = f.ownerName
            dogName    = f.dogName
            isMale     = (f.dogGender == .male)
            isNeutered = f.isNeutered
            isFriendly = f.isFriendly
            weight     = Int(f.dogWeight)
            username   = f.ownerName.isEmpty ? f.email : f.ownerName
        }

        @ViewBuilder
        private func labeledField(_ label: String, text: Binding<String>) -> some View {
            HStack {
                Text(label).frame(width: 110, alignment: .leading)
                TextField("", text: text)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)
            }
        }

        private func applyPickedImage() {
            if let ui = pickedUIImage {
                dogImage = Image(uiImage: ui)
                if let data = ui.jpegData(compressionQuality: 0.9) {
                    let url = FileManager.default.temporaryDirectory
                        .appendingPathComponent("dog-\(UUID().uuidString).jpg")
                    try? data.write(to: url)
                    userViewModel.setDogPictureUrl(url: url.absoluteString)
                }
            }
        }

        // Small status banner
        @ViewBuilder
        private var stateBanner: some View {
            if let state = vmState {
                switch state {
                case is UserState.Loading:
                    ProgressView("Please wait...").padding(.horizontal)
                case let e as UserState.Error:
                    Text(e.message).foregroundColor(.red).padding(.horizontal)
                case is UserState.Loaded:
                    Text("Saved!").foregroundColor(.green).padding(.horizontal)
                default:
                    EmptyView()
                }
            } else {
                EmptyView()
            }
        }
    }
