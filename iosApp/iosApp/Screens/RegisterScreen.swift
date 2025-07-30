//import SwiftUI
//import PhotosUI
//import Shared
////import FirebaseAuth
////import FirebaseFirestore
//
//
//struct RegisterScreen: View {
//    // MARK: — Form State
//    @State private var email           = ""
//    @State private var password        = ""
//    @State private var ownerName       = ""
//    @State private var dogName         = ""
//    @State private var selectedBreed: Breed? = .mixed
//    
//    @State private var isMale          = true
//    @State private var isNeutered      = true
//    @State private var isFriendly      = true
//
//    @State private var selectedWeight  = 25
//    @State private var showImagePicker = false
//    @State private var dogImage: UIImage? = nil
//    @State private var didRegister     = false
//
//    private let weightRange = Array(5...50)
//
//    var body: some View {
//        ZStack {
//            // Gradient background
//            LinearGradient(
//                colors: [
//                    Color(red: 252/255, green: 241/255, blue: 196/255),
//                    Color(red: 176/255, green: 212/255, blue: 248/255)
//                ],
//                startPoint: .top, endPoint: .bottom
//            )
//            .ignoresSafeArea()
//
//            ScrollView(showsIndicators: false) {
//                VStack(spacing: 24) {
//                    
//                    // HEADER
//                    Text("Let’s Get To Know You!")
//                        .font(.title2)
//                        .fontWeight(.bold)
//                        .padding(.top, 20)
//                    
//                    // TEXT FIELDS
//                    Group {
//                        ClearableTextField(placeholder: "Enter Email",         text: $email)
//                        ClearableSecureField(placeholder: "Enter Password",   text: $password)
//                        ClearableTextField(placeholder: "Enter Owner’s Name", text: $ownerName)
//                        ClearableTextField(placeholder: "Enter Dog’s Name",   text: $dogName)
//                    }
//
//                    // BREED ROW (aligned with the other questions)
//                    HStack {
//                        Text("What is your dog’s breed?")
//                            .font(.subheadline)
//                            .fontWeight(.semibold)
//                        Spacer()
//                        Menu {
//                            ForEach(Breed.allCases) { breed in
//                                Button(breed.displayName) {
//                                    selectedBreed = breed
//                                }
//                            }
//                        } label: {
//                            Text(selectedBreed.displayName)
//                                .font(.subheadline)
//                                .foregroundColor(.blue)
//                                .padding(.horizontal, 12)
//                                .padding(.vertical, 8)
//                                .background(Color.gray.opacity(0.1))
//                                .cornerRadius(8)
//                        }
//                    }
//
//                    // GENDER ROW
//                    HStack {
//                        Text("Gender")
//                            .font(.subheadline)
//                            .fontWeight(.semibold)
//                        Spacer()
//                        Button {
//                            isMale.toggle()
//                        } label: {
//                            Text(isMale ? "Male" : "Female")
//                                .font(.subheadline)
//                                .fontWeight(.semibold)
//                                .frame(width: 80, height: 32)
//                                .background(
//                                    RoundedRectangle(cornerRadius: 16)
//                                        .fill(isMale
//                                              ? Color.blue.opacity(0.2)
//                                              : Color.pink.opacity(0.2))
//                                )
//                                .foregroundColor(isMale ? .blue : .pink)
//                        }
//                    }
//
//                    // NEUTERED / SPAYED
//                    HStack {
//                        Text("Is The Dog Neutered/Spayed?")
//                            .font(.subheadline)
//                        Spacer()
//                        Toggle("", isOn: $isNeutered)
//                            .labelsHidden()
//                            .toggleStyle(SwitchToggleStyle(tint: .blue))
//                        Text(isNeutered ? "Yes" : "No")
//                            .font(.subheadline)
//                    }
//
//                    // FRIENDLY?
//                    HStack {
//                        Text("Is The Dog Friendly?")
//                            .font(.subheadline)
//                        Spacer()
//                        Toggle("", isOn: $isFriendly)
//                            .labelsHidden()
//                            .toggleStyle(SwitchToggleStyle(tint: .blue))
//                        Text(isFriendly ? "Yes" : "No")
//                            .font(.subheadline)
//                    }
//
//                    // WEIGHT PICKER & PHOTO
//                    HStack(alignment: .top) {
//                        VStack(alignment: .leading, spacing: 6) {
//                            Text("Dog Weight in Kilograms")
//                                .font(.subheadline)
//                            Picker(selection: $selectedWeight, label: EmptyView()) {
//                                ForEach(weightRange, id: \.self) { w in
//                                    Text("\(w)").tag(w)
//                                }
//                            }
//                            .pickerStyle(WheelPickerStyle())
//                            .frame(width: 80, height: 80)
//                        }
//
//                        Spacer()
//
//                        VStack(spacing: 6) {
//                            Text("Tap To Upload Picture Of Your Dog")
//                                .font(.subheadline)
//                            Button {
//                                showImagePicker = true
//                            } label: {
//                                Group {
//                                    if let img = dogImage {
//                                        Image(uiImage: img)
//                                            .resizable()
//                                            .scaledToFill()
//                                    } else {
//                                        Image(systemName: "photo")
//                                            .resizable()
//                                            .scaledToFit()
//                                            .foregroundColor(.gray)
//                                    }
//                                }
//                                .frame(width: 80, height: 80)
//                                .background(Color.gray.opacity(0.1))
//                                .clipShape(Circle())
//                            }
//                        }
//                    }
//
//                    // SIGN UP BUTTON
//                    Button("Sign Up") {
//                        //Task {
////                            do {
////                                let result = try await Auth.auth()
////                                    .createUser(withEmail: email, password: password)
////                                let user = result.user
////                                let db   = Firestore.firestore()
////                                let data: [String: Any] = [
////                                    "uid":         user.uid,
////                                    "email":       user.email ?? email,
////                                    "ownerName":   ownerName,
////                                    "dogName":     dogName,
////                                    "breed":       selectedBreed.rawValue,
////                                    "isMale":      isMale,
////                                    "isNeutered":  isNeutered,
////                                    "isFriendly":  isFriendly,
////                                    "weightKg":    selectedWeight
////                                ]
////                                try await db.collection("users")
////                                             .document(user.uid)
////                                             .setData(data)
////                                didRegister = true
////                            } catch {
////                                print("Error Creating User: \(error)")
////                            }
//                  //      }
//                    }
//                    .fontWeight(.semibold)
//                    .frame(maxWidth: .infinity)
//                    .padding(.vertical,12)
//                    .background(Color.blue)
//                    .foregroundColor(.white)
//                    .cornerRadius(12)
//                    .padding(.vertical, 16)
//                }
//                .padding(.horizontal)
//                .padding(.bottom, 20)
//            }
//            .sheet(isPresented: $showImagePicker) {
//                PhotoPicker(image: $dogImage)
//            }
//        }
//    }
//}
//
//struct RegisterScreen_Previews: PreviewProvider {
//    static var previews: some View {
//        RegisterScreen()
//    }
//}
