package us.codecraft.webmagic.downloader.htmlunit;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

import us.codecraft.webmagic.ErrorUrlMap;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.downloader.log.EsLowLevelClient;
import us.codecraft.webmagic.downloader.log.SaveLogMysql;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.ProxyProvider;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.CharsetUtils;

public class HtmlUnitDownloader extends AbstractDownloader {
	private transient Logger logger = LoggerFactory.getLogger(getClass());

	// htmlunit 生成方法
	private HtmlunitGenerator htmlunitGenerator = new HtmlunitGenerator();
	// 保存html unit WebClient 客户端
	private final Map<String, WebClient> htmlunitClients = new HashMap<String, WebClient>();

	private ProxyProvider proxyProvider;

	private boolean responseHeader = true;

	public void setProxyProvider(ProxyProvider proxyProvider) {
		this.proxyProvider = proxyProvider;
	}

	@Override
	public Page download(Request request, Task task) {
		if (task == null || task.getSite() == null) {
			throw new NullPointerException("task or site can not be null");
		}

		WebClient htmlunitClient = getHtmlunitClient(task.getSite());
		
		
		WebClient webClient = new WebClient();
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		// webClient.setUseInsecureSSL(false);
		webClient.getOptions().setCssEnabled(false);
		// webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		// webClient.setTimeout(Integer.MAX_VALUE);
		// webClient.setThrowExceptionOnScriptError(false);
		// HtmlPage rootPage =
		// webClient.getPage("https://item.jd.com/6655831.html#crumb-wrap");

		// webClient.setJavaScriptEnabled(true);
		webClient.getOptions().setCssEnabled(false);
		
		Proxy proxy = proxyProvider != null ? proxyProvider.getProxy(task) : null;
		// 设置代理
		ProxyConfig proxyConfig = htmlunitClient.getOptions().getProxyConfig();
		if (proxy != null) {
			proxyConfig.setProxyHost(proxy.getHost());
			proxyConfig.setProxyPort(proxy.getPort());
		}


		Page page = Page.fail();

		try {
			
			HtmlPage rootPage = webClient.getPage(request.getUrl());
			//在webClient.getPage 之后设置有效
			webClient.waitForBackgroundJavaScript(10*1000);  
			
			//HtmlPage rootPage = htmlunitClient.getPage(request.getUrl());
			WebResponse htmlunitRes = rootPage.getWebResponse();
			
			page = new Page();
			
			page.setRawText(rootPage.asXml());
			
			page.setBytes(rootPage.asXml().getBytes());
			if (!request.isBinaryContent()) {
				page.setCharset("utf-8");
			}
			
			
			
			page.setUrl(new PlainText(request.getUrl()));
			page.setRequest(request);
			
			page.setStatusCode(htmlunitRes.getStatusCode());
			page.setDownloadSuccess(true);
			
			if (responseHeader) {
				htmlunitRes.getResponseHeaders();
				page.setHeaders(convertHeaders(htmlunitRes.getResponseHeaders()));
			}
			
			onSuccess(request);
			
			 //记录日志
            logger.info("downloading page success {}", request.getUrl());
            
            //######################ELASTICSERCH##########################
            EsLowLevelClient es = new EsLowLevelClient();
            Map<String, String> params = Collections.singletonMap("pretty", "true");
            String jsonString = "{\"url\": \"" + request.getUrl() + "\",\"status\": \"success\",\"time\": \"" + new Date().toString() + "\"}";
          //  es.sendEsRequest("POST", "/webmagic/log", params, jsonString);
            
            //如果url失败过，现在成功了，需要从失败列表中删除该url
            ErrorUrlMap.handleErrorUrlMap(request.getUrl(), "success");
            
            String title = page.getHtml().xpath("/html/head/title/text()").get();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SaveLogMysql.saveLogs(task.getUUID(),request.getUrl(), title, "0", sdf.format(new Date()));
            
			return page;
			
			
		} catch (Exception e) {
			
			// 记录日志
			logger.warn("download page {} error", request.getUrl(), e);
			// ######################ELASTICSERCH##########################
			EsLowLevelClient es = new EsLowLevelClient();
			Map<String, String> params = Collections.singletonMap("pretty", "true");
			String jsonString = "{\"url\": \"" + request.getUrl() + "\",\"status\": \"error\",\"time\": \""
					+ new Date().toString() + "\"}";
			//es.sendEsRequest("POST", "/webmagic/log", params, jsonString);
			
			 //请求失败，记录该url
            boolean flag = ErrorUrlMap.handleErrorUrlMap(request.getUrl(), "error");
            if(!flag) {
            	ErrorUrlMap.rewriteErrorUrl(request,task);
            }
			
            String title = page.getHtml().xpath("/html/head/title/text()").get();
            
			//存入mysql
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SaveLogMysql.saveLogs(task.getUUID(),request.getUrl(), title, "1", sdf.format(new Date()));

			onError(request);
			return page;
		} finally {
			if(htmlunitClient!=null) {
				//htmlunitClient.close();
			}
			if(webClient!=null) {
				webClient.close();
			}
			if (proxyProvider != null && proxy != null) {
                proxyProvider.returnProxy(proxy, page, task);
            }
		}


	}

	@Override
	public void setThread(int threadNum) {
		// TODO Auto-generated method stub

	}

	private WebClient getHtmlunitClient(Site site) {
		if (site == null) {
			return htmlunitGenerator.getClient(null);
		}
		String domain = site.getDomain();
		WebClient htmlunitClient = htmlunitClients.get(domain);

		if (htmlunitClient == null) {
			synchronized (this) {
				htmlunitClient = htmlunitClients.get(domain);
				if (htmlunitClient == null) {
					htmlunitClient = htmlunitGenerator.getClient(site);
					htmlunitClients.put(domain, htmlunitClient);
				}
			}
		}
		return htmlunitClient;

	}

	protected Page handleResponse(Request request, String charset, WebResponse htmlunitRes, Task task)
			throws IOException {

		byte[] bytes = IOUtils.toByteArray(htmlunitRes.getContentAsStream());
		String contentType = htmlunitRes.getContentType() == null ? "" : htmlunitRes.getContentType();

		Page page = new Page();
		page.setBytes(bytes);
		if (!request.isBinaryContent()) {
			if (charset == null) {
				charset = getHtmlCharset(contentType, bytes);
			}
			page.setCharset(charset);
			page.setRawText(new String(bytes, charset));
			
		}
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		page.setStatusCode(htmlunitRes.getStatusCode());
		page.setDownloadSuccess(true);
		if (responseHeader) {
			htmlunitRes.getResponseHeaders();
			page.setHeaders(convertHeaders(htmlunitRes.getResponseHeaders()));
		}
		return page;

	}

	private String getHtmlCharset(String contentType, byte[] contentBytes) throws IOException {
		String charset = CharsetUtils.detectCharset(contentType, contentBytes);
		if (charset == null) {
			charset = Charset.defaultCharset().name();
			logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()",
					Charset.defaultCharset());
		}
		return charset;
	}

	private Map<String, List<String>> convertHeaders(List<NameValuePair> headers) {
		Map<String, List<String>> results = new HashMap<String, List<String>>();
		for (NameValuePair header : headers) {
			List<String> list = results.get(header.getName());
			if (list == null) {
				list = new ArrayList<String>();
				results.put(header.getName(), list);
			}
			list.add(header.getValue());
		}
		return results;
	}

}
