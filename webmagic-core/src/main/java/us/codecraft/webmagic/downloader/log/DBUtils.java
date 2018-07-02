package us.codecraft.webmagic.downloader.log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils {
	private static Logger logger = LoggerFactory.getLogger(DBUtils.class);
	
	private static String driverName;
	private static String url;
	private static String username;
	private static String password;
	private static String maxActive;
	private static String maxWait;

	private static BasicDataSource ds;

	static {
		InputStream in = null;
		try {
			// 加载属性文件
			/*
			 * java.util.Properties 该类可以读取后缀为.properties的 配置文件，并解析文件中的数据，将以 map的形式表示。
			 * 文件中"="左为key,右为value
			 */
			String path = Thread.currentThread().getContextClassLoader().getResource("/").getPath();
			Properties prop = new Properties();
			prop.load(new FileInputStream(path + "crw.properties"));

			driverName = prop.getProperty("jdbc.driver");
			url = prop.getProperty("jdbc.url");
			username = prop.getProperty("jdbc.username");
			password = prop.getProperty("jdbc.password");
			maxActive = prop.getProperty("ds.maxActive");
			maxWait = prop.getProperty("ds.maxWait");

			// 实例化连接池
			ds = new BasicDataSource();
			// 设置连接池参数
			ds.setDriverClassName(driverName);
			ds.setUrl(url);
			ds.setUsername(username);
			ds.setPassword(password);
			// 设置最大连接数
			ds.setMaxActive(Integer.valueOf(maxActive));
			// 设置最大等待时间
			ds.setMaxWait(Long.valueOf(maxWait));

		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// e.printStackTrace();
					logger.info(e.getMessage());
				}
			}
		}
	}
	
	public static Connection getConnection() {
		Connection conn = null;
		try {
			// 从连接池中获取连接
			conn = ds.getConnection();
		} catch (Exception e) {
			// 日志
			e.printStackTrace();
		}
		return conn;
	}

	public static void closeConnection(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws SQLException {
		System.out.println(getConnection());
	}

	
}
