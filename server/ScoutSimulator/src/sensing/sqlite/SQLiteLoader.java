package sensing.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SQLiteLoader {
	
	Connection conn = null;
	private final String sqlite = "jdbc:sqlite:";
	
	private JsonParser parser;
	
	public SQLiteLoader(File dbFile) throws ClassNotFoundException{
		
		Class.forName("org.sqlite.JDBC");
		
		try {
			conn = DriverManager.getConnection(sqlite+dbFile.getAbsolutePath());
			parser = new JsonParser();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public List<JsonObject> fetchTableValues() throws SQLException{
		
		String value;
		Statement stmt = null;
		List<JsonObject> values = new ArrayList<JsonObject>();
		
		String sql = "SELECT value FROM data;";
		
		stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		
		while(rs.next()){
			value = rs.getString(1);
			values.add((JsonObject) parser.parse(value));
			//System.out.println(gson.toJson(value));
		}
		
		rs.close();
		stmt.close();
		
		return values;
	}
		
	public void close() throws SQLException{
		conn.close();
	}

}
