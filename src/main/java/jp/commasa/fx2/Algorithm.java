package jp.commasa.fx2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Price;
import jp.commasa.fx2.dto.PriceEx;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class Algorithm {

	private ResourceBundle bundle;
	private Gson gson = new Gson();
	private Jedis jedis = null;
	private Map<String, Price> previous = new HashMap<String, Price>();

	public Algorithm(ResourceBundle bundle) {
		this.bundle = bundle;
	}
	
	public List<Order> run(Price p) {
		if (p == null || p.getSymbol() == null ) return null;
		PriceEx ex = new PriceEx(p);
		if (jedis == null || jedis.isConnected() == false) {
			JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");
			jedis = pool.getResource();
		}
		Price prev = previous.get(p.getSymbol());
		if (prev != null) {
			ex.setDiff( (p.getAsk()+p.getBid())/2 - (prev.getAsk()+prev.getBid())/2 );
			ex.setDiffsp( (p.getAsk()-p.getBid()) - (prev.getAsk()-prev.getBid()) );
		} else {
			ex.setDiff(0d);
			ex.setDiffsp(0d);
		}
		previous.put(ex.getSymbol(), ex);
		String key = p.getSymbol();
		String val = gson.toJson(ex);
		jedis.set(key,  val);
		jedis.publish("RATE", val);
		return null;
	}
}
