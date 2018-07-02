package us.codecraft.webmagic;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.scheduler.Scheduler;

/**
 * 保存请求失败的url，并记录失败的次数
 * @ClassName: ErrorUrlMap 
 * @Description: 
 * @date 2018年5月31日 下午4:10:41 
 *
 */
public class ErrorUrlMap {
	private static Logger logger = LoggerFactory.getLogger(ErrorUrlMap.class);

	public static Map<String,Integer> errorUrlMap = new HashMap<String,Integer>();
	
	/**
	 * 根据请求状态，增加修改 errorUrlMap
	 * @param url
	 * @param status
	 * @return  返回false 时,需要回填到待爬取url列表中
	 */
	public static boolean handleErrorUrlMap(String url,String status) {
		Integer i = errorUrlMap.get(url);
		if("success".equals(status)) {
			//请求成功时，判断是否失败过，
			//如果失败过，而现在已经成功，则在map中删除该条记录
			if(i!=null) {
				errorUrlMap.remove(url);
			}
			return true;
		}else if("error".equals(status)){
			//请求失败时，判断是否失败过
			//没有失败过，失败次数置 1，否则加1，失败次数大于3，丢弃
			if(i==null) {
				errorUrlMap.put(url, 1);
				return false;
			}else if(i< 3){
				errorUrlMap.put(url, i + 1);
				logger.info("url : " + url + " 失败次数 " + i+1);
				return false;
			}
			
		}
		return true;
	}
	
	/**
	 * 回填出错的url
	 * @param requset
	 * @param task
	 */
	public static void rewriteErrorUrl(Request request,Task task) {
		logger.info("回填出错的url task_id = task.getUUID() = " + task.getUUID());
		//url 回填
    	Spider spider  = (Spider) RunningTaskMap.runningTaskMap.get(task.getUUID());
    	if(spider !=null) {
    		Scheduler scheduler = spider.getScheduler();
        	//设置使得该url可以重复访问 只要非空即可，不需要指定特殊值
        	request.putExtra(Request.CYCLE_TRIED_TIMES,"0");
        	scheduler.push(request, task);
        	logger.info("回填失败的url：" + request.getUrl());
    	}
	}
	
	
}
