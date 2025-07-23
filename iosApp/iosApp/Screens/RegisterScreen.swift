import SwiftUI
import UIKit

struct RegisterScreen: View {
    // MARK: — Form State
    @State private var username        = ""
    @State private var password        = ""
    @State private var ownerName       = ""
    @State private var dogName         = ""
    
    @State private var isMale          = true   // gender state
    @State private var isNeutered      = true
    @State private var isFriendly      = true
    
    @State private var selectedWeight  = 25
    @State private var showImagePicker = false
    @State private var dogImage: UIImage? = nil
    
    // Weight from 5 to 50
    private let weightRange = Array(5...50)
    
    var body: some View {
        ZStack {
            // Gradient background
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    
                    // HEADER
                    Text("Let’s Get To Know You!")
                        .font(.title2)
                        .fontWeight(.bold)
                        .padding(.top, 20)
                    
                    // TEXT FIELDS
                    Group {
                        ClearableTextField(
                            placeholder: "Enter Username",
                            text: $username
                        )
                        ClearableSecureField(
                            placeholder: "Enter Password",
                            text: $password
                        )
                        ClearableTextField(
                            placeholder: "Enter Owner’s Name",
                            text: $ownerName
                        )
                        ClearableTextField(
                            placeholder: "Enter Dog’s Name",
                            text: $dogName
                        )
                    }
                    
                    // GENDER ROW
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Gender")
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .offset(x: 12, y: 0)

                            
                        
                        // Single pill-button, fixed size, aligned left
                        Button {
                            isMale.toggle()
                        } label: {
                            Text(isMale ? "Male" : "Female")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .frame(width: 80, height: 32)                 // ← FIXED SIZE
                                .background(
                                    RoundedRectangle(cornerRadius: 16)
                                        .fill(
                                            isMale
                                                ? Color.blue.opacity(0.2)
                                                : Color.pink.opacity(0.2)
                                        )
                                )
                                .foregroundColor(isMale ? .blue : .pink)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)  // ← ALIGN LEFT
                    }
                    
                    // NEUTERED / SPAYED
                    HStack {
                        Text("Is The Dog Neutered/Spayed?")
                            .font(.subheadline)
                        Spacer()
                        Toggle("", isOn: $isNeutered)
                            .labelsHidden()
                            .toggleStyle(SwitchToggleStyle(tint: .blue))
                        Text(isNeutered ? "Yes" : "No")
                            .font(.subheadline)
                    }
                    
                    // FRIENDLY?
                    HStack {
                        Text("Is The Dog Friendly?")
                            .font(.subheadline)
                        Spacer()
                        Toggle("", isOn: $isFriendly)
                            .labelsHidden()
                            .toggleStyle(SwitchToggleStyle(tint: .blue))
                        Text(isFriendly ? "Yes" : "No")
                            .font(.subheadline)
                    }
                    
                    // WEIGHT PICKER & PHOTO
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Dog Weight in Kilograms")
                                .font(.subheadline)
                            Picker(selection: $selectedWeight, label: EmptyView()) {
                                ForEach(weightRange, id: \.self) { w in
                                    Text("\(w)").tag(w)
                                }
                            }
                            .pickerStyle(WheelPickerStyle())
                            .frame(width: 80, height: 80)
                        }
                        
                        Spacer()
                        
                        VStack(spacing: 6) {
                            Text("Tap To Upload Picture Of Your Dog")
                                .font(.subheadline)
                            Button {
                                showImagePicker = true
                            } label: {
                                Group {
                                    if let img = dogImage {
                                        Image(uiImage: img)
                                            .resizable()
                                            .scaledToFill()
                                    } else {
                                        Image(systemName: "photo")
                                            .resizable()
                                            .scaledToFit()
                                            .foregroundColor(.gray)
                                    }
                                }
                                .frame(width: 80, height: 80)
                                .background(Color.gray.opacity(0.2))
                                .clipShape(Circle())
                            }
                        }
                    }
                    
                    // SIGN UP BUTTON
                    Button("Sign Up") {
                        // your sign-up logic
                    }
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                    .padding(.vertical, 16)
                    
                } // VStack
                .padding(.horizontal)
                .padding(.bottom, 20)
            } // ScrollView
            .sheet(isPresented: $showImagePicker) {
                ImagePicker(selectedImage: $dogImage)
            }
        } // ZStack
    }
}

struct RegisterScreen_Previews: PreviewProvider {
    static var previews: some View {
        RegisterScreen()
    }
}


// MARK: — ClearableTextField & SecureField

struct ClearableTextField: View {
    let placeholder: String
    @Binding var text: String
    
    var body: some View {
        TextField(placeholder, text: $text)
            .padding(12)
            .background(Color.gray.opacity(0.1))
            .cornerRadius(8)
            .overlay(
                HStack {
                    Spacer()
                    if !text.isEmpty {
                        Button { text = "" } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                        .padding(.trailing, 8)
                    }
                }
            )
    }
}

struct ClearableSecureField: View {
    let placeholder: String
    @Binding var text: String
    
    var body: some View {
        HStack {
            SecureField(placeholder, text: $text)
            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.gray)
                }
            }
        }
        .padding(12)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(8)
    }
}


// MARK: — UIKit Image Picker Bridge

struct ImagePicker: UIViewControllerRepresentable {
    @Environment(\.presentationMode) private var presentation
    @Binding var selectedImage: UIImage?
    
    func makeCoordinator() -> Coordinator { Coordinator(self) }
    class Coordinator: NSObject, UINavigationControllerDelegate, UIImagePickerControllerDelegate {
        let parent: ImagePicker
        init(_ parent: ImagePicker) { self.parent = parent }
        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]
        ) {
            if let img = info[.originalImage] as? UIImage {
                parent.selectedImage = img
            }
            parent.presentation.wrappedValue.dismiss()
        }
    }
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        return picker
    }
    func updateUIViewController(
        _ uiViewController: UIImagePickerController,
        context: Context
    ) { }
}
