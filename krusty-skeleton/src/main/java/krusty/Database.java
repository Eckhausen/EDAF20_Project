package krusty;

import spark.Request;
import spark.Response;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import static krusty.Jsonizer.toJson;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	//private static final String jdbcString = "jdbc:mysql://localhost/krusty";
	private static final String jdbcString = "jdbc:mysql://localhost:3306/krusty?" +
			"useSSL=false" +
			"&allowPublicKeyRetrieval=true" +
			"&serverTimezone=UTC" +
			"&database=krusty";

	// For use with MySQL or PostgreSQL
	private static final String jdbcUsername = "root";
	private static final String jdbcPassword = "password";

	public Connection connect() {
		// Connect to database here
		Connection connection = null;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(jdbcString, jdbcUsername, jdbcPassword);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}

	public String getCustomers(Request req, Response res) {
		String json = "";
		String query = "SELECT Customer_name AS name, Address AS address FROM customers";
		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "customers");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
	}

	public String getRawMaterials(Request req, Response res) {
		String json = "";
		String query = "SELECT Ingredient_name AS name, Quantity AS amount, iUnit as unit FROM ingredients";
		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "raw-materials");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
	}

	public String getCookies(Request req, Response res) {
		String json = "";
		String query = "SELECT Cookie_name AS name FROM products";
		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "cookies");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
		//return "{\"cookies\":[]}";
	}

	public String getRecipes(Request req, Response res) {
		String json = "";
		String query = "SELECT krusty.recipes.Cookie_name AS cookie, " +
				"krusty.recipes.Ingredient_name AS raw_material, " +
				"krusty.recipes.Amount as amount, " +
				"krusty.recipes.rUnit AS unit " +
				"FROM recipes";
		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "recipes");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
	}

	public String getPallets(Request req, Response res) {
		String json = "";
		//Inkommande GET URL:
		//from=2018-01-01&to=2020-01-01&cookie=Amneris
		//Resultat:
		/*
		* SELECT Pallet_id AS id, Cookie_Name AS cookie, Production_date AS production_date,
		* orders.Customer_name AS customer, Blocked AS blocked
		* FROM pallets
		* JOIN orders ON orders.Order_id = pallets.Order_id
		* WHERE production_date >= ?
		* AND production_date <= ?
		* AND cookie = ?
		* ORDER BY production_date DESC;
		*
		* */

		String query = "SELECT Pallet_id AS id, " +
				"Cookie_Name AS cookie, " +
				"Production_date AS production_date, " +
				"orders.Customer_name AS customer, " +
				"Blocked AS blocked " +
				"FROM pallets " +
				"JOIN orders ON " +
				"orders.Order_id = pallets.Order_id";

		ArrayList<String> values = new ArrayList<>();
		StringBuilder whereClause = new StringBuilder();

		if (req.queryParams("from") != null) {
			addCondition(whereClause, "production_date >= ?");
			values.add(req.queryParams("from"));
		}
		if (req.queryParams("to") != null) {
			addCondition(whereClause, "production_date <= ?");
			values.add(req.queryParams("to"));
		}
		if (req.queryParams("cookie") != null) {
			addCondition(whereClause, "cookie = ?");
			values.add(req.queryParams("cookie"));
		}
		if (req.queryParams("blocked") != null) {
			addCondition(whereClause, "blocked = ? ");
			values.add(req.queryParams("blocked").equals("yes") ? "1" : "0");
		}

		//Bygg vidare på queryn om det finns where conditions
		if (whereClause.length() > 0) {
			query +=  whereClause.toString() + " ORDER BY production_date DESC;";
		}

		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "pallets");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
		//return "{\"pallets\":[]}";
	}

	public String reset(Request req, Response res) {
		return "{}";
	}

	public String createPallet(Request req, Response res) {
		return "{}";
	}
}
