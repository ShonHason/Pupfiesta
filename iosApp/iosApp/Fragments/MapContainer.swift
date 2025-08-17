//  MapContainer.swift
//  iosApp

import SwiftUI
import MapKit
import Shared

private struct GardenAnnotationItem: Identifiable {
    let id: String
    let name: String
    let coordinate: CLLocationCoordinate2D
}

struct MapContainer: View {
    @Binding var region: MKCoordinateRegion
    @Binding var camera: MapCameraPosition
    let userCoord: CLLocationCoordinate2D?
    let radiusMeters: Int32
    let gardens: [DogGarden]
    let onSelect: (DogGarden) -> Void

    // map DogGarden -> lightweight annotation items
    private func makeItems(_ gs: [DogGarden]) -> [GardenAnnotationItem] {
        var out: [GardenAnnotationItem] = []
        out.reserveCapacity(gs.count)
        for g in gs {
            out.append(
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
        return out
    }

    var body: some View {
        Group {
            if #available(iOS 17.0, *) {
                let items = makeItems(gardens)
                let circleCenter = userCoord ?? region.center
                let ringCoords = circlePolyline(center: circleCenter, radius: CLLocationDistance(radiusMeters))

                Map(position: $camera, interactionModes: .all) {
                    // Blue dot
                    UserAnnotation()

                    // ---- RADIUS RING (no fill) ----
                    MapPolyline(coordinates: ringCoords)
                        .stroke(Color.gray.opacity(0.85), lineWidth: 2) // 85% gray

                    // Custom, tappable pins
                    ForEach(items) { item in
                        Annotation(item.name, coordinate: item.coordinate, anchor: .bottom) {
                            Button {
                                if let g = gardens.first(where: { $0.id == item.id }) {
                                    onSelect(g)
                                }
                            } label: {
                                PawPinView()
                            }
                            .buttonStyle(.plain)
                        }
                        .annotationTitles(.hidden)
                    }
                }
                .mapControls {
                    MapUserLocationButton()
                    MapCompass()
                }
                .onAppear { camera = .region(region) }
                .onChange(of: region.center.latitude)  { _ in camera = .region(region) }
                .onChange(of: region.center.longitude) { _ in camera = .region(region) }
                .onChange(of: region.span.latitudeDelta)  { _ in camera = .region(region) }
                .onChange(of: region.span.longitudeDelta) { _ in camera = .region(region) }

            } else {
                // iOS 13–16 fallback
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
    }

    // MARK: - Helpers (iOS 17+)
    /// Build a ring as a polyline so there is never a filled area (works around iOS 18.5 black-fill bug).
    @available(iOS 17.0, *)
    private func circlePolyline(center: CLLocationCoordinate2D, radius: CLLocationDistance, segments: Int = 180) -> [CLLocationCoordinate2D] {
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
