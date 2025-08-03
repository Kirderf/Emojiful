package com.hrznstudio.emojiful.mixin;

import com.hrznstudio.emojiful.Constants;
import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.render.EmojiFontHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;


@Mixin(GuiGraphics.class)
public class GuiGraphicsRenderMixin {


    @Shadow @Final public GuiRenderState guiRenderState;

    @Shadow @Final private Matrix3x2fStack pose;

    @Shadow @Final public GuiGraphics.ScissorStack scissorStack;

    // Target GuiGraphics.drawString which is what ChatComponent uses
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void onGuiDrawStringFormatted(Font font, FormattedCharSequence reorderingProcessor, int x, int y, int color, boolean bl, CallbackInfo ci) {
        if (reorderingProcessor == null) {
            return;
        }

        // Extract the text from FormattedCharSequence
        StringBuilder builder = new StringBuilder();
        reorderingProcessor.accept((pos, style, ch) -> {
            builder.append((char) ch);
            return true;
        });
        String text = builder.toString();
        if (text.startsWith(EmojiFontHelper.SCAPED_STRING)) {
            ci.cancel();
            FormattedCharSequence plainSequence = emojiful$getFormattedCharSequence(reorderingProcessor, text);

            this.guiRenderState.submitText(new GuiTextRenderState(font, plainSequence, new Matrix3x2f(this.pose), x, y, color, 0, bl, this.scissorStack.peek()));
            return;
        }

        try {
            Pair<String, HashMap<Integer, Emoji>> cache = EmojiFontHelper.RECENT_STRINGS.get(text);
            String processedText = cache.getLeft();
            HashMap<Integer, Emoji> emojis = cache.getRight();

            if (!emojis.isEmpty() || text.contains(EmojiFontHelper.SCAPED_STRING)) {
                Constants.LOG.info("[GUI GRAPHICS] Found emojis in text: {}, emojis: {}", text, emojis.size());
                GuiGraphics guiGraphics = (GuiGraphics) (Object) this;

                // Render emojis at their positions
                net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource =
                        net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();

                EmojiFontHelper.EmojiCharacterRenderer renderer = new EmojiFontHelper.EmojiCharacterRenderer(
                        emojis,
                        bufferSource,
                        guiGraphics,
                        x, y,
                        false,
                        false,
                        0xF000F0
                );
                FormattedCharSequence sequence = FormattedCharSequence.forward(processedText, net.minecraft.network.chat.Style.EMPTY);

                sequence.accept(renderer);

                ci.cancel();
            }
        } catch (ExecutionException e) {
            Constants.LOG.error("[GUI GRAPHICS] Error processing emoji text", e);
        }
    }

    @Unique
    private static @NotNull FormattedCharSequence emojiful$getFormattedCharSequence(FormattedCharSequence reorderingProcessor, String text) {
        final var textFinal = text.substring(EmojiFontHelper.SCAPED_STRING.length());
        return sink -> {
            AtomicInteger textPos = new AtomicInteger();
            return reorderingProcessor.accept((pos, style, ch) -> {
                if (textPos.get() < textFinal.length()) {
                    char plainChar = textFinal.charAt(textPos.getAndIncrement());
                    return sink.accept(pos, style, plainChar);
                }
                return true;
            });
        };
    }

}