package us.codecraft.webmagic.downloader.htmlunit;

import java.util.Map;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;

import us.codecraft.webmagic.Site;

public class HtmlunitGenerator {

	public WebClient getClient(Site site) {
		return generateClient(site);
	}

	/**
	 * 获取htmlunit WebClient
	 * @param site
	 * @return
	 */
	private WebClient generateClient(Site site) {
		// WebClient webClient = new WebClient(BrowserVersion.FIREFOX_3_6);
		//创建一个webclient  
		WebClient webClient = new WebClient();
		// 启动JS  
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setTimeout(10000);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		//运行错误时，不抛出异常  
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		
		// SSL support //忽略ssl认证  
		// webClient.setUseInsecureSSL(true);
		//禁用Css，可避免自动二次请求CSS进行渲染  
		webClient.getOptions().setCssEnabled(false);
		// AJAX support  // 设置Ajax异步  
		//webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		
		webClient.getOptions().setRedirectEnabled(true);
		
		//webClient.waitForBackgroundJavaScript(10000);

     
		if (site.getUserAgent() != null) {
			webClient.addRequestHeader("User-Agent", site.getUserAgent());
		} else {
			webClient.addRequestHeader("User-Agent", "");
		}

		if (site.isUseGzip()) {
			webClient.addRequestHeader("Accept-Encoding", "gzip");
		}

		//设置cookie
		generateCookie(webClient, site);

		return webClient;

	}

	/**
	 * 设置cookie
	 * @param webClient
	 * @param site
	 */
	private void generateCookie(WebClient webClient, Site site) {
		if (site.isDisableCookieManagement()) {
			webClient.getCookieManager().setCookiesEnabled(false);
			return;
		}

		CookieManager cookieManager = webClient.getCookieManager();

		for (Map.Entry<String, String> cookieEntry : site.getCookies().entrySet()) {
			Cookie cookie = new Cookie(site.getDomain(), cookieEntry.getKey(), cookieEntry.getValue());
			cookieManager.addCookie(cookie);
		}

		for (Map.Entry<String, Map<String, String>> domainEntry : site.getAllCookies().entrySet()) {
			for (Map.Entry<String, String> cookieEntry : domainEntry.getValue().entrySet()) {
				Cookie cookie = new Cookie(domainEntry.getKey(), cookieEntry.getKey(), cookieEntry.getValue());
				cookieManager.addCookie(cookie);
			}
		}

	}

}
