import SwiftUI

enum Tab: String, CaseIterable, Identifiable {
    case nowPlaying = "Now Playing", stations = "Stations", explore = "Explore", favorites = "Favorites", settings = "Settings"
    var id: String { rawValue }
    var symbol: String {
        switch self {
        case .nowPlaying: "play.circle.fill"; case .stations: "radio"; case .explore: "square.grid.2x2"
        case .favorites: "heart.fill"; case .settings: "gearshape"
        }
    }
}

struct ContentView: View {
    @State private var tab: Tab = .stations
    @EnvironmentObject var player: PlayerController

    var body: some View {
        #if os(macOS)
        NavigationSplitView {
            List(Tab.allCases.filter { $0 != .settings }, selection: Binding(get: { tab }, set: { tab = $0 ?? .stations })) { t in
                Label(t.rawValue, systemImage: t.symbol).tag(t)
            }
            .navigationSplitViewColumnWidth(min: 180, ideal: 200)
        } detail: {
            NavigationStack { screen(tab) }
                .safeAreaInset(edge: .bottom) { MiniPlayer(tab: $tab) }
        }
        #else
        TabView(selection: $tab) {
            ForEach(Tab.allCases) { t in
                NavigationStack { screen(t) }
                    .safeAreaInset(edge: .bottom) { if t != .nowPlaying { MiniPlayer(tab: $tab) } }
                    .tabItem { Label(t.rawValue, systemImage: t.symbol) }
                    .tag(t)
            }
        }
        #endif
    }

    @ViewBuilder private func screen(_ t: Tab) -> some View {
        switch t {
        case .nowPlaying: NowPlayingView()
        case .stations: StationsView()
        case .explore: ExploreView()
        case .favorites: FavoritesView()
        case .settings: SettingsView()
        }
    }
}

struct MiniPlayer: View {
    @Binding var tab: Tab
    @EnvironmentObject var player: PlayerController

    var body: some View {
        if let s = player.current {
            HStack(spacing: 12) {
                StationLogo(station: s, size: 44)
                VStack(alignment: .leading) {
                    Text(s.name).font(.headline).lineLimit(1)
                    Text(player.nowPlayingTitle ?? player.status.rawValue).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer()
                Button { player.toggle() } label: {
                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill").font(.title2)
                }.buttonStyle(.plain)
            }
            .padding(12)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
            .padding(.horizontal).padding(.bottom, 6)
            .onTapGesture { tab = .nowPlaying }
        }
    }
}
