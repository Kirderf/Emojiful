package com.hrznstudio.emojiful.api;

import com.hrznstudio.emojiful.Constants;
import com.hrznstudio.emojiful.platform.Services;
import com.hrznstudio.emojiful.util.EmojiUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class EmojiFromTwitmoji extends Emoji {

    @Override
    public String getUrl() {
        return "https://raw.githubusercontent.com/iamcal/emoji-data/master/img-twitter-64/" + location;
    }

    @Override
    public void checkLoad() {
        if (imageThread == null && !finishedLoading) {
            Constants.LOG.info("[EMOJI LOAD] Starting to load image for emoji: {}", name);
            this.loadImage();
        } else if (!loadedTextures) {
            loadedTextures = true;
        }
    }

    @Override
    protected void loadImage() {
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
                    NativeImage nativeImage = EmojiUtil.convertToNativeImage(ImageIO.read(cache));
                    Constants.LOG.info("[EMOJI LOAD] Successfully converted to NativeImage for {}: {}x{}",
                            name, nativeImage.getWidth(), nativeImage.getHeight());

                    // Schedule texture operations on the main render thread
                    this.loadDynamicTexture(nativeImage);
                } catch (IOException e) {
                    Constants.LOG.error("[EMOJI LOAD] Error loading image for {}", name, e);
                    // Set error state
                    frames.clear();
                    frames.add(error_texture);
                    this.finishedLoading = true;
                }
            }
        } else {
            Constants.LOG.info("[EMOJI LOAD] Cache does not exist for {}, checking if download thread exists: {}", name, this.imageThread != null);
            if (this.imageThread == null) {
                Constants.LOG.info("[EMOJI LOAD] Starting download for emoji: {} from URL: {}", name, getUrl());
                loadTextureFromServer();
            }
        }
    }

    @Override
    protected void loadTextureFromServer() {
        this.imageThread = new Thread("Emojiful Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
            @Override
            public void run() {
                HttpURLConnection httpurlconnection = null;
                try {
                    httpurlconnection = (HttpURLConnection) (new URL(getUrl())).openConnection(Minecraft.getInstance().getProxy());
                    httpurlconnection.setDoInput(true);
                    httpurlconnection.setDoOutput(false);
                    httpurlconnection.connect();
                    if (httpurlconnection.getResponseCode() / 100 == 2) {
                        httpurlconnection.getContentLength();
                        File cacheFile = getCache();
                        if (cacheFile != null) {
                            FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), cacheFile);
                        }
                        EmojiFromTwitmoji.this.finishedLoading = true;
                        Constants.LOG.info("[EMOJI DOWNLOAD] Download complete for {}, calling loadImage()", name);
                        loadImage();
                    } else {
                        Constants.LOG.error("[EMOJI DOWNLOAD] Failed to download {}: HTTP {}", name, httpurlconnection.getResponseCode());
                        EmojiFromTwitmoji.this.frames = new ArrayList<>();
                        EmojiFromTwitmoji.this.frames.add(noSignal_texture);
                        EmojiFromTwitmoji.this.deleteOldTexture = true;
                        EmojiFromTwitmoji.this.finishedLoading = true;
                        Constants.LOG.info("[EMOJI DOWNLOAD] Set error state for {}", name);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    EmojiFromTwitmoji.this.deleteOldTexture = true;

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
