package org.main.nbtgrabber2.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class Nbtgrabber2Client implements ClientModInitializer {
    private static KeyBinding grabKeyBinding;
    private static KeyBinding summonKeyBinding;

    @Override
    public void onInitializeClient() {
        // Initialize keybindings
        grabKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nbtgrabber.grab", // Translation key
                InputUtil.Type.KEYSYM, // Input type
                GLFW.GLFW_KEY_G,       // Default key (G)
                "category.nbtgrabber"  // Category in controls settings
        ));

        summonKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nbtgrabber.summon", // Translation key
                InputUtil.Type.KEYSYM,   // Input type
                GLFW.GLFW_KEY_H,         // Default key (H)
                "category.nbtgrabber"    // Category in controls settings
        ));

        // Register tick event listener
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (grabKeyBinding.wasPressed()) {
                grabNearestEntityNbt(client);
            }
            if (summonKeyBinding.wasPressed()) {
                copySummonCommand(client);
            }
        });
    }

    private void grabNearestEntityNbt(MinecraftClient client) {
        var player = client.player;
        if (player == null) return;

        var nearestEntity = findNearestEntity(player);
        if (nearestEntity != null) {
            var nbt = new NbtCompound();
            nearestEntity.writeNbt(nbt);
            var nbtData = nbt.asString();

            // Copy to clipboard
            client.keyboard.setClipboard(nbtData);
            player.sendMessage(Text.of("Copied NBT data to clipboard: " + nbtData), false);
        } else {
            player.sendMessage(Text.of("No entities nearby to grab NBT data from!"), false);
        }
    }

    private void copySummonCommand(MinecraftClient client) {
        var player = client.player;
        if (player == null) return;

        var nearestEntity = findNearestEntity(player);
        if (nearestEntity != null) {
            var nbt = new NbtCompound();
            nearestEntity.writeNbt(nbt);

            // Extract the entity's type and position
            String entityType = nearestEntity.getType().toString();
            double x = nearestEntity.getX();
            double y = nearestEntity.getY();
            double z = nearestEntity.getZ();

            // Generate summon command
            String summonCommand = String.format(
                    "/summon %s %.2f %.2f %.2f %s",
                    entityType,
                    x, y, z,
                    nbt.isEmpty() ? "" : nbt
            );

            // Copy to clipboard
            client.keyboard.setClipboard(summonCommand);
            player.sendMessage(Text.of("Copied summon command to clipboard: " + summonCommand), false);
        } else {
            player.sendMessage(Text.of("No entities nearby to generate summon command!"), false);
        }
    }

    private Entity findNearestEntity(Entity player) {
        return player.getWorld().getEntitiesByClass(
                        Entity.class,
                        player.getBoundingBox().expand(10),
                        entity -> entity != null && !entity.equals(player)
                ).stream()
                .min((e1, e2) -> Double.compare(
                        e1.squaredDistanceTo(player),
                        e2.squaredDistanceTo(player)
                ))
                .orElse(null);
    }
}
