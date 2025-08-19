//  MapContainer.swift
//  iosApp

import SwiftUI
import MapKit
import Shared

// Lightweight item for annotations
private struct GardenAnnotationItem: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
}

/// Wrapper view used everywhere in the app
struct MapContainer: View {
    @Binding var region: MKCoordinateRegion
    @Binding var camera: MapCameraPosition
    let userCoord: CLLocationCoordinate2D?
    let radiusMeters: Int32
    let gardens: [DogGarden]
    let onSelect: (DogGarden) -> Void

    var body: some View {
        Group {
            if #available(iOS 17.0, *) {
                Map17Wrapper(
                    region: $region,
                    camera: $camera,
                    userCoord: userCoord,
                    radiusMeters: radiusMeters,
                    gardens: gardens,
                    onSelect: onSelect
                )
            } else {
                // iOS 13–16: MKMapView path that updates pins in place
                LegacyRadiusMap(
                    region: $region,
                    center: userCoord ?? region.center,
                    radiusMeters: CLLocationDistance(radiusMeters),
                    showUserDot: true,
                    followUser: true,
                    gardens: gardens,
                    onSelect: onSelect
                )
            }
        }
        // Your ZoomControls overlay (works on all iOS versions)
        .overlay(alignment: .bottomTrailing) {
            ZoomControls(region: $region)
                .padding(.trailing, 12)
                .padding(.bottom, 12)
        }
    }
}

@available(iOS 17.0, *)
private struct Map17Wrapper: View {
    @Binding var region: MKCoordinateRegion
    @Binding var camera: MapCameraPosition

    // Precomputed data (keeps ViewBuilder light)
    private let circleCenter: CLLocationCoordinate2D
    private let ringCoords: [CLLocationCoordinate2D]
    private let items: [GardenAnnotationItem]
    private let gardens: [DogGarden]
    private let onSelect: (DogGarden) -> Void

    init(region: Binding<MKCoordinateRegion>,
         camera: Binding<MapCameraPosition>,
         userCoord: CLLocationCoordinate2D?,
         radiusMeters: Int32,
         gardens: [DogGarden],
         onSelect: @escaping (DogGarden) -> Void) {
        self._region = region
        self._camera = camera
        self.gardens = gardens
        self.onSelect = onSelect

        // Use current center (do NOT recenter just because radius changed)
        let center = userCoord ?? region.wrappedValue.center
        self.circleCenter = center

        // Annotation items
        var tmpItems: [GardenAnnotationItem] = []
        tmpItems.reserveCapacity(gardens.count)
        for g in gardens {
            tmpItems.append(
                GardenAnnotationItem(
                    id: g.id,
                    name: g.name,
                    coordinate: CLLocationCoordinate2D(
                        latitude: g.location.latitude,
                        longitude: g.location.longitude
                    )
                )
            )
        }
        self.items = tmpItems

        // Stroke-only radius ring
        self.ringCoords = circlePolylineCoords(center: center, radius: CLLocationDistance(radiusMeters))

        platformLogger(
            tag: "PUP",
            message: "MapContainer(iOS17) render pins=\(tmpItems.count) radius=\(radiusMeters) center=(\(center.latitude), \(center.longitude))"
        )
    }

    var body: some View {
        Map(position: $camera, interactionModes: .all) {
            // User dot
            UserAnnotation()

            // Radius ring (no fill)
            MapPolyline(coordinates: ringCoords)
                .stroke(Color.gray.opacity(0.85), lineWidth: 2)

            // Pins – keep builder tiny, no anchor arg
            ForEach(items) { item in
                Annotation(item.name, coordinate: item.coordinate) {
                    PinButton(id: item.id, gardens: gardens, onSelect: onSelect)
                }
            }
        }
        .mapControls {
            MapUserLocationButton()
            MapCompass()
        }
        // Keep camera synced with region (no remounts)
        .onAppear { camera = .region(region) }
        .onChange(of: region.center.latitude)  { _ in camera = .region(region) }
        .onChange(of: region.center.longitude) { _ in camera = .region(region) }
        .onChange(of: region.span.latitudeDelta)  { _ in camera = .region(region) }
        .onChange(of: region.span.longitudeDelta) { _ in camera = .region(region) }
    }
}

@available(iOS 17.0, *)
private struct PinButton: View {
    let id: String
    let gardens: [DogGarden]
    let onSelect: (DogGarden) -> Void

    var body: some View {
        Button {
            if let g = gardens.first(where: { $0.id == id }) { onSelect(g) }
        } label: {
            PawPinView()
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Helpers

@available(iOS 17.0, *)
private func circlePolylineCoords(center: CLLocationCoordinate2D,
                                  radius: CLLocationDistance,
                                  segments: Int = 180) -> [CLLocationCoordinate2D] {
    guard segments > 2 else { return [center] }
    let R = 6_371_000.0 // Earth radius (m)
    let lat1 = center.latitude * .pi / 180
    let lon1 = center.longitude * .pi / 180
    let angularDistance = radius / R

    var coords: [CLLocationCoordinate2D] = []
    coords.reserveCapacity(segments + 1)

    for i in 0...segments {
        let bearing = (Double(i) / Double(segments)) * 2.0 * .pi
        let sinLat1 = sin(lat1), cosLat1 = cos(lat1)
        let sinAd = sin(angularDistance), cosAd = cos(angularDistance)

        let sinLat2 = sinLat1 * cosAd + cosLat1 * sinAd * cos(bearing)
        let lat2 = asin(sinLat2)
        let y = sin(bearing) * sinAd * cosLat1
        let x = cosAd - sinLat1 * sinLat2
        let lon2 = lon1 + atan2(y, x)

        coords.append(CLLocationCoordinate2D(latitude: lat2 * 180 / .pi,
                                             longitude: lon2 * 180 / .pi))
    }
    return coords
}

// MARK: - Custom Pin

/// Red teardrop map pin with a white outline and white paw.
private struct PawPinView: View {
    var body: some View {
        ZStack {
            TeardropShape().fill(Color.red)
            TeardropShape().stroke(Color.white, lineWidth: 1)
            Image(systemName: "pawprint.fill")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(.white)
                .offset(y: -6)
        }
        .frame(width: 24, height: 34)
        .shadow(radius: 1)
        .contentShape(Rectangle())
    }
}

private struct TeardropShape: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let w = rect.width, h = rect.height
        let headDiameter = min(w, h * 0.65)
        let headRect = CGRect(x: (w - headDiameter)/2, y: 0, width: headDiameter, height: headDiameter)
        p.addEllipse(in: headRect)
        let tipY = h - 1, baseY = headRect.maxY - 2
        let halfBase = headDiameter * 0.32
        p.move(to: CGPoint(x: w/2, y: tipY))
        p.addLine(to: CGPoint(x: (w/2) - halfBase, y: baseY))
        p.addLine(to: CGPoint(x: (w/2) + halfBase, y: baseY))
        p.closeSubpath()
        return p
    }
}
