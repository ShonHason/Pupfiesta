//
//  RegisterView.swift
//  iosApp
//

import SwiftUI
import PhotosUI
import Shared

// Helpers to bridge Kotlin enum -> Swift UI
private func allBreeds() -> [Breed] {
    let arr = SwiftBridge().allBreeds() // KotlinArray<Breed>
    var result: [Breed] = []
    let n = Int(arr.size)
    result.reserveCapacity(n)
    for i in 0..<n {
        if let b = arr.get(index: Int32(i)) as? Breed {
            result.append(b)
        }
    }
    return result
}

private func breedTitle(_ b: Breed) -> String {
    b.name.lowercased().replacingOccurrences(of: "_", with: " ").capitalized
}

struct RegisterView: View {
    // Back
    @Environment(\.dismiss) private var dismiss

    // KMM VM
    let viewModel: UserViewModel
    var onRegistered: () -> Void = {}   // ← add this

    @State private var vmState: UserState
    @State private var didRegister = false

    
    init(viewModel: UserViewModel, onRegistered: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.onRegistered = onRegistered
        _vmState = State(initialValue: viewModel.userState.value)
    }
    
    // Local UI bindings
    @State private var email           = ""
    @State private var password        = ""
    @State private var ownerName       = ""
    @State private var dogName         = ""
    @State private var selectedBreed: Breed = .mixed
    @State private var isMale          = true
    @State private var isNeutered      = true
    @State private var isFriendly      = true
    @State private var selectedWeight  = 25
    @State private var showImagePicker = false
    @State private var dogImage: UIImage? = nil
    @State private var errorMessage    = ""
    @State private var observeTask: Task<Void, Never>? = nil

    private let weightRange = Array(5...50)

    init(viewModel: UserViewModel) {
        self.viewModel = viewModel
        _vmState = State(initialValue: viewModel.userState.value)
    }

    var body: some View {
        ZStack {
            // Background
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    Text("Let’s Get To Know You!")
                        .font(.title2).bold().padding(.top, 20)

                    // MARK: Fields → VM
                    Group {
                        ClearableTextField(placeholder: "Enter Email", text: $email)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled(true)
                            .onChange(of: email) { viewModel.setEmail(v: $0) }

                        ClearableSecureField(placeholder: "Enter Password", text: $password)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled(true)
                            .onChange(of: password) { viewModel.setPassword(v: $0) }

                        ClearableTextField(placeholder: "Enter Owner’s Name", text: $ownerName)
                            .onChange(of: ownerName) { viewModel.setOwnerName(v: $0) }

                        ClearableTextField(placeholder: "Enter Dog’s Name", text: $dogName)
                            .onChange(of: dogName) { viewModel.setDogName(v: $0) }
                    }
                    .padding(.horizontal)

                    // MARK: Breed
                    HStack {
                        Text("What is your dog’s breed?")
                            .font(.subheadline).fontWeight(.semibold)
                        Spacer()
                        Menu {
                            ForEach(allBreeds(), id: \.name) { b in
                                Button(breedTitle(b)) {
                                    selectedBreed = b
                                    viewModel.setDogBreed(breed: b)
                                }
                            }
                        } label: {
                            Text(breedTitle(selectedBreed))
                                .font(.subheadline)
                                .foregroundColor(.blue)
                                .padding(.horizontal, 12).padding(.vertical, 8)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(8)
                        }
                    }
                    .padding(.horizontal)

                    // MARK: Gender toggle
                    HStack {
                        Text("Gender").font(.subheadline).fontWeight(.semibold)
                        Spacer()
                        Button {
                            isMale.toggle()
                            viewModel.setDogGender(gender: isMale ? .male : .female)
                        } label: {
                            Text(isMale ? "Male" : "Female")
                                .font(.subheadline).fontWeight(.semibold)
                                .frame(width: 80, height: 32)
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(isMale ? Color.blue.opacity(0.2)
                                                     : Color.pink.opacity(0.2))
                                )
                                .foregroundColor(isMale ? .blue : .pink)
                        }
                    }
                    .padding(.horizontal)

                    // MARK: Neutered
                    HStack {
                        Text("Is The Dog Neutered/Spayed?").font(.subheadline)
                        Spacer()
                        Toggle("", isOn: $isNeutered)
                            .labelsHidden()
                            .onChange(of: isNeutered) { viewModel.setIsNeutered(v: $0) }
                        Text(isNeutered ? "Yes" : "No").font(.subheadline)
                    }
                    .padding(.horizontal)

                    // MARK: Friendly
                    HStack {
                        Text("Is The Dog Friendly?").font(.subheadline)
                        Spacer()
                        Toggle("", isOn: $isFriendly)
                            .labelsHidden()
                            .onChange(of: isFriendly) { viewModel.setIsFriendly(v: $0) }
                        Text(isFriendly ? "Yes" : "No").font(.subheadline)
                    }
                    .padding(.horizontal)

                    // MARK: Weight + Photo
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Dog Weight in Kilograms").font(.subheadline)
                            Picker("", selection: $selectedWeight) {
                                ForEach(weightRange, id: \.self) { w in Text("\(w)").tag(w) }
                            }
                            .pickerStyle(.wheel)
                            .frame(width: 80, height: 80)
                            .onChange(of: selectedWeight) {
                                viewModel.setDogWeight(kg: Int32($0))
                            }
                            Text("\(selectedWeight) kg").font(.subheadline)
                        }

                        Spacer()

                        VStack(spacing: 6) {
                            Text("Tap To Upload Picture Of Your Dog").font(.subheadline)
                            Button { showImagePicker = true } label: {
                                Group {
                                    if let img = dogImage {
                                        Image(uiImage: img).resizable().scaledToFill()
                                    } else {
                                        Image(systemName: "photo")
                                            .resizable().scaledToFit()
                                            .foregroundColor(.gray)
                                    }
                                }
                                .frame(width: 80, height: 80)
                                .background(Color.gray.opacity(0.1))
                                .clipShape(Circle())
                            }
                        }
                    }
                    .padding(.horizontal)

                    // Sign Up
                    Button("Sign Up") { viewModel.signUp() }
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                        .padding(.horizontal)
                        .padding(.vertical, 16)

                    // VM feedback
                    stateBanner
                    Spacer(minLength: 20)
                }
            }
            .sheet(isPresented: $showImagePicker) {
                PhotoPicker(image: $dogImage)
                    .onDisappear {
                        if let img = dogImage, let data = img.jpegData(compressionQuality: 0.9) {
                            let url = FileManager.default.temporaryDirectory
                                .appendingPathComponent("dog-\(UUID().uuidString).jpg")
                            try? data.write(to: url)
                            viewModel.setDogPictureUrl(url: url.absoluteString)
                        }
                    }
            }
        }
        .navigationTitle("Register")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: { dismiss() }) {
                    HStack(spacing: 6) {
                        Image(systemName: "chevron.left")
                        Text("Back")
                    }
                }
            }
        }
        .onAppear {
            observeTask?.cancel()
            observeTask = Task {
                for await s in viewModel.userState {
                    await MainActor.run {
                        vmState = s
                        if let i = s as? UserState.Initial { syncFromInitial(i) }
                        if s is UserState.Loaded { didRegister = true }
                        if let e = s as? UserState.Error { errorMessage = e.message }
                    }
                }
            }
            if let i = vmState as? UserState.Initial { syncFromInitial(i) }
        }
        .onDisappear { observeTask?.cancel(); observeTask = nil }
        .navigationDestination(isPresented: $didRegister) {
            Text("Registered ✅")
        }
        .alert("Error",
               isPresented: Binding(
                get: { (vmState is UserState.Error) && !errorMessage.isEmpty },
                set: { _ in }
               )) {
            Button("OK") { errorMessage = "" }
        } message: { Text(errorMessage) }
    }

    // MARK: — Banner by VM state
    @ViewBuilder
    private var stateBanner: some View {
        switch vmState {
        case is UserState.Loading:
            ProgressView("Please wait...").padding(.horizontal)
        case let e as UserState.Error:
            Text(e.message).foregroundColor(.red).padding(.horizontal)
        case is UserState.Loaded:
            Text("Success!").foregroundColor(.green).padding(.horizontal)
        default:
            EmptyView()
        }
    }

    // MARK: — Sync SwiftUI fields from VM Initial.data
    private func syncFromInitial(_ s: UserState.Initial) {
        let d = s.data
        if email != d.email { email = d.email }
        if password != d.password { password = d.password }
        if ownerName != d.ownerName { ownerName = d.ownerName }
        if dogName != d.dogName { dogName = d.dogName }
        if selectedBreed != d.dogBreed { selectedBreed = d.dogBreed }
        let male = (d.dogGender == .male)
        if isMale != male { isMale = male }
        if isNeutered != d.isNeutered { isNeutered = d.isNeutered }
        if isFriendly != d.isFriendly { isFriendly = d.isFriendly }
        if selectedWeight != Int(d.dogWeight) { selectedWeight = Int(d.dogWeight) }
    }
}

// MARK: — Preview (placeholder)
// struct RegisterView_Previews: PreviewProvider {
//     static var previews: some View {
//         // Requires a concrete KMM VM instance.
//         Text("Preview requires a RegisterViewModel instance")
//     }
// }
