//
//  TabRootView.swift
//  iosApp
//
//  Created by Shon Hason on 12/08/2025.
//

import SwiftUI
import Shared

struct TabsRootView: View {
    let dogsVM: DogsViewModel
    let gardensVM: DogGardensViewModel

    @State private var selected = 0

    init(dogsVM: DogsViewModel, gardensVM: DogGardensViewModel) {
        self.dogsVM = dogsVM
        self.gardensVM = gardensVM
            
        // Make the tab bar subtle / transparent
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundEffect = nil
        appearance.backgroundColor = .clear
        appearance.shadowColor = .clear
        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView(selection: $selected) {
            YardView(viewModel: gardensVM)
                .tabItem {
                    Image(systemName: "pawprint.fill")
                    Text("Yard")
                }
                .tag(0)

            EditProfileView(dogsViewModel: dogsVM)
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(1)
        }
        .accentColor(.blue) // tint for selected icon
    }
}
