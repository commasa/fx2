package jp.commasa.fx2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Position;
import jp.commasa.fx2.dto.Price;
import jp.commasa.fx2.dto.PriceEx;
import jp.commasa.fx2.dto.Report;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class Algorithm {

	private Logger log = LogManager.getLogger(this.getClass());
	private ResourceBundle bundle;
	private Gson gson = new Gson();
	private JedisPool pool = null;
	private Jedis jedis = null;
	private Map<String, List<PriceEx>> history = new HashMap<String, List<PriceEx>>();
	private Map<String, Position> position = new HashMap<String, Position>();
	private int size = 100;
	private int net = 5;
	private BigDecimal amount = new BigDecimal(10000);
	private BigDecimal volatility = new BigDecimal(20);
	private BigDecimal maxamount = new BigDecimal(30000);

	public Algorithm(ResourceBundle bundle) {
		this.bundle = bundle;
		try {
			String value = this.bundle.getString("size");
			size = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(size) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("net");
			net = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(net) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("amount");
			amount = new BigDecimal(value);
		} catch(Exception e) {
			log.info("Parameter(amount) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("maxamount");
			maxamount = new BigDecimal(value);
		} catch(Exception e) {
			log.info("Parameter(maxamount) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("volatility");
			volatility = new BigDecimal(value);
		} catch(Exception e) {
			log.info("Parameter(volatility) is invalid.", e);
		}
	}
	
	public List<Order> run(Price p) {
		//レート処理
		if (p == null || p.getSymbol() == null ) return null;
		PriceEx ex = new PriceEx(p);
		if (jedis == null || jedis.isConnected() == false) {
			pool = new JedisPool(new JedisPoolConfig(), "localhost");
			jedis = pool.getResource();
		}
		PriceEx prev = null;
		boolean uniq = false;
		if (history.get(p.getSymbol()) == null) history.put(p.getSymbol(), new ArrayList<PriceEx>());
		List<PriceEx> previous = history.get(p.getSymbol());
		if (previous.size() > 0) prev = previous.get(previous.size()-1);
		if (prev != null) {
			if (p.getTickNo()!=null && !p.getTickNo().equals(prev.getTickNo())) {
				BigDecimal scale = new BigDecimal(10000);
				if (p.getSymbol().endsWith("JPY")) scale=new BigDecimal(100);
				BigDecimal ca = new BigDecimal(p.getAsk());
				BigDecimal cb = new BigDecimal(p.getBid());
				BigDecimal pa = new BigDecimal(prev.getAsk());
				BigDecimal pb = new BigDecimal(prev.getBid());
				BigDecimal cm = ca.add(cb).divide(new BigDecimal(2));
				BigDecimal pm = pa.add(pb).divide(new BigDecimal(2));
				BigDecimal csp = ca.subtract(cb);
				BigDecimal psp = pa.subtract(pb);
				ex.setDiff( cm.subtract(pm).multiply(scale).setScale(1, BigDecimal.ROUND_HALF_UP) );
				ex.setDiffsp( csp.subtract(psp).multiply(scale).setScale(1, BigDecimal.ROUND_HALF_UP) );
				ex.setSumlong(prev.getSumlong());
				ex.setSumshort(prev.getSumshort());
				int c = ex.getDiff().compareTo(BigDecimal.ZERO);
				if (c > 0) ex.setSumlong(ex.getSumlong().add(ex.getDiff()));
				if (c < 0) ex.setSumshort(ex.getSumshort().add(ex.getDiff().abs()));
				previous.add(ex);
				uniq = true;
			} else {
				ex.setDiff(prev.getDiff());
				ex.setDiffsp(prev.getDiffsp());
				ex.setSumlong(prev.getSumlong());
				ex.setSumshort(prev.getSumshort());
			}
		} else {
			ex.setDiff(BigDecimal.ZERO);
			ex.setDiffsp(BigDecimal.ZERO);
			previous.add(ex);
		}
		//保持対象外となったデータの削除
		while (previous.size()>size) {
			PriceEx d = previous.get(0);
			int c = d.getDiff().compareTo(BigDecimal.ZERO);
			if (c > 0) ex.setSumlong(ex.getSumlong().subtract(d.getDiff()));
			if (c < 0) ex.setSumshort(ex.getSumshort().subtract(d.getDiff().abs()));			
			previous.remove(0);
		}
		if (previous.size() > net) {
			int s = previous.size() - net;
			BigDecimal n = BigDecimal.ZERO;
			for (int i=s; i<previous.size(); i++) {
				n = n.add(previous.get(i).getDiff());
			}
			ex.setNet(n);
		} else {
			ex.setNet(BigDecimal.ZERO);
		}
		String key = p.getSymbol();
		String val = gson.toJson(ex);
		jedis.set(key,  val);
		jedis.publish("RATE", val);
		String valHist = gson.toJson(previous);
		jedis.set(key+":history",  valHist);
		
		// 新規注文
		List<Order> result = new ArrayList<Order>();
		int netC1 = ex.getNet().compareTo(BigDecimal.ZERO);
		int netC2 = ex.getSumlong().subtract(ex.getSumshort()).compareTo(BigDecimal.ZERO);
		BigDecimal vola = ex.getSumlong().add(ex.getSumshort());
		Position pos = position.get(key);
		BigDecimal nowAmt = BigDecimal.ZERO;
		if (pos!=null) nowAmt = pos.getAmount();
		if (uniq && vola.compareTo(volatility) >= 0 && nowAmt.abs().compareTo(maxamount) < 0) {
			if (netC1 > 0 && netC2 > 0 ) {
				Order order = new Order(ex.getSymbol(), amount, ex.getTickNo());
				result.add(order);
			}
			if (netC1 < 0 && netC2 < 0 ) {
				Order order = new Order(ex.getSymbol(), amount.multiply(new BigDecimal(-1)), ex.getTickNo());
				result.add(order);
			}
		}
		log.debug(ex.getSymbol()+"("+ ex.getTickNo().toPlainString() +") = result["+result.size()+"] : netC1="+netC1+" netC2="+netC2+" vola="+vola+" nowAmt="+nowAmt);
		return result;
	}
	
	public void addReport(Report report) {
		String key = report.getSymbol();
		Position pos = position.get(key);
		if (pos==null) {
			pos = new Position();
			position.put(key, pos);
		}
		pos.addReport(report);
		if (jedis == null || jedis.isConnected() == false) {
			pool = new JedisPool(new JedisPoolConfig(), "localhost");
			jedis = pool.getResource();
		}
		String val = gson.toJson(pos);
		jedis.set(key+":position",  val);
	}

	public List<Order> finish() {
		//スクエア注文
		List<Order> result = new ArrayList<Order>();
		BigDecimal tickNo = new BigDecimal("9000000000");
		for (Map.Entry<String, Position> entry : position.entrySet()) {
			tickNo = tickNo.add(BigDecimal.ONE);
			Order order = new Order(entry.getKey(), entry.getValue().getAmount(), tickNo);
			result.add(order);
		}
		pool.close();
		return result;
	}

}
