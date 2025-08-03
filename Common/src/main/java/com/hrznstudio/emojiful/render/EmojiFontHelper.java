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
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
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
    public static LoadingCache<String, Pair<String, HashMap<Integer, Emoji>>> RECENT_STRINGS = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofSeconds(60)).build(new CacheLoader<>() {
        @Override
        public Pair<String, HashMap<Integer, Emoji>> load(String key) {
            return getEmojiFormattedString(key);
        }
    });
    public static String SCAPED_STRING = "\\\\\\\\";

    public EmojiFontHelper() {

    }

    public static Pair<String, HashMap<Integer, Emoji>> getEmojiFormattedString(String text) {
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

    public static class CharacterProcessor implements FormattedCharSequence {

        public final int pos;
        public final Style style;
        public final int character;

        public CharacterProcessor(int pos, Style style, int character) {
            this.pos = pos;
            this.style = style;
            this.character = character;
        }

        @Override
        public boolean accept(FormattedCharSink iCharacterConsumer) {
            return iCharacterConsumer.accept(pos, style, character);
        }
    }

    public static class EmojiCharacterRenderer implements FormattedCharSink {
        final MultiBufferSource buffer;
        final GuiGraphics guiGraphics;
        private final boolean dropShadow;
        private final boolean seeThrough;
        private final int packedLight;
        private float x;
        private final float y;
        private final HashMap<Integer, Emoji> emojis;
        @Nullable
        private List<BakedGlyph.Effect> effects;

        public EmojiCharacterRenderer(HashMap<Integer, Emoji> emojis, MultiBufferSource buffer, GuiGraphics guiGraphics, float x, float y, boolean dropShadow, boolean seeThrough, int packedLight) {
            this.buffer = buffer;
            this.guiGraphics = guiGraphics;
            this.emojis = emojis;
            this.x = x;
            this.y = y;
            this.dropShadow = dropShadow;
            this.seeThrough = seeThrough;
            this.packedLight = packedLight;
            
            Constants.LOG.info("[EMOJI RENDER] EmojiCharacterRenderer created with {} emojis at positions: {}", 
                emojis.size(), emojis.keySet());
            for (var entry : emojis.entrySet()) {
                Constants.LOG.info("[EMOJI RENDER]   Position {}: {}", entry.getKey(), entry.getValue().name);
            }
        }

        private void addEffect(BakedGlyph.Effect effect) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }
            this.effects.add(effect);
        }

        @Override
        public boolean accept(int pos, Style style, int charInt) {
            // Check if there's an emoji at this position
            if (Services.CONFIG.renderEmoji() && this.emojis.containsKey(pos)) {
                Emoji emoji = this.emojis.get(pos);
                Constants.LOG.debug("[EMOJI RENDER] Found emoji at position {}: {}", pos, emoji.name);
                
                if (!this.dropShadow) {
                    Constants.LOG.debug("[EMOJI RENDER] Rendering emoji: {} at ({}, {})", emoji.name, this.x, this.y);
                    try {
                        EmojiUtil.renderEmoji(emoji, this.x, this.y, this.guiGraphics);
                    } catch (Exception e) {
                        Constants.LOG.error("[EMOJI RENDER] Failed to render emoji texture: {}", e.getMessage(), e);
                    }
                } else {
                    Constants.LOG.debug("[EMOJI RENDER] Skipping emoji render (shadow pass): {}", emoji.name);
                }
            }
            
            // Always advance x position for all characters (including spaces that replaced emojis)
            Font font = Minecraft.getInstance().font;
            this.x += font.width(String.valueOf((char) charInt));
            
            return true;
        }

        private int getTextColor(Style style, int defaultColor) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                int alpha = ARGB.alpha(defaultColor);
                int rgb = textColor.getValue();
                return ARGB.color(alpha, rgb);
            }
            return defaultColor;
        }

        private int getShadowColor(Style style, int textColor) {
            Integer shadowColor = style.getShadowColor();
            if (shadowColor != null) {
                float textAlpha = ARGB.alphaFloat(textColor);
                float shadowAlpha = ARGB.alphaFloat(shadowColor);
                return textAlpha != 1.0F ? ARGB.color(ARGB.as8BitChannel(textAlpha * shadowAlpha), shadowColor) : shadowColor;
            } else {
                return this.dropShadow ? ARGB.scaleRGB(textColor, 0.25F) : 0;
            }
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