package com.macromind.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loads static Riot Data Dragon data, cached on disk per patch; falls back to the newest cache when offline. */
public class DataDragonClient {

    private static final String DEFAULT_BASE = "https://ddragon.leagueoflegends.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Everything one successful load produced. Swapped in as a whole, so readers never see a half-updated mix. */
    private record Catalogue(String version, Map<String, ChampionBaseStats> champions,
                             Map<String, ChampionClass> classes, List<Item> items) {
        static final Catalogue EMPTY = new Catalogue("", Map.of(), Map.of(), List.of());
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final DataDragonCache cache;
    private final String base;
    private volatile Catalogue catalogue = Catalogue.EMPTY;

    public DataDragonClient(Path cacheDir) {
        this(cacheDir, DEFAULT_BASE);
    }

    public DataDragonClient(Path cacheDir, String baseUrl) {
        this.cache = new DataDragonCache(cacheDir);
        this.base = baseUrl;
    }

    /** Never throws. Without network it uses the newest cached patch; with neither, advice stays disabled. */
    public void load() {
        Optional<String> latest = latestVersion();
        if (latest.isEmpty()) {
            System.err.println("Data Dragon unreachable and no cache found: advice is DISABLED until it loads");
            return;
        }
        String version = latest.get();
        try {
            loadVersion(version);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            System.err.println("Data Dragon load failed, advice is DISABLED: " + e.getMessage());
        }
    }

    private void loadVersion(String version) throws IOException, InterruptedException {
        String championJson = fileContent(version, DataDragonCache.CHAMPIONS);
        String itemJson = fileContent(version, DataDragonCache.ITEMS);
        Map<String, ChampionBaseStats> champions = DataDragonParser.parseChampions(championJson);
        List<Item> items = DataDragonParser.parseItems(itemJson);
        if (champions.isEmpty() || items.isEmpty()) {
            cache.discard(version);
            System.err.println("Data Dragon data for " + version + " was unusable and was discarded");
            return;
        }
        catalogue = new Catalogue(version, champions, DataDragonParser.parseChampionClasses(championJson), items);
    }

    /** The patch the loaded data belongs to; empty until a load succeeds. Used to build item icon URLs. */
    public String version() {
        return catalogue.version();
    }

    public boolean isLoaded() {
        return !catalogue.items().isEmpty();
    }

    public Optional<ChampionBaseStats> championBaseStats(String championName) {
        return Optional.ofNullable(catalogue.champions().get(championName));
    }

    /** Unknown champions default to BRUISER, the most neutral class for the rules. */
    public ChampionClass championClass(String championName) {
        return catalogue.classes().getOrDefault(championName, ChampionClass.BRUISER);
    }

    public List<Item> items() {
        return catalogue.items();
    }

    private Optional<String> latestVersion() {
        try {
            JsonNode versions = JSON.readTree(fetch(base + "/api/versions.json"));
            String version = versions.path(0).asText();
            if (DataDragonCache.isVersion(version)) {
                return Optional.of(version);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            System.err.println("Data Dragon version lookup failed: " + e.getMessage());
        }
        return cache.newestVersion();
    }

    private String fileContent(String version, String file) throws IOException, InterruptedException {
        Optional<String> cached = cache.read(version, file);
        if (cached.isPresent()) {
            return cached.get();
        }
        String body = fetch(base + "/cdn/" + version + "/data/en_US/" + file);
        cache.write(version, file, body);
        return body;
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(REQUEST_TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }
}
