package me.dablakbandit.ao.spigot.authservices;

import com.mojang.authlib.yggdrasil.Check_1_20_2;
import me.dablakbandit.ao.spigot.SpigotLoader;
import net.minecraft.server.MinecraftServer;

import java.util.logging.Level;

public class NMSAuthSetup {

	public static void setUp(SpigotLoader spigotLoader) throws Exception {
		spigotLoader.log(Level.INFO, "Attempting setup 1.20+ Auth service");
		Check_1_20_2.setup(spigotLoader.getAOInstance());
	}

	public static void setOnlineMode(boolean onlineMode) {
		try {
			MinecraftServer.getServer().setUsesAuthentication(onlineMode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
