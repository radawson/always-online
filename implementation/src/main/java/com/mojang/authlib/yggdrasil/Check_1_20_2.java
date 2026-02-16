package com.mojang.authlib.yggdrasil;

import me.dablakbandit.ao.hybrid.IAlwaysOnline;
import me.dablakbandit.ao.utils.ReflectionUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.Proxy;

public class Check_1_20_2 {

	private static final Field fieldServices;
	private static final Field fieldServicesSessionService;
	private static final Field fieldServicesKeySet;
	private static final Field fieldMinecraftClient;
	private static final Field fieldProxy;
	private static final Constructor<?> conServices;

	static {
		try {
			fieldServices = ReflectionUtil.getFirstFieldOfType(MinecraftServer.class, Services.class);
			fieldServicesSessionService = ReflectionUtil.getFirstFieldOfType(Services.class, com.mojang.authlib.minecraft.MinecraftSessionService.class);
			fieldServicesKeySet = ReflectionUtil.getFirstFieldOfType(YggdrasilMinecraftSessionService.class, ServicesKeySet.class);
			fieldMinecraftClient = ReflectionUtil.getFirstFieldOfType(YggdrasilMinecraftSessionService.class, com.mojang.authlib.minecraft.client.MinecraftClient.class);
			fieldProxy = ReflectionUtil.getFirstFieldOfType(com.mojang.authlib.minecraft.client.MinecraftClient.class, Proxy.class);
			conServices = Services.class.getConstructors()[0];
		} catch (Exception e) {
			throw new RuntimeException("Failed to resolve NMS fields", e);
		}
	}

	public static boolean valid() {
		return true;
	}

	public static void setup(IAlwaysOnline alwaysOnline) throws Exception {
		MinecraftServer minecraftServer = MinecraftServer.getServer();
		Services services = (Services) fieldServices.get(minecraftServer);
		YggdrasilMinecraftSessionService oldSessionService = (YggdrasilMinecraftSessionService) fieldServicesSessionService.get(services);
		ServicesKeySet servicesKeySet = (ServicesKeySet) fieldServicesKeySet.get(oldSessionService);
		Object minecraftClient = fieldMinecraftClient.get(oldSessionService);
		Proxy proxy = (Proxy) fieldProxy.get(minecraftClient);

		NMSAuthSessionService service = new NMSAuthSessionService(alwaysOnline, oldSessionService, servicesKeySet, proxy, YggdrasilEnvironment.PROD.getEnvironment(), alwaysOnline.getDatabase());

		Object[] objects = new Object[conServices.getParameterCount()];
		objects[0] = service;
		for (int i = 1; i < conServices.getParameterCount(); i++) {
			objects[i] = ReflectionUtil.getFirstFieldOfType(Services.class, conServices.getParameterTypes()[i]).get(services);
		}

		Object newServices = conServices.newInstance(objects);
		fieldServices.set(minecraftServer, newServices);
	}
}
