# Canton Zhou (CCZ180000)
# Griffin Davis (GCD180000)
# CS6360-002 Project Phase III
# Fall 2022

# PART E
# 1
SELECT E.First_name, E.Middle_name, E.Last_name, 
COUNT(D.Employee_id) as Deliverers_supervising
FROM EMPLOYEE E, DELIVERER D
WHERE E.Employee_id=D.Manager_id
GROUP BY E.First_name, E.Middle_name, E.Last_name
ORDER BY COUNT(D.Employee_id) DESC
LIMIT 1;

# 2
SELECT AVG(Order_count)
FROM potential_silver_member;

# 3
SELECT C.Customer_id, C.First_name, C.Middle_name, C.Last_name, 
C.Delivery_address, C.Phone_number, C.Joining_date
FROM CUSTOMER C
WHERE C.Customer_id IN
	( SELECT O.Customer_id
	  FROM ORDERS O, RESTAURANT_TYPE RT
	  WHERE O.Shop_name=RT.Name
      AND RT.Type=(SELECT Type FROM top_restaurant_type)
	);
    
# 4
# ASSUMING MONTH = 31 DAYS
SELECT C.Customer_id, C.First_name, C.Middle_name, C.Last_name, 
C.Delivery_address, C.Phone_number, C.Joining_date
FROM CUSTOMER C, SILVER_CARD_MEMBER S
WHERE C.Customer_id=S.Customer_id
AND DATEDIFF(C.Joining_date, S.Issuing_date)>=-31
AND DATEDIFF(C.Joining_date, S.Issuing_date)<0;

# 5
SELECT E.First_name, E.Middle_name, E.Last_name, COUNT(O.Order_id) as Order_count
FROM EMPLOYEE E, DELIVERER D, ORDERS O, VEHICLE V, PAYMENT P
WHERE E.Employee_id=D.Employee_id AND V.Deliverer_id=D.Employee_id
AND O.Plate_number=V.Plate_number AND O.Confirmation_number=P.Confirmation_number
AND extract(month from P.Payment_time) = extract(month from CURRENT_TIMESTAMP)
GROUP BY E.First_name, E.Middle_name, E.Last_name
ORDER BY COUNT(O.Order_id) DESC
LIMIT 1;

# 6
SELECT R.Name, COUNT(P.Code) as Promotion_count
FROM RESTAURANT R, PROMOTION P
WHERE R.Name=P.Shop_name
GROUP BY R.Name
ORDER BY COUNT(P.Code) DESC
LIMIT 1;

# 7
SELECT C.First_name, C.Middle_name, C.Last_name
FROM CUSTOMER C
WHERE NOT EXISTS
	(SELECT DISTINCT R.Name
	FROM RESTAURANT_TYPE R
    WHERE R.Type = 'Fast Food' AND 
          R.Name NOT IN
	(SELECT DISTINCT O.Shop_name
    FROM ORDERS O
    WHERE O.Customer_id = C.Customer_id));
    
# 8
SELECT R.Name, C.First_name, C.Last_name, O.Subtotal
FROM RESTAURANT R, ORDERS O, CUSTOMER C
WHERE O.Shop_name = R.Name AND C.Customer_id = O.Customer_id;

# 9
SELECT A.Area
FROM SHOP S, RESTAURANT R, AREA_MANAGER A
WHERE S.Name = R.Name AND A.Employee_id = S.Area_manager_id
GROUP BY A.Area
HAVING COUNT(*) = 
	(SELECT MAX(x.occurence)
    FROM (SELECT AA.Area, COUNT(*) as occurence
		  FROM SHOP SS, RESTAURANT RR, AREA_MANAGER AA
		  WHERE SS.Name = RR.Name AND AA.Employee_id = SS.Area_manager_id
		  GROUP BY AA.Area) x);
          
# 10
SELECT y.Name, S.Weekday, S.Open_time, S.Close_time
FROM SHOP_SCHEDULE S, (
	SELECT R.Name as Name
	FROM ORDERS O, RESTAURANT R, PAYMENT P
	WHERE O.Shop_name = R.Name AND O.Confirmation_number = P.Confirmation_number AND P.Payment_time >= (CURDATE() - interval 1 month)
	GROUP BY R.Name
	HAVING COUNT(*) =(
		SELECT MAX(x.occurence)
		FROM (
			SELECT RR.Name, COUNT(*) as occurence
			FROM ORDERS OO, RESTAURANT RR, PAYMENT PP
			WHERE OO.Shop_name = RR.Name AND OO.Confirmation_number = PP.Confirmation_number AND PP.Payment_time >= (CURDATE() - interval 1 month)
			GROUP BY RR.Name
		)x
	)
) y
WHERE y.Name = S.Name;

# 11
SELECT E.First_Name, E.Middle_name, E.Last_name
FROM EMPLOYEE E
WHERE E.Meal_pass_id != '';

# 12
SELECT P.Supermarket_name
FROM PRODUCT P
GROUP BY P.Supermarket_name
HAVING COUNT(*) = 
	(SELECT MAX(x.occurence)
    FROM (SELECT P.Supermarket_name, COUNT(*) as occurence
			FROM PRODUCT P
			GROUP BY P.Supermarket_name) x);
            
# 13
SELECT P.Product_name, P.Supermarket_name, P.Price
FROM PRODUCT P
ORDER BY P.Product_name
