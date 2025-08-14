import SwiftUI

struct ClearableTextField: View {
    let placeholder: String
    @Binding var text: String

    private var clearButton: some View {
        Group {
            if !text.isEmpty {
                Button { text = "" } label: {
                    Image(systemName: "xmark.circle.fill")
                        .opacity(0.5)
                }
                .buttonStyle(.plain)
                .padding(.trailing, 8)
            }
        }
    }

    var body: some View {
        TextField(placeholder, text: $text)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled(true)
            .padding(12)
            .background(Color(.secondarySystemBackground))
            .cornerRadius(8)
            .overlay(clearButton, alignment: .trailing)   // <- no closure form
    }
}
