package us.codecraft.webmagic.downloader.log;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * ElasticSearch 查询工具类
 * ElasticSearch Java Low Level REST Client
 * 
 * @author Wang P.P.
 * @date 2018-04-24 14:31
 */
public class EsLowLevelClient {
	public static final String[] nodes = { "192.168.75.159", "192.168.75.158" };

	/**
	 * 发送查询请求
	 * 
	 * @param reqMethod
	 *            请求方法 GET , POST , PUT , HEAD , DELETE
	 * @param endpoint
	 *            请求uri
	 * @param params
	 *            请求参数
	 * @param jsonString
	 *            请求的json数据
	 * @return
	 */
	public String sendEsRequest(String reqMethod, String endpoint, Map<String, String> params, String jsonString) {
		RestClient restClient = null;
		String queryResult = null;
		try {
			// 创建客户端
			restClient = RestClient.builder(new HttpHost(nodes[0], 9200, "http"), new HttpHost(nodes[1], 9200, "http"))
					.build();
			Response response = null;
			if (jsonString != null) {
				// 封装json数据
				HttpEntity entity = new NStringEntity(jsonString, ContentType.APPLICATION_JSON);
				// 发送请求
				response = restClient.performRequest(reqMethod, endpoint, params, entity);
			} else {
				response = restClient.performRequest(reqMethod, endpoint, params);
			}
			// 读取返回结果
			if (response != null && response.getEntity() != null) {
				queryResult = EntityUtils.toString(response.getEntity());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (restClient != null) {
				try {
					restClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return queryResult;
	}

	/**
	 * 向ElasticSearch 插入数据 id自动生成
	 * 
	 * @param endpoint
	 * @param jsonString
	 * @return 返回响应结果，出错时会包含错误字符串，成功返回插入的相关信息
	 */
	public String insertDocumentToEs(String endpoint, String jsonString) {
		EsLowLevelClient client = new EsLowLevelClient();
		Map<String, String> params = Collections.emptyMap();
		String str = client.sendEsRequest("POST", endpoint, params, jsonString);
		return str;
	}

	/**
	 * 向ElasticSearch 插入数据 id由自己载endpoint中指定，成功返回插入的相关信息
	 * 
	 * @param endpoint
	 * @param jsonString
	 * @return 返回响应结果 出错时会包含错误字符串
	 */
	public String insertDocumentToEsWithId(String endpoint, String jsonString) {
		EsLowLevelClient client = new EsLowLevelClient();
		Map<String, String> params = Collections.emptyMap();
		String str = client.sendEsRequest("PUT", endpoint, params, jsonString);
		return str;
	}

	/**
	 * 更新整个文档
	 * 
	 * @param endpoint
	 * @param jsonString
	 * @return
	 */
	public String updateEsDocument(String endpoint, String jsonString) {
		return insertDocumentToEsWithId(endpoint, jsonString);
	}

	/**
	 * 根据 id 查询文档，id在endpoint中指定
	 * 
	 * @param endpoint
	 * @param params
	 * @return
	 */
	public String getDocumentById(String endpoint, Map<String, String> params) {
		EsLowLevelClient client = new EsLowLevelClient();
		String str = client.sendEsRequest("GET", endpoint, params, "");
		return str;
	}

	/**
	 * 根据id删除文档
	 * 
	 * @param endpoint
	 * @param params
	 * @return
	 */
	public String deleteDocumentById(String endpoint) {
		EsLowLevelClient client = new EsLowLevelClient();
		Map<String, String> params = Collections.emptyMap();
		String str = client.sendEsRequest("DELETE", endpoint, params, "");
		return str;
	}

	/**
	 * 根据id检查文档是否存在 id在endpoint中指定
	 * 
	 * @param endpoint
	 * @return
	 */
	// public String checkDocumentIsExistOrNot(String endpoint) {
	// EsLowLevelClient client = new EsLowLevelClient();
	// Map<String, String> params = Collections.emptyMap();
	// String str = client.sendEsRequest("HEAD", endpoint, params, null);
	// return str;
	// }

	public static void main(String[] args) {
		System.out.println("start : " + System.currentTimeMillis());
		EsLowLevelClient client = new EsLowLevelClient();
		Map<String, String> params = Collections.singletonMap("pretty", "true");
		// String jsonString = "{" + "\"user\":\"kimchy\"," +
		// "\"postDate\":\"2013-01-30\","
		// + "\"message\":\"trying out Elasticsearch\"" + "}";
		// String str =
		// client.sendEsRequest("GET","/posts/doc/1",params,jsonString);

		String jsonString = "{\"query\":{\"match\":{\"about\":\"rock climbing\"}}}";
		String str = client.sendEsRequest("POST", "/megacorp/employee/_search", params, jsonString);
		if (str != null) {
			// System.out.println("#######: " + str);

			// 第一种方式
			JSONObject jsonObject = JSONObject.parseObject(str);
			JSONObject jsonObject01 = jsonObject.getJSONObject("hits");

			// System.out.println("##########01 " +
			// jsonObject01.toJSONString());

			JSONArray jsonArray = jsonObject01.getJSONArray("hits");

			for (int i = 0; i < jsonArray.size(); i++) {
				System.out
						.println("########03 : " + jsonArray.getJSONObject(i).getJSONObject("_source").toJSONString());
			}

		}
		System.out.println("end : " + System.currentTimeMillis());

		System.out.println();
		System.out.println();
		System.out.println();
		String temp01 = "{\"first_name\":\"Douglas\",\"last_name\":\"我爱北京天安门啊\",\"age\":35,\"about\":\"I like to build cabinets\",\"interests\":[\"forestry\"]}";

		// String str00 = client.insertDocumentToEs("/megacorp/employee",
		// temp01);
		// params.put("_source", "first_name,age");
		String str00 = client.getDocumentById("/megacorp/employee/HAat9mIB7RKOwombg1zm?_source=first_name,age", params);
		// String str00 =
		// client.checkDocumentIsExistOrNot("/megacorp/employee/HAat9mIB7RKOwombg1zm");

		System.out.println(str00);
	}

}