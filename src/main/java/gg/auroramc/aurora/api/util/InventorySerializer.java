package gg.auroramc.aurora.api.util;

import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.Base64;
import java.util.Collection;

public class InventorySerializer {
    public static byte[] serializeItemsAsBytes(Collection<ItemStack> items) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            final DataOutput output = new DataOutputStream(outputStream);
            output.writeInt(items.size());
            for (final ItemStack item : items) {
                if (item == null || item.isEmpty()) {
                    // Ensure the correct order by including empty/null items
                    output.writeInt(0);
                    continue;
                }

                final byte[] itemBytes = item.serializeAsBytes();
                output.writeInt(itemBytes.length);
                output.write(itemBytes);
            }
            return outputStream.toByteArray();
        } catch (final IOException e) {
            throw new RuntimeException("Error while writing itemstack", e);
        }
    }

    public static String serializeItemsAsBase64(Collection<ItemStack> items) {
        return Base64.getEncoder().encodeToString(serializeItemsAsBytes(items));
    }

    public static ItemStack[] deserializeItemsFromBase64(String base64) {
        return deserializeItemsFromBytes(Base64.getDecoder().decode(base64));
    }

    public static ItemStack[] deserializeItemsFromBytes(final byte[] bytes) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            final DataInputStream input = new DataInputStream(inputStream);

            final int count = input.readInt();
            final ItemStack[] items = new ItemStack[count];
            for (int i = 0; i < count; i++) {
                final int length = input.readInt();
                if (length == 0) {
                    // Empty item, keep entry as empty
                    items[i] = ItemStack.empty();
                    continue;
                }

                final byte[] itemBytes = new byte[length];
                input.read(itemBytes);
                items[i] = ItemStack.deserializeBytes(itemBytes);
            }
            return items;
        } catch (final java.io.IOException e) {
            throw new RuntimeException("Error while reading itemstack", e);
        }
    }
}
