package jp.commasa.fx2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Price;
import jp.commasa.fx2.dto.PriceEx;

public class BwAlgorithm extends AbstractAlgorithm {

	private Map<String, List<PriceEx>> history = new HashMap<String, List<PriceEx>>();
	private int size = 100;
	private int movavg1 = 90;
	private int movavg2 = 30;
	private int bandwalk = 5;
	private int alpha1 = 1;
	private int alpha2 = 2;
	private int alpha3 = 3;
	private BigDecimal volatility = BigDecimal.valueOf(20);

	@Override
	protected void init() {
		try {
			String value = this.bundle.getString("size");
			size = Integer.valueOf(value);
		} catch (Exception e) {
			log.info("Parameter(size) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("movavg1");
			movavg1 = Integer.valueOf(value);
			value = this.bundle.getString("movavg2");
			movavg2 = Integer.valueOf(value);
		} catch (Exception e) {
			log.info("Parameter(movavg) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("bandwalk");
			bandwalk = Integer.valueOf(value);
		} catch (Exception e) {
			log.info("Parameter(bandwalk) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("alpha1");
			alpha1 = Integer.valueOf(value);
			value = this.bundle.getString("alpha2");
			alpha2 = Integer.valueOf(value);
			value = this.bundle.getString("alpha3");
			alpha3 = Integer.valueOf(value);
		} catch (Exception e) {
			log.info("Parameter(movavg) is invalid.", e);
		}
		try {
			String value = this.bundle.getString("volatility");
			volatility = new BigDecimal(value);
		} catch (Exception e) {
			log.info("Parameter(volatility) is invalid.", e);
		}
	}

	@Override
	protected List<Order> runAlgorithm(Price p) {
		// レート処理
		PriceEx ex = new PriceEx(p);
		PriceEx prev = null;
		boolean uniq = false;
		List<PriceEx> previous = history.get(p.getSymbol());
		if (previous == null) {
			previous = new ArrayList<PriceEx>();
			history.put(p.getSymbol(), previous);
		}
		if (previous.size() > 0)
			prev = previous.get(previous.size() - 1);
		if (prev != null) {
			if (p.getTickNo() != null && !p.getTickNo().equals(prev.getTickNo())) {
				previous.add(ex);
				uniq = true;
			}
		} else {
			previous.add(ex);
		}
		// 保持対象外となったデータの削除
		while (previous.size() > size) {
			previous.remove(0);
		}
		double scale = 10000;
		if (ex.getSymbol().endsWith("JPY"))
			scale = 100;
		// ボリンジャーバンドもどき
		int ma1len = movavg1;
		double v1 = 0;
		if (previous.size() > movavg1) {
			int st = previous.size() - movavg1;
			double sum = 0;
			for (int i = st; i < previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += previous.get(i).getMid();
					if (i > 0)
						v1 += Math.abs(previous.get(i).getMid() - previous.get(i - 1).getMid());
				} else {
					ma1len--;
				}
			}
			if (ma1len > 0) {
				ex.setMovAvg1(sum / ma1len);
			} else {
				ex.setMovAvg1(0D);
			}
			ex.setVolatility1(v1 * scale);
			sum = 0;
			for (int i = st; i < previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += Math.pow(previous.get(i).getMid() - ex.getMovAvg1(), 2);
				}
			}
			if (ma1len > 0) {
				ex.setAlpha1(sum / ma1len);
			} else {
				ex.setAlpha1(0D);
			}
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
			for (int i = st; i < previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += previous.get(i).getMid();
					if (i > 0)
						v2 += Math.abs(previous.get(i).getMid() - previous.get(i - 1).getMid());
				} else {
					ma2len--;
				}
			}
			if (ma2len > 0) {
				ex.setMovAvg2(sum / ma2len);
			} else {
				ex.setMovAvg2(0D);
			}
			ex.setVolatility2(v2 * scale);
			sum = 0;
			for (int i = st; i < previous.size(); i++) {
				if (previous.get(i).getAsk() > 0 && previous.get(i).getBid() > 0) {
					sum += Math.pow(previous.get(i).getMid() - ex.getMovAvg2(), 2);
				}
			}
			if (ma2len > 0) {
				ex.setAlpha2(sum / ma2len);
			} else {
				ex.setAlpha2(0D);
			}
		} else {
			ex.setMovAvg2(0D);
			ex.setAlpha2(0D);
			ex.setVolatility2(0D);
		}
		// status
		if (ex.getMovAvg1() < 1) {
			ex.setStatus1("");
		} else {
			double mid = ex.getMid();
			ex.setStatus1("MA");
			if (mid > (ex.getMovAvg1() + alpha1 * ex.getAlpha1())) {
				ex.setStatus1("+a1");
				if (mid > (ex.getMovAvg1() + alpha2 * ex.getAlpha1())) {
					ex.setStatus1("+a2");
					if (mid > (ex.getMovAvg1() + alpha3 * ex.getAlpha1())) {
						ex.setStatus1("+a3");
					}
				}
			}
			if (mid < (ex.getMovAvg1() - alpha1 * ex.getAlpha1())) {
				ex.setStatus1("-a1");
				if (mid < (ex.getMovAvg1() - alpha2 * ex.getAlpha1())) {
					ex.setStatus1("-a2");
					if (mid < (ex.getMovAvg1() - alpha3 * ex.getAlpha1())) {
						ex.setStatus1("-a3");
					}
				}
			}
		}
		if (ex.getMovAvg2() < 1) {
			ex.setStatus2("");
		} else {
			double mid = ex.getMid();
			ex.setStatus2("MA");
			if (mid > (ex.getMovAvg2() + alpha1 * ex.getAlpha2())) {
				ex.setStatus2("+a1");
				if (mid > (ex.getMovAvg2() + alpha2 * ex.getAlpha2())) {
					ex.setStatus2("+a2");
					if (mid > (ex.getMovAvg2() + alpha3 * ex.getAlpha2())) {
						ex.setStatus2("+a3");
					}
				}
			}
			if (mid < (ex.getMovAvg2() - alpha1 * ex.getAlpha2())) {
				ex.setStatus2("-a1");
				if (mid < (ex.getMovAvg2() - alpha2 * ex.getAlpha2())) {
					ex.setStatus2("-a2");
					if (mid < (ex.getMovAvg2() - alpha3 * ex.getAlpha2())) {
						ex.setStatus2("-a3");
					}
				}
			}
		}
		int cnt1 = 0;
		int cnt2 = 0;
		boolean isBreak1 = false;
		boolean isBreak2 = false;
		String status1 = ex.getStatus1();
		String status2 = ex.getStatus2();
		for (int i = previous.size() - 1; i >= 0; i--) {
			if (status1.equals(previous.get(i).getStatus1())) {
				cnt1++;
			} else {
				isBreak1 = true;
			}
			if (status2.equals(previous.get(i).getStatus2())) {
				cnt2++;
			} else {
				isBreak2 = true;
			}
			if (isBreak1 && isBreak2)
				break;
		}
		ex.setStatusCount1(cnt1);
		ex.setStatusCount2(cnt2);

		// 書き込み
		String key = p.getSymbol();
		String val = gson.toJson(ex);
		jedis.set(key, val);
		jedis.publish("RATE", val);
		String valHist = gson.toJson(previous);
		jedis.set(key + ":history", valHist);

		// 注文
		List<Order> result = new ArrayList<Order>();
		String optmsg = "";
		if (uniq && ex.getVolatility1() >= volatility.doubleValue()) {
			// 新規
			if (ex.getStatusCount1() >= bandwalk /*
													 * && ex.getStatusCount2() >=
													 * bandwalk
													 */ ) {
				// バンドウォーク順張り or 逆張り
				if (("+a2".equals(ex.getStatus1()) && ex.getStatus2().startsWith("+"))
						|| ("-a2".equals(ex.getStatus1()) && ex.getStatus2().startsWith("+"))) {
					Order order = new Order(ex.getSymbol(), amount, ex.getTickNo());
					result.add(order);
					optmsg = " <OPEN long>";
				}
				// バンドウォーク順張り or 逆張り
				if (("-a2".equals(ex.getStatus1()) && ex.getStatus2().startsWith("-"))
						|| ("+a2".equals(ex.getStatus1()) && ex.getStatus2().startsWith("-"))) {
					Order order = new Order(ex.getSymbol(), amount.multiply(BigDecimal.valueOf(-1)), ex.getTickNo());
					result.add(order);
					optmsg = " <OPEN short>";
				}
			} else {
				// 跳ね 順張り
				if ("+a3".equals(ex.getStatus1())) {
					Order order = new Order(ex.getSymbol(), amount, ex.getTickNo());
					result.add(order);
					optmsg = " <OPEN long>";
				}
				// 跳ね 順張り
				if ("-a3".equals(ex.getStatus1())) {
					Order order = new Order(ex.getSymbol(), amount.multiply(BigDecimal.valueOf(-1)), ex.getTickNo());
					result.add(order);
					optmsg = " <OPEN short>";
				}
			}
		}
		log.info(ex.getSymbol() + "(" + ex.getTickNo().toPlainString() + ") = result[" + result.size() + "] : status1="
				+ ex.getStatus1() + " count1=" + ex.getStatusCount1() + " status2=" + ex.getStatus2() + " count2="
				+ ex.getStatusCount2() + optmsg);
		return result;
	}

}
