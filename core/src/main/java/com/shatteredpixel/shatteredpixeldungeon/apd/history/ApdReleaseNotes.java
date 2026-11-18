package com.shatteredpixel.shatteredpixeldungeon.apd.history;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Player release notes are curated independently of the complete internal APK ledger. */
public final class ApdReleaseNotes {
    private static final Pattern INTERNAL = Pattern.compile("(?i)\\bcodex\\b|\\bTEST\\s*\\d+\\b");
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "New Content", "Gameplay", "Quality of Life", "Bug Fixes", "Statistics / UIMSD",
            "Buffs", "Nerfs", "Developer Commentary"));

    private ApdReleaseNotes() { }

    public static List<ApdHistory.Build> releases(String data) {
        List<ApdHistory.Build> releases = ApdHistory.builds(data);
        for (ApdHistory.Build release : releases) {
            if (!release.label.startsWith("APD ") || INTERNAL.matcher(release.label).find()
                    || INTERNAL.matcher(release.status).find())
                throw new IllegalArgumentException("Release notes require APD branding");
            if (!release.date.isEmpty() && !release.date.matches("\\d{4}-\\d{2}-\\d{2}"))
                throw new IllegalArgumentException("Invalid release date");
            if (release.id.endsWith("-preview") && !release.date.isEmpty())
                throw new IllegalArgumentException("An unreleased preview cannot have a publication date");
        }
        return releases;
    }

    public static List<ApdHistory.Entry> entries(String data) {
        List<ApdHistory.Entry> entries = ApdHistory.entries(data);
        if (entries.isEmpty()) throw new IllegalArgumentException("Missing player release notes");
        Set<String> descriptions = new HashSet<>();
        for (ApdHistory.Entry entry : entries) {
            if (!CATEGORIES.contains(entry.category) || INTERNAL.matcher(entry.title).find()
                    || INTERNAL.matcher(entry.text).find() || entry.sprite.equals("V061_PAGE")
                    || entry.text.trim().split("\\s+").length > 100
                    || !descriptions.add(entry.text))
                throw new IllegalArgumentException("Invalid player release note");
        }
        return entries;
    }

    /** Internal IDs remain unchanged; even the optional technical index uses neutral labels. */
    public static String technicalLabel(ApdHistory.Build build) {
        return build.label.replaceFirst("(?i)^Codex TEST\\s*", "APD Test ");
    }
}
