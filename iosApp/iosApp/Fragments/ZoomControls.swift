//
//  ZoomControls.swift
//  iosApp
//
//  Created by Shon Hason on 16/08/2025.
//
//  ZoomControls.swift
//  iosApp
//
//  Created by Shon Hason on 16/08/2025.
//
import SwiftUI
import MapKit

public struct ZoomControls: View {
    @Binding var region: MKCoordinateRegion
    var minDelta: CLLocationDegrees
    var maxDelta: CLLocationDegrees
    var factor: Double

    public init(
        region: Binding<MKCoordinateRegion>,
        minDelta: CLLocationDegrees = 0.002,
        maxDelta: CLLocationDegrees = 2.0,
        factor: Double = 0.6
    ) {
        self._region = region
        self.minDelta = minDelta
        self.maxDelta = maxDelta
        self.factor = factor
    }

    public var body: some View {
        VStack(spacing: 8) {
            Button(action: zoomIn) {
                Image(systemName: "plus")
                    .font(.headline)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel("Zoom in")

            Button(action: zoomOut) {
                Image(systemName: "minus")
                    .font(.headline)
                    .frame(width: 36, height: 36)
            }
            .accessibilityLabel("Zoom out")
        }
        .padding(8)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 3)
    }

    private func zoomIn() {
        region.span = MKCoordinateSpan(
            latitudeDelta: max(minDelta, region.span.latitudeDelta * factor),
            longitudeDelta: max(minDelta, region.span.longitudeDelta * factor)
        )
    }

    private func zoomOut() {
        region.span = MKCoordinateSpan(
            latitudeDelta: min(maxDelta, region.span.latitudeDelta / factor),
            longitudeDelta: min(maxDelta, region.span.longitudeDelta / factor)
        )
    }
}
