package jp.commasa.fx2;

import java.util.List;
import java.util.ResourceBundle;

import jp.commasa.fx2.dto.Order;
import jp.commasa.fx2.dto.Price;
import jp.commasa.fx2.dto.Report;

public interface Algorithm {

	public void init(ResourceBundle bundle);

	public List<Order> run(Price p);

	public void execReport(Report report);

	public List<Order> finish();

}
