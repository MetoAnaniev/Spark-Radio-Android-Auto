import SwiftUI

struct ExploreView: View {
    @EnvironmentObject var store: StationStore
    private let columns = [GridItem(.adaptive(minimum: 150), spacing: 14)]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 14) {
                ForEach(Genre.allCases) { g in
                    NavigationLink(value: g) {
                        VStack(spacing: 10) {
                            Image(systemName: g.symbol).font(.largeTitle)
                            Text(g.label).font(.headline)
                            Text("\(store.all.filter { $0.genre == g }.count) stations").font(.caption).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, minHeight: 120)
                        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 22))
                    }.buttonStyle(.plain)
                }
            }.padding()
        }
        .navigationTitle("Explore")
        .navigationDestination(for: Genre.self) { GenreView(genre: $0) }
    }
}

struct GenreView: View {
    let genre: Genre
    @EnvironmentObject var store: StationStore
    @EnvironmentObject var player: PlayerController

    var body: some View {
        let list = store.all.filter { $0.genre == genre }
        List(list) { StationRow(station: $0, queue: list) }
            .navigationTitle(genre.label)
            .toolbar {
                Button { if let f = list.first { player.play(f, in: list) } } label: { Label("Play first", systemImage: "play.fill") }
                    .disabled(list.isEmpty)
            }
    }
}

struct FavoritesView: View {
    @EnvironmentObject var store: StationStore

    var body: some View {
        let favs = store.favorites
        List {
            ForEach(favs) { StationRow(station: $0, queue: favs) }
                .onMove { store.moveFavorites(from: $0, to: $1) }
        }
        .overlay {
            if favs.isEmpty {
                VStack(spacing: 8) {
                    Image(systemName: "heart").font(.largeTitle)
                    Text("Tap the heart on any station to add it here.").foregroundStyle(.secondary)
                }
            }
        }
        .navigationTitle("Favorites")
        #if os(iOS)
        .toolbar { EditButton() }
        #endif
    }
}

struct NowPlayingView: View {
    @EnvironmentObject var store: StationStore
    @EnvironmentObject var player: PlayerController

    var body: some View {
        if let s = player.current {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 40) { StationLogo(station: s, size: 320); controls(s) }.padding(40)
                VStack(spacing: 24) { StationLogo(station: s, size: 260); controls(s) }.padding()
            }
            .navigationTitle("Now Playing")
        } else {
            VStack(spacing: 16) {
                Image(systemName: "radio").font(.system(size: 60))
                Text("Pick a station to start listening").foregroundStyle(.secondary)
                if let last = store.station(id: store.lastStationID) {
                    Button("Resume \(last.name)") { player.play(last, in: store.all) }.buttonStyle(.borderedProminent)
                }
            }
            .navigationTitle("Now Playing")
        }
    }

    private func controls(_ s: Station) -> some View {
        VStack(spacing: 14) {
            Text(s.name).font(.largeTitle.bold()).multilineTextAlignment(.center)
            if let t = player.nowPlayingTitle { Text(t).font(.title3).multilineTextAlignment(.center) }
            Text("\(s.genre.label) · \(s.country)").foregroundStyle(.secondary)
            if !s.details.isEmpty { Text(s.details).font(.footnote).foregroundStyle(.secondary) }
            HStack(spacing: 6) {
                if player.status == .connecting { ProgressView().controlSize(.small) }
                Text(player.status.rawValue).font(.caption).foregroundStyle(player.status == .error ? .red : .secondary)
            }
            HStack(spacing: 36) {
                Button { player.previous() } label: { Image(systemName: "backward.fill").font(.title) }
                Button { player.toggle() } label: {
                    Image(systemName: player.isPlaying ? "pause.circle.fill" : "play.circle.fill").font(.system(size: 76))
                }
                Button { player.next() } label: { Image(systemName: "forward.fill").font(.title) }
            }.buttonStyle(.plain)
            Button { store.toggleFavorite(s) } label: {
                Label(store.isFavorite(s) ? "Favorited" : "Favorite", systemImage: store.isFavorite(s) ? "heart.fill" : "heart")
            }.buttonStyle(.bordered)
        }
        .onAppear { store.lastStationID = s.id }
        .onChange(of: s.id) { store.lastStationID = $0 }
    }
}

struct SettingsView: View {
    @AppStorage("theme") private var theme = "dark"

    var body: some View {
        Form {
            Picker("Appearance", selection: $theme) {
                Text("Dark").tag("dark"); Text("Light").tag("light"); Text("System").tag("system")
            }
            Section("About") {
                LabeledContent("Version", value: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "")
                LabeledContent("Directory", value: "Radio Browser (radio-browser.info)")
                Link("Privacy policy", destination: URL(string: "https://sparklab.bg/radiospark/privacy")!)
            }
        }
        .navigationTitle("Settings")
        .frame(minWidth: 360)
    }
}
