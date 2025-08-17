//  LegacyRadiusMap.swift
//  iosApp

import SwiftUI
import MapKit
import Shared

@available(iOS, introduced: 13, deprecated: 17)
struct LegacyRadiusMap: UIViewRepresentable {
    @Binding var region: MKCoordinateRegion
    var center: CLLocationCoordinate2D
    var radiusMeters: CLLocationDistance
    var showUserDot: Bool
    var followUser: Bool
    var gardens: [DogGarden]
    var onSelect: (DogGarden) -> Void

    final class Coordinator: NSObject, MKMapViewDelegate, CLLocationManagerDelegate {
        let manager = CLLocationManager()
        weak var map: MKMapView?
        var followUser = false
        var gardens: [DogGarden] = []
        var onSelect: ((DogGarden) -> Void)?

        func startIfNeeded() {
            guard followUser else { return }
            if manager.authorizationStatus == .notDetermined { manager.requestWhenInUseAuthorization() }
            manager.delegate = self
            manager.startUpdatingLocation()
        }

        func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
            guard followUser else { return }
            if manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways {
                manager.startUpdatingLocation()
            }
        }

        // RING ONLY: clear fill + light stroke
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let circle = overlay as? MKCircle {
                let r = MKCircleRenderer(circle: circle)
                r.fillColor   = UIColor.clear
                r.strokeColor = UIColor.systemGray2.withAlphaComponent(0.85) // 85% gray
                r.lineWidth   = 2
                return r
            }
            return MKOverlayRenderer(overlay: overlay)
        }

        // Red marker with white paw
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation { return nil }
            let id = "garden"
            let view = mapView.dequeueReusableAnnotationView(withIdentifier: id) as? MKMarkerAnnotationView
                ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: id)
            view.annotation = annotation
            view.markerTintColor = .systemRed
            view.glyphImage = UIImage(systemName: "pawprint.fill")
            view.canShowCallout = false
            return view
        }

        func mapView(_ mapView: MKMapView, didSelect view: MKAnnotationView) {
            guard let ann = view.annotation else { return }
            if let id = ann.subtitle ?? nil, let g = gardens.first(where: { $0.id == id }) {
                onSelect?(g); return
            }
            if let name = ann.title ?? nil, let g = gardens.first(where: { $0.name == name }) {
                onSelect?(g)
            }
        }
    }

    func makeCoordinator() -> Coordinator {
        let c = Coordinator()
        c.followUser = followUser
        c.gardens = gardens
        c.onSelect = onSelect
        return c
    }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView(frame: .zero)
        map.delegate = context.coordinator
        context.coordinator.map = map
        map.showsUserLocation = showUserDot
        map.userTrackingMode = followUser ? .follow : .none
        map.setRegion(region, animated: false)
        context.coordinator.startIfNeeded()
        map.addOverlay(MKCircle(center: center, radius: radiusMeters))
        addAnnotations(map)
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        let eps = 1e-9
        let centerChanged =
            abs(map.region.center.latitude  - region.center.latitude)  > eps ||
            abs(map.region.center.longitude - region.center.longitude) > eps
        let spanChanged =
            abs(map.region.span.latitudeDelta  - region.span.latitudeDelta)  > eps ||
            abs(map.region.span.longitudeDelta - region.span.longitudeDelta) > eps
        if centerChanged || spanChanged { map.setRegion(region, animated: false) }

        map.removeOverlays(map.overlays)
        map.addOverlay(MKCircle(center: center, radius: radiusMeters))

        map.removeAnnotations(map.annotations.filter { !($0 is MKUserLocation) })
        addAnnotations(map)
        context.coordinator.gardens = gardens
    }

    private func addAnnotations(_ map: MKMapView) {
        let anns = gardens.map { g -> MKPointAnnotation in
            let a = MKPointAnnotation()
            a.title = g.name
            a.subtitle = g.id
            a.coordinate = CLLocationCoordinate2D(latitude: g.location.latitude, longitude: g.location.longitude)
            return a
        }
        map.addAnnotations(anns)
    }
}
