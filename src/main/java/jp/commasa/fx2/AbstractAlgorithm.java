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
import jp.commasa.fx2.dto.Report;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public abstract class AbstractAlgorithm implements Algorithm {

	protected Logger log = LogManager.getLogger(this.getClass());
	protected ResourceBundle bundle;
	protected Gson gson = new Gson();
	protected JedisPool pool = null;
	protected Jedis jedis = null;
	protected BigDecimal amount = BigDecimal.valueOf(5000);
	private Map<String, Position> position = new HashMap<String, Position>();
	private BigDecimal profit = BigDecimal.valueOf(1000);
	private BigDecimal loss = BigDecimal.valueOf(10000);
	private BigDecimal maxamount = BigDecimal.valueOf(30000);
	private int reportInterval = 0;

	protected abstract void init(); 
	protected abstract List<Order> runAlgorithm(Price p); 

	@Override
	public void init(ResourceBundle bundle) {
		pool = new JedisPool(new JedisPoolConfig(), "localhost");
		jedis = pool.getResource();
		this.bundle = bundle;
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
			String value = this.bundle.getString("profit");
			profit = new BigDecimal(value);
			value = this.bundle.getString("loss");
			loss = new BigDecimal(value);
		} catch(Exception e) {
			log.info("Parameter(profit/loss) is invalid.", e);
		}
		init();
	}

	@Override
	public List<Order> run(Price p) {
		if (p == null || p.getSymbol() == null ) return null;
		String symbol = p.getSymbol();
		String val = gson.toJson(p);
		jedis.publish(symbol, val);
		Position pos = position.get(symbol);
		if (pos == null) {
			pos = new Position();
			position.put(symbol, pos);
		}
		if (pos.getOrderCount() > 0) {
			log.info("order skip: order count = " + pos.getOrderCount()); 
			return null;
		}
		// 決済
		List<Order> result = new ArrayList<Order>();
		BigDecimal nowAmt = pos.getAmount();
		BigDecimal pl = pos.getCostPL(p);
		int plc = pl.compareTo(BigDecimal.ZERO);
		if ( plc < 0 && loss.compareTo(pl.abs()) < 0 && pos.getAmount().compareTo(BigDecimal.ZERO) != 0 ) {
			// 決済（損切）
			Order order = new Order(symbol, pos.getAmount().multiply(BigDecimal.valueOf(-1)), p.getTickNo());
			result.add(order);
			pos.orderCount(1);
			log.info("ORDER <CLOSE loss>: symbol="+symbol+"("+p.getTickNo().toPlainString()+") costpl="+pl+" nowAmt="+nowAmt);
		} else if ( plc > 0 && profit.compareTo(pl.abs()) < 0 && pos.getAmount().compareTo(BigDecimal.ZERO) != 0 ) {
			// 決済（利確）
			Order order = new Order(symbol, pos.getAmount().multiply(BigDecimal.valueOf(-1)), p.getTickNo());
			result.add(order);
			pos.orderCount(1);
			log.info("ORDER <CLOSE profit>: symbol="+symbol+"("+p.getTickNo().toPlainString()+") costpl="+pl+" nowAmt="+nowAmt);
		}
		// 新規
		if ( result.size() == 0 ) {
			result = runAlgorithm(p);
			if ( result != null && result.size() > 0 ) {
				if ( nowAmt.abs().compareTo(maxamount) < 0 ) {
					pos.orderCount(result.size());
					log.info("ORDER <OPEN["+result.size()+"]>: symbol="+symbol+"("+p.getTickNo().toPlainString()+") costpl="+pl+" nowAmt="+nowAmt);
				} else {
					result = null;
					log.info("ORDER <MAX AMOUNT>: symbol="+symbol+"("+p.getTickNo().toPlainString()+") costpl="+pl+" nowAmt="+nowAmt);
				}
			}
		}
		// PL通知
		this.reportInterval++;
		try {
			BigDecimal mid = BigDecimal.valueOf(p.getMid());
			if ( mid.compareTo(BigDecimal.ZERO) > 0 ) {
				String plString = symbol+": "+pos.toString(mid);
				jedis.publish("PL", plString);
				if ( this.reportInterval > 550 ) {
					log.info("REPORT : "+plString);
					this.reportInterval = 0;
				}
			}
		} catch (Exception e) {}
		return result;
	}

	@Override
	public void execReport(Report report) {
		String symbol = report.getSymbol();
		Position pos = position.get(symbol);
		if (pos==null) {
			pos = new Position();
			position.put(symbol, pos);
		}
		pos.addReport(Report.getTrans(report));
		pos.orderCount(-1);
		log.info("Execution : "+symbol+": "+pos.toString(report.getAvgPx()));
	}

	@Override
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
