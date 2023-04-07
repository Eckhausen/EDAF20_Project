package krusty;

import spark.Request;
import spark.Response;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;

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
		* LEFT JOIN orders ON orders.Order_id = pallets.Order_id
		* WHERE production_date >= ?
		* AND production_date <= ?
		* AND cookie = ?
		* ORDER BY production_date DESC;
		*
		* */

		/*String query = "SELECT Pallet_id AS id, " +
				"Cookie_Name AS cookie, " +
				"Production_date AS production_date, " +
				"Customer_name AS customer, " +
				"Blocked AS blocked " +
				"FROM pallets " +
				"LEFT JOIN orders ON " +
				"orders.Order_id = pallets.Order_id";*/

		String query = "SELECT Cookie_Name AS cookie, " +
				"Blocked AS blocked " +
				"FROM pallets " +
				"LEFT JOIN orders ON " +
				"orders.Order_id = pallets.Order_id " +
				"ORDER BY cookie ASC";

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
			values.add(req.queryParams("blocked").equals("yes") ? "yes" : "no");
		}

		//Bygg vidare på queryn om det finns where conditions
		if (whereClause.length() > 0) {
			query +=  whereClause.toString() + " ORDER BY production_date DESC";
		}

		try(Connection connection = connect()){
			PreparedStatement ps = connection.prepareStatement(query);

			for (int i = 0; i < values.size(); i++) {
				ps.setString(i + 1, values.get(i)); //setString metoden börjar på alltid på 1. Därav i+1
			}
			ResultSet rs = ps.executeQuery();
			json = Jsonizer.toJson(rs, "pallets");
		} catch(SQLException e){
			e.printStackTrace();
		}
		return json;
		//return "{\"pallets\":[]}";
	}


	// hjälpmetod som bygger vidare på queryn
	private void addCondition(StringBuilder whereClause, String condition) {
		if (whereClause.length() == 0) {
			whereClause.append(" WHERE ");
		} else {
			whereClause.append(" AND ");
		}
		whereClause.append(condition);
	}

	public String reset(Request req, Response res) throws SQLException {
		String[] resetTables = {"Customers", "Products", "Ingredients", "Recipes"};
		Connection connection = null;
		try {
			connection = connect();
			connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
			connection.setAutoCommit(false);

			dropAndCreateTables(connection);
			for (String table : resetTables) {
				initData(connection, table);
			}
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException rollbackException) {
					rollbackException.printStackTrace();
				}
			}
			return "{\"status\": \"error\"}";
		} finally {
			if (connection != null) {
				try {
					connection.setAutoCommit(true);
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return "{\"status\": \"ok\"}";
	}

	//Vi försökte med multiple statements i ett script men tyvärr gick det inte så bra
	//eftersom det inte finns en inbyggd metod i jdbc för detta.
	private void dropAndCreateTables(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("SET FOREIGN_KEY_CHECKS = 0");

			statement.execute("DROP TABLE IF EXISTS pallets, order_amount, orders, recipes, ingredients, " +
					"customers, products");

			statement.execute("CREATE TABLE products (Cookie_name VARCHAR(50) primary key)");

			statement.execute("CREATE TABLE customers (Customer_name VARCHAR(50), Address VARCHAR(50) not null, " +
					"primary key(Customer_Name))");

			statement.execute("CREATE TABLE orders (Order_id int auto_increment primary key, Order_date DATE not null, " +
					"Customer_name VARCHAR(50) not null, foreign key(Customer_Name) references customers(Customer_Name))");

			statement.execute("CREATE TABLE ingredients (Ingredient_name VARCHAR(50), Quantity int not null, " +
					"iUnit VARCHAR(50) not null, Arrival_date DATE not null, Last_order_amount int not null, " +
					"primary key(Ingredient_Name))");

			statement.execute("CREATE TABLE order_Amount (numPallets int not null, " +
					"Cookie_name VARCHAR(50) not null, Order_id INT not null, " +
					"primary key(Order_id, Cookie_name), foreign key(Order_id) " +
					"references orders(Order_id), foreign key(Cookie_name) references products(Cookie_name))");

			statement.execute("CREATE TABLE recipes (Cookie_name VARCHAR(50), Ingredient_name VARCHAR(50), " +
					"Amount int not null, rUnit VARCHAR(50) not null, primary key(Ingredient_name, Cookie_name), " +
					"foreign key(Ingredient_name) references ingredients(Ingredient_name), foreign key(Cookie_name) " +
					"references products(Cookie_name))");

			statement.execute("CREATE TABLE pallets (Pallet_id int auto_increment, Cookie_Name VARCHAR(50), " +
					"Production_date DATE not null, Delivery_date DATE not null, Blocked BOOLEAN not null, " +
					"Location VARCHAR(50) not null, " + "Order_id int, primary key(Pallet_id), foreign key(Order_id) " +
					"references orders(Order_id), " + "foreign key(Cookie_Name) references products(Cookie_Name))");

			statement.execute("SET FOREIGN_KEY_CHECKS = 1");
		}
	}

	private void initData(Connection connection, String table) throws SQLException {
			String data = readFile(table + ".sql");
			connection.createStatement().execute(data);
		}

	/** Reads a given file from disk and returns the content of the file as a string. */
		private String readFile(String file) {
			try {
				String path = "src/main/resources/" + file;
				return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "";
		}

		public String createPallet(Request req, Response res) {
			String cookieName = req.queryParams("cookie");
			if (cookieName == null || cookieName.isEmpty()) {
				return "{\"status\":\"error\"}";
			}

			String result = "{}";
			try (Connection connection = connect()) {
				// Start the transaction
				connection.setAutoCommit(false);

				try {
					// Check if the cookie exists
					String cookieExistsQuery = "SELECT * FROM products WHERE Cookie_name = ?";
					PreparedStatement cookieExistsPs = connection.prepareStatement(cookieExistsQuery);
					cookieExistsPs.setString(1, cookieName);
					ResultSet cookieExistsRs = cookieExistsPs.executeQuery();
					if(cookieExistsRs.next() == false){
						connection.rollback();
						return "{\"status\":\"unknown cookie\"}";
					}

					// Insert a new pallet and retrieve the generated id
					String insertPalletQuery = "INSERT INTO pallets (Cookie_Name, Production_date, Delivery_date, Blocked, Location) VALUES (?, DATE(NOW()), ?, ?, ?)";
					PreparedStatement insertPalletPs = connection.prepareStatement(insertPalletQuery, Statement.RETURN_GENERATED_KEYS);
					insertPalletPs.setString(1, cookieName);
					insertPalletPs.setString(2, "2023-05-08");
					insertPalletPs.setInt(3, 0);
					insertPalletPs.setString(4, "Test");


					int affectedRows = insertPalletPs.executeUpdate();
					ResultSet generatedKeys = insertPalletPs.getGeneratedKeys();
					int palletId = -1;

					if (generatedKeys.next()) {
						palletId = generatedKeys.getInt(1);
					}

					if (affectedRows > 0 && palletId != -1) {
						// Retrieve the recipe for the specified cookie
						String recipeQuery = "SELECT Ingredient_name, Amount FROM recipes WHERE Cookie_name = ?";
						PreparedStatement recipePs = connection.prepareStatement(recipeQuery);
						recipePs.setString(1, cookieName);
						ResultSet recipeRs = recipePs.executeQuery();

						// Update stock quantities of raw materials used in the recipe
						String updateStockQuery = "UPDATE ingredients SET Quantity = Quantity - ? WHERE Ingredient_name = ?";
						PreparedStatement updateStockPs = connection.prepareStatement(updateStockQuery);

						while (recipeRs.next()) {
							String ingredientName = recipeRs.getString("Ingredient_name");
							double amount = recipeRs.getDouble("Amount");

							updateStockPs.setDouble(1, amount);
							updateStockPs.setString(2, ingredientName);
							updateStockPs.executeUpdate();
						}

						// Commit the transaction
						connection.commit();
						result = "{\"status\":\"ok\", \"id\":" + palletId + "}";
					} else {
						// Something went wrong, rollback the transaction
						connection.rollback();
						result = "{\"status\":\"error\"}";
					}
				} catch (SQLException e) {
					// If any exception occurs, rollback the transaction
					connection.rollback();
					e.printStackTrace();
					result = "{\"status\":\"error\"}";
				} finally {
					// Set auto-commit mode back to true
					connection.setAutoCommit(true);
				}
			} catch (SQLException e) {
				e.printStackTrace();
				result = "{\"status\":\"error\"}";
			}
			return result;
		}
}
