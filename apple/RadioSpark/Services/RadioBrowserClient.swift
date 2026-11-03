import Foundation

/// Free, keyless Radio Browser directory — same queries and limits as the Android client.
actor RadioBrowserClient {
    private let mirrors = ["https://de1.api.radio-browser.info", "https://de2.api.radio-browser.info"]
    private var cache: [Station] = []
    private var cachedAt = Date.distantPast
    private let cacheDuration: TimeInterval = 15 * 60

    private struct Row: Decodable {
        let stationuuid: String; let name: String; let url: String?; let url_resolved: String?
        let favicon: String?; let tags: String?; let countrycode: String?; let codec: String?; let bitrate: Int?
    }

    func stations() async throws -> [Station] {
        if !cache.isEmpty, Date().timeIntervalSince(cachedAt) < cacheDuration { return cache }
        let selections: [(String?, Int)] = [("BG", 500), ("GR", 150), (nil, 1000)]
        var loaded: [Station] = []
        await withTaskGroup(of: [Station].self) { group in
            for (cc, limit) in selections { group.addTask { await self.load(countryCode: cc, limit: limit) } }
            for await batch in group { loaded += batch }
        }
        var seen = Set<String>()
        loaded = loaded.filter { seen.insert($0.id).inserted }
        if loaded.isEmpty {
            if !cache.isEmpty { return cache }
            throw URLError(.cannotConnectToHost)
        }
        cache = loaded; cachedAt = Date()
        return loaded
    }

    private func load(countryCode: String?, limit: Int) async -> [Station] {
        for mirror in mirrors.shuffled() {
            var c = URLComponents(string: mirror + "/json/stations/search")!
            c.queryItems = [.init(name: "hidebroken", value: "true"), .init(name: "order", value: "clickcount"),
                            .init(name: "reverse", value: "true"), .init(name: "limit", value: String(limit))]
            if let cc = countryCode {
                c.queryItems! += [.init(name: "countrycode", value: cc), .init(name: "countrycodeExact", value: "true")]
            }
            var req = URLRequest(url: c.url!, timeoutInterval: 20)
            req.setValue("RadioSpark/1.6 (Apple; contact: SparkLab Academy)", forHTTPHeaderField: "User-Agent")
            guard let (data, resp) = try? await URLSession.shared.data(for: req),
                  (resp as? HTTPURLResponse)?.statusCode == 200,
                  let rows = try? JSONDecoder().decode([Row].self, from: data) else { continue }
            let mapped = rows.compactMap(Self.map)
            if !mapped.isEmpty { return mapped }
        }
        return []
    }

    private static func map(_ r: Row) -> Station? {
        let raw = (r.url_resolved?.trimmingCharacters(in: .whitespaces)).flatMap { $0.isEmpty ? nil : $0 } ?? r.url ?? ""
        let name = r.name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty, raw.hasPrefix("http://") || raw.hasPrefix("https://"), let url = URL(string: raw) else { return nil }
        let cc = (r.countrycode ?? "").uppercased()
        let tags = r.tags ?? ""
        let details = [tags.split(separator: ",").first.map { $0.trimmingCharacters(in: .whitespaces) },
                       r.codec.flatMap { $0.isEmpty ? nil : $0 },
                       (r.bitrate ?? 0) > 0 ? "\(r.bitrate!) kbps" : nil]
            .compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · ")
        let logo = r.favicon.flatMap { $0.hasPrefix("http") ? URL(string: $0) : nil }
        let country = cc.count == 2 ? (Locale(identifier: "en").localizedString(forRegionCode: cc) ?? cc) : "International"
        return Station(id: "rb_\(r.stationuuid)", name: name, streamURL: url, logoURL: logo,
                       genre: .from(tags: tags, countryCode: cc), details: details.isEmpty ? "Live radio" : details,
                       country: country, source: .remote)
    }
}
