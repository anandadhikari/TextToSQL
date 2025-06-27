-- USERS
INSERT INTO users (username, email, password_hash, first_name, last_name, created_at)
VALUES ('ianmcbride', 'john95@example.net', '3d5e17f236fc5a52efda7...', 'John', 'Smith', NOW());
INSERT INTO users (username, email, password_hash, first_name, last_name, created_at)
VALUES ('lisabrown', 'jane88@example.org', '40a3c97f5362cf4323b2d...', 'Lisa', 'Brown', NOW());
INSERT INTO users (username, email, password_hash, first_name, last_name, created_at)
VALUES ('samdoe', 'sam88@example.org', '9cbad7154b8fe6175ce7c...', 'Sam', 'Doe', NOW());
INSERT INTO users (username, email, password_hash, first_name, last_name, created_at)
VALUES ('emilywhite', 'emily88@example.com', '1b0e7c5a6aaf8e17bb3aa...', 'Emily', 'White', NOW());
INSERT INTO users (username, email, password_hash, first_name, last_name, created_at)
VALUES ('robertgray', 'robgray@example.com', '6c2bd9c4ec50e50d4903c...', 'Robert', 'Gray', NOW());

-- CATEGORIES
INSERT INTO categories (name, description) VALUES ('Books', 'Various categories of books.');
INSERT INTO categories (name, description) VALUES ('Electronics', 'Phones, tablets, and laptops.');
INSERT INTO categories (name, description) VALUES ('Fashion', 'Clothing and accessories.');

-- PRODUCTS
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Notebook', 'High-quality paper notebook', 59.99, 100, 1);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Smartphone', 'Latest Android flagship', 699.99, 40, 2);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Bluetooth Speaker', 'Portable wireless speaker', 129.99, 25, 2);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Backpack', 'Ergonomic, waterproof backpack', 89.99, 50, 3);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Java Book', 'Complete guide to Java', 39.99, 75, 1);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Jeans', 'Slim fit denim jeans', 49.99, 60, 3);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Tablet', '10-inch display tablet', 349.99, 35, 2);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('T-shirt', 'Cotton unisex T-shirt', 19.99, 100, 3);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Laptop', 'Lightweight laptop with SSD', 999.99, 20, 2);
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('Pen Set', 'Premium writing instruments', 24.99, 200, 1);

-- ORDERS
INSERT INTO orders (user_id, total_amount, shipping_address, payment_method)
VALUES (1, 1399.98, '123 Main St, Mumbai', 'credit_card');
INSERT INTO orders (user_id, total_amount, shipping_address, payment_method)
VALUES (3, 749.99, '456 Elm St, Delhi', 'credit_card');
INSERT INTO orders (user_id, total_amount, shipping_address, payment_method)
VALUES (5, 229.99, '789 Oak St, Bangalore', 'credit_card');

-- PAYMENTS
INSERT INTO payments (order_id, amount, payment_method, transaction_id, status)
VALUES (1, 1399.98, 'credit_card', 'TXN12345678', 'completed');
INSERT INTO payments (order_id, amount, payment_method, transaction_id, status)
VALUES (2, 749.99, 'credit_card', 'TXN23456789', 'completed');
INSERT INTO payments (order_id, amount, payment_method, transaction_id, status)
VALUES (3, 229.99, 'credit_card', 'TXN34567890', 'completed');

-- SHIPPING
INSERT INTO shipping (order_id, carrier, status)
VALUES (1, 'FedEx', 'shipped');
INSERT INTO shipping (order_id, carrier, status)
VALUES (2, 'DHL', 'delivered');
INSERT INTO shipping (order_id, carrier, status)
VALUES (3, 'BlueDart', 'in_transit');

-- DISCOUNTS
INSERT INTO discounts (code, description, discount_type, discount_value, valid_from, valid_to)
VALUES ('SAVE10', '10% off on total order', 'percentage', 10.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));
INSERT INTO discounts (code, description, discount_type, discount_value, valid_from, valid_to)
VALUES ('FLAT50', 'Flat Rs.50 off', 'fixed_amount', 50.00, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- ORDER_DISCOUNTS
INSERT INTO order_discounts (order_id, discount_id, applied_value)
VALUES (1, 1, 139.99);
INSERT INTO order_discounts (order_id, discount_id, applied_value)
VALUES (2, 2, 50.00);

-- WISHLISTS
INSERT INTO wishlists (user_id, product_id)
VALUES (2, 1);
INSERT INTO wishlists (user_id, product_id)
VALUES (2, 5);
INSERT INTO wishlists (user_id, product_id)
VALUES (4, 3);
INSERT INTO wishlists (user_id, product_id)
VALUES (1, 7);
INSERT INTO wishlists (user_id, product_id)
VALUES (3, 2);

-- INVENTORY_LOG
INSERT INTO inventory_log (product_id, quantity_change, previous_quantity, new_quantity, change_type, reference_id)
VALUES (1, -1, 101, 100, 'purchase', 1);
INSERT INTO inventory_log (product_id, quantity_change, previous_quantity, new_quantity, change_type, reference_id)
VALUES (2, -2, 42, 40, 'purchase', 1);
INSERT INTO inventory_log (product_id, quantity_change, previous_quantity, new_quantity, change_type, reference_id)
VALUES (5, -1, 76, 75, 'purchase', 2);
INSERT INTO inventory_log (product_id, quantity_change, previous_quantity, new_quantity, change_type, reference_id)
VALUES (7, -1, 36, 35, 'purchase', 2);
INSERT INTO inventory_log (product_id, quantity_change, previous_quantity, new_quantity, change_type, reference_id)
VALUES (3, -1, 26, 25, 'purchase', 3);

-- USER_ADDRESSES
INSERT INTO user_addresses (user_id, address_type, street_address1, city, state, postal_code, country)
VALUES (1, 'home', '123 Main St', 'Mumbai', 'MH', '400001', 'India');
INSERT INTO user_addresses (user_id, address_type, street_address1, city, state, postal_code, country)
VALUES (2, 'home', '456 Elm St', 'Delhi', 'DL', '110001', 'India');
INSERT INTO user_addresses (user_id, address_type, street_address1, city, state, postal_code, country)
VALUES (3, 'home', '789 Oak St', 'Bangalore', 'KA', '560001', 'India');
INSERT INTO user_addresses (user_id, address_type, street_address1, city, state, postal_code, country)
VALUES (4, 'home', '101 Pine St', 'Chennai', 'TN', '600001', 'India');
INSERT INTO user_addresses (user_id, address_type, street_address1, city, state, postal_code, country)
VALUES (5, 'home', '202 Cedar St', 'Hyderabad', 'TS', '500001', 'India');

-- AUDIT_LOG
INSERT INTO audit_log (action_type, table_name, old_values, new_values)
VALUES ('INSERT', 'users', '{}', '{}');
INSERT INTO audit_log (action_type, table_name, old_values, new_values)
VALUES ('UPDATE', 'orders', '{}', '{}');
INSERT INTO audit_log (action_type, table_name, old_values, new_values)
VALUES ('INSERT', 'products', '{}', '{}');
