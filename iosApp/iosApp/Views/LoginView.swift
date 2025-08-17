import SwiftUI
import Shared

struct LoginView: View {
    // Back
    @Environment(\.dismiss) private var dismiss

    // MARK: — Form State
    @State private var email        = ""
    @State private var password     = ""
    @State private var showPassword = false

    // ViewModels
    let loginViewModel: UserViewModel

    // VM observation
    @State private var vmState: UserState
    @State private var observeTask: Task<Void, Never>? = nil
    @State private var errorMessage = ""

    // Navigation
    @State private var goToGarden = false   // trigger navigation on success

    init(loginViewModel: UserViewModel) {
        self.loginViewModel = loginViewModel
        _vmState = State(initialValue: loginViewModel.userState.value)
    }

    /// Read Google Places key from Info.plist
    private var placesKey: String {
        Bundle.main.object(forInfoDictionaryKey: "GOOGLE_PLACES_API_KEY") as? String ?? ""
    }

    var body: some View {
        ZStack {
            // Hidden link to TabsRootView after successful sign-in
            NavigationLink(
                destination: TabsRootView(
                    dogsVM: DogsViewModel(firebaseRepo: RemoteFirebaseRepository()),
                    gardensVM: DogGardensViewModel(
                        firebaseRepo: RemoteFirebaseRepository(),
                        gardensRepo: GoogleGardensRepository(client: httpClient(), apiKey: placesKey),
                        defaultLanguage: "he"
                    )
                ),
                isActive: $goToGarden
            ) { EmptyView() }
            .hidden()

            // Background
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer(minLength: 40)

                Text("PupFiesta")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                // Email
                ClearableTextField(
                    placeholder: "Enter Email",
                    text: $email
                )
                .padding(.horizontal, 16)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .onChange(of: email) { _, newValue in
                    loginViewModel.setEmail(v: newValue)
                }

                // Password + controls
                ZStack(alignment: .trailing) {
                    Group {
                        if showPassword {
                            TextField("Enter Password", text: $password)
                        } else {
                            SecureField("Enter Password", text: $password)
                        }
                    }
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)
                    .padding(12)
                    .background(Color.gray.opacity(0.1))
                    .cornerRadius(8)
                    .onChange(of: password) { _, newValue in
                        loginViewModel.setPassword(v: newValue)
                    }

                    HStack(spacing: 12) {
                        Button { showPassword.toggle() } label: {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(.gray)
                        }

                        NavigationLink("Recover Password?", destination: RecoverPasswordScreen())
                            .font(.footnote)
                            .foregroundColor(.gray)
                    }
                    .padding(.trailing, 16)
                }
                .padding(.horizontal, 16)

                // Sign in
                Button {
                    loginViewModel.signIn()
                } label: {
                    Text("Sign In")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 16)

                // Socials (stubs)
                Text("Or continue with")
                    .font(.footnote)
                    .foregroundColor(.gray)

                HStack(spacing: 32) {
                    Button { /* Google action */ } label: {
                        Image("GoogleIcon")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 30, height: 30)
                            .frame(width: 100, height: 50)
                            .background(Color.white.opacity(0.8))
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
                    }

                    Button { /* Apple action */ } label: {
                        Image(systemName: "apple.logo")
                            .resizable()
                            .scaledToFit()
                            .foregroundColor(.black)
                            .frame(width: 30, height: 30)
                            .frame(width: 100, height: 50)
                            .background(Color.white.opacity(0.8))
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
                    }
                }

                // Bottom prompt
                HStack(spacing: 4) {
                    Text("if you don’t have an account you can")
                        .font(.footnote)
                        .foregroundColor(.gray)
                    NavigationLink(
                        "Register here!",
                        destination: RegisterView(
                            viewModel: loginViewModel,
                            onRegistered: { goToGarden = true }
                        )
                    )
                    .font(.footnote)
                    .fontWeight(.semibold)
                    .foregroundColor(.blue)
                }
                .padding(.top, 16)

                Spacer()
            }
        }
        .navigationTitle("Login")
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
                for await s in loginViewModel.userState {
                    await MainActor.run {
                        vmState = s
                        if let e = s as? UserState.Error {
                            errorMessage = e.message
                        }
                        if s is UserState.Loaded {
                            goToGarden = true
                        }
                    }
                }
            }
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
}

// Optional destination
struct RecoverPasswordScreen: View {
    var body: some View {
        Text("🔑 Recover Password")
            .font(.largeTitle)
            .navigationTitle("Recover")
    }
}
