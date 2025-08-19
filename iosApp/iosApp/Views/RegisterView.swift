//
//  RegisterView.swift
//  iosApp
//

import SwiftUI
import PhotosUI
import Shared   // KMM module (Breed, Gender, UserViewModel, UserState)
import Foundation

// MARK: - Kotlin enum helpers (use your existing SwiftBridge-based function)
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

// MARK: - View

struct RegisterView: View {
    @Environment(\.dismiss) private var dismiss

    let viewModel: UserViewModel
    var onRegistered: () -> Void = {}

    @State private var vmState: UserState
    @State private var observeTask: Task<Void, Never>?

    // Local UI state (mirrors VM)
    @State private var email           = ""
    @State private var password        = ""
    @State private var ownerName       = ""
    @State private var dogName         = ""
    @State private var selectedBreed: Breed = .mixed
    @State private var isMale          = true
    @State private var isNeutered      = true
    @State private var isFriendly      = true
    @State private var selectedWeight  = 25

    // Image picking/upload
    @State private var showImagePicker = false
    @State private var dogImage: UIImage? = nil
    @State private var isUploading = false
    @State private var uploadError: String? = nil
    @State private var uploadedUrl: String? = nil

    // Errors
    @State private var errorMessage = ""

    init(viewModel: UserViewModel, onRegistered: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.onRegistered = onRegistered
        _vmState = State(initialValue: viewModel.userState.value)
    }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 252/255, green: 241/255, blue: 196/255),
                         Color(red: 176/255, green: 212/255, blue: 248/255)],
                startPoint: .top, endPoint: .bottom
            ).ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    Text("Let’s Get To Know You!")
                        .font(.title2).bold().padding(.top, 20)

                    fieldsSection

                    breedRow
                    genderRow
                    neuteredRow
                    friendlyRow
                    weightAndPhotoSection

                    signUpButton

                    stateBanner

                    Spacer(minLength: 20)
                }
            }
            .sheet(isPresented: $showImagePicker) {
                PhotoPicker(image: $dogImage)
            }
            .onChange(of: dogImage) { newImage in
                guard let img = newImage else { return }
                startCloudinaryUpload(image: img)
            }
        }
        .navigationTitle("Register")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button {
                    dismiss()
                } label: {
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
                        if s is UserState.Loaded { onRegistered() }
                        if let e = s as? UserState.Error { errorMessage = e.message }
                    }
                }
            }
            if let i = vmState as? UserState.Initial { syncFromInitial(i) }
        }
        .onDisappear { observeTask?.cancel(); observeTask = nil }
        .alert("Error",
               isPresented: Binding(
                get: { (vmState is UserState.Error) && !errorMessage.isEmpty },
                set: { _ in }
               )) {
            Button("OK") { errorMessage = "" }
        } message: { Text(errorMessage) }
    }

    // MARK: - Sections (split to avoid “type-check in reasonable time”)

    private var fieldsSection: some View {
        VStack(spacing: 16) {
            labeledField(title: "Email") {
                TextField("Enter Email", text: $email)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)
                    .keyboardType(.emailAddress)
                    .onChange(of: email) { viewModel.setEmail(v: $0) }
            }
            labeledField(title: "Password") {
                SecureField("Enter Password", text: $password)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)
                    .onChange(of: password) { viewModel.setPassword(v: $0) }
            }
            labeledField(title: "Owner’s Name") {
                TextField("Enter Owner’s Name", text: $ownerName)
                    .onChange(of: ownerName) { viewModel.setOwnerName(v: $0) }
            }
            labeledField(title: "Dog’s Name") {
                TextField("Enter Dog’s Name", text: $dogName)
                    .onChange(of: dogName) { viewModel.setDogName(v: $0) }
            }
        }
        .padding(.horizontal)
    }

    private var breedRow: some View {
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
                    .background(Color.gray.opacity(0.12))
                    .cornerRadius(8)
            }
        }
        .padding(.horizontal)
    }

    private var genderRow: some View {
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
                            .fill(isMale ? Color.blue.opacity(0.2) : Color.pink.opacity(0.2))
                    )
                    .foregroundColor(isMale ? .blue : .pink)
            }
        }
        .padding(.horizontal)
    }

    private var neuteredRow: some View {
        HStack {
            Text("Is The Dog Neutered/Spayed?").font(.subheadline)
            Spacer()
            Toggle("", isOn: $isNeutered)
                .labelsHidden()
                .onChange(of: isNeutered) { viewModel.setIsNeutered(v: $0) }
            Text(isNeutered ? "Yes" : "No").font(.subheadline)
        }
        .padding(.horizontal)
    }

    private var friendlyRow: some View {
        HStack {
            Text("Is The Dog Friendly?").font(.subheadline)
            Spacer()
            Toggle("", isOn: $isFriendly)
                .labelsHidden()
                .onChange(of: isFriendly) { viewModel.setIsFriendly(v: $0) }
            Text(isFriendly ? "Yes" : "No").font(.subheadline)
        }
        .padding(.horizontal)
    }

    private var weightAndPhotoSection: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Dog Weight in Kilograms").font(.subheadline)
                Picker("", selection: $selectedWeight) {
                    ForEach(5...50, id: \.self) { w in Text("\(w)").tag(w) }
                }
                .pickerStyle(.wheel)
                .frame(width: 80, height: 80)
                .onChange(of: selectedWeight) { viewModel.setDogWeight(kg: Int32($0)) }

                Text("\(selectedWeight) kg").font(.subheadline)
            }

            Spacer()

            VStack(spacing: 6) {
                Text("Tap To Upload Picture Of Your Dog").font(.subheadline)

                ZStack {
                    Button { showImagePicker = true } label: {
                        Group {
                            if let img = dogImage {
                                Image(uiImage: img).resizable().scaledToFill()
                            } else {
                                Image(systemName: "photo")
                                    .resizable().scaledToFit()
                                    .padding(20).opacity(0.35)
                            }
                        }
                        .frame(width: 80, height: 80)
                        .background(Color.gray.opacity(0.12))
                        .clipShape(Circle())
                        .contentShape(Circle())
                    }
                    .buttonStyle(.plain)

                    if isUploading {
                        ProgressView()
                            .frame(width: 80, height: 80)
                            .background(Color.black.opacity(0.25))
                            .clipShape(Circle())
                    }

                    if !isUploading, uploadedUrl != nil {
                        VStack {
                            Spacer()
                            HStack {
                                Spacer()
                                Text("✓").font(.headline).bold().padding(4)
                            }
                        }
                        .frame(width: 80, height: 80)
                    }
                }

                if let uploadError {
                    Text(uploadError)
                        .font(.footnote)
                        .foregroundColor(.red)
                }
            }
        }
        .padding(.horizontal)
    }

    private var signUpButton: some View {
        Button {
            if isUploading { return }   // prevent submit mid-upload
            viewModel.signUp()
        } label: {
            HStack {
                if vmState is UserState.Loading { ProgressView().controlSize(.small) }
                Text(vmState is UserState.Loading ? "Signing Up…" : "Sign Up")
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background((!isUploading && !(vmState is UserState.Loading)) ? Color.blue : Color.blue.opacity(0.6))
            .foregroundColor(.white)
            .cornerRadius(12)
        }
        .disabled(isUploading || (vmState is UserState.Loading))
        .padding(.horizontal)
        .padding(.vertical, 16)
    }

    // MARK: - VM state banner

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

    // MARK: - Sync SwiftUI <- VM

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
        if let url = d.dogPictureUrl, !url.isEmpty { uploadedUrl = url }
    }

    // MARK: - Upload

    private func startCloudinaryUpload(image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.9) else {
            uploadError = "Couldn't read image data"
            return
        }
        isUploading = true
        uploadError = nil
        uploadedUrl = nil

        CloudinaryUploader.upload(data) { url in
            DispatchQueue.main.async {
                self.isUploading = false
                if let url, !url.isEmpty {
                    self.uploadedUrl = url
                    self.viewModel.setDogPictureUrl(url: url)   // <- VM sees the Cloudinary URL
                } else {
                    self.uploadError = "Upload failed"
                }
            }
        }
    }

    // Small helper to style labeled fields consistently
    @ViewBuilder
    private func labeledField<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.subheadline)
            content()
                .padding(12)
                .background(Color.black.opacity(0.05))
                .cornerRadius(8)
        }
    }
}
