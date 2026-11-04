-- =============================================================================
-- Migration V001 : Schéma initial de la base de données PostgreSQL
-- Application : Système Intégré de Gestion Scolaire & Caisse
-- =============================================================================

-- 1. Années Académiques
CREATE TABLE IF NOT EXISTS academic_years (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,          -- Ex: '2024-2025'
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE'
        CHECK (status IN ('PLANIFIEE', 'ACTIVE', 'CLOTUREE', 'ARCHIVEE')),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Structure Hiérarchique Scolaire
-- Section (Maternelle, Primaire, Secondaire)
CREATE TABLE IF NOT EXISTS sections (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Promotion (ex: 1ère, 2ème, 3ème...) rattachée à une Section
CREATE TABLE IF NOT EXISTS promotions (
    id SERIAL PRIMARY KEY,
    section_id INTEGER NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    level_order INTEGER NOT NULL DEFAULT 1,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Option d'étude (Scientifique, Commerciale, Littéraire, Tronc Commun...) rattachée à une Promotion
CREATE TABLE IF NOT EXISTS academic_options (
    id SERIAL PRIMARY KEY,
    promotion_id INTEGER NOT NULL REFERENCES promotions(id) ON DELETE RESTRICT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Classe concrète (ex: 1ère Scientifique A) rattachée à une Option et une Promotion
CREATE TABLE IF NOT EXISTS classes (
    id SERIAL PRIMARY KEY,
    option_id INTEGER NOT NULL REFERENCES academic_options(id) ON DELETE RESTRICT,
    promotion_id INTEGER NOT NULL REFERENCES promotions(id) ON DELETE RESTRICT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    titulaire VARCHAR(150),
    capacity INTEGER NOT NULL DEFAULT 40,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Gestion des Élèves
CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    post_name VARCHAR(100),
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('M', 'F')),
    birth_date DATE,
    address TEXT,
    phone VARCHAR(30),
    classe_id INTEGER REFERENCES classes(id) ON DELETE RESTRICT,
    academic_year VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIF'
        CHECK (status IN ('ACTIF', 'INACTIF', 'ARCHIVE', 'SUSPENDU', 'ABANDON')),
    guardian_name VARCHAR(150),
    guardian_phone VARCHAR(30),
    guardian_relation VARCHAR(50),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Gestion du Personnel
CREATE TABLE IF NOT EXISTS staff (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    post_name VARCHAR(100),
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('M', 'F')),
    department VARCHAR(100) NOT NULL,
    function_title VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(150),
    base_salary NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (base_salary >= 0),
    currency VARCHAR(5) NOT NULL CHECK (currency IN ('USD', 'CDF')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIF'
        CHECK (status IN ('ACTIF', 'INACTIF', 'CONGE', 'ARCHIVE')),
    hired_at DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Grille Tarifaire & Frais Scolaires
CREATE TABLE IF NOT EXISTS fees (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    promotion_id INTEGER REFERENCES promotions(id) ON DELETE RESTRICT,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(5) NOT NULL CHECK (currency IN ('USD', 'CDF')),
    periodicity VARCHAR(30) NOT NULL DEFAULT 'ANNUEL'
        CHECK (periodicity IN ('MENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL', 'PONCTUEL')),
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    academic_year VARCHAR(20) NOT NULL
);

-- 6. Caisses & Sessions de Caisse
CREATE TABLE IF NOT EXISTS cash_registers (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    assigned_cashier VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'CLOSED'
        CHECK (status IN ('OPEN', 'CLOSED', 'BLOCKED'))
);

CREATE TABLE IF NOT EXISTS cash_sessions (
    id SERIAL PRIMARY KEY,
    cash_register_id INTEGER NOT NULL REFERENCES cash_registers(id) ON DELETE RESTRICT,
    cashier_username VARCHAR(50) NOT NULL,
    opened_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITHOUT TIME ZONE,
    initial_balance_usd NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (initial_balance_usd >= 0),
    initial_balance_cdf NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (initial_balance_cdf >= 0),
    current_balance_usd NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (current_balance_usd >= 0),
    current_balance_cdf NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (current_balance_cdf >= 0),
    counted_balance_usd NUMERIC(15, 2),
    counted_balance_cdf NUMERIC(15, 2),
    difference_usd NUMERIC(15, 2),
    difference_cdf NUMERIC(15, 2),
    closure_notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'REPORT_GENERATED', 'REPORT_PRINTED', 'RECONCILIATION', 'CLOSED')),
    is_report_generated BOOLEAN NOT NULL DEFAULT FALSE,
    is_report_printed BOOLEAN NOT NULL DEFAULT FALSE,
    is_exceptional BOOLEAN NOT NULL DEFAULT FALSE
);

-- 7. Paiements & Encaissements
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    student_id INTEGER REFERENCES students(id) ON DELETE RESTRICT,
    staff_id INTEGER REFERENCES staff(id) ON DELETE RESTRICT,
    fee_id INTEGER REFERENCES fees(id) ON DELETE RESTRICT,
    cash_session_id INTEGER REFERENCES cash_sessions(id) ON DELETE RESTRICT,
    cashier_username VARCHAR(50) NOT NULL,
    cash_register_name VARCHAR(100) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(5) NOT NULL CHECK (currency IN ('USD', 'CDF')),
    payment_method VARCHAR(30) NOT NULL
        CHECK (payment_method IN ('ESPECES', 'MOBILE_MONEY', 'VIREMENT', 'CHEQUE', 'AUTRE')),
    reference VARCHAR(100),
    justification TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'VALIDEE'
        CHECK (status IN ('VALIDEE', 'ANNULEE', 'CORRIGEE')),
    payment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Décaissements & Dépenses
CREATE TABLE IF NOT EXISTS expenses (
    id SERIAL PRIMARY KEY,
    voucher_number VARCHAR(50) NOT NULL UNIQUE,
    cash_session_id INTEGER REFERENCES cash_sessions(id) ON DELETE RESTRICT,
    beneficiary_name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(5) NOT NULL CHECK (currency IN ('USD', 'CDF')),
    payment_method VARCHAR(30) NOT NULL DEFAULT 'ESPECES',
    reference VARCHAR(100),
    justification TEXT NOT NULL,
    attachment_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'VALIDEE'
        CHECK (status IN ('VALIDEE', 'EN_ATTENTE', 'ANNULEE', 'CORRIGEE')),
    requires_authorization BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(50),
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Lignes Budgétaires
CREATE TABLE IF NOT EXISTS budgets (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    allocated_amount NUMERIC(15, 2) NOT NULL CHECK (allocated_amount >= 0),
    spent_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (spent_amount >= 0),
    currency VARCHAR(5) NOT NULL CHECK (currency IN ('USD', 'CDF')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIF'
        CHECK (status IN ('ACTIF', 'ALERTE_80', 'ALERTE_90', 'DEPASSE', 'CLOTURE'))
);

-- 10. Comptes Utilisateurs & Sécurité
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    post_name VARCHAR(100),
    email VARCHAR(150),
    phone VARCHAR(30),
    role VARCHAR(30) NOT NULL
        CHECK (role IN ('PROMOTEUR', 'CAISSIERE', 'DIRECTEUR', 'SECRETARIAT', 'ADMIN_TECHNIQUE')),
    cash_register_code VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIF'
        CHECK (status IN ('ACTIF', 'BLOQUE', 'DESACTIVE')),
    last_login TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 11. Journal d'Audit Immuable
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50)
);

-- Index d'optimisation
CREATE INDEX IF NOT EXISTS idx_students_matricule ON students(matricule);
CREATE INDEX IF NOT EXISTS idx_payments_receipt_number ON payments(receipt_number);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_expenses_voucher_number ON expenses(voucher_number);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(username);
