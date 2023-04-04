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
        		String[] resetTables = {"Customers", "Products", "Ingredients", "Recipes", "Pallets"};
        		setForeignKeyCheck(false);

        		for(String table : resetTables){
        			try (Connection connection = connect()){
        			Statement stmt = connection.createStatement();

        				// Truncate the table
        				String sql = "TRUNCATE TABLE " + table; //Resetar alla tables i stringvektorn
        				stmt.executeUpdate(sql);

						switch (table) {
							case "Customers":
								initData("Customers.sql");
								break;
							case "Products":
								initData("Products.sql");
								break;
							case "Recipes":
								initData("Recipes.sql");
								break;
							case "Ingredients":
								initData("Ingredients.sql");
								break;
						}

        			}catch(SQLException e){
                     			e.printStackTrace();
                     		}
        		}
        		setForeignKeyCheck(true);
        		return "{\n\t\"status\": \"ok\"\n}";
        	}

        private void initData(String file) throws SQLException{
                String data = readFile(file);
                try(Connection connection = connect()){
                    connection.createStatement().execute(data);
                } catch (SQLException e){
                    e.printStackTrace();
                }
            }

    	private void setForeignKeyCheck(boolean on) throws SQLException {
            // Create a statement object to execute the SQL query
           try(Connection connection = connect()) {
                   Statement statement = connection.createStatement();
            // Build the SQL query to set the FOREIGN_KEY_CHECKS value to 1 or 0
            String sql = "SET FOREIGN_KEY_CHECKS = " + (on ? "1" : "0") + ";";
            // Execute the SQL query to set the FOREIGN_KEY_CHECKS value
            statement.executeQuery(sql);
            // Close the statement object to release resources
            statement.close();
            }catch(SQLException e){
                e.printStackTrace();
            }
        }

        /** Reads a given file from disk and returns the content of the file as a string. */
        	private String readFile(String file) {
        		try {
        			String path = "src/main/resources/" + file;
        			return new String(Files.readAllBytes(Paths.get(path)));
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        		return "";
        	}

	public String createPallet(Request req, Response res) {
		//TEST#####################################
			String cookieName = req.queryParams("cookie");
			if (cookieName == null || cookieName.isEmpty()) {
				res.status(400);
				return "{\"status\":\"error\"}";
			}

			String result = "{}";
			try (Connection connection = connect()) {
				// Start the transaction
				connection.setAutoCommit(false);

				try {
					// Check if the cookie exists
					String cookieExistsQuery = "SELECT COUNT(*) as count FROM products WHERE Cookie_name = ?";
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
					insertPalletPs.setString(2, "2023-04-14");
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
