//
//  CloudinaryManager.swift
//  iosApp
//
//  Created by Shon Hason on 19/08/2025.

import Foundation
import Cloudinary

enum CloudinaryManager {
    static let shared: CLDCloudinary = {
        let cloudName = (Foundation.Bundle.main.object(forInfoDictionaryKey: "CLOUDINARY_CLOUD_NAME") as? String)
            ?? "dlqydpa1y" // your cloud name
        let config = CLDConfiguration(cloudName: cloudName, secure: true)
        return CLDCloudinary(configuration: config)
    }()
}
