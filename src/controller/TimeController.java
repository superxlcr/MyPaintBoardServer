package controller;

import java.util.ArrayList;
import java.util.List;

/**
 * 计时控制器
 * @author superxlcr
 *
 */
public class TimeController {

	private static TimeController instance = null;
	
	public static TimeController getInstance() {
		if (instance == null) {
			instance = new TimeController();
		}
		return instance;
	}
	
	private List<Long> timeList;
	
	private TimeController() {
		timeList = new ArrayList<>();
	}
	
	/**
	 * 开始计时
	 * @return 计时编号
	 */
	public int begin() {
		int index = timeList.size();
		timeList.add(System.currentTimeMillis());
		return index;
	}
	
	/**
	 * 结束计时
	 * @param index 计时编号
	 * @return 耗费时间：xx分xx秒xx毫秒
	 */
	public String end(int index) {
		if (index < 0 || index >= timeList.size()) {
			return "";
		}
		long time = System.currentTimeMillis() - timeList.get(index);
		int ms = (int)(time % 100);
		int s = (int)(time / 100 % 60);
		int min = (int)(time / 100 / 60);
		return String.format("%dmin:%ds:%dms", min, s, ms);
	}
	
}
