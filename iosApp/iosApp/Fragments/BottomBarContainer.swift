import SwiftUI

/// A reusable, solid‐background bottom bar with a top divider.
struct BottomBarContainer: View {
    /// The two tabs our bar supports
    enum Tab: Hashable { case yard, profile }

    /// Which tab is currently selected
    @Binding var selectedTab: Tab

    /// Callbacks when the user taps Home or Profile
    var onYardTap: () -> Void
    var onProfileTap: () -> Void

    /// Vertical padding *above* the icons
    var topPadding: CGFloat = 8
    /// Vertical padding *below* the icons (inside safe area)
    var bottomPadding: CGFloat = 0
    /// Background color of the bar
    var backgroundColor: Color = Color(UIColor.systemBackground)

    var body: some View {
        VStack(spacing: 0) {
            // 1) divider line
            Divider()
                .background(Color.gray.opacity(0.3))

            // 2) the buttons
            HStack(spacing: 100) {
                Button {
                    selectedTab = .yard
                    onYardTap()
                } label: {
                    VStack(spacing: 4) {
                        Image ("dog-house")
                            .renderingMode(.template)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: 24, height: 24)
                        Text("Yard").font(.footnote)
                    }
                    .foregroundColor(selectedTab == .yard ? .blue : .gray)
                }

                Button {
                    selectedTab = .profile
                    onProfileTap()
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: "person.crop.circle")
                        Text("Profile").font(.footnote)
                    }
                    .foregroundColor(selectedTab == .profile ? .blue : .gray)
                    
                }
            }
            .padding(.top, topPadding)           // push icons down
            .padding(.bottom, bottomPadding)     // extra bottom room
            .frame(maxWidth: .infinity)
        }
        .background(backgroundColor)             // solid background
        .ignoresSafeArea(edges: .bottom)         // stick under home‐indicator
    }
}


