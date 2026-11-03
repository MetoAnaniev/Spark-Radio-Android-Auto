import Foundation

/// Catalog + favorites + user stations, persisted as JSON in UserDefaults.
@MainActor
final class StationStore: ObservableObject {
    @Published private(set) var remote: [Station] = []
    @Published private(set) var userStations: [Station] = []
    @Published private(set) var favoriteIDs: [String] = []   // ordered
    @Published private(set) var isLoading = false
    @Published private(set) var loadError: String?
    @Published var lastStationID: String? { didSet { defaults.set(lastStationID, forKey: Keys.last) } }

    private let client = RadioBrowserClient()
    private let defaults = UserDefaults.standard
    private enum Keys { static let user = "userStations", favs = "favoriteIDs", last = "lastStationID", known = "knownStations" }

    init() {
        userStations = Self.decode(defaults.data(forKey: Keys.user)) ?? []
        favoriteIDs = defaults.stringArray(forKey: Keys.favs) ?? []
        lastStationID = defaults.string(forKey: Keys.last)
        // Favorites from the live directory must survive restarts even offline.
        remote = Self.decode(defaults.data(forKey: Keys.known)) ?? []
    }

    var all: [Station] {
        var seen = Set<String>()
        return (userStations + DefaultStations.all + remote).filter { seen.insert($0.id).inserted }
    }

    var favorites: [Station] {
        let byID = Dictionary(all.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        return favoriteIDs.compactMap { byID[$0] }
    }

    var countries: [String] {
        Array(Set(all.map(\.country))).sorted { a, b in
            a == "Bulgaria" ? true : b == "Bulgaria" ? false : a < b
        }
    }

    func station(id: String?) -> Station? { id.flatMap { id in all.first { $0.id == id } } }

    func refresh() async {
        isLoading = true; defer { isLoading = false }
        do {
            remote = try await client.stations()
            loadError = nil
            let favRemote = remote.filter { favoriteIDs.contains($0.id) }
            defaults.set(try? JSONEncoder().encode(favRemote), forKey: Keys.known)
        } catch {
            loadError = "Live directory unavailable — showing built-in stations."
        }
    }

    func isFavorite(_ s: Station) -> Bool { favoriteIDs.contains(s.id) }

    func toggleFavorite(_ s: Station) {
        if let i = favoriteIDs.firstIndex(of: s.id) { favoriteIDs.remove(at: i) } else { favoriteIDs.append(s.id) }
        saveFavorites()
    }

    func moveFavorites(from: IndexSet, to: Int) {
        var ids = favorites.map(\.id); ids.move(fromOffsets: from, toOffset: to)
        favoriteIDs = ids; saveFavorites()
    }

    func save(_ station: Station) {
        var s = station; s.source = .user
        if let i = userStations.firstIndex(where: { $0.id == s.id }) { userStations[i] = s } else { userStations.insert(s, at: 0) }
        defaults.set(try? JSONEncoder().encode(userStations), forKey: Keys.user)
    }

    func delete(_ station: Station) {
        userStations.removeAll { $0.id == station.id }
        favoriteIDs.removeAll { $0 == station.id }
        defaults.set(try? JSONEncoder().encode(userStations), forKey: Keys.user)
        saveFavorites()
    }

    private func saveFavorites() {
        defaults.set(favoriteIDs, forKey: Keys.favs)
        defaults.set(try? JSONEncoder().encode(remote.filter { favoriteIDs.contains($0.id) }), forKey: Keys.known)
    }

    private static func decode<T: Decodable>(_ data: Data?) -> T? { data.flatMap { try? JSONDecoder().decode(T.self, from: $0) } }

    /// Same rules as Android `ValidateStreamUrl`: http(s) with a host.
    static func isValidStreamURL(_ text: String) -> Bool {
        guard let u = URL(string: text.trimmingCharacters(in: .whitespaces)), let scheme = u.scheme?.lowercased(),
              ["http", "https"].contains(scheme), let host = u.host, !host.isEmpty else { return false }
        return true
    }
}
