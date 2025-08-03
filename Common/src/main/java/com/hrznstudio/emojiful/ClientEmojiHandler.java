package com.hrznstudio.emojiful;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.gson.JsonElement;
import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.api.EmojiCategory;
import com.hrznstudio.emojiful.api.EmojiFromTwitmoji;
import com.hrznstudio.emojiful.platform.Services;
import com.hrznstudio.emojiful.util.ProfanityFilter;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientEmojiHandler {
    public static final List<EmojiCategory> CATEGORIES = new ArrayList<>();
    public static List<String> ALL_EMOJIS = new ArrayList<>();
    public static HashMap<EmojiCategory, List<Emoji[]>> SORTED_EMOJIS_FOR_SELECTION = new LinkedHashMap<>();
    public static List<Emoji> EMOJI_WITH_TEXTS = new ArrayList<>();
    public static int lineAmount;

    public static void setup() {
        new Thread(() -> {
            Constants.LOG.info("[EMOJI SETUP] Starting to load emojis");
            preInitEmojis();
            indexEmojis();
            Constants.LOG.info("[EMOJI SETUP] Loaded {} emojis total", Constants.EMOJI_LIST.size());
            Constants.LOG.info("[EMOJI SETUP] Categories loaded: {}", CATEGORIES.size());
            for (EmojiCategory category : CATEGORIES) {
                int emojiCount = Constants.EMOJI_MAP.getOrDefault(category.name(), new ArrayList<>()).size();
                Constants.LOG.info("[EMOJI SETUP]   Category '{}': {} emojis (worldBased: {})", 
                    category.name(), emojiCount, category.worldBased());
            }
        }).start();
    }

    public static void indexEmojis() {
        ALL_EMOJIS = Constants.EMOJI_LIST.stream()
                .map(emoji -> emoji.strings)
                .flatMap(Collection::stream)
                .toList();
        SORTED_EMOJIS_FOR_SELECTION = new LinkedHashMap<>();
        for (EmojiCategory category : CATEGORIES) {
            ++lineAmount;
            Emoji[] array = new Emoji[9];
            int i = 0;
            for (Emoji emoji : Constants.EMOJI_MAP.getOrDefault(category.name(), new ArrayList<>())) {
                array[i] = emoji;
                ++i;
                if (i >= array.length) {
                    SORTED_EMOJIS_FOR_SELECTION.computeIfAbsent(category, s -> new ArrayList<>()).add(array);
                    array = new Emoji[9];
                    i = 0;
                    ++lineAmount;
                }
            }
            if (i > 0) {
                SORTED_EMOJIS_FOR_SELECTION.computeIfAbsent(category, s -> new ArrayList<>()).add(array);
                ++lineAmount;
            }
        }
        Constants.LOG.info("Indexed emojis");
    }

    private static void preInitEmojis() {
        Constants.LOG.info("[EMOJI SETUP] Config - loadCustom: {}, loadTwemoji: {}", 
            Services.CONFIG.loadCustom(), Services.CONFIG.loadTwemoji());
        
        if (Services.CONFIG.loadCustom()) {
            Constants.LOG.info("[EMOJI SETUP] Loading custom emojis...");
            loadCustomEmojis();
        } else {
            Constants.LOG.info("[EMOJI SETUP] Custom emoji loading is disabled");
        }
        
        //loadGithubEmojis();
        if (Services.CONFIG.loadTwemoji()){
            Constants.LOG.info("[EMOJI SETUP] Loading Twemoji emojis...");
            CATEGORIES.addAll(Stream.of("Smileys & Emotion", "Animals & Nature", "Food & Drink", "Activities", "Travel & Places", "Objects", "Symbols", "Flags")
                    .map(s -> new EmojiCategory(s, false))
                    .toList());
            loadTwemojis();
        } else {
            Constants.LOG.info("[EMOJI SETUP] Twemoji loading is disabled");
        }
        
        if (Services.CONFIG.getProfanityFilter()) ProfanityFilter.loadConfigs();
        Constants.LOG.info("[EMOJI SETUP] Pre-initialization completed");
    }

    private static void loadCustomEmojis() {
        Constants.LOG.info("Loading custom emojis");
        try {
            YamlReader reader = new YamlReader(new StringReader(CommonClass.readStringFromURL("https://raw.githubusercontent.com/InnovativeOnlineIndustries/emojiful-assets/1.20-plus/Categories.yml")));
            ArrayList<String> categories = (ArrayList<String>) reader.read();
            for (String category : categories) {
                CATEGORIES.add(new EmojiCategory(category.replace(".yml", ""), false));
                List<Emoji> emojis = CommonClass.readCategory(category);
                emojis.forEach(emoji -> emoji.location = CommonClass.cleanURL(emoji.location));
                Constants.EMOJI_LIST.addAll(emojis);
                Constants.EMOJI_MAP.put(category.replace(".yml", ""), emojis);
            }
        } catch (Exception e) {
            Constants.error = true;
            Constants.LOG.error("An exception was caught whilst loading custom emojis", e);
        }
    }

    public static void loadTwemojis() {
        Constants.LOG.info("[EMOJI SETUP] Loading Twemoji emojis from external source");
        try {
            JsonElement jsonData = CommonClass.readJsonFromUrl("https://raw.githubusercontent.com/iamcal/emoji-data/master/emoji.json");
            if (jsonData == null || !jsonData.isJsonArray()) {
                Constants.LOG.error("[EMOJI SETUP] Failed to load Twemoji data - invalid JSON response");
                return;
            }

            for (JsonElement element : jsonData.getAsJsonArray()) {
                if (element.getAsJsonObject().get("has_img_twitter").getAsBoolean()) {
                    EmojiFromTwitmoji emoji = new EmojiFromTwitmoji();
                    emoji.name = "twemojis_" + element.getAsJsonObject().get("short_name").getAsString();
                    emoji.location = element.getAsJsonObject().get("image").getAsString();
                    emoji.sort = element.getAsJsonObject().get("sort_order").getAsInt();
                    element.getAsJsonObject().get("short_names").getAsJsonArray().forEach(jsonElement -> emoji.strings.add(":" + jsonElement.getAsString() + ":"));
                    if (emoji.strings.contains(":face_with_symbols_on_mouth:")) {
                        emoji.strings.add(":swear:");
                    }
                    if (!element.getAsJsonObject().get("texts").isJsonNull()) {
                        element.getAsJsonObject().get("texts").getAsJsonArray().forEach(jsonElement -> emoji.texts.add(jsonElement.getAsString()));
                    }
                    Constants.EMOJI_MAP.computeIfAbsent(element.getAsJsonObject().get("category").getAsString(), s -> new ArrayList<>()).add(emoji);
                    Constants.EMOJI_LIST.add(emoji);
                    if (!emoji.texts.isEmpty()) {
                        ClientEmojiHandler.EMOJI_WITH_TEXTS.add(emoji);
                    }
                }
            }
            ClientEmojiHandler.EMOJI_WITH_TEXTS.sort(Comparator.comparingInt(o -> o.sort));
            Constants.EMOJI_MAP.values().forEach(emojis -> emojis.sort(Comparator.comparingInt(o -> o.sort)));
        } catch (Exception e) {
            Constants.error = true;
            Constants.LOG.error("[EMOJI SETUP] Error loading Twemoji emojis", e);
        }
    }


}

