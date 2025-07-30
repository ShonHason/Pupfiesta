import SwiftUI
import Shared

struct LoginScreen: View {
    // MARK: — Form State
    @State private var email        = ""
    @State private var password     = ""
    @State private var showPassword = false
§
    var body: some View {
        NavigationStack {
            ZStack {
                // MARK: — Full-screen Gradient
                LinearGradient(
                    colors: [
                        Color(red: 252/255, green: 241/255, blue: 196/255),
                        Color(red: 176/255, green: 212/255, blue: 248/255)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                VStack(spacing: 32) {
                    Spacer(minLength: 40)

                    // MARK: — Title
                    Text("PupFiesta")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity, alignment: .center)

                    // MARK: — Email Field
                    ClearableTextField(
                        placeholder: "Enter Email",
                        text: $email
                    )
                    .padding(.horizontal, 16)

                    // MARK: — Password Field + Recover Link
                    ZStack(alignment: .trailing) {
                        Group {
                            if showPassword {
                                TextField("Enter Password", text: $password)
                            } else {
                                SecureField("Enter Password", text: $password)
                            }
                        }
                        .autocapitalization(.none)
                        .padding(12)
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(8)

                        HStack(spacing: 12) {
                            // Eye toggle
                            Button {
                                showPassword.toggle()
                            } label: {
                                Image(systemName: showPassword ? "eye.slash" : "eye")
                                    .foregroundColor(.gray)
                            }

                            // Recover password link
                            NavigationLink("Recover Password?", destination: RecoverPasswordScreen())
                                .font(.footnote)
                                .foregroundColor(.gray)
                        }
                        .padding(.trailing, 16)
                    }
                    .padding(.horizontal, 16)

                    // MARK: — Sign In Button
                    Button {
                        // your sign-in logic
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

                    // MARK: — Or continue with
                    Text("Or continue with")
                        .font(.footnote)
                        .foregroundColor(.gray)

                    // MARK: — Social Buttons
                    HStack(spacing: 32) {
                        Button {
                            // Google action
                        } label: {
                            Image("GoogleIcon") // your asset
                                .resizable()
                                .scaledToFit()
                                .frame(width: 30, height: 30)
                                .frame(width: 100, height: 50)
                                .background(Color.white.opacity(0.8))
                                .cornerRadius(12)
                                .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
                        }

                        Button {
                            // Apple action
                        } label: {
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

                    // MARK: — Bottom Prompt
                    HStack(spacing: 4) {
                        Text("if you don’t have an account you can")
                            .font(.footnote)
                            .foregroundColor(.gray)
                        NavigationLink("Register here!", destination: LoginScreen())
                            .font(.footnote)
                            .fontWeight(.semibold)
                            .foregroundColor(.blue)
                    }
                    .padding(.top, 16)

                    Spacer()
                }
            }
            .navigationBarHidden(true)
        }
    }
}

// MARK: — Dummy Destinations

struct RecoverPasswordScreen: View {
    var body: some View {
        Text("🔑 Recover Password")
            .font(.largeTitle)
            .navigationTitle("Recover")
    }
}




// MARK: — Preview

struct LoginScreen_Previews: PreviewProvider {
    static var previews: some View {
        LoginScreen()
    }
}

