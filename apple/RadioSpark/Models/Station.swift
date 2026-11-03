import Foundation

enum Genre: String, CaseIterable, Codable, Identifiable {
    case pop, rock, jazz, classical, newsTalk = "news_talk", electronic, chill, bulgarian, greek, kids, other
    var id: String { rawValue }

    var label: String {
        switch self {
        case .pop: "Pop"; case .rock: "Rock"; case .jazz: "Jazz"; case .classical: "Classical"
        case .newsTalk: "News / Talk"; case .electronic: "Electronic"; case .chill: "Chill"
        case .bulgarian: "Bulgarian"; case .greek: "Greek"; case .kids: "Kids"; case .other: "Other"
        }
    }

    var symbol: String {
        switch self {
        case .pop: "music.note.list"; case .rock: "guitars"; case .jazz: "pianokeys"
        case .classical: "music.quarternote.3"; case .newsTalk: "newspaper"; case .electronic: "waveform"
        case .chill: "leaf"; case .kids: "figure.and.child.holdinghands"
        case .bulgarian, .greek, .other: "radio"
        }
    }

    /// Mirrors the Android `genreFrom` mapping so both platforms classify stations identically.
    static func from(tags: String, countryCode: String) -> Genre {
        let t = tags.lowercased()
        func has(_ words: String...) -> Bool { words.contains { t.contains($0) } }
        if has("rock", "metal") { return .rock }
        if has("jazz", "blues") { return .jazz }
        if has("classical", "classic") { return .classical }
        if has("news", "talk") { return .newsTalk }
        if has("electronic", "dance", "techno") { return .electronic }
        if has("chill", "lounge", "ambient") { return .chill }
        if has("kids", "children") { return .kids }
        if has("pop", "hits") { return .pop }
        if countryCode == "BG" { return .bulgarian }
        if countryCode == "GR" { return .greek }
        return .other
    }
}

struct Station: Identifiable, Codable, Hashable {
    enum Source: String, Codable { case builtIn, user, remote }

    var id: String
    var name: String
    var streamURL: URL
    var logoURL: URL?
    var genre: Genre = .other
    var details: String = ""
    var country: String = "International"
    var source: Source = .builtIn

    var isUserEditable: Bool { source == .user }
}

enum DefaultStations {
    private static func s(_ id: String, _ name: String, _ url: String, _ g: Genre, _ d: String) -> Station {
        Station(id: id, name: name, streamURL: URL(string: url)!, genre: g, details: d, country: "Bulgaria")
    }

    static let all: [Station] = [
        s("static_radio_nova_bg", "Radio Nova", "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIO_NOVAAAC_H.aac?dist=WEBSITEBG", .electronic, "Bulgarian electronic radio · AAC high quality"),
        s("static_bg_radio", "BG Radio", "https://play.global.audio/bgradio128", .bulgarian, "Bulgarian music"),
        s("static_energy_bg", "Energy NRJ Bulgaria", "https://play.global.audio/nrj128", .pop, "Hits and favorites"),
        s("static_city_hiphop_bg", "City HipHop R&B", "https://play.global.audio/cityrabhi.aac", .pop, "Hip-hop and R&B"),
        s("static_veronika_bg", "Radio Veronika", "https://play.global.audio/veronikahi.aac", .bulgarian, "Bulgarian and Balkan pop-folk"),
        s("static_btv_radio", "bTV Radio", "https://cdn.bweb.bg/radio/btv-radio.mp3", .newsTalk, "News and talk"),
        s("static_veselina_bg", "Radio Veselina", "https://bss1.neterra.tv/veselina/veselina.m3u8", .bulgarian, "Bulgarian and Balkan music"),
        s("static_bg_estrada", "BG Estrada", "https://play.global.audio/bgestrada", .bulgarian, "Bulgarian classics"),
        s("static_energy_90s_bg", "Energy NRJ Bulgaria 90s Only", "https://play.global.audio/energy-90s", .pop, "Nonstop 90s hits"),
        s("static_vanilla_bg", "Vanilla", "https://play.global.audio/vanillahi.aac", .chill, "Relaxed listening"),
    ]
}
