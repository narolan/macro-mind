package com.macromind.infra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The on-disk copy of Data Dragon files, one pair of files per patch version. */
final class DataDragonCache {

    static final String CHAMPIONS = "championFull.json";
    static final String ITEMS = "item.json";

    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)+");

    private final Path dir;

    DataDragonCache(Path dir) {
        this.dir = dir;
    }

    static boolean isVersion(String text) {
        return VERSION.matcher(text).matches();
    }

    /** The file's content, or empty when it was never cached. */
    Optional<String> read(String version, String file) throws IOException {
        Path path = pathOf(version, file);
        return Files.exists(path) ? Optional.of(Files.readString(path)) : Optional.empty();
    }

    /** Writes through a temp file and an atomic move, so a crash never leaves a half-written cache file behind. */
    void write(String version, String file, String content) throws IOException {
        Path path = pathOf(version, file);
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, file, ".tmp");
        Files.writeString(temp, content);
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Removes both files of a version; best effort, because a stuck file is re-validated on the next load. */
    void discard(String version) {
        for (String file : List.of(CHAMPIONS, ITEMS)) {
            try {
                Files.deleteIfExists(pathOf(version, file));
            } catch (IOException ignored) {
                // see above
            }
        }
    }

    /** The highest version for which both files are cached. */
    Optional<String> newestVersion() {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith("-" + ITEMS))
                    .map(name -> name.substring(0, name.length() - ITEMS.length() - 1))
                    .filter(DataDragonCache::isVersion)
                    .filter(version -> Files.exists(pathOf(version, CHAMPIONS)))
                    .max(DataDragonCache::compareVersions);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // The version becomes part of a file name, so anything that is not a plain patch number is refused.
    private Path pathOf(String version, String file) {
        if (!isVersion(version)) {
            throw new IllegalArgumentException("Not a patch version: " + version);
        }
        return dir.resolve(version + "-" + file);
    }

    private static int compareVersions(String a, String b) {
        return Arrays.compare(parts(a), parts(b));
    }

    private static int[] parts(String version) {
        return Stream.of(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }
}
