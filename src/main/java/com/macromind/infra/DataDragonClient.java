package com.macromind.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.ChampionClass;
import com.macromind.domain.model.Item;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Loads static Riot Data Dragon data, cached on disk per patch; falls back to the newest cache when offline. */
public class DataDragonClient {

    private static final String DEFAULT_BASE = "https://ddragon.leagueoflegends.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)+");
    private static final String CHAMPIONS = "championFull.json";
    private static final String ITEMS = "item.json";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final Path cacheDir;
    private final String base;
    private volatile Map<String, ChampionBaseStats> champions = Map.of();
    private volatile Map<String, ChampionClass> classes = Map.of();
    private volatile List<Item> items = List.of();

    public DataDragonClient(Path cacheDir) {
        this(cacheDir, DEFAULT_BASE);
    }

    public DataDragonClient(Path cacheDir, String baseUrl) {
        this.cacheDir = cacheDir;
        this.base = baseUrl;
    }

    /** Never throws. Without network it uses the newest cached patch; with neither, advice stays disabled. */
    public void load() {
        Optional<String> version = latestVersion();
        if (version.isEmpty()) {
            System.err.println("Data Dragon unreachable and no cache found: advice is DISABLED until it loads");
            return;
        }
        try {
            String championJson = cached(version.get(), CHAMPIONS);
            String itemJson = cached(version.get(), ITEMS);
            Map<String, ChampionBaseStats> parsedChampions = DataDragonParser.parseChampions(championJson);
            List<Item> parsedItems = DataDragonParser.parseItems(itemJson);
            if (parsedChampions.isEmpty() || parsedItems.isEmpty()) {
                discard(version.get());
                System.err.println("Data Dragon data for " + version.get() + " was unusable and was discarded");
                return;
            }
            classes = DataDragonParser.parseChampionClasses(championJson);
            champions = parsedChampions;
            items = parsedItems;
        } catch (Exception e) {
            System.err.println("Data Dragon load failed, advice is DISABLED: " + e.getMessage());
        }
    }

    public boolean isLoaded() {
        return !items.isEmpty();
    }

    public Optional<ChampionBaseStats> championBaseStats(String championName) {
        return Optional.ofNullable(champions.get(championName));
    }

    /** Unknown champions default to BRUISER, the most neutral class for the rules. */
    public ChampionClass championClass(String championName) {
        return classes.getOrDefault(championName, ChampionClass.BRUISER);
    }

    public List<Item> items() {
        return items;
    }

    private Optional<String> latestVersion() {
        try {
            String version = new ObjectMapper().readTree(fetch(base + "/api/versions.json")).get(0).asText();
            if (VERSION.matcher(version).matches()) {
                return Optional.of(version);
            }
        } catch (Exception e) {
            System.err.println("Data Dragon version lookup failed: " + e.getMessage());
        }
        return newestCachedVersion();
    }

    private Optional<String> newestCachedVersion() {
        if (!Files.isDirectory(cacheDir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(cacheDir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith("-" + ITEMS))
                    .map(n -> n.substring(0, n.length() - ITEMS.length() - 1))
                    .filter(v -> VERSION.matcher(v).matches())
                    .filter(v -> Files.exists(cacheDir.resolve(v + "-" + CHAMPIONS)))
                    .max(DataDragonClient::compareVersions);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static int compareVersions(String a, String b) {
        int[] left = Stream.of(a.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] right = Stream.of(b.split("\\.")).mapToInt(Integer::parseInt).toArray();
        return java.util.Arrays.compare(left, right);
    }

    private String cached(String version, String file) throws IOException, InterruptedException {
        Path path = cacheDir.resolve(version + "-" + file);
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        String body = fetch(base + "/cdn/" + version + "/data/en_US/" + file);
        Files.createDirectories(cacheDir);
        Path temp = Files.createTempFile(cacheDir, file, ".tmp");
        Files.writeString(temp, body);
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return body;
    }

    private void discard(String version) {
        for (String file : List.of(CHAMPIONS, ITEMS)) {
            try {
                Files.deleteIfExists(cacheDir.resolve(version + "-" + file));
            } catch (IOException ignored) {
                // best effort: a stuck file is re-validated on the next load
            }
        }
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
