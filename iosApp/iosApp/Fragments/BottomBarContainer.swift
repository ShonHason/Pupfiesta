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

struct BottomBarContainer_Previews: PreviewProvider {
    static var previews: some View {
        ZStack(alignment: .bottom) {
            // sample gradient background
            LinearGradient(
                colors: [
                    Color(red: 252/255, green: 241/255, blue: 196/255),
                    Color(red: 176/255, green: 212/255, blue: 248/255)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            BottomBarContainer(
                selectedTab: .constant(.profile),
                onYardTap:    { print("🏠 tapped") },
                onProfileTap: { print("👤 tapped") },
                topPadding:    30,    // raise or lower icons
                bottomPadding: 2,    // extra room below
                backgroundColor: Color(red: 176/255, green: 212/255, blue: 248/255)
            )
        }
    }
}
