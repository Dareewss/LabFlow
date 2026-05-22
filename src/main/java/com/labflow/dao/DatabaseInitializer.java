package com.labflow.dao;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.labflow.util.AppConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            createTables(stmt);
            runSchemaMigrations(conn, stmt);
            rebuildLegacyTables(conn, stmt);
            stmt.execute("PRAGMA foreign_keys = ON");
            seedUser(conn, "admin", "admin123", "Administrator", "ADMIN");
            seedUser(conn, "professor", "professor123", "Professor", "PROFESSOR");
            seedUser(conn, "technician", "technician123", "Technician", "TECHNICIAN");
            seedRecoveryKeyIfMissing(conn, "admin", "ADMIN-RESET-2026");
            seedRecoveryKeyIfMissing(conn, "professor", "PROF-RESET-2026");
            seedRecoveryKeyIfMissing(conn, "technician", "TECH-RESET-2026");
            seedDefaultLab(conn);
            seedPresentationDemoLab(conn);
            logger.info("Database initialized");
        } catch (Exception e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException(e);
        }
    }

    private static void runSchemaMigrations(Connection conn, Statement stmt) throws Exception {
        int schemaVersion = currentSchemaVersion(conn);
        if (schemaVersion < 2) {
            logger.info("Running schema migrations from version {} to {}", schemaVersion, AppConstants.DB_SCHEMA_VERSION);
        }
        migrateTables(conn, stmt);
        seedSchemaVersion(conn);
    }

    private static void createTables(Statement stmt) throws Exception {
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_plain_demo TEXT,
                    recovery_key_hash TEXT,
                    full_name TEXT,
                    role TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_preferences (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS labs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    invite_code TEXT UNIQUE NOT NULL,
                    protected_lab INTEGER NOT NULL DEFAULT 0,
                    created_by_user_id INTEGER NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(created_by_user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS lab_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(lab_id, user_id),
                    FOREIGN KEY(lab_id) REFERENCES labs(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS equipment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT,
                    location TEXT NOT NULL,
                    status TEXT NOT NULL,
                    qr_code TEXT UNIQUE,
                    qr_code_path TEXT,
                    serial_number TEXT UNIQUE,
                    manufacturer TEXT,
                    model TEXT,
                    purchase_date TEXT,
                    last_maintenance_date TEXT,
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS equipment_containers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(lab_id, name),
                    FOREIGN KEY(lab_id) REFERENCES labs(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS borrow_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    borrow_date TEXT NOT NULL,
                    expected_return_date TEXT,
                    actual_return_date TEXT,
                    status TEXT NOT NULL,
                    return_condition TEXT,
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS fault_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    equipment_id INTEGER NOT NULL,
                    reported_by_user_id INTEGER NOT NULL,
                    assigned_to_user_id INTEGER,
                    description TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT,
                    resolved_at TEXT,
                    resolution_notes TEXT,
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(reported_by_user_id) REFERENCES users(id),
                    FOREIGN KEY(assigned_to_user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS fault_attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fault_report_id INTEGER NOT NULL,
                    file_path TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    mime_type TEXT,
                    file_size INTEGER,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(fault_report_id) REFERENCES fault_reports(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    entity_type TEXT,
                    entity_id INTEGER,
                    description TEXT,
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'INFO',
                    is_read INTEGER NOT NULL DEFAULT 0,
                    entity_type TEXT,
                    entity_id INTEGER,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(lab_id) REFERENCES labs(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    color TEXT NOT NULL DEFAULT '#6B0F1A',
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(lab_id, name),
                    FOREIGN KEY(lab_id) REFERENCES labs(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS equipment_tags (
                    equipment_id INTEGER NOT NULL,
                    tag_id INTEGER NOT NULL,
                    PRIMARY KEY(equipment_id, tag_id),
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(tag_id) REFERENCES tags(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS maintenance_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    equipment_id INTEGER NOT NULL,
                    performed_by_user_id INTEGER,
                    maintenance_date TEXT NOT NULL,
                    notes TEXT,
                    result_status TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(lab_id) REFERENCES labs(id),
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(performed_by_user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    equipment_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    start_datetime TEXT NOT NULL,
                    end_datetime TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(lab_id) REFERENCES labs(id),
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    equipment_id INTEGER NOT NULL,
                    user_id INTEGER,
                    movement_type TEXT NOT NULL,
                    quantity_change INTEGER NOT NULL,
                    old_quantity INTEGER NOT NULL,
                    new_quantity INTEGER NOT NULL,
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(lab_id) REFERENCES labs(id),
                    FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS checklist_templates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    equipment_category TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(lab_id) REFERENCES labs(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS checklist_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    template_id INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    required INTEGER NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(template_id) REFERENCES checklist_templates(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS return_checklist_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    borrow_record_id INTEGER NOT NULL,
                    checklist_item_text TEXT NOT NULL,
                    checked INTEGER NOT NULL DEFAULT 0,
                    notes TEXT,
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(borrow_record_id) REFERENCES borrow_records(id)
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_points (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    lab_id INTEGER NOT NULL,
                    points INTEGER DEFAULT 0,
                    total_earned INTEGER DEFAULT 0,
                    UNIQUE(user_id, lab_id),
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS points_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    lab_id INTEGER NOT NULL,
                    points_delta INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS weekly_reports (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    lab_id INTEGER NOT NULL,
                    week_start DATE NOT NULL,
                    content TEXT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(lab_id, week_start)
                )
                """);
    }

    private static void migrateTables(Connection conn, Statement stmt) throws Exception {
        addColumnIfMissing(conn, stmt, "users", "full_name", "TEXT");
        addColumnIfMissing(conn, stmt, "users", "password_plain_demo", "TEXT");
        addColumnIfMissing(conn, stmt, "users", "recovery_key_hash", "TEXT");
        addColumnIfMissing(conn, stmt, "users", "is_active", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(conn, stmt, "labs", "color_palette", "TEXT DEFAULT 'RED'");
        addColumnIfMissing(conn, stmt, "equipment", "qr_code_path", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "serial_number", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "manufacturer", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "model", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "purchase_date", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "last_maintenance_date", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "notes", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "updated_at", "TEXT DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing(conn, stmt, "equipment", "lab_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "equipment", "container_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "equipment", "maintenance_interval_days", "INTEGER");
        addColumnIfMissing(conn, stmt, "equipment", "next_maintenance_date", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "maintenance_notes", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "is_archived", "INTEGER DEFAULT 0");
        addColumnIfMissing(conn, stmt, "equipment", "archived_at", "TEXT");
        addColumnIfMissing(conn, stmt, "equipment", "archived_by_user_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "equipment", "item_type", "TEXT DEFAULT 'ASSET'");
        addColumnIfMissing(conn, stmt, "equipment", "quantity", "INTEGER DEFAULT 1");
        addColumnIfMissing(conn, stmt, "equipment", "minimum_quantity", "INTEGER DEFAULT 0");
        addColumnIfMissing(conn, stmt, "equipment", "unit", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "borrow_date", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "expected_return_date", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "actual_return_date", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "return_condition", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "created_at", "TEXT DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing(conn, stmt, "borrow_records", "borrow_signature_path", "TEXT");
        addColumnIfMissing(conn, stmt, "borrow_records", "return_signature_path", "TEXT");
        addColumnIfMissing(conn, stmt, "fault_reports", "reported_by_user_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "fault_reports", "assigned_to_user_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "fault_reports", "severity", "TEXT DEFAULT 'MINOR'");
        addColumnIfMissing(conn, stmt, "fault_reports", "updated_at", "TEXT");
        addColumnIfMissing(conn, stmt, "fault_reports", "resolution_notes", "TEXT");
        addColumnIfMissing(conn, stmt, "fault_reports", "priority", "TEXT DEFAULT 'NORMAL'");
        addColumnIfMissing(conn, stmt, "activity_log", "entity_type", "TEXT");
        addColumnIfMissing(conn, stmt, "activity_log", "entity_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "activity_log", "description", "TEXT");
        addColumnIfMissing(conn, stmt, "activity_log", "timestamp", "TEXT DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing(conn, stmt, "activity_log", "lab_id", "INTEGER");
        addColumnIfMissing(conn, stmt, "activity_log", "metadata_json", "TEXT");
        Set<String> equipmentColumns = columns(conn, "equipment");
        Set<String> borrowColumns = columns(conn, "borrow_records");
        Set<String> faultColumns = columns(conn, "fault_reports");
        Set<String> activityColumns = columns(conn, "activity_log");
        stmt.execute("UPDATE equipment SET location = 'Main Lab' WHERE location IS NULL OR trim(location) = ''");
        stmt.execute("UPDATE equipment SET status = 'AVAILABLE' WHERE status IS NULL OR trim(status) = ''");
        if (equipmentColumns.contains("image_path")) {
            stmt.execute("UPDATE equipment SET qr_code_path = image_path WHERE qr_code_path IS NULL AND image_path IS NOT NULL");
        }
        if (borrowColumns.contains("borrowed_at")) {
            stmt.execute("UPDATE borrow_records SET borrow_date = borrowed_at WHERE borrow_date IS NULL AND borrowed_at IS NOT NULL");
        }
        if (borrowColumns.contains("returned_at")) {
            stmt.execute("UPDATE borrow_records SET actual_return_date = returned_at WHERE actual_return_date IS NULL AND returned_at IS NOT NULL");
        }
        stmt.execute("UPDATE borrow_records SET borrow_date = datetime('now') WHERE borrow_date IS NULL");
        if (faultColumns.contains("user_id")) {
            stmt.execute("UPDATE fault_reports SET reported_by_user_id = user_id WHERE reported_by_user_id IS NULL AND user_id IS NOT NULL");
        }
        stmt.execute("UPDATE fault_reports SET reported_by_user_id = 1 WHERE reported_by_user_id IS NULL");
        stmt.execute("UPDATE fault_reports SET status = 'OPEN' WHERE status = 'NEW'");
        stmt.execute("UPDATE fault_reports SET status = 'IN_PROGRESS' WHERE status IN ('IN_ANALYSIS', 'IN_WORK')");
        if (activityColumns.contains("target_entity")) {
            stmt.execute("UPDATE activity_log SET entity_type = target_entity WHERE entity_type IS NULL AND target_entity IS NOT NULL");
        }
        if (activityColumns.contains("target_id")) {
            stmt.execute("UPDATE activity_log SET entity_id = target_id WHERE entity_id IS NULL AND target_id IS NOT NULL");
        }
        if (activityColumns.contains("created_at")) {
            stmt.execute("UPDATE activity_log SET timestamp = created_at WHERE timestamp IS NULL AND created_at IS NOT NULL");
        }
        stmt.execute("UPDATE equipment SET is_archived = 0 WHERE is_archived IS NULL");
        stmt.execute("UPDATE equipment SET item_type = 'ASSET' WHERE item_type IS NULL OR trim(item_type) = ''");
        stmt.execute("UPDATE equipment SET quantity = 1 WHERE quantity IS NULL");
        stmt.execute("UPDATE equipment SET minimum_quantity = 0 WHERE minimum_quantity IS NULL");
        stmt.execute("UPDATE fault_reports SET priority = 'NORMAL' WHERE priority IS NULL OR trim(priority) = ''");
        stmt.execute("UPDATE labs SET color_palette = 'RED' WHERE color_palette IS NULL OR trim(color_palette) = ''");
        stmt.execute("UPDATE users SET is_active = 1 WHERE is_active IS NULL");
        createIndexes(stmt);
    }

    private static void rebuildLegacyTables(Connection conn, Statement stmt) throws Exception {
        if (columns(conn, "borrow_records").contains("borrowed_at")) {
            stmt.execute("ALTER TABLE borrow_records RENAME TO borrow_records_old");
            stmt.execute("""
                    CREATE TABLE borrow_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        equipment_id INTEGER NOT NULL,
                        user_id INTEGER NOT NULL,
                        borrow_date TEXT NOT NULL,
                        expected_return_date TEXT,
                        actual_return_date TEXT,
                        status TEXT NOT NULL,
                        return_condition TEXT,
                        notes TEXT,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
            stmt.execute("""
                    INSERT INTO borrow_records (id, equipment_id, user_id, borrow_date, expected_return_date,
                    actual_return_date, status, return_condition, notes, created_at)
                    SELECT id, equipment_id, user_id, COALESCE(borrow_date, borrowed_at, datetime('now')),
                    expected_return_date, COALESCE(actual_return_date, returned_at), COALESCE(status, 'ACTIVE'),
                    return_condition, notes, COALESCE(created_at, borrow_date, borrowed_at, datetime('now'))
                    FROM borrow_records_old
                    """);
            stmt.execute("DROP TABLE borrow_records_old");
        }
        if (columns(conn, "fault_reports").contains("user_id")) {
            stmt.execute("ALTER TABLE fault_reports RENAME TO fault_reports_old");
            stmt.execute("""
                    CREATE TABLE fault_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        equipment_id INTEGER NOT NULL,
                        reported_by_user_id INTEGER NOT NULL,
                        assigned_to_user_id INTEGER,
                        description TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        status TEXT NOT NULL,
                        created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT,
                        resolved_at TEXT,
                        resolution_notes TEXT,
                        FOREIGN KEY(equipment_id) REFERENCES equipment(id),
                        FOREIGN KEY(reported_by_user_id) REFERENCES users(id),
                        FOREIGN KEY(assigned_to_user_id) REFERENCES users(id)
                    )
                    """);
            stmt.execute("""
                    INSERT INTO fault_reports (id, equipment_id, reported_by_user_id, assigned_to_user_id, description,
                    severity, status, created_at, updated_at, resolved_at, resolution_notes)
                    SELECT id, equipment_id, COALESCE(reported_by_user_id, user_id, 1), assigned_to_user_id, description,
                    COALESCE(severity, 'MINOR'),
                    CASE WHEN status = 'NEW' THEN 'OPEN' WHEN status IN ('IN_ANALYSIS', 'IN_WORK') THEN 'IN_PROGRESS' ELSE COALESCE(status, 'OPEN') END,
                    COALESCE(created_at, datetime('now')), updated_at, resolved_at, resolution_notes
                    FROM fault_reports_old
                    """);
            stmt.execute("DROP TABLE fault_reports_old");
        }
        if (columns(conn, "activity_log").contains("target_entity")) {
            stmt.execute("ALTER TABLE activity_log RENAME TO activity_log_old");
            stmt.execute("""
                    CREATE TABLE activity_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER,
                        action TEXT NOT NULL,
                        entity_type TEXT,
                        entity_id INTEGER,
                        description TEXT,
                        timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
                        lab_id INTEGER,
                        metadata_json TEXT,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
            stmt.execute("""
                    INSERT INTO activity_log (id, user_id, action, entity_type, entity_id, description, timestamp, lab_id, metadata_json)
                    SELECT id, user_id, action, COALESCE(entity_type, target_entity), COALESCE(entity_id, target_id),
                    description, COALESCE(timestamp, created_at, datetime('now')), lab_id, metadata_json
                    FROM activity_log_old
                    """);
            stmt.execute("DROP TABLE activity_log_old");
        }
    }

    private static void addColumnIfMissing(Connection conn, Statement stmt, String table, String column, String definition) throws Exception {
        if (!columns(conn, table).contains(column.toLowerCase())) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private static Set<String> columns(Connection conn, String table) throws Exception {
        Set<String> names = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(" + table + ")");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name").toLowerCase());
            }
        }
        return names;
    }

    private static void createIndexes(Statement stmt) throws Exception {
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_equipment_lab ON equipment(lab_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_equipment_status ON equipment(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_equipment_category ON equipment(category)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_equipment_item_type ON equipment(item_type)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_fault_reports_status ON fault_reports(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_fault_reports_severity ON fault_reports(severity)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_fault_reports_priority ON fault_reports(priority)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_fault_attachments_report ON fault_attachments(fault_report_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_borrow_records_equipment ON borrow_records(equipment_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_borrow_records_user ON borrow_records(user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_borrow_records_status ON borrow_records(status)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_lab ON reservations(lab_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_equipment ON reservations(equipment_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_stock_movements_equipment ON stock_movements(equipment_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_checklist_templates_lab ON checklist_templates(lab_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_return_checklist_borrow ON return_checklist_results(borrow_record_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_tags_lab_name ON tags(lab_id, name)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_points_lab_user ON user_points(lab_id, user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_points_history_lab_user ON points_history(lab_id, user_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_weekly_reports_lab_week ON weekly_reports(lab_id, week_start)");
    }

    private static void seedUser(Connection conn, String username, String password, String fullName, String role) throws Exception {
        String countSql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement count = conn.prepareStatement(countSql)) {
            count.setString(1, username);
            try (ResultSet rs = count.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        String sql = "INSERT INTO users (username, password_hash, password_plain_demo, full_name, role, is_active) VALUES (?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setString(3, password);
            ps.setString(4, fullName);
            ps.setString(5, role);
            ps.executeUpdate();
        }
    }

    private static void seedRecoveryKeyIfMissing(Connection conn, String username, String recoveryKey) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE users
                SET recovery_key_hash = ?
                WHERE username = ? AND (recovery_key_hash IS NULL OR trim(recovery_key_hash) = '')
                """)) {
            ps.setString(1, BCrypt.hashpw(recoveryKey, BCrypt.gensalt()));
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private static void seedSchemaVersion(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO app_preferences (key, value) VALUES ('db_schema_version', ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """)) {
            ps.setString(1, AppConstants.DB_SCHEMA_VERSION);
            ps.executeUpdate();
        }
    }

    private static int currentSchemaVersion(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT value FROM app_preferences WHERE key = 'db_schema_version'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (Exception e) {
            logger.warn("Could not read schema version, defaulting to 0", e);
        }
        return 0;
    }

    private static void seedDefaultLab(Connection conn) throws Exception {
        int adminId = userId(conn, "admin");
        int professorId = userId(conn, "professor");
        int technicianId = userId(conn, "technician");
        int labId = labIdByInvite(conn, "TEST-LAB");
        if (labId <= 0) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO labs (name, invite_code, protected_lab, created_by_user_id) VALUES (?, ?, 1, ?)")) {
                ps.setString(1, "Test Lab");
                ps.setString(2, "TEST-LAB");
                ps.setInt(3, adminId);
                ps.executeUpdate();
                try (Statement idStmt = conn.createStatement();
                     ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        labId = rs.getInt(1);
                    }
                }
            }
        }
        seedMember(conn, labId, adminId, "ADMIN");
        seedMember(conn, labId, professorId, "PROFESSOR");
        seedMember(conn, labId, technicianId, "TECHNICIAN");
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE equipment SET lab_id = " + labId + " WHERE lab_id IS NULL");
            stmt.executeUpdate("UPDATE activity_log SET lab_id = " + labId + " WHERE lab_id IS NULL");
        }
    }

    private static void seedPresentationDemoLab(Connection conn) throws Exception {
        int adminId = ensureUser(conn, "admin", "admin123", "Administrator", "ADMIN", "ADMIN-RESET-2026");
        int professorId = ensureUser(conn, "professor", "professor123", "Professor", "PROFESSOR", "PROF-RESET-2026");
        int technicianId = ensureUser(conn, "technician", "technician123", "Technician", "TECHNICIAN", "TECH-RESET-2026");
        int studentAlphaId = ensureUser(conn, "student.alpha", "student123", "Mara Ionescu", "STUDENT", "ALPHA-RESET-2026");
        int studentBetaId = ensureUser(conn, "student.beta", "student123", "David Popa", "STUDENT", "BETA-RESET-2026");
        int studentGammaId = ensureUser(conn, "student.gamma", "student123", "Ana Dumitrescu", "STUDENT", "GAMMA-RESET-2026");
        int guestDemoId = ensureUser(conn, "guest.demo", "guest123", "Visiting Guest", "GUEST", "GUEST-RESET-2026");

        migrateLegacyPresentationDemoLab(conn);
        int labId = ensureLab(conn, "Lab Normal", "LAB-NORMAL", adminId, true, "GREEN");
        seedMember(conn, labId, adminId, "ADMIN");
        seedMember(conn, labId, professorId, "PROFESSOR");
        seedMember(conn, labId, technicianId, "TECHNICIAN");
        seedMember(conn, labId, studentAlphaId, "STUDENT");
        seedMember(conn, labId, studentBetaId, "STUDENT");
        seedMember(conn, labId, studentGammaId, "STUDENT");
        seedMember(conn, labId, guestDemoId, "GUEST");

        Map<String, Integer> tags = new LinkedHashMap<>();
        tags.put("fragile", ensureTag(conn, labId, "fragile", "#F59E0B"));
        tags.put("expensive", ensureTag(conn, labId, "expensive", "#EF4444"));
        tags.put("requires training", ensureTag(conn, labId, "requires training", "#8B5CF6"));
        tags.put("robotics", ensureTag(conn, labId, "robotics", "#06B6D4"));
        tags.put("consumable", ensureTag(conn, labId, "consumable", "#10B981"));
        tags.put("maintenance", ensureTag(conn, labId, "maintenance", "#F97316"));
        tags.put("presentation demo", ensureTag(conn, labId, "presentation demo", "#EC4899"));

        Map<String, Integer> containers = new LinkedHashMap<>();
        containers.put("Robotics Bay", ensureContainer(conn, labId, "Robotics Bay"));
        containers.put("Electronics Bench", ensureContainer(conn, labId, "Electronics Bench"));
        containers.put("Chemistry Storage", ensureContainer(conn, labId, "Chemistry Storage"));
        containers.put("Safety Locker", ensureContainer(conn, labId, "Safety Locker"));
        containers.put("Teacher Cabinet", ensureContainer(conn, labId, "Teacher Cabinet"));

        LocalDate today = LocalDate.now();
        int arduinoCartId = ensureEquipment(conn, labId, containers.get("Robotics Bay"), new DemoEquipment(
                "DEMO-RBT-001", "Arduino Classroom Cart", "Robotics", "Robotics Zone",
                "AVAILABLE", "Mobile charging and transport cart for Arduino classroom kits.",
                "LabFlow Demo", "ARD-CART-12",
                today.minusDays(220), today.minusDays(20), 90, today.plusDays(70),
                "Quarterly controller firmware inspection", "Prepared for the annual robotics showcase.",
                "ASSET", 1, 0, null, false, List.of("robotics", "requires training", "presentation demo")
        ), tags);
        int printerId = ensureEquipment(conn, labId, containers.get("Robotics Bay"), new DemoEquipment(
                "DEMO-RBT-002", "Prusa MK4 3D Printer", "Robotics", "Innovation Corner",
                "BORROWED", "High-use prototyping printer for capstone teams.",
                "Prusa Research", "MK4",
                today.minusDays(310), today.minusDays(35), 60, today.plusDays(25),
                "Nozzle calibration before each public demo", "Currently allocated to a senior robotics project.",
                "ASSET", 1, 0, null, false, List.of("robotics", "expensive", "requires training", "presentation demo")
        ), tags);
        int oscilloscopeId = ensureEquipment(conn, labId, containers.get("Electronics Bench"), new DemoEquipment(
                "DEMO-ELC-001", "Rigol Oscilloscope DS1054Z", "Electronics", "Bench E2",
                "BORROWED", "Four-channel oscilloscope used in instrumentation labs.",
                "Rigol", "DS1054Z",
                today.minusDays(410), today.minusDays(48), 120, today.plusDays(72),
                "Probe compensation check recommended monthly", "Marked in demos as an overdue borrow example.",
                "ASSET", 1, 0, null, false, List.of("fragile", "expensive", "presentation demo")
        ), tags);
        int solderingStationId = ensureEquipment(conn, labId, containers.get("Electronics Bench"), new DemoEquipment(
                "DEMO-ELC-002", "Hakko Soldering Station FX-888D", "Electronics", "Bench E4",
                "DEFECT", "Bench soldering station with thermal stability issues.",
                "Hakko", "FX-888D",
                today.minusDays(260), today.minusDays(90), 90, today.minusDays(2),
                "Heating element requires replacement", "Open defect example for technician workflow.",
                "ASSET", 1, 0, null, false, List.of("maintenance", "presentation demo")
        ), tags);
        int multimeterId = ensureEquipment(conn, labId, containers.get("Electronics Bench"), new DemoEquipment(
                "DEMO-ELC-003", "Digital Multimeter Classroom Set", "Electronics", "Bench E1",
                "AVAILABLE", "Shared multimeter set for beginner diagnostics practice.",
                "Keysight", "U1232A",
                today.minusDays(500), today.minusDays(61), 180, today.plusDays(119),
                "Annual calibration on summer break", "Includes spare probes and silicone sleeves.",
                "ASSET", 1, 0, null, false, List.of("presentation demo")
        ), tags);
        int spectrophotometerId = ensureEquipment(conn, labId, containers.get("Chemistry Storage"), new DemoEquipment(
                "DEMO-CHE-001", "Vernier Spectrophotometer", "Chemistry", "Chemistry Room",
                "AVAILABLE", "Advanced optics station for absorbance experiments.",
                "Vernier", "SpectroVis Plus",
                today.minusDays(365), today.minusDays(62), 120, today.plusDays(58),
                "Store in anti-dust cabinet after use", "Commonly reserved before chemistry practical exams.",
                "ASSET", 1, 0, null, false, List.of("fragile", "expensive", "presentation demo")
        ), tags);
        int centrifugeId = ensureEquipment(conn, labId, containers.get("Chemistry Storage"), new DemoEquipment(
                "DEMO-CHE-002", "Mini Centrifuge", "Chemistry", "Chemistry Room",
                "MAINTENANCE", "Mini centrifuge currently blocked for safety inspection.",
                "Eppendorf", "MiniSpin",
                today.minusDays(280), today.minusDays(95), 60, today.minusDays(12),
                "Latch safety review pending", "Maintenance overdue example used in dashboard alerts.",
                "ASSET", 1, 0, null, false, List.of("maintenance", "presentation demo")
        ), tags);
        int safetyGogglesId = ensureEquipment(conn, labId, containers.get("Safety Locker"), new DemoEquipment(
                "DEMO-SAF-001", "Safety Goggles Classroom Set", "Safety", "Safety Locker",
                "AVAILABLE", "Shared set of classroom goggles for chemistry and fabrication sessions.",
                "Uvex", "Lab Guard",
                today.minusDays(180), today.minusDays(15), 180, today.plusDays(165),
                "Sanitize after shared sessions", "Used to demo reservations and quick borrow flows.",
                "ASSET", 1, 0, null, false, List.of("presentation demo")
        ), tags);
        int glovesId = ensureEquipment(conn, labId, containers.get("Safety Locker"), new DemoEquipment(
                "DEMO-CON-001", "Nitrile Gloves Box", "Consumables", "Safety Locker",
                "AVAILABLE", "Disposable gloves for wet chemistry and maintenance tasks.",
                "Kimtech", "NITRILE-M",
                today.minusDays(40), today.minusDays(5), 30, today.plusDays(25),
                "Restock before next large wet lab", "Low stock consumable example.",
                "CONSUMABLE", 6, 15, "boxes", false, List.of("consumable", "presentation demo")
        ), tags);
        int stripsId = ensureEquipment(conn, labId, containers.get("Chemistry Storage"), new DemoEquipment(
                "DEMO-CON-002", "pH Indicator Strips", "Consumables", "Chemistry Storage",
                "AVAILABLE", "Fast indicator strips for acid-base experiments.",
                "Merck", "PH-STRIP-100",
                today.minusDays(24), today.minusDays(3), 30, today.plusDays(27),
                "Reorder before public demo day", "Second low stock example.",
                "CONSUMABLE", 9, 10, "packs", false, List.of("consumable", "presentation demo")
        ), tags);
        int laptopId = ensureEquipment(conn, labId, containers.get("Teacher Cabinet"), new DemoEquipment(
                "DEMO-CMP-001", "Laptop Station 01", "Computers", "Teacher Cabinet",
                "AVAILABLE", "Instructor laptop for projection, QR scans, and live dashboards.",
                "Dell", "Latitude 7440",
                today.minusDays(160), today.minusDays(18), 120, today.plusDays(102),
                "Battery health check each semester", "Returned-on-time example asset.",
                "ASSET", 1, 0, null, false, List.of("fragile", "expensive", "presentation demo")
        ), tags);
        ensureEquipment(conn, labId, containers.get("Teacher Cabinet"), new DemoEquipment(
                "DEMO-ARC-001", "Legacy Bench Power Supply", "Electronics", "Teacher Cabinet",
                "AVAILABLE", "Old supply retained only for archive/restore demonstrations.",
                "Korad", "KA3005D",
                today.minusDays(900), today.minusDays(400), 180, today.minusDays(220),
                "Archived after replacement program", "Archived example asset.",
                "ASSET", 1, 0, null, true, List.of("presentation demo")
        ), tags);
        ensureEquipment(conn, labId, containers.get("Teacher Cabinet"), new DemoEquipment(
                "DEMO-RET-001", "VR Headset Mk I", "Immersive Tech", "Teacher Cabinet",
                "RETIRED", "First-generation headset kept for retirement demos only.",
                "Meta", "Quest Legacy",
                today.minusDays(1020), today.minusDays(300), 180, today.minusDays(120),
                "Retired after repeated tracking issues", "Retired equipment example.",
                "ASSET", 1, 0, null, false, List.of("presentation demo")
        ), tags);

        seedMaintenanceRecord(conn, labId, arduinoCartId, technicianId, today.minusDays(20), "Firmware updated and wheel brakes tightened.", "COMPLETED");
        seedMaintenanceRecord(conn, labId, printerId, technicianId, today.minusDays(35), "Nozzle cleaned and bed level recalibrated.", "COMPLETED");
        seedMaintenanceRecord(conn, labId, spectrophotometerId, technicianId, today.minusDays(62), "Optics bay dust removed and calibration verified.", "COMPLETED");
        seedMaintenanceRecord(conn, labId, centrifugeId, technicianId, today.minusDays(95), "Lid lock became unreliable during inspection.", "FOLLOW_UP_REQUIRED");

        int overdueBorrowId = ensureBorrowRecord(conn, oscilloscopeId, studentAlphaId,
                today.minusDays(24).atTime(10, 15), today.minusDays(14), null,
                "ACTIVE", null, "[DEMO-BORROW-OVERDUE] Overdue oscilloscope for instrumentation capstone.");
        int activeBorrowId = ensureBorrowRecord(conn, printerId, studentBetaId,
                today.minusDays(4).atTime(14, 5), today.plusDays(3), null,
                "ACTIVE", null, "[DEMO-BORROW-ACTIVE] 3D printer reserved for robotics assembly.");
        int returnedOnTimeBorrowId = ensureBorrowRecord(conn, laptopId, studentGammaId,
                today.minusDays(9).atTime(9, 30), today.minusDays(4), today.minusDays(5).atTime(16, 20),
                "RETURNED", "GOOD", "[DEMO-BORROW-ONTIME] Laptop returned before workshop closing.");
        int returnedDefectBorrowId = ensureBorrowRecord(conn, multimeterId, guestDemoId,
                today.minusDays(15).atTime(11, 10), today.minusDays(8), today.minusDays(7).atTime(12, 45),
                "RETURNED_DEFECT", "DEFECT", "[DEMO-BORROW-DEFECT] Multimeter returned with damaged probe lead.");
        int returnedLateBorrowId = ensureBorrowRecord(conn, arduinoCartId, studentAlphaId,
                today.minusDays(20).atTime(8, 45), today.minusDays(13), today.minusDays(10).atTime(13, 0),
                "RETURNED", "GOOD", "[DEMO-BORROW-LATE] Arduino cart returned after showcase rehearsal.");
        ensureBorrowRecord(conn, safetyGogglesId, professorId,
                today.minusDays(6).atTime(9, 0), today.minusDays(2), today.minusDays(2).atTime(15, 10),
                "RETURNED", "GOOD", "[DEMO-BORROW-PROF] Safety gear issued for faculty chemistry supervision.");

        int checklistTemplateId = ensureChecklistTemplate(conn, labId, "Electronics Return Checklist", "Electronics");
        ensureChecklistItem(conn, checklistTemplateId, "Power cable included", true, 1);
        ensureChecklistItem(conn, checklistTemplateId, "No visible casing damage", true, 2);
        ensureChecklistItem(conn, checklistTemplateId, "Accessories returned", true, 3);
        seedChecklistResult(conn, returnedDefectBorrowId, "Power cable included", true, "Returned with the original power lead.");
        seedChecklistResult(conn, returnedDefectBorrowId, "No visible casing damage", false, "Probe insulation cracked near the handle.");
        seedChecklistResult(conn, returnedDefectBorrowId, "Accessories returned", true, "Protective pouch returned.");

        ensureFaultReport(conn, solderingStationId, studentBetaId, technicianId,
                "[DEMO-FAULT-1] Heating element trips the breaker during warm-up.", "MAJOR", "HIGH",
                "OPEN", today.minusDays(6).atTime(12, 20), today.minusDays(6).atTime(12, 20), null,
                "Awaiting replacement part approval.");
        ensureFaultReport(conn, centrifugeId, professorId, technicianId,
                "[DEMO-FAULT-2] Rotor lid lock fails during spin cycle.", "CRITICAL", "URGENT",
                "IN_PROGRESS", today.minusDays(3).atTime(9, 40), today.minusDays(1).atTime(11, 0), null,
                "Technician isolated the unit and ordered a replacement latch.");
        ensureFaultReport(conn, multimeterId, guestDemoId, technicianId,
                "[DEMO-FAULT-3] Probe readings fluctuate under load.", "MINOR", "LOW",
                "RESOLVED", today.minusDays(12).atTime(15, 10), today.minusDays(7).atTime(13, 35), today.minusDays(7).atTime(13, 35),
                "Replaced the damaged probe and verified calibration.");
        ensureFaultReport(conn, printerId, studentGammaId, null,
                "[DEMO-FAULT-4] Print quality dropped after support material jam.", "MINOR", "NORMAL",
                "REJECTED", today.minusDays(18).atTime(16, 15), today.minusDays(16).atTime(10, 0), today.minusDays(16).atTime(10, 0),
                "Issue was maintenance debris, not a hardware defect.");

        ensureReservation(conn, labId, spectrophotometerId, professorId,
                today.plusDays(1).atTime(10, 0), today.plusDays(1).atTime(13, 0),
                "APPROVED", "[DEMO-RES-APPROVED] Faculty chemistry practical block.");
        ensureReservation(conn, labId, arduinoCartId, studentGammaId,
                today.plusDays(5).atTime(9, 0), today.plusDays(5).atTime(12, 0),
                "PENDING", "[DEMO-RES-PENDING] Student robotics sprint planning session.");
        ensureReservation(conn, labId, safetyGogglesId, guestDemoId,
                today.plusDays(7).atTime(14, 0), today.plusDays(7).atTime(16, 0),
                "REJECTED", "[DEMO-RES-REJECTED] Guest request missing supervisor approval.");
        ensureReservation(conn, labId, multimeterId, studentBetaId,
                today.minusDays(8).atTime(11, 0), today.minusDays(8).atTime(13, 0),
                "COMPLETED", "[DEMO-RES-COMPLETED] Finished electronics calibration workshop.");
        ensureReservation(conn, labId, laptopId, studentAlphaId,
                today.plusDays(2).atTime(8, 30), today.plusDays(2).atTime(10, 30),
                "CANCELLED", "[DEMO-RES-CANCELLED] Laptop no longer needed after room switch.");

        seedStockMovement(conn, labId, glovesId, technicianId, "CONSUME_STOCK", -8, 14, 6, "Used during chemistry practical preparation.");
        seedStockMovement(conn, labId, stripsId, professorId, "CONSUME_STOCK", -3, 12, 9, "Used for acid-base demonstration.");
        seedStockMovement(conn, labId, glovesId, technicianId, "ADD_STOCK", 14, 0, 14, "Initial stock for the demo laboratory.");
        seedStockMovement(conn, labId, stripsId, technicianId, "ADD_STOCK", 12, 0, 12, "Initial stock for the demo laboratory.");

        seedNotification(conn, labId, adminId, "Critical fault reported", "Mini Centrifuge requires urgent attention before the next chemistry session.", "DANGER", "FAULT_REPORT", centrifugeId);
        seedNotification(conn, labId, adminId, "Maintenance overdue", "Mini Centrifuge has passed its maintenance window by 12 days.", "WARNING", "EQUIPMENT", centrifugeId);
        seedNotification(conn, labId, technicianId, "New fault assignment", "You were assigned the centrifuge lid-lock issue for follow-up.", "INFO", "FAULT_REPORT", centrifugeId);
        seedNotification(conn, labId, professorId, "Reservation approved", "Spectrophotometer reservation is approved for tomorrow's lab block.", "SUCCESS", "RESERVATION", spectrophotometerId);
        seedNotification(conn, labId, adminId, "Low stock warning", "Nitrile Gloves Box is below minimum threshold.", "WARNING", "EQUIPMENT", glovesId);
        seedNotification(conn, labId, adminId, "Pending reservation review", "Arduino Classroom Cart has a pending reservation for next week.", "INFO", "RESERVATION", arduinoCartId);

        seedActivityLog(conn, adminId, labId, "CREATE_LAB", "LAB", labId, "[DEMO-ACT-1] Created Lab Normal for live judging.");
        seedActivityLog(conn, technicianId, labId, "MARK_MAINTENANCE_COMPLETED", "EQUIPMENT", arduinoCartId, "[DEMO-ACT-2] Completed maintenance on Arduino Classroom Cart.");
        seedActivityLog(conn, studentAlphaId, labId, "BORROW_EQUIPMENT", "BORROW_RECORD", overdueBorrowId, "[DEMO-ACT-3] Borrowed oscilloscope for capstone instrumentation.");
        seedActivityLog(conn, studentBetaId, labId, "REPORT_FAULT", "FAULT_REPORT", solderingStationId, "[DEMO-ACT-4] Reported soldering station breaker issue.");
        seedActivityLog(conn, professorId, labId, "CREATE_RESERVATION", "RESERVATION", spectrophotometerId, "[DEMO-ACT-5] Reserved spectrophotometer for practical assessment.");
        seedActivityLog(conn, adminId, labId, "CHANGE_ACCENT_COLOR", "LAB", labId, "[DEMO-ACT-6] Updated the Lab Normal palette for presentation visuals.");
        seedActivityLog(conn, adminId, labId, "IMPORT_TEST_KITS", "LAB", labId, "[DEMO-ACT-7] Imported starter kits for public demo.");

        seedPointsHistory(conn, labId, studentAlphaId, List.of(
                new DemoPointsEntry(10, "Returnare la timp", today.minusDays(21).atTime(17, 0)),
                new DemoPointsEntry(-5, "Penalizare returnare întârziată", today.minusDays(10).atTime(13, 5)),
                new DemoPointsEntry(5, "Raportare defecțiune", today.minusDays(6).atTime(12, 30))
        ));
        seedPointsHistory(conn, labId, studentBetaId, List.of(
                new DemoPointsEntry(10, "Returnare la timp", today.minusDays(28).atTime(15, 40)),
                new DemoPointsEntry(5, "Raportare defecțiune", today.minusDays(6).atTime(12, 45)),
                new DemoPointsEntry(10, "Presentation demo contribution", today.minusDays(2).atTime(9, 0))
        ));
        seedPointsHistory(conn, labId, studentGammaId, List.of(
                new DemoPointsEntry(10, "Returnare la timp", today.minusDays(5).atTime(16, 22)),
                new DemoPointsEntry(5, "Raportare defecțiune", today.minusDays(18).atTime(16, 20))
        ));
        seedPointsHistory(conn, labId, guestDemoId, List.of(
                new DemoPointsEntry(5, "Raportare defecțiune", today.minusDays(12).atTime(15, 15)),
                new DemoPointsEntry(-5, "Penalizare returnare întârziată", today.minusDays(7).atTime(12, 50))
        ));

        seedWeeklyReport(conn, labId, currentWeekStart(),
                "The lab stayed active this week with multiple student borrows, one overdue oscilloscope loan, and a critical centrifuge issue already routed to the technician. Low stock alerts remain active for nitrile gloves and pH indicator strips. Prioritize the centrifuge repair, close the overdue borrow with student follow-up, and review the pending Arduino cart reservation before the robotics sprint.");
    }

    private static void migrateLegacyPresentationDemoLab(Connection conn) throws Exception {
        int legacyLabId = labIdByInvite(conn, "DEMO-LAB");
        int normalLabId = labIdByInvite(conn, "LAB-NORMAL");
        if (legacyLabId <= 0 || normalLabId > 0) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE labs
                SET name = ?, invite_code = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            ps.setString(1, "Lab Normal");
            ps.setString(2, "LAB-NORMAL");
            ps.setInt(3, legacyLabId);
            ps.executeUpdate();
        }
    }

    private static int ensureUser(Connection conn, String username, String password, String fullName, String role, String recoveryKey) throws Exception {
        seedUser(conn, username, password, fullName, role);
        seedRecoveryKeyIfMissing(conn, username, recoveryKey);
        return userId(conn, username);
    }

    private static int ensureLab(Connection conn, String name, String inviteCode, int createdByUserId, boolean protectedLab, String palette) throws Exception {
        int labId = labIdByInvite(conn, inviteCode);
        if (labId <= 0) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO labs (name, invite_code, protected_lab, created_by_user_id, color_palette)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                ps.setString(1, name);
                ps.setString(2, inviteCode);
                ps.setInt(3, protectedLab ? 1 : 0);
                ps.setInt(4, createdByUserId);
                ps.setString(5, palette);
                ps.executeUpdate();
                try (Statement idStmt = conn.createStatement();
                     ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return labIdByInvite(conn, inviteCode);
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE labs
                SET name = ?, protected_lab = ?, created_by_user_id = ?,
                    color_palette = CASE
                        WHEN color_palette IS NULL OR trim(color_palette) = '' THEN ?
                        ELSE color_palette
                    END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """)) {
            ps.setString(1, name);
            ps.setInt(2, protectedLab ? 1 : 0);
            ps.setInt(3, createdByUserId);
            ps.setString(4, palette);
            ps.setInt(5, labId);
            ps.executeUpdate();
        }
        return labId;
    }

    private static int ensureTag(Connection conn, int labId, String name, String color) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tags WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO tags (lab_id, name, color) VALUES (?, ?, ?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            ps.setString(3, color);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tags WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int ensureContainer(Connection conn, int labId, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM equipment_containers WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO equipment_containers (lab_id, name) VALUES (?, ?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM equipment_containers WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int ensureEquipment(Connection conn, int labId, Integer containerId, DemoEquipment spec, Map<String, Integer> tagIds) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, qr_code FROM equipment WHERE serial_number = ?")) {
            ps.setString(1, spec.serialNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int equipmentId = rs.getInt("id");
                    if (rs.getString("qr_code") == null || rs.getString("qr_code").isBlank()) {
                        updateQrCode(conn, equipmentId);
                    }
                    seedEquipmentTags(conn, equipmentId, spec.tagNames(), tagIds);
                    return equipmentId;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO equipment (lab_id, name, category, description, location, status, qr_code, qr_code_path,
                container_id, serial_number, manufacturer, model, purchase_date, last_maintenance_date,
                maintenance_interval_days, next_maintenance_date, maintenance_notes, notes,
                item_type, quantity, minimum_quantity, unit, is_archived, archived_at, archived_by_user_id, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """)) {
            ps.setInt(1, labId);
            ps.setString(2, spec.name());
            ps.setString(3, spec.category());
            ps.setString(4, spec.description());
            ps.setString(5, spec.location());
            ps.setString(6, spec.status());
            if (containerId == null || containerId <= 0) {
                ps.setObject(7, null);
            } else {
                ps.setInt(7, containerId);
            }
            ps.setString(8, spec.serialNumber());
            ps.setString(9, spec.manufacturer());
            ps.setString(10, spec.model());
            ps.setString(11, formatDate(spec.purchaseDate()));
            ps.setString(12, formatDate(spec.lastMaintenanceDate()));
            if (spec.maintenanceIntervalDays() == null) {
                ps.setObject(13, null);
            } else {
                ps.setInt(13, spec.maintenanceIntervalDays());
            }
            ps.setString(14, formatDate(spec.nextMaintenanceDate()));
            ps.setString(15, spec.maintenanceNotes());
            ps.setString(16, spec.notes());
            ps.setString(17, spec.itemType());
            ps.setInt(18, spec.quantity());
            ps.setInt(19, spec.minimumQuantity());
            ps.setString(20, spec.unit());
            ps.setInt(21, spec.archived() ? 1 : 0);
            ps.setString(22, spec.archived() ? formatDateTime(LocalDateTime.now().minusDays(30)) : null);
            ps.setObject(23, spec.archived() ? userId(conn, "admin") : null);
            ps.executeUpdate();
        }
        int equipmentId = -1;
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM equipment WHERE serial_number = ?")) {
            ps.setString(1, spec.serialNumber());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    equipmentId = rs.getInt(1);
                }
            }
        }
        if (equipmentId > 0) {
            updateQrCode(conn, equipmentId);
            seedEquipmentTags(conn, equipmentId, spec.tagNames(), tagIds);
        }
        return equipmentId;
    }

    private static void updateQrCode(Connection conn, int equipmentId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE equipment SET qr_code = COALESCE(qr_code, ?), updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, "LABFLOW-EQ-" + equipmentId);
            ps.setInt(2, equipmentId);
            ps.executeUpdate();
        }
    }

    private static void seedEquipmentTags(Connection conn, int equipmentId, List<String> tagNames, Map<String, Integer> tagIds) throws Exception {
        if (tagNames == null) {
            return;
        }
        for (String tagName : tagNames) {
            Integer tagId = tagIds.get(tagName);
            if (tagId == null || tagId <= 0) {
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO equipment_tags (equipment_id, tag_id) VALUES (?, ?)")) {
                ps.setInt(1, equipmentId);
                ps.setInt(2, tagId);
                ps.executeUpdate();
            }
        }
    }

    private static void seedMaintenanceRecord(Connection conn, int labId, int equipmentId, int userId, LocalDate maintenanceDate, String notes, String resultStatus) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM maintenance_records
                WHERE lab_id = ? AND equipment_id = ? AND maintenance_date = ? AND coalesce(result_status, '') = coalesce(?, '')
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, equipmentId);
            ps.setString(3, formatDate(maintenanceDate));
            ps.setString(4, resultStatus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO maintenance_records (lab_id, equipment_id, performed_by_user_id, maintenance_date, notes, result_status)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, userId);
            ps.setString(4, formatDate(maintenanceDate));
            ps.setString(5, notes);
            ps.setString(6, resultStatus);
            ps.executeUpdate();
        }
    }

    private static int ensureBorrowRecord(Connection conn, int equipmentId, int userId, LocalDateTime borrowDate, LocalDate expectedReturnDate,
                                          LocalDateTime actualReturnDate, String status, String returnCondition, String notes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM borrow_records WHERE notes = ?")) {
            ps.setString(1, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO borrow_records (equipment_id, user_id, borrow_date, expected_return_date, actual_return_date, status, return_condition, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, userId);
            ps.setString(3, formatDateTime(borrowDate));
            ps.setString(4, formatDate(expectedReturnDate));
            ps.setString(5, formatDateTime(actualReturnDate));
            ps.setString(6, status);
            ps.setString(7, returnCondition);
            ps.setString(8, notes);
            ps.setString(9, formatDateTime(borrowDate));
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM borrow_records WHERE notes = ?")) {
            ps.setString(1, notes);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int ensureChecklistTemplate(Connection conn, int labId, String name, String equipmentCategory) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM checklist_templates WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO checklist_templates (lab_id, name, equipment_category) VALUES (?, ?, ?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            ps.setString(3, equipmentCategory);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM checklist_templates WHERE lab_id = ? AND lower(name) = lower(?)")) {
            ps.setInt(1, labId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void ensureChecklistItem(Connection conn, int templateId, String text, boolean required, int sortOrder) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM checklist_items WHERE template_id = ? AND lower(text) = lower(?)")) {
            ps.setInt(1, templateId);
            ps.setString(2, text);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO checklist_items (template_id, text, required, sort_order) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, templateId);
            ps.setString(2, text);
            ps.setInt(3, required ? 1 : 0);
            ps.setInt(4, sortOrder);
            ps.executeUpdate();
        }
    }

    private static void seedChecklistResult(Connection conn, int borrowRecordId, String itemText, boolean checked, String notes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM return_checklist_results
                WHERE borrow_record_id = ? AND lower(checklist_item_text) = lower(?)
                """)) {
            ps.setInt(1, borrowRecordId);
            ps.setString(2, itemText);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO return_checklist_results (borrow_record_id, checklist_item_text, checked, notes)
                VALUES (?, ?, ?, ?)
                """)) {
            ps.setInt(1, borrowRecordId);
            ps.setString(2, itemText);
            ps.setInt(3, checked ? 1 : 0);
            ps.setString(4, notes);
            ps.executeUpdate();
        }
    }

    private static void ensureFaultReport(Connection conn, int equipmentId, int reportedByUserId, Integer assignedToUserId,
                                          String description, String severity, String priority, String status,
                                          LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt,
                                          String resolutionNotes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM fault_reports WHERE description = ?")) {
            ps.setString(1, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO fault_reports (equipment_id, reported_by_user_id, assigned_to_user_id, description, severity, priority, status,
                created_at, updated_at, resolved_at, resolution_notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, equipmentId);
            ps.setInt(2, reportedByUserId);
            if (assignedToUserId == null) {
                ps.setObject(3, null);
            } else {
                ps.setInt(3, assignedToUserId);
            }
            ps.setString(4, description);
            ps.setString(5, severity);
            ps.setString(6, priority);
            ps.setString(7, status);
            ps.setString(8, formatDateTime(createdAt));
            ps.setString(9, formatDateTime(updatedAt));
            ps.setString(10, formatDateTime(resolvedAt));
            ps.setString(11, resolutionNotes);
            ps.executeUpdate();
        }
    }

    private static void ensureReservation(Connection conn, int labId, int equipmentId, int userId,
                                          LocalDateTime startDateTime, LocalDateTime endDateTime,
                                          String status, String notes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM reservations WHERE notes = ?")) {
            ps.setString(1, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO reservations (lab_id, equipment_id, user_id, start_datetime, end_datetime, status, notes, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, userId);
            ps.setString(4, formatDateTime(startDateTime));
            ps.setString(5, formatDateTime(endDateTime));
            ps.setString(6, status);
            ps.setString(7, notes);
            ps.setString(8, formatDateTime(startDateTime.minusDays(1)));
            ps.setString(9, formatDateTime(startDateTime.minusHours(12)));
            ps.executeUpdate();
        }
    }

    private static void seedStockMovement(Connection conn, int labId, int equipmentId, int userId, String movementType,
                                          int quantityChange, int oldQuantity, int newQuantity, String notes) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM stock_movements
                WHERE lab_id = ? AND equipment_id = ? AND movement_type = ? AND quantity_change = ? AND notes = ?
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, equipmentId);
            ps.setString(3, movementType);
            ps.setInt(4, quantityChange);
            ps.setString(5, notes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO stock_movements (lab_id, equipment_id, user_id, movement_type, quantity_change, old_quantity, new_quantity, notes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, equipmentId);
            ps.setInt(3, userId);
            ps.setString(4, movementType);
            ps.setInt(5, quantityChange);
            ps.setInt(6, oldQuantity);
            ps.setInt(7, newQuantity);
            ps.setString(8, notes);
            ps.setString(9, formatDateTime(LocalDateTime.now().minusDays(3)));
            ps.executeUpdate();
        }
    }

    private static void seedNotification(Connection conn, int labId, int userId, String title, String message, String type,
                                         String entityType, int entityId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM notifications WHERE lab_id = ? AND user_id = ? AND title = ?")) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            ps.setString(3, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO notifications (lab_id, user_id, title, message, type, is_read, entity_type, entity_id, created_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)
                """)) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, type);
            ps.setString(6, entityType);
            ps.setInt(7, entityId);
            ps.setString(8, formatDateTime(LocalDateTime.now().minusHours(6)));
            ps.executeUpdate();
        }
    }

    private static void seedActivityLog(Connection conn, int userId, int labId, String action, String entityType, int entityId, String description) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM activity_log WHERE description = ?")) {
            ps.setString(1, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO activity_log (user_id, action, entity_type, entity_id, description, timestamp, lab_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, entityType);
            ps.setInt(4, entityId);
            ps.setString(5, description);
            ps.setString(6, formatDateTime(LocalDateTime.now().minusHours(4)));
            ps.setInt(7, labId);
            ps.executeUpdate();
        }
    }

    private static void seedPointsHistory(Connection conn, int labId, int userId, List<DemoPointsEntry> entries) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM points_history WHERE lab_id = ? AND user_id = ?")) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        int points = 0;
        int totalEarned = 0;
        for (DemoPointsEntry entry : entries) {
            points += entry.pointsDelta();
            if (entry.pointsDelta() > 0) {
                totalEarned += entry.pointsDelta();
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO points_history (user_id, lab_id, points_delta, reason, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                ps.setInt(1, userId);
                ps.setInt(2, labId);
                ps.setInt(3, entry.pointsDelta());
                ps.setString(4, entry.reason());
                ps.setString(5, formatDateTime(entry.createdAt()));
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO user_points (user_id, lab_id, points, total_earned)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(user_id, lab_id) DO NOTHING
                """)) {
            ps.setInt(1, userId);
            ps.setInt(2, labId);
            ps.setInt(3, points);
            ps.setInt(4, totalEarned);
            ps.executeUpdate();
        }
    }

    private static void seedWeeklyReport(Connection conn, int labId, LocalDate weekStart, String content) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM weekly_reports WHERE lab_id = ? AND week_start = ?")) {
            ps.setInt(1, labId);
            ps.setString(2, formatDate(weekStart));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO weekly_reports (lab_id, week_start, content) VALUES (?, ?, ?)")) {
            ps.setInt(1, labId);
            ps.setString(2, formatDate(weekStart));
            ps.setString(3, content);
            ps.executeUpdate();
        }
    }

    private static String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.toString().replace('T', ' ');
    }

    private static LocalDate currentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private static int userId(Connection conn, String username) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private static int labIdByInvite(Connection conn, String inviteCode) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM labs WHERE invite_code = ?")) {
            ps.setString(1, inviteCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void seedMember(Connection conn, int labId, int userId, String role) throws Exception {
        if (labId <= 0 || userId <= 0) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO lab_members (lab_id, user_id, role) VALUES (?, ?, ?)")) {
            ps.setInt(1, labId);
            ps.setInt(2, userId);
            ps.setString(3, role);
            ps.executeUpdate();
        }
    }

    private record DemoEquipment(
            String serialNumber,
            String name,
            String category,
            String location,
            String status,
            String description,
            String manufacturer,
            String model,
            LocalDate purchaseDate,
            LocalDate lastMaintenanceDate,
            Integer maintenanceIntervalDays,
            LocalDate nextMaintenanceDate,
            String maintenanceNotes,
            String notes,
            String itemType,
            int quantity,
            int minimumQuantity,
            String unit,
            boolean archived,
            List<String> tagNames
    ) {
    }

    private record DemoPointsEntry(int pointsDelta, String reason, LocalDateTime createdAt) {
    }
}
