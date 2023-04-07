set foreign_key_checks = 0;

DROP TABLE IF EXISTS recipes;
DROP TABLE IF EXISTS ingredients;
DROP TABLE IF EXISTS pallets;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS order_Amount;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;

create table products(
Cookie_name VARCHAR(50) primary key);

create table customers(
Customer_name VARCHAR(50),
Address VARCHAR(50) not null,
primary key(Customer_Name));

create table orders(
Order_id int auto_increment primary key,
Order_date DATE not null,
Customer_name VARCHAR(50) not null,
foreign key(Customer_Name) references customers(Customer_Name));

create table ingredients (
Ingredient_name VARCHAR(50),
Quantity int not null,
iUnit VARCHAR(50) not null,
Arrival_date DATE not null,
Last_order_amount int not null,
primary key(Ingredient_Name));

create table order_Amount(
numPallets int not null,
Cookie_name VARCHAR(50) not null,
Order_id INT not null,
primary key(Order_id, Cookie_name),
foreign key(Order_id) references orders(Order_id),
foreign key(Cookie_name) references products(Cookie_name));

create table recipes (
Cookie_name VARCHAR(50),
Ingredient_name VARCHAR(50),
Amount int not null,
rUnit VARCHAR(50) not null,
primary key(Ingredient_name, Cookie_name),
foreign key(Ingredient_name) references ingredients(Ingredient_name),
foreign key(Cookie_name) references products(Cookie_name));

create table pallets(
Pallet_id int auto_increment,
Cookie_Name VARCHAR(50),
Production_date DATE not null,
Delivery_date DATE not null,
Blocked BOOLEAN not null,
Location VARCHAR(50) not null,
Order_id int,
primary key(Pallet_id),
foreign key(Order_id) references orders(Order_id),
foreign key(Cookie_Name) references products(Cookie_Name));

set foreign_key_checks = 1
