import SwiftUI
import Lottie

struct LandingScreen: View {
    var body: some View {
        NavigationStack {
            ZStack {
                // MARK: — Background
                LinearGradient(
                    colors: [
                        Color(red: 252/255, green: 241/255, blue: 196/255),
                        Color(red: 176/255, green: 212/255, blue: 248/255)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )                    .ignoresSafeArea()

                VStack(spacing: 40) {
                    // MARK: — Title + Lottie (with your exact modifiers)
                    ZStack(alignment: .top) {
                        Text("Welcome to Pupfiesta!")
                            .font(.title)
                            .fontWeight(.bold)

                        LottieView(name: "pet_lovers", loopMode: .loop)
                            .frame(width: 24, height: 24)
                            .offset(y: -270)
                            .scaleEffect(0.5)
                    }
                    .padding(.top, 20)

                    // MARK: — NavigationLinks for Login & Register
                    HStack(spacing: 20) {
                        NavigationLink {
                            LoginScreen()
                        } label: {
                            Text("Login")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(25)
                        }

                        NavigationLink {
                            RegisterScreen()
                        } label: {
                            Text("Register")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                                .background(Color.green)
                                .foregroundColor(.white)
                                .cornerRadius(25)
                        }
                    }
                    .padding(.horizontal, 40)
                }
                .padding()
            }
            .navigationBarHidden(true)
        }
    }
}




    struct LandingScreen_Previews: PreviewProvider {
        static var previews: some View {
            LandingScreen()
        }
    }
