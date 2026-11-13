package com.macromind.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.macromind.domain.model.ChampionBaseStats;
import com.macromind.domain.model.Item;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loads static Riot Data Dragon data once at startup, cached on disk per patch version. */
public class DataDragonClient {

    private static final String BASE = "https://ddragon.leagueoflegends.com";

    private final HttpClient http = HttpClient.newHttpClient();
    private final Path cacheDir;
    private Map<String, ChampionBaseStats> champions = Map.of();
    private Map<String, com.macromind.domain.model.ChampionClass> classes = Map.of();
    private List<Item> items = List.of();

    public DataDragonClient(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /** Never throws: on failure the data stays empty and the estimator falls back to zero base stats. */
    public void load() {
        try {
            String version = new ObjectMapper().readTree(fetch(BASE + "/api/versions.json")).get(0).asText();
            String championJson = cached(version, "championFull.json");
            champions = DataDragonParser.parseChampions(championJson);
            classes = DataDragonParser.parseChampionClasses(championJson);
            items = DataDragonParser.parseItems(cached(version, "item.json"));
        } catch (Exception e) {
            System.err.println("Data Dragon unavailable, enemy stats will be unreliable: " + e.getMessage());
        }
    }

    public Optional<ChampionBaseStats> championBaseStats(String championName) {
        return Optional.ofNullable(champions.get(championName));
    }

    /** Unknown champions default to BRUISER, the most neutral class for the rules. */
    public com.macromind.domain.model.ChampionClass championClass(String championName) {
        return classes.getOrDefault(championName, com.macromind.domain.model.ChampionClass.BRUISER);
    }

    public List<Item> items() {
        return items;
    }

    private String cached(String version, String file) throws IOException, InterruptedException {
        Path path = cacheDir.resolve(version + "-" + file);
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        String body = fetch(BASE + "/cdn/" + version + "/data/en_US/" + file);
        Files.createDirectories(cacheDir);
        Files.writeString(path, body);
        return body;
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }
}
