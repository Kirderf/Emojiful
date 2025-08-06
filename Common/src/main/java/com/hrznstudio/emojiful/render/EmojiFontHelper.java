package com.hrznstudio.emojiful.render;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.hrznstudio.emojiful.Constants;
import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.platform.Services;
import com.hrznstudio.emojiful.util.EmojiUtil;
import io.netty.util.internal.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiFontHelper {

    public static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, 0.03F);
    public static LoadingCache<String, Pair<String, HashMap<Integer, ? extends Emoji>>> RECENT_STRINGS = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofSeconds(60)).build(new CacheLoader<>() {
        @Override
        public Pair<String, HashMap<Integer, ? extends Emoji>> load(String key) {
            return getEmojiFormattedString(key);
        }
    });
    public static String SCAPED_STRING = "\\\\\\\\";

    public EmojiFontHelper() {

    }

    public static Pair<String, HashMap<Integer, ? extends Emoji>> getEmojiFormattedString(String text) {
        HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
        if (Services.CONFIG.renderEmoji() && !StringUtil.isNullOrEmpty(text)) {
            String unformattedText = ChatFormatting.stripFormatting(text);
            if (StringUtil.isNullOrEmpty(unformattedText))
                return Pair.of(text, emojis);
            if (text.startsWith(SCAPED_STRING)){
                return Pair.of(text, emojis);
            }
            for (Emoji emoji : Constants.EMOJI_LIST) {
                Pattern pattern = emoji.getRegex();
                Matcher matcher = pattern.matcher(unformattedText);
                while (matcher.find()) {
                    if (!matcher.group().isEmpty()) {
                        String emojiText = matcher.group();
                        int index = text.indexOf(emojiText);
                        emojis.put(index, emoji);
                        HashMap<Integer, Emoji> clean = new LinkedHashMap<>();
                        for (Integer integer : new ArrayList<>(emojis.keySet())) {
                            if (integer > index) {
                                Emoji e = emojis.get(integer);
                                emojis.remove(integer);
                                clean.put(integer - emojiText.length() + 1, e);
                            }
                        }
                        emojis.putAll(clean);
                        unformattedText = unformattedText.replaceFirst(Pattern.quote(emojiText), "☃");
                        text = text.replaceFirst("(?i)" + Pattern.quote(emojiText), "☃");
                    }
                }
            }
        }
        return Pair.of(text, emojis);
    }

    public static class EmojiCharacterRenderer implements FormattedCharSink {
        final MultiBufferSource buffer;
        final GuiGraphics guiGraphics;
        private final boolean dropShadow;
        private final boolean seeThrough;
        private final int packedLight;
        private float x;
        private final float y;
        private final HashMap<Integer, ? extends Emoji> emojis;
        @Nullable
        private List<BakedGlyph.Effect> effects;

        public EmojiCharacterRenderer(HashMap<Integer, ? extends Emoji> emojis, MultiBufferSource buffer, GuiGraphics guiGraphics, float x, float y, boolean dropShadow, boolean seeThrough, int packedLight) {
            this.buffer = buffer;
            this.guiGraphics = guiGraphics;
            this.emojis = emojis;
            this.x = x;
            this.y = y;
            this.dropShadow = dropShadow;
            this.seeThrough = seeThrough;
            this.packedLight = packedLight;
        }

        private void addEffect(BakedGlyph.Effect effect) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }
            this.effects.add(effect);
        }

        @Override
        public boolean accept(int pos, Style style, int charInt) {
            Font font = Minecraft.getInstance().font;

            if (Services.CONFIG.renderEmoji() && this.emojis.containsKey(pos)) {
                var emoji = this.emojis.get(pos);
                Constants.LOG.debug("[EMOJI RENDER] Found emoji at position {}: {}", pos, emoji.name);
                
                if (!this.dropShadow) {
                    Constants.LOG.debug("[EMOJI RENDER] Rendering emoji: {} at ({}, {})", emoji.name, this.x, this.y);
                    try {
                        EmojiUtil.renderEmoji(emoji, this.x, this.y, this.guiGraphics);
                        this.x += 10;
                    } catch (Exception e) {
                        Constants.LOG.error("[EMOJI RENDER] Failed to render emoji texture: {}", e.getMessage(), e);
                    }
                } else {
                    Constants.LOG.debug("[EMOJI RENDER] Skipping emoji render (shadow pass): {}", emoji.name);
                }
            } else {
                if (charInt != '☃') {
                    guiGraphics.drawString(font, String.valueOf((char) charInt), (int) this.x, (int) this.y, -1);
                    this.x += font.width(String.valueOf((char) charInt));
                }
            }

            return true;
        }

        public float finish(int backgroundColor, float originalX) {
            // Handle background color if specified
            if (backgroundColor != 0) {
                this.addEffect(new BakedGlyph.Effect(
                        originalX - 1.0F, this.y - 1.0F, this.x + 1.0F, this.y + 9.0F,
                        -0.01F, backgroundColor
                ));
            }

            // Render all accumulated effects
            if (this.effects != null) {
                FontSet fontSet = Minecraft.getInstance().font.getFontSet(Style.DEFAULT_FONT);
                BakedGlyph whiteGlyph = fontSet.whiteGlyph();

                Font.GlyphVisitor glyphVisitor = Font.GlyphVisitor.forMultiBufferSource(
                        buffer, new Matrix4f(), seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, packedLight
                );

                for (BakedGlyph.Effect effect : this.effects) {
                    glyphVisitor.acceptEffect(whiteGlyph, effect);
                }
            }

            return this.x;
        }
    }
}