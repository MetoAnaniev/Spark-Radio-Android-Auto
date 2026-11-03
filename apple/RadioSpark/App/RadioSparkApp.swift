import SwiftUI

@main
struct RadioSparkApp: App {
    @StateObject private var store = StationStore()
    @StateObject private var player = PlayerController()
    @AppStorage("theme") private var theme = "dark"

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .environmentObject(player)
                .tint(Color("AccentColor"))
                .preferredColorScheme(theme == "dark" ? .dark : theme == "light" ? .light : nil)
                .task { await store.refresh() }
        }
        #if os(macOS)
        .defaultSize(width: 1100, height: 720)
        .commands {
            CommandMenu("Playback") {
                Button("Play / Pause") { player.toggle() }.keyboardShortcut(.space, modifiers: .command)
                Button("Next Station") { player.next() }.keyboardShortcut(.rightArrow, modifiers: .command)
                Button("Previous Station") { player.previous() }.keyboardShortcut(.leftArrow, modifiers: .command)
            }
        }
        Settings { SettingsView().environmentObject(store) }
        #endif
    }
}
