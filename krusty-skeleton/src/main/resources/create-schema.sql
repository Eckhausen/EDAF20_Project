create table customers
(
    Customer_name varchar(50) not null
        primary key,
    Address       varchar(50) not null
);

create table ingredients
(
    Ingredient_name   varchar(50) not null
        primary key,
    Quantity          int         not null,
    iUnit             varchar(50) not null,
    Arrival_date      date        not null,
    Last_order_amount int         not null
);

create table orders
(
    Order_id      int auto_increment
        primary key,
    Order_date    date        not null,
    Customer_name varchar(50) not null,
        foreign key (Customer_name) references customers (Customer_name)
);

create table products
(
    Cookie_name varchar(50) not null
        primary key
);

create table order_amount
(
    numPallets  int         not null,
    Cookie_name varchar(50) not null,
    Order_id    int         not null,
    primary key (Order_id, Cookie_name),
        foreign key (Order_id) references orders (Order_id),
        foreign key (Cookie_name) references products (Cookie_name)
);

create table pallets
(
    Pallet_id       int auto_increment
        primary key,
    Cookie_Name     varchar(50) null,
    Production_date date        not null,
    Delivery_date   date        not null,
    Blocked         varchar(3)  not null,
    Location        varchar(50) not null,
    Order_id        int         null,
        foreign key (Order_id) references orders (Order_id),
        foreign key (Cookie_Name) references products (Cookie_name)
);

create table recipes
(
    Cookie_name     varchar(50) not null,
    Ingredient_name varchar(50) not null,
    Amount          int         not null,
    rUnit           varchar(50) not null,
    primary key (Ingredient_name, Cookie_name),
        foreign key (Ingredient_name) references ingredients (Ingredient_name),
        foreign key (Cookie_name) references products (Cookie_name)
);

