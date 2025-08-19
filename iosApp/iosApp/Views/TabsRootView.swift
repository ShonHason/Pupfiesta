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
    let userVM: UserViewModel

    @State private var selected: Int = 0

    init(dogsVM: DogsViewModel, gardensVM: DogGardensViewModel, userVM: UserViewModel) {
        self.dogsVM = dogsVM
        self.gardensVM = gardensVM
        self.userVM = userVM

        // Tab bar appearance inside init
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundEffect = nil
        appearance.backgroundColor = .clear
        appearance.shadowColor = .clear

        let itemAppearance = UITabBarItemAppearance()
        itemAppearance.normal.titleTextAttributes = [.foregroundColor: UIColor.secondaryLabel]
        itemAppearance.selected.titleTextAttributes = [.foregroundColor: UIColor.label]
        appearance.stackedLayoutAppearance = itemAppearance
        appearance.inlineLayoutAppearance = itemAppearance
        appearance.compactInlineLayoutAppearance = itemAppearance

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
    }

    var body: some View {
        TabView(selection: $selected) {
            YardView(viewModel: gardensVM, userViewModel: userVM)
                .tabItem {
                    Image(systemName: "pawprint.fill")
                    Text("Yard")
                }
                .tag(0 as Int)

            EditProfileView(dogsViewModel: dogsVM)
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(1 as Int)
        }
        .tint(.blue)
    }
}
