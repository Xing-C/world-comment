package cn.zbx1425.worldcomment.item;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.gui.CommentToolScreen;
import cn.zbx1425.worldcomment.image.CommentScreenshot;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class CommentToolItem extends Item {

    public CommentToolItem() {
        super(new Properties().stacksTo(1));
    }

    private static final SoundEvent shutterSoundEvent = SoundEvent.createFixedRangeEvent(
            new ResourceLocation("worldcomment:shutter"), 16
    );

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack item = player.getItemInHand(usedHand);
        if (!level.isClientSide) return InteractionResultHolder.pass(item);
        if (!item.is(Main.ITEM_COMMENT_TOOL.get())) return InteractionResultHolder.fail(item);

        CommentScreenshot.grab(path -> {
            Minecraft.getInstance().execute(() -> {
                player.playSound(shutterSoundEvent);
                ClientLogics.openCommentToolScreen(path);
            });
        });

        return InteractionResultHolder.success(item);
    }

    private static class ClientLogics {

        public static void openCommentToolScreen(Path path) {
            Minecraft.getInstance().setScreen(new CommentToolScreen(path));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        return super.useOn(context);
    }
}