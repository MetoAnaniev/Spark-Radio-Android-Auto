import SwiftUI

struct StationsView: View {
    @EnvironmentObject var store: StationStore
    @State private var query = ""
    @State private var editing: Station?
    @State private var adding = false

    private var filtered: [Station] {
        let q = query.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return store.all }
        return store.all.filter { "\($0.name) \($0.genre.label) \($0.country)".lowercased().contains(q) }
    }

    var body: some View {
        let list = filtered
        List {
            if let err = store.loadError { Text(err).font(.footnote).foregroundStyle(.secondary) }
            if !store.userStations.isEmpty, query.isEmpty {
                Section("My Stations") {
                    ForEach(store.userStations) { s in
                        StationRow(station: s, queue: list)
                            .contextMenu {
                                Button("Edit") { editing = s }
                                Button("Delete", role: .destructive) { store.delete(s) }
                            }
                    }
                }
            }
            ForEach(Dictionary(grouping: list.filter { $0.source != .user }, by: \.country)
                .sorted { $0.key == "Bulgaria" ? true : $1.key == "Bulgaria" ? false : $0.key < $1.key }, id: \.key) { country, items in
                Section(country) { ForEach(items) { StationRow(station: $0, queue: list) } }
            }
        }
        .overlay { if store.isLoading && store.remote.isEmpty { ProgressView("Loading live directory…") } }
        .searchable(text: $query, prompt: "Station, genre or country")
        .refreshable { await store.refresh() }
        .navigationTitle("Stations")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { adding = true } label: { Label("Add station", systemImage: "plus") }
            }
        }
        .sheet(isPresented: $adding) { EditStationView(station: nil) }
        .sheet(item: $editing) { EditStationView(station: $0) }
    }
}

struct EditStationView: View {
    let station: Station?
    @EnvironmentObject var store: StationStore
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var url = ""
    @State private var logo = ""
    @State private var genre: Genre = .other
    @State private var country = ""

    private var valid: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty && StationStore.isValidStreamURL(url) }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name", text: $name)
                TextField("Stream URL (http/https)", text: $url)
                    #if os(iOS)
                    .keyboardType(.URL).textInputAutocapitalization(.never)
                    #endif
                if !url.isEmpty && !StationStore.isValidStreamURL(url) {
                    Text("Enter a valid http or https stream URL").font(.caption).foregroundStyle(.red)
                }
                TextField("Logo URL (optional)", text: $logo)
                Picker("Genre", selection: $genre) { ForEach(Genre.allCases) { Text($0.label).tag($0) } }
                TextField("Country", text: $country)
            }
            .navigationTitle(station == nil ? "Add station" : "Edit station")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.save(Station(id: station?.id ?? UUID().uuidString,
                                           name: name.trimmingCharacters(in: .whitespaces),
                                           streamURL: URL(string: url.trimmingCharacters(in: .whitespaces))!,
                                           logoURL: URL(string: logo), genre: genre, details: "My station",
                                           country: country.isEmpty ? "International" : country, source: .user))
                        dismiss()
                    }.disabled(!valid)
                }
            }
        }
        .frame(minWidth: 360, minHeight: 360)
        .onAppear {
            guard let s = station else { return }
            name = s.name; url = s.streamURL.absoluteString; logo = s.logoURL?.absoluteString ?? ""
            genre = s.genre; country = s.country
        }
    }
}
