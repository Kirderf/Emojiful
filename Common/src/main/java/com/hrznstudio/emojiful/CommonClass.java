package com.hrznstudio.emojiful;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.api.EmojiCategory;
import com.hrznstudio.emojiful.api.EmojiFromGithub;
import com.hrznstudio.emojiful.platform.Services;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class CommonClass {


    public static String readStringFromURL(String requestURL) {
        try {
            try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),
                    StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static JsonElement readJsonFromUrl(String url) {
        String jsonText = readStringFromURL(url);
        JsonElement json = new JsonParser().parse(jsonText);
        return json;
    }

    //public static void main(String[] s) throws IOException {
    //    ClientEmojiHandler.loadTwemojis();
    //    var folder = new File("raw_assets");
    //    HashMap<String, List<YamlEmoji>> emojis = new HashMap<>();
    //    for (String string : Constants.EMOJI_MAP.keySet()) {
    //        for (Emoji emoji : Constants.EMOJI_MAP.get(string)) {
    //            var found = false;
    //            var emoji_name = emoji.name.replace("-", "_");
    //            for (File file : folder.listFiles()) {
    //                var filename = file.getName().replace(".png", "").replace("-", "_");
    //                if (!found){
    //                    if (emoji_name.equals(filename) ||
    //                            emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").equals(filename)) ||
    //                            emoji_name.equals(filename.replace("woman", "female").replace("man", "male")) ||
    //                            emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").equals(filename.replace("woman", "female").replace("man", "male")))
    //                    ){
    //                        found = true;
    //                        copy(string, file, emojis, emoji);
    //                        break;
    //                    }
    //                }
    //            }
    //            if (!found){
    //                System.out.println(emoji.name + "?");
    //            }
    //        }
    //    }
    //    createYML(emojis);
    //    if (false){
    //        emojis = new HashMap<>();
    //        for (File file : folder.listFiles()) {
    //            if (file.getName().contains("_tone")) continue;;
    //            var filename = file.getName().replace(".png", "").replace("-", "_");
    //            var fileNameSplit = filename.split("_");
    //            var found = false;
    //            for (String string : Constants.EMOJI_MAP.keySet()) {
    //                if (!found){
    //                    for (Emoji emoji : Constants.EMOJI_MAP.get(string)) {
    //                        if (emoji.name.replace("-", "_").equals(filename) ||
    //                                emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").equals(filename)) ||
    //                                emoji.name.replace("-", "_").equals(filename.replace("woman", "female").replace("man", "male")) ||
    //                                emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").equals(filename.replace("woman", "female").replace("man", "male")))
    //                        ){
    //                            found = true;
    //                            System.out.println(emoji.name + "=" + file.getName());
    //                            copy(string, file, emojis, emoji);
    //                            break;
    //                        }
    //                    }
    //                }
    //            }
    //            for (String string : Constants.EMOJI_MAP.keySet()) {
    //                if (!found){
    //                    for (Emoji emoji : Constants.EMOJI_MAP.get(string)) {
    //                        if (emoji.name.replace("-", "_").contains(filename) ||
    //                                emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").contains(filename)) ||
    //                                emoji.name.replace("-", "_").contains(filename.replace("woman", "female").replace("man", "male")) ||
    //                                emoji.strings.stream().anyMatch(s1 -> s1.replaceAll(":", "").replace("-", "_").contains(filename.replace("woman", "female").replace("man", "male")))
    //                        ){
    //                            found = true;
    //                            System.out.println(emoji.name + "->" + file.getName());
    //                            copy(string, file, emojis, emoji);
    //                            break;
    //                        }
    //                    }
    //                }
    //            /*if (!found){
    //                for (Emoji emoji : Constants.EMOJI_MAP.get(string)) {
    //                    if (Arrays.stream(fileNameSplit).anyMatch(s1 -> emoji.name.contains(s1)) || Arrays.stream(fileNameSplit).anyMatch(s1 -> emoji.texts.stream().anyMatch(s2 -> s2.contains(s1)))){
    //                        found = true;
    //                        copy(string, file);
    //                        break;
    //                    }
    //                }
    //            }*/
    //            }
    //            if (!found){
    //                System.out.println();
    //                //copy("Missing", file, new HashMap<>(), emoji);
    //            }
    //        }
    //        createYML(emojis);
    //    }
//
    //}
//
    //private static void createYML(HashMap<String, List<YamlEmoji>> emojis) throws IOException {
    //    for (String category : emojis.keySet()) {
    //        var yamlFile = new File(category + ".yml");
    //        if (!yamlFile.exists()) yamlFile.createNewFile();
    //        YamlWriter writer = new YamlWriter(new FileWriter(category + ".yml"));
    //        writer.write(emojis.get(category));
    //        writer.close();
    //    }
    //}
//
    //public static void copy(String category, File sourceFile, HashMap<String, List<YamlEmoji>> emojis, Emoji emoji){
    //    var folderTo = new File("assets/" + category);
    //    if (!folderTo.exists()) folderTo.mkdir();
    //    try {
    //        var name = sourceFile.getName().replace(".png", "").replace("-", "_");
    //        emojis.computeIfAbsent(category, s -> new ArrayList<>()).add(new YamlEmoji(name, emoji.strings, "assets/" + category + "/" + sourceFile.getName()));
    //        FileUtils.copyFile(sourceFile, new File(folderTo, sourceFile.getName()));
    //    } catch (IOException e) {
    //        throw new RuntimeException(e);
    //    }
    //}

    //public static class YamlEmoji {
    //    public String name;
    //    public List<String> strings;
    //    public String location;
//
    //    public YamlEmoji() {
    //    }
//
    //    public YamlEmoji(String name, List<String> strings, String location) {
    //        this.name = name;
    //        this.strings = strings;
    //        this.location = location;
    //    }
    //}

    public static List<Emoji> readCategory(String cat) throws YamlException {
        YamlReader categoryReader = new YamlReader(new StringReader(readStringFromURL("https://raw.githubusercontent.com/InnovativeOnlineIndustries/emojiful-assets/1.20-plus/" + cleanURL(cat))));
        return Lists.newArrayList(categoryReader.read(Emoji[].class));
    }

    public static String cleanURL(String string){
        return string.replaceAll(" ", "%20").replaceAll("&", "%26");
    }


    public static boolean shouldKeyBeIgnored(int keyCode){
        return keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT;
    }

    public static <T> void updateEmojisFromNetwork(List<T> networkEmojis) {
        Constants.LOG.info("Updating emojis from network data, found {} emojis", networkEmojis.size());
        // Clear existing world-based emojis
        ClientEmojiHandler.CATEGORIES.removeIf(EmojiCategory::worldBased);
        Constants.EMOJI_LIST.removeIf(Emoji::worldBased);
        Constants.EMOJI_MAP.entrySet().removeIf(entry -> entry.getValue().removeIf(Emoji::worldBased));

        // Convert network data to emojis
        for (T emojiDataObj : networkEmojis) {
            // Use reflection or interface to extract data
            String name = Services.PLATFORM.getEmojiName(emojiDataObj);
            String category = Services.PLATFORM.getEmojiCategory(emojiDataObj);
            String url = Services.PLATFORM.getEmojiUrl(emojiDataObj);

            EmojiFromGithub emoji = new EmojiFromGithub();
            emoji.name = name;
            emoji.strings = new ArrayList<>();
            emoji.strings.add(":" + name + ":");
            emoji.location = name;
            emoji.url = url;
            emoji.worldBased = true;

            Constants.EMOJI_MAP.computeIfAbsent(category, s -> new ArrayList<>()).add(emoji);
            Constants.EMOJI_LIST.add(emoji);

            if (ClientEmojiHandler.CATEGORIES.stream().noneMatch(cat -> cat.name().equalsIgnoreCase(category.toLowerCase()))) {
                ClientEmojiHandler.CATEGORIES.addFirst(new EmojiCategory(category, true));
            }
        }

        ClientEmojiHandler.indexEmojis();
    }
}