# Canton Zhou (CCZ180000)
# Griffin Davis (GCD180000)
# CS6360-002 Project Phase III
# Fall 2022

# PART D
# 1
CREATE VIEW annual_top_3_customers AS
SELECT C.First_name, C.Last_name, SUM(O.Subtotal) as Orders_total
FROM CUSTOMER C, ORDERS O, PAYMENT P
WHERE C.Customer_id=O.Customer_id 
AND O.Confirmation_number=P.Confirmation_number 
AND extract(year from P.Payment_time) = extract(year from CURRENT_TIMESTAMP)
GROUP BY C.First_name, C.Last_name
ORDER BY SUM(O.Subtotal) DESC
LIMIT 3;

# 2
CREATE VIEW top_restaurant_type AS
SELECT RT.Type, COUNT(O.Order_id) as Order_count
FROM RESTAURANT_TYPE RT, ORDERS O, PAYMENT P
WHERE RT.Name=O.Shop_name AND O.Confirmation_number=P.Confirmation_number
AND extract(year from P.Payment_time) = extract(year from CURRENT_TIMESTAMP)
GROUP BY RT.Type
ORDER BY COUNT(O.Order_id) DESC
LIMIT 1;

# 3
CREATE VIEW potential_silver_member AS
SELECT C.Customer_id, C.First_name, C.Middle_name, C.Last_name, 
C.Delivery_address, C.Phone_number, C.Joining_date, COUNT(O.Order_id) as Order_count
FROM CUSTOMER C, ORDERS O
WHERE NOT EXISTS (
	SELECT C.Customer_id
    FROM SILVER_CARD_MEMBER S
    WHERE C.Customer_id=S.Customer_id
) AND C.Customer_id=O.Customer_id
GROUP BY C.Customer_id, C.First_name, C.Middle_name, C.Last_name, 
C.Delivery_address, C.Phone_number, C.Joining_date
HAVING COUNT(O.Order_id) > 10;

# 4
CREATE VIEW best_area_manager AS
SELECT E.Employee_id, E.First_name, E.Middle_name, E.Last_name, E.Address, E.Phone_number, E.Date_of_birth, E.Gender, E.Meal_pass_id, A.Start_date, A.Area, COUNT(*) as Num_contracts
FROM EMPLOYEE E, AREA_MANAGER A, SHOP S
WHERE E.Employee_id = A.Employee_id AND E.Employee_id = S.Area_manager_id AND S.Start_date >= (CURDATE() - interval 1 year)
GROUP BY E.Employee_id, E.First_name, E.Middle_name, E.Last_name, E.Address, E.Phone_number, E.Date_of_birth, E.Gender, E.Meal_pass_id, A.Start_date, A.Area
HAVING COUNT(*) = (
	SELECT MAX(X.occurrence)
    FROM (
		SELECT E.Employee_id, E.First_name, E.Middle_name, E.Last_name, E.Address, E.Phone_number, E.Date_of_birth, E.Gender, E.Meal_pass_id, A.Start_date, A.Area, COUNT(*) as occurrence
		FROM EMPLOYEE E, AREA_MANAGER A, SHOP S
		WHERE E.Employee_id = A.Employee_id AND E.Employee_id = S.Area_manager_id AND S.Start_date >= (CURDATE() - interval 1 year)
		GROUP BY E.Employee_id, E.First_name, E.Middle_name, E.Last_name, E.Address, E.Phone_number, E.Date_of_birth, E.Gender, E.Meal_pass_id, A.Start_date, A.Area
    ) X
);

# 5
CREATE VIEW top_restaurants AS
SELECT RT.Type, RT.Name, COUNT(*) as Num_orders
FROM RESTAURANT_TYPE RT, ORDERS O, PAYMENT P
WHERE RT.Name = O.Shop_name AND O.Confirmation_number = P.Confirmation_number AND P.Payment_time >= (CURDATE() - interval 1 month)
GROUP BY RT.Type, RT.Name
HAVING COUNT(*) = (
	SELECT MAX(x.Num_orders) as Max_orders
	FROM (
		SELECT RTT.Name, RTT.Type, COUNT(*) as Num_orders
		FROM ORDERS O, PAYMENT P, RESTAURANT_TYPE RTT
		WHERE RT.Type = RTT.Type AND RTT.Name = O.Shop_name AND O.Confirmation_number = P.Confirmation_number AND P.Payment_time >= (CURDATE() - interval 1 month)
		GROUP BY RTT.Name, RTT.Type
	) x
	GROUP BY x.Type
);
