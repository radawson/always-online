package me.dablakbandit.ao.utils;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.dablakbandit.ao.hybrid.IAlwaysOnline;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.*;
import java.util.List;
import java.util.Map;

public class CheckMethods {

	private static final CookieHandler COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);

	public static boolean directSessionServerStatus(IAlwaysOnline alwaysOnline, Gson gson) {
		String serverResponse = "{}";
		try {
			serverResponse = sendGet("https://sessionserver.mojang.com/session/minecraft/profile/069a79f444e94726a5befca90e38aaf5");
			if (serverResponse.isEmpty()) return false;
		} catch (IOException | URISyntaxException e) {
			return false;
		}
		Type type = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, String> data = gson.fromJson(serverResponse, type);
		if (!data.containsKey("id")) {
			return false;
		}
		return "069a79f444e94726a5befca90e38aaf5".equals(data.get("id"));
	}

	/**
	 * Checks if the Microsoft Minecraft Services API (api.minecraftservices.com) is reachable.
	 * Used for Microsoft-account authentication. A 401 response counts as "up" since it means
	 * the server responded; only connection failures indicate the service is down.
	 */
	public static boolean microsoftApiReachable() {
		HttpURLConnection con = null;
		try {
			con = openConnection("https://api.minecraftservices.com/minecraft/profile");
			int responseCode = con.getResponseCode();
			// Consume response body to allow proper connection cleanup
			try (InputStream in = con.getInputStream()) {
				ByteStreams.exhaust(in);
			} catch (IOException e) {
				try (InputStream err = con.getErrorStream()) {
					if (err != null) ByteStreams.exhaust(err);
				} catch (IOException ignored) {
				}
			}
			// Any HTTP response (including 401 Unauthorized) means the service is reachable
			return responseCode > 0;
		} catch (IOException | URISyntaxException e) {
			return false;
		} finally {
			if (con != null) con.disconnect();
		}
	}

	private static HttpURLConnection openConnection(String url) throws IOException, URISyntaxException {
		URL obj = new URL(url);
		URI uri = obj.toURI();
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setDefaultUseCaches(false);
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("User-Agent", "AlwaysOnline");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		return con;
	}

	private static String sendGet(String url) throws IOException, URISyntaxException {
		URL obj = new URL(url);
		URI uri = obj.toURI();

		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setDefaultUseCaches(false);
		con.setRequestMethod("GET");
		con.setRequestProperty("Accept", "application/json,text/html");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Connection", "close");
		con.setRequestProperty("User-Agent", "AlwaysOnline");

		for (Map.Entry<String, List<String>> pair : COOKIE_MANAGER.get(uri, con.getRequestProperties()).entrySet()) {
			String key = pair.getKey();
			for (String cookie : pair.getValue())
				con.addRequestProperty(key, cookie);
		}
		InputStream serverResponseStream;
		try {
			serverResponseStream = con.getInputStream();
		} catch (IOException e) {
			serverResponseStream = con.getErrorStream();
		}

		if (serverResponseStream == null) return "{}";
		COOKIE_MANAGER.put(uri, con.getHeaderFields());
		BufferedReader in = new BufferedReader(new InputStreamReader(serverResponseStream));
		String inputLine;
		StringBuilder response = new StringBuilder();
		while ((inputLine = in.readLine()) != null) response.append(inputLine);
		serverResponseStream.close();
		con.disconnect();
		return response.toString();
	}

}
