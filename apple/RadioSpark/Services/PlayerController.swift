import AVFoundation
import MediaPlayer
import SwiftUI

/// Single AVPlayer shared by every screen, wired to lock screen / Control Center / Mac media keys.
@MainActor
final class PlayerController: NSObject, ObservableObject {
    enum Status: String { case idle = "Ready", connecting = "Connecting…", playing = "Playing", paused = "Paused", error = "Playback error" }

    @Published private(set) var current: Station?
    @Published private(set) var status: Status = .idle
    @Published private(set) var nowPlayingTitle: String?
    var queue: [Station] = []

    private let player = AVPlayer()
    private var observers: [NSKeyValueObservation] = []
    private var metadataOutput: AVPlayerItemMetadataOutput?

    var isPlaying: Bool { status == .playing || status == .connecting }

    override init() {
        super.init()
        #if os(iOS)
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, policy: .longFormAudio)
        #endif
        observers.append(player.observe(\.timeControlStatus) { [weak self] p, _ in
            Task { @MainActor in self?.syncStatus(p.timeControlStatus) }
        })
        setupRemoteCommands()
    }

    func play(_ station: Station, in queue: [Station]? = nil) {
        if let queue { self.queue = queue }
        current = station; nowPlayingTitle = nil; status = .connecting
        #if os(iOS)
        try? AVAudioSession.sharedInstance().setActive(true)
        #endif
        let item = AVPlayerItem(url: station.streamURL)
        let output = AVPlayerItemMetadataOutput(identifiers: nil)
        output.setDelegate(self, queue: .main)
        item.add(output); metadataOutput = output
        observers.append(item.observe(\.status) { [weak self] item, _ in
            if item.status == .failed { Task { @MainActor in self?.status = .error } }
        })
        player.replaceCurrentItem(with: item)
        player.play()
        updateNowPlaying()
    }

    func toggle() {
        guard let current else { return }
        if isPlaying { player.pause(); status = .paused }
        else if player.currentItem == nil || status == .error { play(current) }
        else { play(current) } // live radio: reconnect to the live edge instead of resuming stale buffer
        updateNowPlaying()
    }

    func next() { step(1) }
    func previous() { step(-1) }

    private func step(_ d: Int) {
        guard !queue.isEmpty else { return }
        let i = current.flatMap { c in queue.firstIndex { $0.id == c.id } } ?? -d
        play(queue[(i + d + queue.count) % queue.count])
    }

    private func syncStatus(_ s: AVPlayer.TimeControlStatus) {
        switch s {
        case .playing: status = .playing
        case .waitingToPlayAtSpecifiedRate: if status != .error { status = .connecting }
        case .paused: if status == .playing || status == .connecting { status = .paused }
        @unknown default: break
        }
        updateNowPlaying()
    }

    private func setupRemoteCommands() {
        let c = MPRemoteCommandCenter.shared()
        c.playCommand.addTarget { [weak self] _ in self?.toggleIf(playing: false); return .success }
        c.pauseCommand.addTarget { [weak self] _ in self?.toggleIf(playing: true); return .success }
        c.togglePlayPauseCommand.addTarget { [weak self] _ in self?.toggle(); return .success }
        c.nextTrackCommand.addTarget { [weak self] _ in self?.next(); return .success }
        c.previousTrackCommand.addTarget { [weak self] _ in self?.previous(); return .success }
    }

    private func toggleIf(playing: Bool) { if isPlaying == playing { toggle() } }

    private func updateNowPlaying() {
        guard let current else { MPNowPlayingInfoCenter.default().nowPlayingInfo = nil; return }
        var info: [String: Any] = [
            MPMediaItemPropertyTitle: nowPlayingTitle ?? current.name,
            MPMediaItemPropertyArtist: nowPlayingTitle == nil ? current.genre.label : current.name,
            MPMediaItemPropertyAlbumTitle: current.country,
            MPNowPlayingInfoPropertyIsLiveStream: true,
            MPNowPlayingInfoPropertyPlaybackRate: status == .playing ? 1.0 : 0.0,
        ]
        #if os(iOS)
        if let img = UIImage(named: "StationArtwork") {
            info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: img.size) { _ in img }
        }
        #endif
        MPNowPlayingInfoCenter.default().nowPlayingInfo = info
        #if os(macOS)
        MPNowPlayingInfoCenter.default().playbackState = status == .playing ? .playing : .paused
        #endif
    }
}

extension PlayerController: AVPlayerItemMetadataOutputPushDelegate {
    nonisolated func metadataOutput(_ output: AVPlayerItemMetadataOutput,
                                    didOutputTimedMetadataGroups groups: [AVTimedMetadataGroup],
                                    from track: AVPlayerItemTrack?) {
        guard let item = groups.first?.items.first(where: { $0.commonKey == .commonKeyTitle }) ?? groups.first?.items.first
        else { return }
        Task {
            let title = try? await item.load(.stringValue)
            await MainActor.run {
                let t = title?.trimmingCharacters(in: .whitespacesAndNewlines)
                self.nowPlayingTitle = (t?.isEmpty ?? true) ? nil : t
                self.updateNowPlaying()
            }
        }
    }
}
