# Canton Zhou (CCZ180000)
# Griffin Davis (GCD180000)
# CS6360-002 Project Phase III
# Fall 2022

# Create schema

CREATE TABLE EMPLOYEE (
	Employee_id char(4) NOT NULL, 
    First_name varchar(255) NOT NULL, 
    Middle_name varchar(255), 
    Last_name varchar(255) NOT NULL,
    Address varchar(255) NOT NULL, 
    Phone_number varchar(255) NOT NULL, 
    Date_of_birth date NOT NULL, 
    Gender varchar(255), 
    Meal_pass_id int,
    PRIMARY KEY (Employee_id)
);

CREATE TABLE STAFF (
	Employee_id char(4) NOT NULL,
    Start_date date NOT NULL,
    PRIMARY KEY (Employee_id),
    FOREIGN KEY (Employee_id) REFERENCES EMPLOYEE(Employee_id)
);

CREATE TABLE AREA_MANAGER (
	Employee_id char(4) NOT NULL,
    Start_date date NOT NULL,
    Area varchar(255) NOT NULL,
    PRIMARY KEY (Employee_id),
    FOREIGN KEY (Employee_id) REFERENCES EMPLOYEE(Employee_id)
);

CREATE TABLE DELIVERER (
	Employee_id char(4) NOT NULL,
    Start_date date NOT NULL,
    Manager_id char(4) NOT NULL,
    PRIMARY KEY (Employee_id),
    FOREIGN KEY (Employee_id) REFERENCES EMPLOYEE(Employee_id),
    FOREIGN KEY (Manager_id) REFERENCES AREA_MANAGER(Employee_id)
);

CREATE TABLE VEHICLE (
	Plate_number varchar(255) NOT NULL,
    Deliverer_id char(4) NOT NULL,
    Make varchar(255) NOT NULL,
    Model varchar(255) NOT NULL,
    Color varchar(255) NOT NULL,
    PRIMARY KEY (Plate_number),
    FOREIGN KEY (Deliverer_id) REFERENCES DELIVERER(Employee_id)
);

CREATE TABLE PREMIUM_MEAL_PASS (
	Meal_pass_id int NOT NULL,
    Monthly_usage int NOT NULL,
    Effective_date date NOT NULL,
    Expiration_date date NOT NULL,
    PRIMARY KEY (Meal_pass_id)
);

ALTER TABLE EMPLOYEE ADD CONSTRAINT FOREIGN KEY (Meal_pass_id) REFERENCES PREMIUM_MEAL_PASS(Meal_pass_id);

CREATE TABLE CUSTOMER (
	Customer_id int NOT NULL,
    First_name varchar(255) NOT NULL,
    Middle_name varchar(255),
    Last_name varchar(255) NOT NULL,
    Delivery_address varchar(255) NOT NULL,
    Phone_number varchar(255) NOT NULL,
    Joining_date varchar(255) NOT NULL,
    Meal_pass_id int NOT NULL,
    PRIMARY KEY (Customer_id),
    FOREIGN KEY (Meal_pass_id) REFERENCES PREMIUM_MEAL_PASS(Meal_pass_id)
);

CREATE TABLE ORDERS (
	Order_id int NOT NULL,
    Subtotal decimal NOT NULL,
    Confirmation_number int NOT NULL,
    Customer_id int NOT NULL,
    Promotion_code int,
    Promotion_shop_name varchar(255),
    Shop_name varchar(255) NOT NULL,
    Plate_number varchar(255) NOT NULL,
    PRIMARY KEY (Order_id),
    FOREIGN KEY (Shop_name) REFERENCES SHOP(Name),
    FOREIGN KEY (Plate_number) REFERENCES VEHICLE(Plate_number),
    FOREIGN KEY (Customer_id) REFERENCES CUSTOMER(Customer_id),
    FOREIGN KEY (Promotion_shop_name, Promotion_code) REFERENCES PROMOTION(Shop_name, Code),
    FOREIGN KEY (Confirmation_number) REFERENCES PAYMENT(Confirmation_number)
);

ALTER TABLE EMPLOYEE ADD CONSTRAINT CHECK (REGEXP_LIKE(Employee_id, '^E[[:digit:]]{3}$'));

CREATE TABLE COMMENT (
	Customer_id int NOT NULL,
    Comment_id int NOT NULL,
    Rating_score int NOT NULL,
    Contents varchar(255) NOT NULL,
    CHECK (Rating_score >= 1 AND Rating_score <= 5),
    PRIMARY KEY (Customer_id, Comment_id),
    FOREIGN KEY (Customer_id) REFERENCES CUSTOMER(Customer_id)
);

CREATE TABLE SILVER_CARD_MEMBER (
	Customer_id int NOT NULL,
    Member_card_id int NOT NULL,
    Issuing_date date NOT NULL,
    PRIMARY KEY (Customer_id, Member_card_id),
    FOREIGN KEY (Customer_id) REFERENCES CUSTOMER (Customer_id)
);

CREATE TABLE ORDER_CONTENTS (
	Order_id int NOT NULL,
    Item varchar(255) NOT NULL,
    PRIMARY KEY (Order_id, Item),
    FOREIGN KEY (Order_id) REFERENCES ORDERS(Order_id)
);

CREATE TABLE SHOP (
    Name varchar(255) PRIMARY KEY,
    Phone_number varchar(255) not null,
    Address varchar(255) not null,
    Area_manager_id char(4),
    FOREIGN KEY (Area_manager_id) REFERENCES AREA_MANAGER(Employee_id)
);
 
CREATE  TABLE SHOP_SCHEDULE (
    Name varchar(255),
    Weekday ENUM('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'),
    Open_time TIME not null,
    Close_time TIME not null,
    PRIMARY KEY (Name, Weekday),
    FOREIGN KEY (Name) REFERENCES SHOP(NAME)
);
 
CREATE TABLE RESTAURANT (
    Name varchar(255) PRIMARY KEY,
    FOREIGN KEY (Name) REFERENCES SHOP(Name)
);
 
CREATE TABLE SUPERMARKET (
    Name varchar(255) PRIMARY KEY,
    FOREIGN KEY (Name) REFERENCES SHOP(Name)
);
 
CREATE TABLE RESTAURANT_TYPE (
    Name varchar(255),
    Type varchar(255),
    PRIMARY KEY (Name, Type),
    FOREIGN KEY (Name) REFERENCES RESTAURANT(Name)
);
 
CREATE TABLE PRODUCT (
    Supermarket_name varchar(255),
    Product_id varchar(255) not null,
    Price decimal(5,2) not null,
    Stock int not null,
    Product_name varchar(255) not null,
    PRIMARY KEY (Supermarket_name, Product_id),
    FOREIGN KEY (Supermarket_name) REFERENCES SUPERMARKET(Name),
    FOREIGN KEY (Product_name) REFERENCES PRODUCT_TYPE(Name)
);
 
CREATE TABLE PRODUCT_TYPE (
    Name varchar(255) PRIMARY KEY,
    Description varchar(255) not null
);
 
CREATE TABLE PROMOTION (
    Shop_name varchar(255),
    Code int,
    Description varchar(255),
    PRIMARY KEY (Shop_name, Code),
    FOREIGN KEY (Shop_name) REFERENCES SHOP(Name)
);
 
CREATE TABLE PAYMENT (
    Confirmation_number int PRIMARY KEY,
    Payment_type varchar(255) not null,
    Payment_time datetime not null
);
 
delimiter //
CREATE TRIGGER dob_check
    BEFORE INSERT ON EMPLOYEE
    FOR EACH ROW
BEGIN
IF (NEW.Date_of_birth > CURDATE() - interval 16 year) then
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'EMPLOYEE is not 16 years or older';
END IF;
END; //
delimiter ;