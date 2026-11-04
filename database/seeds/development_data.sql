-- =============================================================================
-- Données de développement et d'initialisation (Seed Data)
-- Application : Système Intégré de Gestion Scolaire & Caisse
-- =============================================================================

-- 1. Année Scolaire Active
INSERT INTO academic_years (code, name, start_date, end_date, status)
VALUES ('2024-2025', 'Année Scolaire 2024-2025', '2024-09-02', '2025-07-02', 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- 2. Structure Pédagogique
-- Sections
INSERT INTO sections (id, code, name, description, is_active) VALUES
(1, 'MAT', 'Maternelle', 'Section préscolaire', TRUE),
(2, 'PRIM', 'Primaire', 'Enseignement primaire de base', TRUE),
(3, 'SEC', 'Secondaire', 'Humanités et cycle terminal', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Promotions
INSERT INTO promotions (id, section_id, code, name, level_order, description, is_active) VALUES
(1, 2, 'P1', '1ère Primaire', 1, 'Première année primaire', TRUE),
(2, 2, 'P6', '6ème Primaire', 6, 'Sixième année primaire', TRUE),
(3, 3, 'S1', '7ème Éducation de Base', 7, 'Septième année (tronc commun)', TRUE),
(4, 3, 'S3', '1ère Humanités (3ème Secondaire)', 9, 'Première année des humanités', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Options d'étude
INSERT INTO academic_options (id, promotion_id, code, name, description, is_active) VALUES
(1, 4, 'SCI', 'Scientifique', 'Option mathématiques et sciences de la nature', TRUE),
(2, 4, 'COM', 'Commerciale & Gestion', 'Option sciences commerciales et informatiques', TRUE),
(3, 4, 'LIT', 'Littéraire', 'Option lettres et langues', TRUE)
ON CONFLICT (code) DO NOTHING;

-- Classes
INSERT INTO classes (id, option_id, promotion_id, code, name, titulaire, capacity, is_active) VALUES
(1, 1, 4, 'CLS-1SC-A', '1ère Scientifique A', 'Prof. Ilunga David', 40, TRUE),
(2, 1, 4, 'CLS-1SC-B', '1ère Scientifique B', 'Prof. Banza Clarisse', 40, TRUE),
(3, 2, 4, 'CLS-1CG-A', '1ère Commerciale A', 'Prof. Kabange Mpoyi', 35, TRUE)
ON CONFLICT (code) DO NOTHING;

-- 3. Comptes Utilisateurs (Mots de passe hachés BCrypt)
-- Hash BCrypt de "Admin1234!" : $2a$10$wK1m.Yj6Yv7UoA2EaZ3N8e0fG9pQ4rS6tU8vW0xY2zA1bC3dE5fG.
INSERT INTO users (id, username, password_hash, first_name, last_name, post_name, email, phone, role, cash_register_code, status) VALUES
(1, 'admin_promoteur', '$2a$10$X8A2R3D4E5F6G7H8I9J0Ke1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', 'Jenos', 'MUTOMBO', 'KASENDE', 'promoteur@ecolejenos.cd', '+243 81 111 1111', 'PROMOTEUR', NULL, 'ACTIF'),
(2, 'caissiere01', '$2a$10$X8A2R3D4E5F6G7H8I9J0Ke1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', 'Sarah', 'KABANGE', 'MPOYI', 'sarah.k@ecolejenos.cd', '+243 82 222 2222', 'CAISSIERE', 'CS-01', 'ACTIF'),
(3, 'directeur', '$2a$10$X8A2R3D4E5F6G7H8I9J0Ke1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', 'David', 'ILUNGA', 'TSHIMANGA', 'david.i@ecolejenos.cd', '+243 83 333 3333', 'DIRECTEUR', NULL, 'ACTIF'),
(4, 'secretariat', '$2a$10$X8A2R3D4E5F6G7H8I9J0Ke1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', 'Clarisse', 'NGOYI', 'BANZA', 'clarisse.n@ecolejenos.cd', '+243 84 444 4444', 'SECRETARIAT', NULL, 'ACTIF'),
(5, 'admin_tech', '$2a$10$X8A2R3D4E5F6G7H8I9J0Ke1L2M3N4O5P6Q7R8S9T0U1V2W3X4Y5Z6', 'Marc', 'LUMUMBA', 'KASONGO', 'tech@ecolejenos.cd', '+243 85 555 5555', 'ADMIN_TECHNIQUE', NULL, 'ACTIF')
ON CONFLICT (username) DO NOTHING;

-- 4. Caisses Enregistreuses
INSERT INTO cash_registers (id, code, name, description, assigned_cashier, status) VALUES
(1, 'CS-01', 'Caisse Principale 01', 'Guichet principal des encaissements scolaires', 'caissiere01', 'OPEN'),
(2, 'CS-02', 'Caisse Secondaire 02', 'Guichet secondaire', NULL, 'CLOSED')
ON CONFLICT (code) DO NOTHING;

-- 5. Tarifs & Frais
INSERT INTO fees (code, name, promotion_id, amount, currency, periodicity, is_mandatory, is_active, academic_year) VALUES
(1, 'FRAIS-MIN-S3', 'Minerval Annuel 1ère Humanités', 4, 350.00, 'USD', 'ANNUEL', TRUE, TRUE, '2024-2025'),
(2, 'FRAIS-INS-S3', 'Frais d''Inscription', 4, 50000.00, 'CDF', 'PONCTUEL', TRUE, TRUE, '2024-2025')
ON CONFLICT (code) DO NOTHING;

-- 6. Budgets Initiaux
INSERT INTO budgets (code, name, category, academic_year, allocated_amount, spent_amount, currency, status) VALUES
('BDG-01', 'Fournitures de Bureau & Pédagogie', 'Pédagogique', '2024-2025', 2500.00, 1200.00, 'USD', 'ACTIF'),
('BDG-02', 'Maintenance et Travaux Bâtiment', 'Infrastructure', '2024-2025', 4500000.00, 3800000.00, 'CDF', 'ALERTE_80')
ON CONFLICT (code) DO NOTHING;
