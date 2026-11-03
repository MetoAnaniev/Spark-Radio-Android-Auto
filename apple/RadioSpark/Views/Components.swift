import SwiftUI

struct StationLogo: View {
    let station: Station
    var size: CGFloat = 52

    var body: some View {
        AsyncImage(url: station.logoURL) { phase in
            if let image = phase.image { image.resizable().scaledToFill() }
            else { Image("StationArtwork").resizable().scaledToFill() }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size * 0.25))
    }
}

struct StationRow: View {
    let station: Station
    let queue: [Station]
    @EnvironmentObject var store: StationStore
    @EnvironmentObject var player: PlayerController

    var body: some View {
        HStack(spacing: 14) {
            StationLogo(station: station)
            VStack(alignment: .leading, spacing: 2) {
                Text(station.name).font(.headline).lineLimit(1)
                Text("\(station.genre.label) · \(station.country)").font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            if player.current?.id == station.id, player.isPlaying {
                Image(systemName: "waveform").foregroundStyle(.tint)
            }
            Button { store.toggleFavorite(station) } label: {
                Image(systemName: store.isFavorite(station) ? "heart.fill" : "heart")
                    .foregroundStyle(store.isFavorite(station) ? Color.red : Color.secondary)
            }.buttonStyle(.plain)
        }
        .contentShape(Rectangle())
        .onTapGesture { player.play(station, in: queue) }
        .padding(.vertical, 4)
    }
}
