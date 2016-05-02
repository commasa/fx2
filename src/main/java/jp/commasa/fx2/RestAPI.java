package jp.commasa.fx2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class RestAPI {

	private String key;
	private String host;
	private String account;

	public RestAPI(String key, String host, String account) {
		this.key = key;
		this.host = host;
		this.account = account;
	}
	
	public String square(String symbol) throws URISyntaxException, MalformedURLException, IOException {
		StringBuffer result = new StringBuffer();
		String s = symbol.replace('/', '_');
		URI uri = new URI("http://" + this.host + "/v1/account/" + this.account + "/positions/" + s);
		URLConnection conn = uri.toURL().openConnection();
		if (conn instanceof HttpsURLConnection) {
			HttpsURLConnection https = (HttpsURLConnection) conn;
			https.setRequestProperty("Authorization", "Bearer " + this.key);
			https.setRequestMethod("DELETE");
			https.connect();
			BufferedReader br = new BufferedReader(new InputStreamReader(https.getInputStream()));
			String line;
			while ((line=br.readLine()) != null) {
				result.append(line.trim()+" ");
			}
			br.close();
			https.disconnect();
		}
		return result.toString();
	}
}
