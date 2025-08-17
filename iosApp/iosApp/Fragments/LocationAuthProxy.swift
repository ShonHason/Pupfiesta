//
//  LocationAuthProxy.swift
//  iosApp
//
//  Created by Shon Hason on 15/08/2025.
//

import Foundation
import CoreLocation

/// Add NSLocationWhenInUseUsageDescription to Info.plist
final class LocationAuthProxy: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var asked = false
    var onAuthorized: (() -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    func requestOnce() {
        switch manager.authorizationStatus {
        case .notDetermined:
            if !asked {
                asked = true
                manager.requestWhenInUseAuthorization()
            }
        case .authorizedAlways, .authorizedWhenInUse:
            onAuthorized?()
        default:
            break
        }
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            onAuthorized?()
        default:
            break
        }
    }
}
