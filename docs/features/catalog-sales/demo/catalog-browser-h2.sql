-- Disposable local H2 fixture for screenshots; never run against TiDB.
INSERT INTO roles(id,name) VALUES(1,'ROLE_ADMIN'),(2,'ROLE_VENDOR'),(3,'ROLE_USER'),(4,'ROLE_MANAGER');
INSERT INTO users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) VALUES(1,1,'catalog-demo','catalog@example.test','not-a-password-hash','Catalog Demo',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version) VALUES(1,1,REPEAT('d',64),CURRENT_TIMESTAMP,DATEADD('HOUR',2,CURRENT_TIMESTAMP),0);
INSERT INTO stores(id,owner_user_id,commission_policy_id,name,slug,address,phone,description,open_time,close_time,status,accepting_orders,created_at,updated_at,version) VALUES(1,1,1,'UTETra Thủ Đức','utetra-thu-duc','1 Võ Văn Ngân, Thủ Đức, TP.HCM','0901234567','Một góc trà quen, cho những cuộc hẹn mới.','00:00','23:59','ACTIVE',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO categories(id,name,slug,description,display_order,is_active,created_at,updated_at) VALUES(1,'Trà sữa','tra-sua','Trà thơm, sữa dịu.',0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,'Trà trái cây','tra-trai-cay','Tươi mát mỗi ngày.',1,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO toppings(id,name,price,is_active,created_at,updated_at) VALUES(1,'Trân châu đen',5000,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,'Thạch vải',7000,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO store_toppings(store_id,topping_id,is_available,created_at,updated_at) VALUES(1,1,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(1,2,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO products(id,store_id,category_id,name,slug,description,base_price,is_active,is_available,is_featured,created_at,updated_at,version) VALUES
(1,1,1,'Trà sữa trân châu','tra-sua-tran-chau','Vị trà đen đậm đà hòa cùng sữa thơm, thêm trân châu dai mềm.',30000,true,true,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
(2,1,1,'Ô long sữa','o-long-sua','Hương ô long rang dịu nhẹ, hậu vị ngọt thanh.',35000,true,true,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
(3,1,2,'Trà đào cam sả','tra-dao-cam-sa','Đào thơm, cam tươi và sả thanh mát.',38000,true,true,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),
(4,1,2,'Trà vải lài','tra-vai-lai','Hương lài nhẹ nhàng cùng vị vải ngọt dịu.',36000,true,false,false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO product_variants(id,product_id,size_name,extra_price,is_active,created_at,updated_at) VALUES(1,1,'S',0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(2,1,'M',5000,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(3,1,'L',10000,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(4,2,'M',0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(5,3,'M',0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(6,4,'M',0,true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO product_toppings(product_id,topping_id) VALUES(1,1),(1,2),(2,1),(3,2);
INSERT INTO store_delivery_areas(store_id,area_code,name,is_active,created_at,updated_at) VALUES(1,'THU_DUC','Khu vực Thủ Đức',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO promotions(id,store_id,created_by,scope,kind,code,name,percent,max_discount,min_order,usage_limit,per_user_limit,starts_at,ends_at,is_active,funded_by,created_at,updated_at,version) VALUES(1,1,1,'SHOP','PRODUCT_PERCENT','UTETRA10','Hẹn nhau một ly trà',10,30000,50000,100,1,DATEADD('DAY',-1,CURRENT_TIMESTAMP),DATEADD('DAY',7,CURRENT_TIMESTAMP),true,'SHOP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);

CREATE TABLE commission_policies(id bigint primary key,name varchar(100),rate_percent decimal(5,2),is_active boolean);
INSERT INTO commission_policies VALUES(1,'Demo policy',5,true),(2,'Inactive policy',7,false);
INSERT INTO users(id,role_id,username,email,password_hash,full_name,is_active,email_verified_at,created_at,updated_at,version) VALUES(2,2,'applicant','applicant@example.test','not-a-password','Applicant',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0),(3,4,'manager','manager@example.test','not-a-password','Manager',true,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO auth_sessions(id,user_id,refresh_token_hash,created_at,expires_at,version) VALUES(2,2,REPEAT('e',64),CURRENT_TIMESTAMP,DATEADD('HOUR',2,CURRENT_TIMESTAMP),0),(3,3,REPEAT('f',64),CURRENT_TIMESTAMP,DATEADD('HOUR',2,CURRENT_TIMESTAMP),0);
INSERT INTO stores(id,owner_user_id,name,slug,address,phone,open_time,close_time,status,accepting_orders,created_at,updated_at,version) VALUES(2,2,'Pending shop','pending-shop','Thu Duc','0901234567','08:00','22:00','PENDING_APPROVAL',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0);
INSERT INTO store_registration_requests(store_id,applicant_id,status,created_at,version) VALUES(2,2,'PENDING',CURRENT_TIMESTAMP,0);
INSERT INTO store_memberships(user_id,store_id,member_role,is_active,assigned_by,created_at,updated_at) VALUES(3,1,'MANAGER',true,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
