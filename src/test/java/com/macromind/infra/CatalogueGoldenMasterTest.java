package com.macromind.infra;

import com.macromind.domain.model.Item;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins what the parser makes of Riot's REAL item catalogue (cached by the app in ~/.macromind/cache): every item's
 * tags and the stats the rules read. Skipped when no cache exists (CI, fresh checkout). Refresh the pinned digest on
 * purpose with -Dmacromind.golden.write=true.
 */
class CatalogueGoldenMasterTest {

    private static final Path CACHE = Path.of(System.getProperty("user.home"), ".macromind", "cache");
    private static final Path PIN = Path.of("src/test/resources/fixtures/catalogue-digest.txt");

    private static Path newestItemFile() throws Exception {
        try (var files = Files.list(CACHE)) {
            return files.filter(p -> p.getFileName().toString().endsWith("-item.json"))
                    .max(java.util.Comparator.comparing(p -> p.getFileName().toString())).orElseThrow();
        }
    }

    static String digest(List<Item> items) {
        List<String> lines = new ArrayList<>();
        for (Item i : items) {
            var s = i.stats();
            lines.add(i.id() + "|" + i.name() + "|" + i.goldValue() + "|ad" + s.attackDamage() + ",ap" + s.abilityPower()
                    + ",ar" + s.armor() + ",mr" + s.magicResist() + ",hp" + s.health() + ",leth" + s.flatArmorPen()
                    + ",mpen" + s.flatMagicPen() + ",as" + s.attackSpeed() + ",haste" + s.cooldownReduction()
                    + ",ls" + s.lifesteal() + ",crit" + s.criticalStrikeChance() + ",omni" + s.omnivamp()
                    + "|" + i.tags().stream().map(Enum::name).collect(Collectors.toCollection(TreeSet::new)));
        }
        lines.sort(String::compareTo);
        return String.join("\n", lines) + "\n";
    }

    @Test
    void realCatalogueParsesToTheSameTagsAndStats() throws Exception {
        assumeTrue(hasItemCache(), "no cached Data Dragon item file");
        Path itemFile = newestItemFile();
        String patch = "# patch " + itemFile.getFileName().toString().replace("-item.json", "");
        String actual = digest(DataDragonParser.parseItems(Files.readString(itemFile)));

        if (Boolean.getBoolean("macromind.golden.write") || !Files.exists(PIN)) {
            Files.writeString(PIN, patch + "\n" + actual);
            return;
        }
        List<String> pinned = Files.readAllLines(PIN);
        assumeTrue(pinned.get(0).equals(patch), "pinned digest is for " + pinned.get(0) + ", cache has " + patch);
        assertEquals(String.join("\n", pinned.subList(1, pinned.size())) + "\n", actual);
    }

    private static boolean hasItemCache() {
        try (var files = Files.list(CACHE)) {
            return files.anyMatch(p -> p.getFileName().toString().endsWith("-item.json"));
        } catch (Exception e) {
            return false;
        }
    }
}
