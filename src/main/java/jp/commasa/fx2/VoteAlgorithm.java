package jp.commasa.fx2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Price;

public class VoteAlgorithm extends AbstractAlgorithm {

	private Map<String, List<Price>> history = new HashMap<String, List<Price>>();
	private int size = 100;
	private int ma1 = size;
	private int ma2 = size / 2;
	private int ma3 = size / 4;

	@Override
	protected void init() {
		try {
			String value = this.bundle.getString("size");
			size = Integer.valueOf(value);
			ma1 = size;
			ma2 = size / 2;
			ma3 = size / 4;
		} catch(Exception e) {
			log.info("Parameter(size) is invalid.", e);
		}
	}

	@Override
	protected List<Order> runAlgorithm(Price p) {
		String symbol = p.getSymbol();
		//レート処理
		if (history.get(symbol) == null) history.put(symbol, new ArrayList<Price>());
		List<Price> previous = history.get(symbol);
		previous.add(p);
		//保持対象外となったデータの削除
		while (previous.size()>size) {
			previous.remove(0);
		}
		if ( previous.size() < ma1 ) {
			return null;
		}
		double maBid1 = 0;
		double maAsk1 = 0;
		double maBid2 = 0;
		double maAsk2 = 0;
		double maBid3 = 0;
		double maAsk3 = 0;
		for ( int i=previous.size()-1; i>=0; i-- ) {
			maBid1 += previous.get(i).getBid();
			maAsk1 += previous.get(i).getAsk();
			if ( i >= previous.size()-ma2 ) {
				maBid2 += previous.get(i).getBid();
				maAsk2 += previous.get(i).getAsk();
			}
			if ( i >= previous.size()-ma3 ) {
				maBid3 += previous.get(i).getBid();
				maAsk3 += previous.get(i).getAsk();
			}
		}
		maBid1 = maBid1 / ma1;
		maAsk1 = maAsk1 / ma1;
		maBid2 = maBid2 / ma2;
		maAsk2 = maAsk2 / ma2;
		maBid3 = maBid3 / ma3;
		maAsk3 = maAsk3 / ma3;
		int voteBid1 = (maBid1 - p.getAsk() > 0 ? 1 : 0);
		int voteAsk1 = (maAsk1 - p.getBid() < 0 ? 1 : 0);
		int voteBid2 = (maBid2 - p.getAsk() > 0 ? 1 : 0);
		int voteAsk2 = (maAsk2 - p.getBid() < 0 ? 1 : 0);
		int voteBid3 = (maBid3 - p.getAsk() > 0 ? 1 : 0);
		int voteAsk3 = (maAsk3 - p.getBid() < 0 ? 1 : 0);
		List<Order> result = new ArrayList<Order>();
		if ( voteAsk1 + voteAsk2 + voteAsk3 > 1 ) {
			Order order = new Order(symbol, amount, p.getTickNo());
			result.add(order);
			log.info("vote ASK: "
					+" bid("+voteBid1+","+voteBid2+","+voteBid3+" : "+maBid1+","+maBid2+","+maBid3+")"
					+" ask("+voteAsk1+","+voteAsk2+","+voteAsk3+" : "+maAsk1+","+maAsk2+","+maAsk3+")"
					+" "+p.getBid()+" "+p.getAsk());
		}
		if ( voteBid1 + voteBid2 + voteBid3 > 1 ) {
			Order order = new Order(symbol, amount.multiply(BigDecimal.valueOf(-1)), p.getTickNo());
			result.add(order);
			log.info("vote BID: "
					+" bid("+voteBid1+","+voteBid2+","+voteBid3+" : "+maBid1+","+maBid2+","+maBid3+")"
					+" ask("+voteAsk1+","+voteAsk2+","+voteAsk3+" : "+maAsk1+","+maAsk2+","+maAsk3+")"
					+" "+p.getBid()+" "+p.getAsk());
		}
		if (result.size() > 1) {
			result = null;
			log.info("!!! vote BOTH: BUY & SELL !!!");
		}
		return result;
	}

}
