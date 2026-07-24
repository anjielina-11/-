INSERT INTO users (username, password_hash, role, real_name, status)
VALUES
    ('farmer', '$2b$10$reRolkCL2PaHMsRXE4C4dO/C1LjSxv5tzyH75Ria9Qww1GLyjiMDO', 'ROLE_FARMER', '演示农户', 1),
    ('tech', '$2b$10$jQv2/Cp4hYBVoX5Oz2YPP.82c/F5Sat/d61FZekDblehSoA3bhY2O', 'ROLE_TECHNICIAN', '演示农技员', 1),
    ('coop', '$2b$10$Jn052GDhhg4BVnp3lTUoSu2wmDKvbnVCy6FNp8W3bMTKsknt4BLSS', 'ROLE_COOP_MANAGER', '演示合作社', 1)
ON CONFLICT DO NOTHING;
