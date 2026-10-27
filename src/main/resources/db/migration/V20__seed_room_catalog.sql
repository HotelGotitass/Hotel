-- Catalogo inicial de habitaciones: comodidades, categorias (con foto) y habitaciones.
-- Idempotente: si ya existe una comodidad/categoria con el mismo nombre o una habitacion con
-- el mismo numero, se conserva la existente (no se duplica ni se sobrescribe).
-- Las fotos son URLs absolutas de Unsplash (licencia libre) porque las imagenes subidas al
-- disco del servidor (uploads/) se pierden en cada despliegue de Cloud Run.

INSERT INTO amenities (name, icon) VALUES
    ('Wi-Fi de alta velocidad', 'wifi'),
    ('Smart TV', 'tv'),
    ('Aire acondicionado', 'ac'),
    ('Desayuno incluido', 'breakfast'),
    ('Cafetera', 'coffee'),
    ('Frigobar', 'minibar'),
    ('Caja fuerte', 'safe'),
    ('Ducha con agua caliente', 'shower'),
    ('Tina de baño', 'bathtub'),
    ('Vista a la ciudad', 'view'),
    ('Terraza privada', 'terrace'),
    ('Cama King', 'king_bed'),
    ('Servicio a la habitación', 'room_service'),
    ('Limpieza diaria', 'cleaning'),
    ('Habitación para no fumadores', 'non_smoking')
ON CONFLICT (name) DO NOTHING;

INSERT INTO room_categories (name, description, base_price, max_capacity, image_url, created_at) VALUES
    ('Habitación Simple',
     'Ideal para viajeros de negocios o en solitario: cama de plaza y media, escritorio de trabajo, Wi-Fi rápido y baño privado con agua caliente.',
     120, 1,
     'https://images.unsplash.com/photo-1560185893-a55cbc8c57e8?auto=format&fit=crop&w=1600&q=80', now()),
    ('Habitación Matrimonial',
     'Cama Queen con ropa de cama de algodón, iluminación cálida y Smart TV. Un refugio tranquilo para parejas que buscan descansar.',
     180, 2,
     'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1600&q=80', now()),
    ('Superior Vista Ciudad',
     'Ventanales de piso a techo con vista a la ciudad, cama Queen, zona de lectura y cafetera para empezar el día sin prisa.',
     230, 2,
     'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1600&q=80', now()),
    ('Deluxe King',
     'Amplia y elegante, con cama King, cabecera tapizada, frigobar y caja fuerte. Pensada para estadías largas con todo a mano.',
     290, 2,
     'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1600&q=80', now()),
    ('Habitación Familiar',
     'Dos camas de dos plazas para hasta cuatro personas, espacio de sobra para el equipaje y desayuno incluido para toda la familia.',
     320, 4,
     'https://images.unsplash.com/photo-1595576508898-0ad5c879a061?auto=format&fit=crop&w=1600&q=80', now()),
    ('Junior Suite',
     'Cama King y sala de estar con sofá cama para un tercer huésped. Decoración clásica, tina de baño y servicio a la habitación.',
     420, 3,
     'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1600&q=80', now()),
    ('Suite Jardín',
     'En el primer piso, abierta a nuestro jardín interior. Maderas nobles, cama King, terraza privada y una atmósfera de total calma.',
     480, 2,
     'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1600&q=80', now()),
    ('Suite Presidencial',
     'Nuestra suite más exclusiva: más de 80 m² con sala, cama King, baño de lujo con tina y vistas panorámicas. Para ocasiones especiales.',
     750, 4,
     'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1600&q=80', now())
ON CONFLICT (name) DO NOTHING;

-- Comodidades por categoria (por nombre, para no depender de ids)
INSERT INTO category_amenities (category_id, amenity_id)
SELECT c.id, a.id
FROM (VALUES
    ('Habitación Simple', 'Wi-Fi de alta velocidad'),
    ('Habitación Simple', 'Smart TV'),
    ('Habitación Simple', 'Ducha con agua caliente'),
    ('Habitación Simple', 'Limpieza diaria'),
    ('Habitación Simple', 'Habitación para no fumadores'),

    ('Habitación Matrimonial', 'Wi-Fi de alta velocidad'),
    ('Habitación Matrimonial', 'Smart TV'),
    ('Habitación Matrimonial', 'Aire acondicionado'),
    ('Habitación Matrimonial', 'Ducha con agua caliente'),
    ('Habitación Matrimonial', 'Limpieza diaria'),
    ('Habitación Matrimonial', 'Habitación para no fumadores'),

    ('Superior Vista Ciudad', 'Wi-Fi de alta velocidad'),
    ('Superior Vista Ciudad', 'Smart TV'),
    ('Superior Vista Ciudad', 'Aire acondicionado'),
    ('Superior Vista Ciudad', 'Vista a la ciudad'),
    ('Superior Vista Ciudad', 'Cafetera'),
    ('Superior Vista Ciudad', 'Ducha con agua caliente'),
    ('Superior Vista Ciudad', 'Limpieza diaria'),

    ('Deluxe King', 'Wi-Fi de alta velocidad'),
    ('Deluxe King', 'Smart TV'),
    ('Deluxe King', 'Aire acondicionado'),
    ('Deluxe King', 'Cama King'),
    ('Deluxe King', 'Frigobar'),
    ('Deluxe King', 'Caja fuerte'),
    ('Deluxe King', 'Cafetera'),
    ('Deluxe King', 'Ducha con agua caliente'),
    ('Deluxe King', 'Limpieza diaria'),

    ('Habitación Familiar', 'Wi-Fi de alta velocidad'),
    ('Habitación Familiar', 'Smart TV'),
    ('Habitación Familiar', 'Aire acondicionado'),
    ('Habitación Familiar', 'Desayuno incluido'),
    ('Habitación Familiar', 'Frigobar'),
    ('Habitación Familiar', 'Ducha con agua caliente'),
    ('Habitación Familiar', 'Limpieza diaria'),

    ('Junior Suite', 'Wi-Fi de alta velocidad'),
    ('Junior Suite', 'Smart TV'),
    ('Junior Suite', 'Aire acondicionado'),
    ('Junior Suite', 'Cama King'),
    ('Junior Suite', 'Desayuno incluido'),
    ('Junior Suite', 'Frigobar'),
    ('Junior Suite', 'Caja fuerte'),
    ('Junior Suite', 'Tina de baño'),
    ('Junior Suite', 'Servicio a la habitación'),
    ('Junior Suite', 'Limpieza diaria'),

    ('Suite Jardín', 'Wi-Fi de alta velocidad'),
    ('Suite Jardín', 'Smart TV'),
    ('Suite Jardín', 'Aire acondicionado'),
    ('Suite Jardín', 'Cama King'),
    ('Suite Jardín', 'Terraza privada'),
    ('Suite Jardín', 'Desayuno incluido'),
    ('Suite Jardín', 'Frigobar'),
    ('Suite Jardín', 'Caja fuerte'),
    ('Suite Jardín', 'Servicio a la habitación'),
    ('Suite Jardín', 'Limpieza diaria'),

    ('Suite Presidencial', 'Wi-Fi de alta velocidad'),
    ('Suite Presidencial', 'Smart TV'),
    ('Suite Presidencial', 'Aire acondicionado'),
    ('Suite Presidencial', 'Cama King'),
    ('Suite Presidencial', 'Vista a la ciudad'),
    ('Suite Presidencial', 'Tina de baño'),
    ('Suite Presidencial', 'Desayuno incluido'),
    ('Suite Presidencial', 'Frigobar'),
    ('Suite Presidencial', 'Caja fuerte'),
    ('Suite Presidencial', 'Cafetera'),
    ('Suite Presidencial', 'Servicio a la habitación'),
    ('Suite Presidencial', 'Limpieza diaria')
) AS ca(category_name, amenity_name)
JOIN room_categories c ON c.name = ca.category_name
JOIN amenities a ON a.name = ca.amenity_name
ON CONFLICT DO NOTHING;

-- 32 habitaciones en 5 pisos, todas disponibles
INSERT INTO rooms (room_number, floor, category_id, status, created_at)
SELECT r.room_number, r.floor, c.id, 'AVAILABLE', now()
FROM (VALUES
    ('101', 1, 'Habitación Simple'),
    ('102', 1, 'Habitación Simple'),
    ('103', 1, 'Habitación Simple'),
    ('104', 1, 'Habitación Simple'),
    ('105', 1, 'Habitación Matrimonial'),
    ('106', 1, 'Habitación Matrimonial'),
    ('107', 1, 'Suite Jardín'),
    ('108', 1, 'Suite Jardín'),

    ('201', 2, 'Habitación Matrimonial'),
    ('202', 2, 'Habitación Matrimonial'),
    ('203', 2, 'Habitación Matrimonial'),
    ('204', 2, 'Habitación Matrimonial'),
    ('205', 2, 'Habitación Familiar'),
    ('206', 2, 'Habitación Familiar'),
    ('207', 2, 'Habitación Familiar'),
    ('208', 2, 'Habitación Familiar'),

    ('301', 3, 'Superior Vista Ciudad'),
    ('302', 3, 'Superior Vista Ciudad'),
    ('303', 3, 'Superior Vista Ciudad'),
    ('304', 3, 'Superior Vista Ciudad'),
    ('305', 3, 'Deluxe King'),
    ('306', 3, 'Deluxe King'),
    ('307', 3, 'Deluxe King'),
    ('308', 3, 'Habitación Familiar'),

    ('401', 4, 'Deluxe King'),
    ('402', 4, 'Deluxe King'),
    ('403', 4, 'Superior Vista Ciudad'),
    ('404', 4, 'Junior Suite'),
    ('405', 4, 'Junior Suite'),
    ('406', 4, 'Junior Suite'),

    ('501', 5, 'Suite Presidencial'),
    ('502', 5, 'Junior Suite')
) AS r(room_number, floor, category_name)
JOIN room_categories c ON c.name = r.category_name
ON CONFLICT (room_number) DO NOTHING;
