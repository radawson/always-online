package me.dablakbandit.ao.hybrid;

import com.google.gson.Gson;
import me.dablakbandit.ao.utils.CheckMethods;

import java.util.logging.Level;

public class MojangSessionCheck implements Runnable {

	private final AlwaysOnline alwaysOnline;
	private final boolean useHeadSessionServer;
	private final boolean useMicrosoftApiCheck;
	private final int totalCheckMethods;
	private final boolean offlineWhenAnyCheckFails;
	private final String messageMojangOffline, messageMojangOnline;
	private final Gson gson;
	private final long lockoutDurationMillis;
	private long lockoutEndTime = 0;

	public MojangSessionCheck(AlwaysOnline alwaysOnline) {
		this.alwaysOnline = alwaysOnline;
		int methodCount = 0;
		boolean headCheck = Boolean.parseBoolean(this.alwaysOnline.config.getProperty("http-head-session-server", "false"));
		if (methodCount == 0 && !headCheck) {
			this.alwaysOnline.nativeExecutor.log(Level.WARNING, "No check methods have been enabled in the configuration. " + "Going to enable the head session server check.");
			headCheck = true;
		}
		this.useHeadSessionServer = headCheck;
		this.useMicrosoftApiCheck = Boolean.parseBoolean(this.alwaysOnline.config.getProperty("microsoft-api-check", "true"));
		this.offlineWhenAnyCheckFails = Boolean.parseBoolean(this.alwaysOnline.config.getProperty("offline-when-any-check-fails", "true"));
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Head Session server check: " + this.useHeadSessionServer);
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Microsoft API check: " + this.useMicrosoftApiCheck);
		if (this.useHeadSessionServer) {
			methodCount++;
			this.gson = new Gson();
		} else {
			this.gson = null;
		}
		if (this.useMicrosoftApiCheck) {
			methodCount++;
		}

		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Total check methods active: " + methodCount);
		this.totalCheckMethods = methodCount;
		this.messageMojangOffline = this.alwaysOnline.config.getProperty("message-mojang-offline", "&5[&2AlwaysOnline&5]&a Mojang servers are now offline!");
		this.messageMojangOnline = this.alwaysOnline.config.getProperty("message-mojang-online", "&5[&2AlwaysOnline&5]&a Mojang servers are now online!");
		int lockoutMinutes = Integer.parseInt(this.alwaysOnline.config.getProperty("down-detector-lockout-minutes", "5"));
		this.lockoutDurationMillis = lockoutMinutes * 60 * 1000L;
		this.alwaysOnline.nativeExecutor.log(Level.INFO, "Down detector lockout duration: " + lockoutMinutes + " minutes");
	}

	@Override
	public void run() {
		if (!alwaysOnline.getCheckSessionStatus()) return;
		int downServiceReport = 0;
		if (this.useHeadSessionServer && !CheckMethods.directSessionServerStatus(alwaysOnline, this.gson))
			downServiceReport++;
		if (this.useMicrosoftApiCheck && !CheckMethods.microsoftApiReachable())
			downServiceReport++;
		boolean consideredOffline = offlineWhenAnyCheckFails
				? (downServiceReport > 0)
				: (downServiceReport >= this.totalCheckMethods);
		long currentTime = System.currentTimeMillis();
		if (consideredOffline) {// Offline
			// Reset lockout timer when down is detected (whether first time or again during lockout)
			this.lockoutEndTime = currentTime + this.lockoutDurationMillis;
			if (!alwaysOnline.getOfflineMode()) {
				alwaysOnline.toggleOfflineMode();
				this.alwaysOnline.saveState();
				this.alwaysOnline.nativeExecutor.log(Level.INFO, "Mojang servers appear to be offline. Enabling mojang offline mode...");
				if (!"null".equals(this.messageMojangOffline))
					this.alwaysOnline.nativeExecutor.broadcastMessage(this.messageMojangOffline);
			}
		} else {// Online
			// Only switch to online mode if lockout period has expired
			if (currentTime >= this.lockoutEndTime && alwaysOnline.getOfflineMode()) {
				alwaysOnline.toggleOfflineMode();
				this.alwaysOnline.saveState();
				this.alwaysOnline.nativeExecutor.log(Level.INFO, "Mojang servers appear to be online. Disabling mojang offline mode...");
				if (!"null".equals(this.messageMojangOnline))
					this.alwaysOnline.nativeExecutor.broadcastMessage(this.messageMojangOnline);
			}
		}
	}

}
