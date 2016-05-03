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
	private int movavg1 = 90;
	private int movavg2 = 30;
	private int bandwalk = 5;
	private int alpha1 = 1;
	private int alpha2 = 2;
	private int alpha3 = 3;
	private BigDecimal amount = BigDecimal.valueOf(10000);
	private BigDecimal volatility = BigDecimal.valueOf(20);
	private BigDecimal maxamount = BigDecimal.valueOf(30000);

	public Algorithm(ResourceBundle bundle) {
		this.bundle = bundle;
		try {
			String value = this.bundle.getString("size");
			size = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(size) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("movavg1");
			movavg1 = Integer.valueOf(value);
			value = this.bundle.getString("movavg2");
			movavg2 = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(movavg) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("bandwalk");
			bandwalk = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(bandwalk) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("alpha1");
			alpha1 = Integer.valueOf(value);
			value = this.bundle.getString("alpha2");
			alpha2 = Integer.valueOf(value);
			value = this.bundle.getString("alpha3");
			alpha3 = Integer.valueOf(value);
		} catch(Exception e) {
			log.info("Parameter(movavg) is invalid.", e);
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
				previous.add(ex);
				uniq = true;
			}
		} else {
			previous.add(ex);
		}
		//保持対象外となったデータの削除
		while (previous.size()>size) {
			previous.remove(0);
		}
		double scale = 10000;
		if (ex.getSymbol().endsWith("JPY")) scale=100;
		//ボリンジャーバンドもどき
		int ma1len = movavg1;
		double v1 = 0;
		if (previous.size() > movavg1) {
			int st = previous.size() - movavg1;
			double sum = 0;
			for (int i=st; i<previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += previous.get(i).getMid();
				} else {
					ma1len--;
				}
				if (i > 0) v1 += Math.abs(previous.get(i).getMid()-previous.get(i-1).getMid());
			}
			if (ma1len>0) { ex.setMovAvg1(sum/ma1len); } else { ex.setMovAvg1(0D); }
			ex.setVolatility1(v1*scale);
			sum = 0;
			for (int i=st; i<previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += Math.pow(previous.get(i).getMid()-ex.getMovAvg1(),2);
				}
			}
			if (ma1len>0) { ex.setAlpha1(sum/ma1len); } else { ex.setAlpha1(0D); }
		} else {
			ex.setMovAvg1(0D);
			ex.setAlpha1(0D);
			ex.setVolatility1(0D);
		}
		double v2 = 0;
		int ma2len = movavg2;
		if (previous.size() > movavg2) {
			int st = previous.size() - movavg2;
			double sum = 0;
			for (int i=st; i<previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += previous.get(i).getMid();
				} else {
					ma2len--;
				}
				if (i > 0) v2 += Math.abs(previous.get(i).getMid()-previous.get(i-1).getMid());
			}
			if (ma2len>0) { ex.setMovAvg2(sum/ma2len); } else { ex.setMovAvg2(0D); }
			ex.setVolatility2(v2*scale);
			sum = 0;
			for (int i=st; i<previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += Math.pow(previous.get(i).getMid()-ex.getMovAvg2(),2);
				}
			}
			if (ma2len>0) { ex.setAlpha2(sum/ma2len); } else { ex.setAlpha2(0D); }
		} else {
			ex.setMovAvg2(0D);
			ex.setAlpha2(0D);
			ex.setVolatility2(0D);
		}
		// status
		if (ex.getMovAvg1() < 1) {
			ex.setStatus("");
		} else {
			double mid = ex.getMid();
			double work = Math.abs(mid - ex.getMovAvg1());
			ex.setStatus("MA");
			if ( Math.abs(mid - (ex.getMovAvg1() + ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() + alpha1*ex.getAlpha1()));
				ex.setStatus("+a1");
			}
			if ( Math.abs(mid - (ex.getMovAvg1() - ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() - alpha1*ex.getAlpha1()));
				ex.setStatus("-a1");
			}
			if ( Math.abs(mid - (ex.getMovAvg1() + 2*ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() + alpha2*ex.getAlpha1()));
				ex.setStatus("+a2");
			}
			if ( Math.abs(mid - (ex.getMovAvg1() - 2*ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() - alpha2*ex.getAlpha1()));
				ex.setStatus("-a2");
			}
			if ( Math.abs(mid - (ex.getMovAvg1() + 3*ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() + alpha3*ex.getAlpha1()));
				ex.setStatus("+a3");
			}
			if ( Math.abs(mid - (ex.getMovAvg1() - 3*ex.getAlpha1())) < work ) {
				work = Math.abs(mid - (ex.getMovAvg1() - alpha3*ex.getAlpha1()));
				ex.setStatus("-a3");
			}
		}
		int cnt = 0;
		String status = ex.getStatus();
		for (int i=previous.size()-1; i>=0; i--) {
			if ( status.equals(previous.get(i).getStatus()) ) {
				cnt++;
			} else {
				break;
			}
		}
		ex.setStatusCount(cnt);

		// 書き込み
		String key = p.getSymbol();
		String val = gson.toJson(ex);
		jedis.set(key,  val);
		jedis.publish("RATE", val);
		String valHist = gson.toJson(previous);
		jedis.set(key+":history",  valHist);
		
		// 注文
		Position pos = position.get(key);
		if (pos == null) {
			pos = new Position();
			position.put(key, pos);
		}
		if (pos.getOrderCount() > 0) {
			log.info("order skip: order count = " + pos.getOrderCount()); 
			return null;
		}
		List<Order> result = new ArrayList<Order>();
		BigDecimal nowAmt = pos.getAmount();
		// TODO 決済
		// 新規
		if ( uniq && ex.getVolatility1() >= volatility.doubleValue() ) {
			if ( ex.getStatusCount() > bandwalk) {
				// バンドウォーク　順張り
				if ("+a2".equals(ex.getStatus()) || "+a3".equals(ex.getStatus())) {
					if (nowAmt.compareTo(maxamount) < 0) {
						Order order = new Order(ex.getSymbol(), amount, ex.getTickNo());
						result.add(order);
						pos.orderCount(1);
					}
				}
				if ("-a2".equals(ex.getStatus()) || "-a3".equals(ex.getStatus())) {
					if (nowAmt.compareTo(maxamount.multiply(BigDecimal.valueOf(-1))) > 0) {
						Order order = new Order(ex.getSymbol(), amount.multiply(BigDecimal.valueOf(-1)), ex.getTickNo());
						result.add(order);
						pos.orderCount(1);
					}
				}
			} else {
				// 逆張り
				if ("+a2".equals(ex.getStatus()) || "+a3".equals(ex.getStatus())) {
					if (nowAmt.compareTo(maxamount.multiply(BigDecimal.valueOf(-1))) > 0) {
						Order order = new Order(ex.getSymbol(), amount.multiply(BigDecimal.valueOf(-1)), ex.getTickNo());
						result.add(order);
						pos.orderCount(1);
					}
				}
				if ("-a2".equals(ex.getStatus()) || "-a3".equals(ex.getStatus())) {
					if (nowAmt.compareTo(maxamount) < 0) {
						Order order = new Order(ex.getSymbol(), amount, ex.getTickNo());
						result.add(order);
						pos.orderCount(1);
					}
				}
			}
		}
		BigDecimal mid = null;
		try { mid = BigDecimal.valueOf(ex.getMid()); } catch (Exception e) {}
		log.debug(ex.getSymbol()+"("+ ex.getTickNo().toPlainString() +") = result["+result.size()+"] : status="+ex.getStatus()+" statusCount="+ex.getStatusCount()+" nowAmt="+nowAmt+" "+pos.getPL(mid));
		return result;
	}
	
	public void addReport(Report report) {
		String key = report.getSymbol();
		Position pos = position.get(key);
		if (pos==null) {
			pos = new Position();
			position.put(key, pos);
		}
		pos.addReport(Report.getTrans(report));
		/*
		try {
			if (jedis == null || jedis.isConnected() == false) {
				pool = new JedisPool(new JedisPoolConfig(), "localhost");
				jedis = pool.getResource();
			}
			String val = gson.toJson(pos);
			jedis.set(key+":position",  val);
		} catch (Exception e) {
			log.error("write position: " + pos.toString(), e);
		}
		*/
		pos.orderCount(-1);
		log.info(pos.getPL(report.getAvgPx()));
	}

	public List<Order> finish() {
		//スクエア注文
		List<Order> result = new ArrayList<Order>();
		BigDecimal tickNo = BigDecimal.valueOf(9000000000L);
		for (Map.Entry<String, Position> entry : position.entrySet()) {
			tickNo = tickNo.add(BigDecimal.ONE);
			Order order = new Order(entry.getKey(), entry.getValue().getAmount().multiply(BigDecimal.valueOf(-1)), tickNo);
			result.add(order);
		}
		pool.close();
		return result;
	}

}
