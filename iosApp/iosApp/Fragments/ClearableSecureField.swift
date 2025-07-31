import SwiftUI

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
