//
//  Item.swift
//  LebenInDeutschland
//
//  Created by Perihan İmamoğlu Erol on 10.10.2026.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
