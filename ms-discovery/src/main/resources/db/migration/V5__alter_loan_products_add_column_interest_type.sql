alter table product_configs.loan_products
    add column if not exists interest_type varchar (255) not null default 'FLAT_RATE';