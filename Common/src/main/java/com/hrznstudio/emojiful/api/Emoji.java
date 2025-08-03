package com.hrznstudio.emojiful.api;

import com.hrznstudio.emojiful.Constants;
import com.hrznstudio.emojiful.platform.Services;
import com.hrznstudio.emojiful.util.EmojiUtil;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Emoji implements Predicate<String> {
    public static final ResourceLocation loading_texture = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/26a0.png");
    public static final ResourceLocation noSignal_texture = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/26d4.png");
    public static final ResourceLocation error_texture = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/26d4.png");

    public static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    public String name;
    public List<String> strings = new ArrayList<>();
    public List<String> texts = new ArrayList<>();
    public String location;
    public int version = 1;
    public int sort = 0;
    public boolean worldBased = false;
    public boolean deleteOldTexture;
    public List<DynamicTexture> img = new ArrayList<>();
    public List<ResourceLocation> frames = new ArrayList<>();
    public boolean finishedLoading = false;
    public boolean loadedTextures = false;
    private String shortString;
    private String regex;
    private Pattern regexPattern;
    private Thread imageThread;
    private Thread gifLoaderThread;

    public void checkLoad() {
        Constants.LOG.info("[EMOJI LOAD] checkLoad() called for emoji: {} - finishedLoading: {}, imageThread: {}, frames.size: {}", 
            name, finishedLoading, imageThread != null, frames.size());
        if (imageThread == null && !finishedLoading) {
            Constants.LOG.info("[EMOJI LOAD] Starting to load image for emoji: {}", name);
            loadImage();
        } else if (!loadedTextures) {
            loadedTextures = true;
        }

    }

    public ResourceLocation getResourceLocationForBinding() {
        checkLoad();
        if (deleteOldTexture) {
            for (DynamicTexture texture : img) {
                if (texture != null) {
                    texture.close();
                }
            }
            deleteOldTexture = false;
        }
        ResourceLocation result = finishedLoading && !frames.isEmpty() ? frames.get((int) (System.currentTimeMillis() / 10D % frames.size())) : loading_texture;
        Constants.LOG.info("[EMOJI LOAD] getResourceLocationForBinding() for {}: finishedLoading={}, frames.size={}, returning={}", 
            name, finishedLoading, frames.size(), result);
        return result;
    }

    @Override
    public boolean test(String s) {
        for (String text : strings)
            if (s.equalsIgnoreCase(text))
                return true;
        return false;
    }

    public boolean worldBased() {
        return worldBased;
    }

    public String getShorterString() {
        if (shortString != null) return shortString;
        shortString = strings.get(0);
        for (String string : strings) {
            if (string.length() < shortString.length()) {
                shortString = string;
            }
        }
        return shortString;
    }

    public Pattern getRegex() {
        if (regexPattern != null) return regexPattern;
        regexPattern = Pattern.compile(getRegexString());
        return regexPattern;
    }

    public String getRegexString() {
        if (regex != null) return regex;
        List<String> processed = new ArrayList<>();
        for (String string : strings) {
            char last = string.toLowerCase().charAt(string.length() - 1);
            String s = string;
            if (last >= 97 && last <= 122) {
                s = string + "\\b";
            }
            char first = string.toLowerCase().charAt(0);
            if (first >= 97 && first <= 122) {
                s = "\\b" + s;
            }
            processed.add(EmojiUtil.cleanStringForRegex(s));
        }
        regex = String.join("|", processed);
        return regex;
    }

    private void loadImage() {
        File cache = getCache();
        Constants.LOG.info("[EMOJI LOAD] loadImage() for emoji: {} - cache file: {}, exists: {}", 
            name, cache.getAbsolutePath(), cache.exists());
            
        if (cache.exists()) {
            Constants.LOG.info("[EMOJI LOAD] Cache exists for {}, file size: {} bytes", name, cache.length());
            if (getUrl().endsWith(".gif") && Services.CONFIG.loadGifEmojis()) {
                Constants.LOG.info("[EMOJI LOAD] Loading GIF emoji: {}", name);
                if (gifLoaderThread == null) {
                    gifLoaderThread = new Thread("Emojiful Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
                        @Override
                        public void run() {
                            try {
                                loadTextureFrames(EmojiUtil.splitGif(cache));
                            } catch (IOException e) {
                                Constants.LOG.error("[EMOJI LOAD] Error loading GIF frames for {}", name, e);
                            }
                        }
                    };
                    this.gifLoaderThread.setDaemon(true);
                    this.gifLoaderThread.start();
                }
            } else {
                Constants.LOG.info("[EMOJI LOAD] Loading static image emoji: {}", name);
                try {
                    BufferedImage bufferedImage = ImageIO.read(cache);
                    Constants.LOG.info("[EMOJI LOAD] Successfully read BufferedImage for {}: {}x{}", 
                        name, bufferedImage.getWidth(), bufferedImage.getHeight());
                    
                    NativeImage nativeImage = EmojiUtil.convertToNativeImage(bufferedImage);
                    Constants.LOG.info("[EMOJI LOAD] Successfully converted to NativeImage for {}: {}x{}", 
                        name, nativeImage.getWidth(), nativeImage.getHeight());

                    DynamicTexture texture = new DynamicTexture(() -> "emoji_texture" + name.toLowerCase(), nativeImage);

                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/emoji/" + name.toLowerCase().replaceAll("[^a-z0-9/._-]", "") + "_" + version);
                    Minecraft.getInstance().getTextureManager().register(location, texture);
                    Constants.LOG.info("[EMOJI LOAD] Successfully registered texture for {}: {}", name, location);

                    frames.clear();
                    frames.add(location);
                    img.add(texture);
                    this.finishedLoading = true;
                    Constants.LOG.info("[EMOJI LOAD] Finished loading emoji: {} - frames.size: {}", name, frames.size());
                } catch (IOException e) {
                    Constants.LOG.error("[EMOJI LOAD] Error loading image for {}", name, e);
                    // Set error state
                    frames.clear();
                    frames.add(error_texture);
                    this.finishedLoading = true;
                }
            }
        } else {
            Constants.LOG.info("[EMOJI LOAD] Cache does not exist for {}, checking if download thread exists: {}",
                name, this.imageThread != null);
            if (this.imageThread == null) {
                Constants.LOG.info("[EMOJI LOAD] Starting download for emoji: {} from URL: {}", name, getUrl());
                loadTextureFromServer();
            }
        }
    }

    public String getUrl() {
        return "https://raw.githubusercontent.com/InnovativeOnlineIndustries/emojiful-assets/1.20-plus/" + location;
    }

    public File getCache() {
        File cacheDir = new File("emojiful/cache/");
        if (!cacheDir.exists()) {
            Constants.LOG.info("[EMOJI LOAD] Cache directory doesn't exist, creating: {}", cacheDir.getAbsolutePath());
            boolean created = cacheDir.mkdirs();
            Constants.LOG.info("[EMOJI LOAD] Cache directory creation result: {}", created);
        }
        File cacheFile = new File(cacheDir, name + "-" + version);
        Constants.LOG.info("[EMOJI LOAD] Cache file path for {}: {}", name, cacheFile.getAbsolutePath());
        return cacheFile;
    }

    public void loadTextureFrames(List<Pair<BufferedImage, Integer>> framesPair) {
        Minecraft.getInstance().executeBlocking(() -> {
            int i = 0;
            for (Pair<BufferedImage, Integer> bufferedImagePair : framesPair) {
                BufferedImage bufferedImage = bufferedImagePair.getKey();
                
                // Convert BufferedImage to NativeImage
                NativeImage nativeImage = EmojiUtil.convertToNativeImage(bufferedImage);
                
                var string = "emoji_frame_" + name.toLowerCase() + "_" + i;

                DynamicTexture dynamicTexture = new DynamicTexture(() -> string, nativeImage);
                
                ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                        "textures/emoji/" + name.toLowerCase().replaceAll("[^a-z0-9/._-]", "") + "_" + version + "_frame" + i);
                Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
                img.add(dynamicTexture);
                
                // Add frame multiple times based on duration
                for (int integer = 0; integer < bufferedImagePair.getValue(); integer++) {
                    frames.add(resourceLocation);
                }
                ++i;
            }
            Emoji.this.finishedLoading = true;
        });
    }

    protected void loadTextureFromServer() {
        this.imageThread = new Thread("Emojiful Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
            @Override
            public void run() {
                HttpURLConnection httpurlconnection = null;
                try {
                    String url = getUrl();
                    Constants.LOG.info("[EMOJI DOWNLOAD] Starting download for {}: {}", name, url);
                    
                    httpurlconnection = (HttpURLConnection) (URI.create(url).toURL()).openConnection(Minecraft.getInstance().getProxy());
                    httpurlconnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                    httpurlconnection.setDoInput(true);
                    httpurlconnection.setDoOutput(false);
                    httpurlconnection.connect();

                    int responseCode = httpurlconnection.getResponseCode();
                    Constants.LOG.info("[EMOJI DOWNLOAD] Response code for {}: {}", name, responseCode);

                    if (responseCode / 100 == 2) {
                        File cacheFile = getCache();
                        Constants.LOG.info("[EMOJI DOWNLOAD] Successfully connected, downloading to: {}", cacheFile.getAbsolutePath());

                        if (getCache() != null) {
                            FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), cacheFile);
                            Constants.LOG.info("[EMOJI DOWNLOAD] Downloaded {} bytes for {}", cacheFile.length(), name);
                        }
                        Emoji.this.finishedLoading = true;
                        Constants.LOG.info("[EMOJI DOWNLOAD] Download complete for {}, calling loadImage()", name);
                        loadImage();
                    } else {
                        Constants.LOG.error("[EMOJI DOWNLOAD] Failed to download {}: HTTP {}", name, responseCode);
                        Emoji.this.frames = new ArrayList<>();
                        Emoji.this.frames.add(noSignal_texture);
                        Emoji.this.deleteOldTexture = true;
                        Emoji.this.finishedLoading = true;
                        Constants.LOG.info("[EMOJI DOWNLOAD] Set error state for {}", name);
                    }
                } catch (Exception exception) {
                    Constants.LOG.error("[EMOJI DOWNLOAD] Exception downloading {}: {}", name, exception.getMessage(), exception);
                    Emoji.this.frames = new ArrayList<>();
                    Emoji.this.frames.add(error_texture);
                    Emoji.this.deleteOldTexture = true;
                    Emoji.this.finishedLoading = true;
                    Constants.LOG.info("[EMOJI DOWNLOAD] Set error state for {} due to exception", name);
                } finally {
                    if (httpurlconnection != null) {
                        httpurlconnection.disconnect();
                    }
                }
            }
        };
        this.imageThread.setDaemon(true);
        this.imageThread.start();
    }

}
