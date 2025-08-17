//
//  GardenTopSheet.swift
//  iosApp
//
//  Created by Shon Hason on 16/08/2025.
//
//  GardenTopSheet.swift
//  iosApp
//
//  Created by Shon Hason on 16/08/2025.
//
import SwiftUI
import CoreLocation
import Shared
import UIKit

struct GardenTopSheet: View {
    let garden: DogGarden
    let userCoord: CLLocationCoordinate2D
    var onClose: () -> Void

    private var distanceText: String {
        let u = CLLocation(latitude: userCoord.latitude, longitude: userCoord.longitude)
        let g = CLLocation(latitude: garden.location.latitude, longitude: garden.location.longitude)
        let d = u.distance(from: g)
        return d >= 1000
            ? String(format: "%.1f km away", d/1000.0)
            : String(format: "%.0f m away", d)
    }

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(garden.name)
                    .font(.headline)
                    .lineLimit(1)
                Spacer()
                Button { onClose() } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                }
                .buttonStyle(.plain)
            }

            HStack(alignment: .firstTextBaseline) {
                Text(distanceText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Spacer()
                if let url = URL(string: garden.mapUrl) {
                    Button("Open in Maps") { UIApplication.shared.open(url) }
                        .font(.subheadline.bold())
                }
            }
        }
        .padding(16)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .shadow(radius: 6)
        .padding(.horizontal, 16)
    }
}
