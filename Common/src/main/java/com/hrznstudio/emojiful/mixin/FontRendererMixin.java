package com.hrznstudio.emojiful.mixin;

import com.hrznstudio.emojiful.api.Emoji;
import com.hrznstudio.emojiful.render.EmojiFontHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Font.class)
public abstract class FontRendererMixin {

    @Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), cancellable = true)
    private void widthFormattedCharSeq(FormattedCharSequence processor, CallbackInfoReturnable<Integer> cir){
        StringBuilder builder = new StringBuilder();
        processor.accept((p_accept_1_, p_accept_2_, ch) -> {
            builder.append((char) ch);
            return true;
        });
        Font thisObject = (Font)(Object)this;
        cir.setReturnValue(thisObject.width(builder.toString()));
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void widthFormattedText(FormattedText formattedText, CallbackInfoReturnable<Integer> cir){
        Font thisObject = (Font)(Object)this;
        cir.setReturnValue(thisObject.width(formattedText.getString()));
    }

    @ModifyArg(method = "width(Ljava/lang/String;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/StringSplitter;stringWidth(Ljava/lang/String;)F"), index = 0)
    private String injectedWidth(String value) {
        if (value != null) {
            try {
                value = EmojiFontHelper.RECENT_STRINGS.get(value).getKey();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }
}
