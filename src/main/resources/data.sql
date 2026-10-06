-- Datos de desarrollo idempotentes. Password de prueba: ver README.
INSERT INTO usuarios (nombre,direccion,telefono,email,password,rol) VALUES
('Administrador','Guatemala','55550001','admin@fastdelivery.com','$2a$10$fdcAB4ApLBx3j43JKpl.1eiwOzs5FfY0fmUAlihj7TZBR7xDLemU6','ADMIN'),
('Repartidor','Guatemala','55550002','repartidor@fastdelivery.com','$2a$10$fdcAB4ApLBx3j43JKpl.1eiwOzs5FfY0fmUAlihj7TZBR7xDLemU6','REPARTIDOR'),
('Cliente','Zona 1, Guatemala','55550003','cliente@fastdelivery.com','$2a$10$fdcAB4ApLBx3j43JKpl.1eiwOzs5FfY0fmUAlihj7TZBR7xDLemU6','CLIENTE')
ON CONFLICT (email) DO NOTHING;
INSERT INTO comercios (nombre,categoria,direccion,abierto)
SELECT 'Fast Burger','RESTAURANTE','Zona 1, Guatemala',true
WHERE NOT EXISTS (SELECT 1 FROM comercios WHERE nombre='Fast Burger');
INSERT INTO productos (comercio_id,nombre,precio,stock,disponible)
SELECT c.id,v.nombre,v.precio,v.stock,true FROM
(SELECT min(id) AS id FROM comercios WHERE nombre='Fast Burger') c
CROSS JOIN (VALUES ('Hamburguesa',35.00,50),('Papas',15.00,100),('Bebida',10.00,100)) AS v(nombre,precio,stock)
WHERE NOT EXISTS (SELECT 1 FROM productos p WHERE p.comercio_id=c.id AND p.nombre=v.nombre);
