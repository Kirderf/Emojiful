package com.hrznstudio.emojiful.api;

import com.hrznstudio.emojiful.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class EmojiFromTwitmoji extends Emoji {

    public SimpleTexture img;
    public ResourceLocation resourceLocation = loading_texture;

    @Override
    public void checkLoad() {
        if (img != null)
            return;

        img = new DownloadImageData(new File("emojiful/cache/" + name + "-" + version), "https://raw.githubusercontent.com/iamcal/emoji-data/master/img-twitter-64/" + location, loading_texture);
        resourceLocation = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "texures/emoji/" + location.toLowerCase() + "_" + version);
        Minecraft.getInstance().getTextureManager().register(resourceLocation, img);
    }

    public ResourceLocation getResourceLocationForBinding() {
        checkLoad();
        if (deleteOldTexture) {
            img.close();
            deleteOldTexture = false;
        }
        return resourceLocation;
    }

    @Override
    public boolean test(String s) {
        for (String text : strings)
            if (s.equalsIgnoreCase(text))
                return true;
        return false;
    }

    public class DownloadImageData extends SimpleTexture {
        private final File cacheFile;
        private final String imageUrl;
        private Thread imageThread;

        public DownloadImageData(File cacheFileIn, String imageUrlIn, ResourceLocation textureResourceLocation) {
            super(textureResourceLocation);
            this.cacheFile = cacheFileIn;
            this.imageUrl = imageUrlIn;
        }

        private void setImage(NativeImage nativeImageIn) {
            Minecraft.getInstance().execute(() -> {
                if (!RenderSystem.isOnRenderThread()) {
                    Minecraft.getInstance().execute(() -> {
                        if (nativeImageIn == null) {
                            EmojiFromTwitmoji.this.resourceLocation = error_texture;
                            EmojiFromTwitmoji.this.deleteOldTexture = true;
                            return;
                        }
                        this.upload(nativeImageIn);
                    });
                } else {
                    this.upload(nativeImageIn);
                }

            });
        }

        private void upload(NativeImage imageIn) {
            DynamicTexture dynamicTexture = new DynamicTexture(String::new, imageIn);
            dynamicTexture.upload();
        }

        @Nullable
        private NativeImage loadTexture(InputStream inputStreamIn) {
            NativeImage nativeimage = null;

            try {
                nativeimage = NativeImage.read(inputStreamIn);
            } catch (IOException ioexception) {
                Constants.LOG.warn("Error while loading the skin texture", ioexception);
            }

            return nativeimage;
        }

        @Override
        public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            if (this.imageThread == null) {
                if (this.cacheFile != null && this.cacheFile.isFile()) {
                    try {
                        FileInputStream fileinputstream = new FileInputStream(this.cacheFile);
                        setImage(this.loadTexture(fileinputstream));
                    } catch (IOException ioexception) {
                        this.loadTextureFromServer();
                    }
                } else {
                    this.loadTextureFromServer();
                }
            }
            return TextureContents.load(resourceManager, img.resourceId());
        }

        protected void loadTextureFromServer() {
            this.imageThread = new Thread("Emojiful Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
                @Override
                public void run() {
                    HttpURLConnection httpurlconnection = null;

                    try {
                        httpurlconnection = (HttpURLConnection) (new URL(DownloadImageData.this.imageUrl)).openConnection(Minecraft.getInstance().getProxy());
                        httpurlconnection.setDoInput(true);
                        httpurlconnection.setDoOutput(false);
                        httpurlconnection.connect();
                        if (httpurlconnection.getResponseCode() / 100 == 2) {
                            int contentLength = httpurlconnection.getContentLength();
                            InputStream inputStream;

                            if (DownloadImageData.this.cacheFile != null) {
                                FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), DownloadImageData.this.cacheFile);
                                inputStream = new FileInputStream(DownloadImageData.this.cacheFile);
                            } else {
                                inputStream = httpurlconnection.getInputStream();
                            }

                            DownloadImageData.this.setImage(DownloadImageData.this.loadTexture(inputStream));
                        } else {
                            EmojiFromTwitmoji.this.resourceLocation = noSignal_texture;
                            EmojiFromTwitmoji.this.deleteOldTexture = true;
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        EmojiFromTwitmoji.this.resourceLocation = error_texture;
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
}
