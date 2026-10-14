
    create table amenities (
        id bigserial not null,
        icon varchar(255),
        name varchar(255) not null unique,
        primary key (id)
    );

    create table category_amenities (
        amenity_id bigint not null,
        category_id bigint not null,
        primary key (amenity_id, category_id)
    );

    create table consumptions (
        quantity integer not null,
        total_price numeric(38,2) not null,
        unit_price numeric(38,2) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        reservation_id bigint not null,
        service_id bigint not null,
        primary key (id)
    );

    create table extra_services (
        is_active boolean not null,
        price numeric(38,2) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        description varchar(255),
        name varchar(255) not null unique,
        primary key (id)
    );

    create table guests (
        created_at timestamp(6) not null,
        id bigserial not null,
        address varchar(255),
        document_number varchar(255) not null unique,
        document_type varchar(255) not null,
        email varchar(255),
        first_name varchar(255) not null,
        last_name varchar(255) not null,
        phone varchar(255),
        primary key (id)
    );

    create table invoices (
        subtotal numeric(38,2) not null,
        tax_amount numeric(38,2) not null,
        total_amount numeric(38,2) not null,
        id bigserial not null,
        issue_date timestamp(6) not null,
        reservation_id bigint not null unique,
        status varchar(255) not null check (status in ('PENDING','PAID','VOID')),
        primary key (id)
    );

    create table reservations (
        check_in_date date not null,
        check_out_date date not null,
        total_price numeric(38,2) not null,
        created_at timestamp(6) not null,
        guest_id bigint not null,
        id bigserial not null,
        room_category_id bigint not null,
        room_id bigint,
        notes varchar(255),
        status varchar(255) not null check (status in ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
        primary key (id)
    );

    create table room_categories (
        base_price float(53) not null,
        max_capacity integer not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        description varchar(255),
        image_url varchar(255),
        name varchar(255) not null unique,
        primary key (id)
    );

    create table rooms (
        floor integer not null,
        category_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        room_number varchar(255) not null unique,
        status varchar(255) not null check (status in ('AVAILABLE','OCCUPIED','CLEANING','MAINTENANCE')),
        primary key (id)
    );

    create table users (
        created_at timestamp(6) not null,
        id bigserial not null,
        email varchar(255) not null unique,
        password varchar(255) not null,
        role varchar(255) not null check (role in ('ADMIN','RECEPCIONISTA')),
        primary key (id)
    );

    create index idx_res_dates_cat 
       on reservations (room_category_id, check_in_date, check_out_date, status);

    alter table if exists category_amenities 
       add constraint FKakw6as9v1n2nv3png10wirma5 
       foreign key (amenity_id) 
       references amenities;

    alter table if exists category_amenities 
       add constraint FKla2s42rmljtg3fyai9tkoi536 
       foreign key (category_id) 
       references room_categories;

    alter table if exists consumptions 
       add constraint FKssgw3mqu7idff93m2mi52is38 
       foreign key (service_id) 
       references extra_services;

    alter table if exists consumptions 
       add constraint FKku3ci9xm9seifa2bekwqivaq9 
       foreign key (reservation_id) 
       references reservations;

    alter table if exists invoices 
       add constraint FKkga8j6hh54xf5hl3qiovlsa4l 
       foreign key (reservation_id) 
       references reservations;

    alter table if exists reservations 
       add constraint FKa0e6sbbuw25f8cwciqaune89n 
       foreign key (guest_id) 
       references guests;

    alter table if exists reservations 
       add constraint FKljt6q1tp205b0h26eiegc5mx6 
       foreign key (room_id) 
       references rooms;

    alter table if exists reservations 
       add constraint FK5ofs9lwi0cc0mwm120y1ddjm5 
       foreign key (room_category_id) 
       references room_categories;

    alter table if exists rooms 
       add constraint FKs3ya1bmk1lymqudi59qf826vl 
       foreign key (category_id) 
       references room_categories;
