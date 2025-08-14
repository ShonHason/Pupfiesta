import SwiftUI

struct ClearableSecureField: View {
    let placeholder: String
    @Binding var text: String

    var body: some View {
        HStack(spacing: 8) {
            SecureField(placeholder, text: $text)
                .textContentType(.password)
                .keyboardType(.asciiCapable)
                .disableAutocorrection(true)            // iOS 14
                .autocorrectionDisabled(true)           // iOS 15+
                .autocapitalization(.none)              // iOS 14
                .textInputAutocapitalization(.never)    // iOS 15+

            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill").opacity(0.5)
                        .accessibilityLabel("Clear password")
                }
                .buttonStyle(.plain) // prevents blue-tinted button style
            }
        }
        .padding(12)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(8)
    }
}
