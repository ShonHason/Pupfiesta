//
//  CloudinaryUploader.swift
//  iosApp
//
//  Created by Shon Hason on 19/08/2025.
import Foundation
import Cloudinary

enum CloudinaryUploader {
    // Read config from Info.plist
    private static var cloudName: String {
        Bundle.main.object(forInfoDictionaryKey: "CLOUDINARY_CLOUD_NAME") as? String ?? ""
    }
    private static var uploadPreset: String {
        Bundle.main.object(forInfoDictionaryKey: "CLOUDINARY_UPLOAD_PRESET") as? String ?? ""
    }
    private static var folder: String? {
        Bundle.main.object(forInfoDictionaryKey: "CLOUDINARY_FOLDER") as? String
    }

    private static let cloudinary: CLDCloudinary = {
        let cfg = CLDConfiguration(cloudName: cloudName, apiKey: nil, apiSecret: nil, secure: true)
        return CLDCloudinary(configuration: cfg)
    }()

    /// Upload raw image data using an **unsigned** upload preset.
    /// Calls back with the **secure https URL** or `nil` on failure.
    static func upload(_ data: Data, completion: @escaping (String?) -> Void) {
        guard !cloudName.isEmpty, !uploadPreset.isEmpty else {
            print("Cloudinary config missing. Ensure CLOUDINARY_CLOUD_NAME & CLOUDINARY_UPLOAD_PRESET in Info.plist")
            completion(nil)
            return
        }

        let publicId = "dog_\(Int(Date().timeIntervalSince1970))"
        let params = CLDUploadRequestParams().setPublicId(publicId)
        if let folder, !folder.isEmpty { _ = params.setFolder(folder) }

        cloudinary.createUploader()
            .upload(data: data, uploadPreset: uploadPreset, params: params) { result, error in
                if let error {
                    print("Cloudinary upload error: \(error)")
                    completion(nil)
                    return
                }
                completion(result?.secureUrl)
            }
    }
}
