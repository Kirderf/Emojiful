package com.hrznstudio.emojiful.util;

import com.hrznstudio.emojiful.Constants;
import com.hrznstudio.emojiful.api.Emoji;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EmojiUtil extends RenderStateShard {

    private EmojiUtil(String string, Runnable runnable, Runnable runnable2) {
        super(string, runnable, runnable2);
    }

    public static NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), true);
        for (int y = 0; y < bufferedImage.getHeight(); ++y) {
            for (int x = 0; x < bufferedImage.getWidth(); ++x) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return nativeImage;
    }


    public static <T extends Emoji> void renderEmoji(T emoji, float x, float y, GuiGraphics buffer) {
        try {

            int size = 10;
            Constants.LOG.info("Getting emoji texture for: {}", emoji.name);
            Constants.LOG.info("Emoji class: {}", emoji.getClass().getName());
            ResourceLocation texture = emoji.getResourceLocationForBinding();

            // Debug logging with proper logger
            //Constants.LOG.info("[EMOJI UTIL] Rendering emoji: {} at ({}, {})", emoji.name, x, y);
            //Constants.LOG.info("[EMOJI UTIL] Texture: {}", texture);
            //Constants.LOG.info("[EMOJI UTIL] Finished loading: {}", emoji.finishedLoading);
            //Constants.LOG.info("[EMOJI UTIL] Frames count: {}", emoji.frames.size());
            //Constants.LOG.info("[EMOJI UTIL] Emoji location: {}", emoji.location);
            //Constants.LOG.info("[EMOJI UTIL] Emoji version: {}", emoji.version);

            if (texture != null && emoji.finishedLoading) {
                Constants.LOG.info("[EMOJI UTIL] Attempting to render emoji texture, x: {}, y: {}, size: {}", x, y, size);
                buffer.blit(texture, size + (int) x+1, size + (int) y-1, (int) x, (int) (y-1), 1, 0, 0, -1);

            } else {
                Constants.LOG.warn("[EMOJI UTIL] Emoji texture is null or not finished loading: {}", emoji.name);
            }
        } catch (Exception e) {
            Constants.LOG.error("[EMOJI UTIL] Error rendering emoji: {}", e.getMessage(), e);
        }
    }

    public static String cleanStringForRegex(String string) {
        return string.replaceAll("\\)", "\\\\)").replaceAll("\\(", "\\\\(").replaceAll("\\|", "\\\\|").replaceAll("\\*", "\\\\*");
    }

    public static List<Pair<BufferedImage, Integer>> splitGif(File file) throws IOException {
        List<Pair<BufferedImage, Integer>> images = new ArrayList<>();
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(ImageIO.createImageInputStream(new FileInputStream(file)), false);
        IIOMetadata metadata = reader.getImageMetadata(0);
        String metaFormatName = metadata.getNativeMetadataFormatName();
        for (int i = 0; i < reader.getNumImages(true); i++) {
            int frameLength = 1;
            BufferedImage image = reader.read(i);
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            newImage.getGraphics().drawImage(image, 0, 0, null);
            IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree(metaFormatName);
            // Find GraphicControlExtension node
            int nNodes = root.getLength();
            for (int j = 0; j < nNodes; j++) {
                Node node = root.item(j);
                if (node.getNodeName().equalsIgnoreCase("GraphicControlExtension")) {
                    // Get delay value
                    frameLength = Integer.parseInt(((IIOMetadataNode) node).getAttribute("delayTime"));
                    // Check if delay is bugged
                    break;
                }
            }
            images.add(Pair.of(newImage, frameLength));
        }
        return images;
    }

}
