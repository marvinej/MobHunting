package one.lindegaard.MobHunting.rewards;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import one.lindegaard.Core.Strings;
import one.lindegaard.Core.Server.Servers;
import one.lindegaard.Core.Shared.Skins;
import one.lindegaard.Core.rewards.CoreCustomItems;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.PlayerSettings;
import one.lindegaard.MobHunting.mobs.MinecraftMob;

public class CustomItems {

	private MobHunting plugin;

	public CustomItems() {
		this.plugin = MobHunting.getInstance();
	}

	// How to get Playerskin
	// https://www.spigotmc.org/threads/how-to-get-a-players-texture.244966/
	// https://minecraft-heads.com/

	/**
	 * Return an ItemStack with the Players head texture.
	 *
	 * @param name
	 * @param money
	 * @return
	 */
	public ItemStack getPlayerHead(UUID uuid, int amount, double money) {
		ItemStack skull = CoreCustomItems.getDefaultPlayerHead(amount);
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
		String[] skinCache = new String[2];

		if (ps.getTexture() == null || ps.getSignature() == null || ps.getTexture().isEmpty()
				|| ps.getSignature().isEmpty()) {
			if (offlinePlayer.isOnline()) {
				Player player = (Player) offlinePlayer;
				Skins sk = CoreCustomItems.getSkinsClass();
				if (sk != null) {
					plugin.getMessages().debug("Trying to fecth skin from Online Player Profile");
					skinCache = sk.getSkin(player);
				} else {
					plugin.getMessages().debug("Trying to fecth skin from Minecraft Servers");
					skinCache = getSkinFromUUID(uuid);
				}
			}

			if ((skinCache == null || skinCache[0] == null || skinCache[0].isEmpty() || skinCache[1] == null
					|| skinCache[1].isEmpty()) && Servers.isMC112OrNewer())
				return getPlayerHeadOwningPlayer(uuid, amount, money);

			if (skinCache != null && skinCache[0] != null && !skinCache[0].isEmpty() && skinCache[1] != null
					&& !skinCache[1].isEmpty()) {
				ps.setTexture(skinCache[0]);
				ps.setSignature(skinCache[1]);
				plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
			} else {
				plugin.getMessages().debug("Empty skin");
				return skull;
			}
		} else {
			if (offlinePlayer.isOnline()) {
				Player player = (Player) offlinePlayer;
				Skins sk = CoreCustomItems.getSkinsClass();
				if (sk != null) {
					String[] skinOnline = sk.getSkin(player);
					if (skinOnline != null && !skinOnline.equals(skinCache)) {
						plugin.getMessages().debug("%s has changed skin, updating MobHunting Skin cache",
								player.getName());
						ps.setTexture(skinOnline[0]);
						ps.setSignature(skinOnline[1]);
						plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
					}
				}
			}
			skinCache[0] = ps.getTexture();
			skinCache[1] = ps.getSignature();
			plugin.getMessages().debug("%s using skin from MobHunting Skin Cache", offlinePlayer.getName());
		}

		skull = new ItemStack(getCustomtexture(UUID.fromString(Reward.MH_REWARD_KILLED_UUID), offlinePlayer.getName(),
				skinCache[0], skinCache[1], money, UUID.randomUUID(), uuid));
		skull.setAmount(amount);
		return skull;
	}

	private String[] getSkinFromUUID(UUID uuid) {
		try {
			URL url_1 = new URL(
					"https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
			InputStreamReader reader_1;
			reader_1 = new InputStreamReader(url_1.openStream());

			JsonElement json = new JsonParser().parse(reader_1);
			if (json.isJsonObject()) {
				JsonObject textureProperty = json.getAsJsonObject().get("properties").getAsJsonArray().get(0)
						.getAsJsonObject();
				String texture = textureProperty.get("value").getAsString();
				String signature = textureProperty.get("signature").getAsString();

				return new String[] { texture, signature };
			} else {
				plugin.getMessages().debug("(1) Could not get skin data from session servers!");
				return null;
			}
		} catch (IOException e) {
			plugin.getMessages().debug("(2) Could not get skin data from session servers!");
			return null;
		}
	}

	public ItemStack getPlayerHeadOwningPlayer(UUID uuid, int amount, double money) {
		ItemStack skull = CoreCustomItems.getDefaultPlayerHead(amount);
		SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		skullMeta.setLore(new ArrayList<String>(Arrays.asList("Hidden(0):" + name,
				"Hidden(1):" + String.format(Locale.ENGLISH, "%.5f", money),
				"Hidden(2):" + Reward.MH_REWARD_KILLER_UUID,
				money == 0 ? "Hidden(3):" : "Hidden(3):" + UUID.randomUUID(), "Hidden(4):" + uuid,
				"Hidden(5):"
						+ Strings.encode(String.format(Locale.ENGLISH, "%.5f", money) + Reward.MH_REWARD_KILLER_UUID),
				plugin.getMessages().getString("mobhunting.reward.lore"))));
		skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
		if (money == 0) {
			skullMeta.setDisplayName(name);
			skull.setAmount(amount);
		} else {
			skullMeta.setDisplayName(name + " (" + plugin.getEconomyManager().format(money) + ")");
			skull.setAmount(1);
		}
		skull.setItemMeta(skullMeta);
		plugin.getMessages().debug("CustomItems: set the skin using OwningPlayer (%s,%s)", name, uuid.toString());
		return skull;
	}

	/**
	 * Return an ItemStack with a custom texture. If Mojang changes the way they
	 * calculate Signatures this method will stop working.
	 *
	 * @param mPlayerUUID
	 * @param mDisplayName
	 * @param mTextureValue
	 * @param mTextureSignature
	 * @param money
	 * @return ItemStack with custom texture.
	 */
	public ItemStack getCustomtexture(UUID mPlayerUUID, String mDisplayName, String mTextureValue,
			String mTextureSignature, double money, UUID uniqueRewardUuid, UUID skinUuid) {
		ItemStack skull = CoreCustomItems.getDefaultPlayerHead(1);
		if (mTextureValue.isEmpty())
			return skull;

		SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

		GameProfile profile = new GameProfile(mPlayerUUID, mDisplayName);
		if (mTextureSignature.isEmpty())
			profile.getProperties().put("textures", new Property("textures", mTextureValue));
		else
			profile.getProperties().put("textures", new Property("textures", mTextureValue, mTextureSignature));
		Field profileField = null;

		try {
			profileField = skullMeta.getClass().getDeclaredField("profile");
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			return skull;
		}

		profileField.setAccessible(true);

		try {
			profileField.set(skullMeta, profile);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		if (mPlayerUUID.equals(UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID)))
			skullMeta.setLore(new ArrayList<String>(Arrays.asList("Hidden(0):" + mDisplayName,
					"Hidden(1):" + String.format(Locale.ENGLISH, "%.5f", money), "Hidden(2):" + mPlayerUUID,
					money == 0 ? "Hidden(3):" : "Hidden(3):" + uniqueRewardUuid, "Hidden(4):" + skinUuid,
					"Hidden(5):" + Strings.encode(String.format(Locale.ENGLISH, "%.5f", money) + mPlayerUUID))));
		else
			skullMeta.setLore(new ArrayList<String>(Arrays.asList("Hidden(0):" + mDisplayName,
					"Hidden(1):" + String.format(Locale.ENGLISH, "%.5f", money), "Hidden(2):" + mPlayerUUID,
					money == 0 ? "Hidden(3):" : "Hidden(3):" + uniqueRewardUuid, "Hidden(4):" + skinUuid,
					"Hidden(5):" + Strings.encode(String.format(Locale.ENGLISH, "%.5f", money) + mPlayerUUID),
					plugin.getMessages().getString("mobhunting.reward.lore"))));
		ChatColor color = ChatColor.GOLD;
		try {
			color = ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor.toUpperCase());
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[BagOfGold] " + ChatColor.RED
					+ "drop-money-on-ground-text-color in your config.yml cant be read.");
		}
		if (money == 0)
			skullMeta.setDisplayName(color + mDisplayName);
		else
			skullMeta.setDisplayName(color + mDisplayName + " (" + plugin.getRewardManager().format(money) + ")");

		skull.setItemMeta(skullMeta);
		return skull;
	}

	public ItemStack getCustomHead(MinecraftMob minecraftMob, String name, int amount, double money, UUID skinUUID) {
		ItemStack skull;
		switch (minecraftMob) {
		case Skeleton:
			skull = CoreCustomItems.getDefaultSkeletonHead(amount);
			skull = minecraftMob.setDisplayNameAndHiddenLores(skull, new Reward(minecraftMob.getFriendlyName(), money,
					UUID.fromString(Reward.MH_REWARD_KILLED_UUID), UUID.randomUUID(), skinUUID));
			break;

		case WitherSkeleton:
			skull = CoreCustomItems.getDefaultWitherSkeletonHead(amount);
			skull = minecraftMob.setDisplayNameAndHiddenLores(skull, new Reward(minecraftMob.getFriendlyName(), money,
					UUID.fromString(Reward.MH_REWARD_KILLED_UUID), UUID.randomUUID(), skinUUID));
			break;

		case Zombie:
			skull = CoreCustomItems.getDefaultZombieHead(amount);
			skull = minecraftMob.setDisplayNameAndHiddenLores(skull, new Reward(minecraftMob.getFriendlyName(), money,
					UUID.fromString(Reward.MH_REWARD_KILLED_UUID), UUID.randomUUID(), skinUUID));
			break;

		case PvpPlayer:
			skull = getPlayerHead(skinUUID, amount, money);
			break;

		case Creeper:
			skull = CoreCustomItems.getDefaultCreeperHead(amount);
			skull = minecraftMob.setDisplayNameAndHiddenLores(skull, new Reward(minecraftMob.getFriendlyName(), money,
					UUID.fromString(Reward.MH_REWARD_KILLED_UUID), UUID.randomUUID(), skinUUID));
			break;

		case EnderDragon:
			skull = CoreCustomItems.getDefaultEnderDragonHead(amount);
			skull = minecraftMob.setDisplayNameAndHiddenLores(skull, new Reward(minecraftMob.getFriendlyName(), money,
					UUID.fromString(Reward.MH_REWARD_KILLED_UUID), UUID.randomUUID(), skinUUID));
			break;

		default:
			ItemStack is = new ItemStack(getCustomtexture(UUID.fromString(Reward.MH_REWARD_KILLED_UUID),
					minecraftMob.getFriendlyName(), minecraftMob.getTextureValue(), minecraftMob.getTextureSignature(),
					money, UUID.fromString(Reward.MH_REWARD_KILLED_UUID), skinUUID));
			is.setAmount(amount);
			return is;
		}
		return skull;
	}

}