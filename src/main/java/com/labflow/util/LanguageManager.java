package com.labflow.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public final class LanguageManager {
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(LanguageManager.class);
    private static final String KEY = "language";
    private static final Map<String, LanguagePack> LANGUAGES = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> EXTRA_VALUES = new LinkedHashMap<>();

    static {
        add("en", "English", Map.ofEntries(
                entry("language", "Language"),
                entry("subtitle", "Laboratory Equipment Management"),
                entry("username", "Username"),
                entry("password", "Password"),
                entry("remember", "Remember me"),
                entry("login", "Login"),
                entry("signup", "Sign Up"),
                entry("testUsers", "Test users: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Laboratory Equipment Management System"),
                entry("emptyCredentials", "Please enter username and password."),
                entry("invalidCredentials", "Invalid username or password."),
                entry("createAccount", "Create Account"),
                entry("fullName", "Full Name"),
                entry("loadingStatus", "Preparing your workspace"),
                entry("cancel", "Cancel"),
                entry("forgotPassword", "Forgot password?"),
                entry("recentAccounts", "Recent accounts"),
                entry("reEnterPassword", "Re-enter password"),
                entry("continueAs", "Continue as recent user"),
                entry("continueButton", "Continue"),
                entry("recentLoginExpired", "More than 24 hours have passed since this account was last verified. Re-enter the password to continue."),
                entry("confirmQuickSwitch", "Continue with recent account"),
                entry("switchPasswordRequired", "That recent account needs a password confirmation."),
                entry("recoveryKey", "Recovery key"),
                entry("newPassword", "New password"),
                entry("resetPassword", "Reset password"),
                entry("forgotPasswordHint", "No email required. Use the recovery key that was generated for this account."),
                entry("passwordResetSuccess", "Password updated. You can sign in now."),
                entry("saveRecoveryKey", "Save your recovery key"),
                entry("recoveryKeyIntro", "This key lets you reset the password locally later, without email."),
                entry("recoveryKeyStore", "Store this key somewhere safe. LabFlow will not show it again.")
        ), List.of(
                "Tip: Right-click equipment rows for quick actions.",
                "Tip: Export inventory reports from the Export section.",
                "Tip: Mark defective returns to create a fault report automatically.",
                "Tip: Use filters together to narrow inventory fast.",
                "Tip: Admins can review every important action in Activity Log.",
                "Tip: The theme switch follows you across sessions."
        ));
        add("ro", "Română", Map.ofEntries(
                entry("language", "Limbă"),
                entry("subtitle", "Managementul echipamentelor de laborator"),
                entry("username", "Utilizator"),
                entry("password", "Parolă"),
                entry("remember", "Ține-mă minte"),
                entry("login", "Autentificare"),
                entry("signup", "Creează cont"),
                entry("testUsers", "Utilizatori test: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Sistem de management pentru echipamente de laborator"),
                entry("emptyCredentials", "Introdu utilizatorul și parola."),
                entry("invalidCredentials", "Utilizator sau parolă incorectă."),
                entry("createAccount", "Creează cont"),
                entry("fullName", "Nume complet"),
                entry("loadingStatus", "Pregătim spațiul tău de lucru"),
                entry("cancel", "Anulează")
        ), List.of(
                "Sfat: Click dreapta pe echipamente pentru acțiuni rapide.",
                "Sfat: Exportă inventarul din secțiunea Export.",
                "Sfat: Returnările defecte creează automat raport de defecțiune.",
                "Sfat: Combină filtrele pentru căutări rapide.",
                "Sfat: Adminii pot vedea acțiunile importante în Activity Log.",
                "Sfat: Tema aleasă rămâne salvată între sesiuni."
        ));
        add("es", "Español", Map.ofEntries(
                entry("language", "Idioma"),
                entry("subtitle", "Gestión de equipos de laboratorio"),
                entry("username", "Usuario"),
                entry("password", "Contraseña"),
                entry("remember", "Recordarme"),
                entry("login", "Iniciar sesión"),
                entry("signup", "Crear cuenta"),
                entry("testUsers", "Usuarios de prueba: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Sistema de gestión de equipos de laboratorio"),
                entry("emptyCredentials", "Introduce usuario y contraseña."),
                entry("invalidCredentials", "Usuario o contraseña incorrectos."),
                entry("createAccount", "Crear cuenta"),
                entry("fullName", "Nombre completo"),
                entry("loadingStatus", "Preparando tu espacio de trabajo"),
                entry("cancel", "Cancelar")
        ), List.of(
                "Consejo: Haz clic derecho en los equipos para acciones rápidas.",
                "Consejo: Exporta el inventario desde la sección Export.",
                "Consejo: Las devoluciones defectuosas crean un reporte automáticamente.",
                "Consejo: Combina filtros para encontrar equipos rápido.",
                "Consejo: Los administradores pueden revisar acciones en Activity Log.",
                "Consejo: El cambio de tema se guarda entre sesiones."
        ));
        add("zh", "中文", Map.ofEntries(
                entry("language", "语言"),
                entry("subtitle", "实验室设备管理"),
                entry("username", "用户名"),
                entry("password", "密码"),
                entry("remember", "记住我"),
                entry("login", "登录"),
                entry("signup", "注册"),
                entry("testUsers", "测试用户：admin/admin123，professor/professor123，technician/technician123"),
                entry("footer", "LabFlow v1.0 - 实验室设备管理系统"),
                entry("emptyCredentials", "请输入用户名和密码。"),
                entry("invalidCredentials", "用户名或密码无效。"),
                entry("createAccount", "创建账户"),
                entry("fullName", "全名"),
                entry("loadingStatus", "正在准备你的工作区"),
                entry("cancel", "取消")
        ), List.of(
                "提示：右键点击设备行可快速操作。",
                "提示：可在 Export 区域导出库存报告。",
                "提示：损坏归还会自动创建故障报告。",
                "提示：组合筛选可以更快找到设备。",
                "提示：管理员可在 Activity Log 查看重要操作。",
                "提示：主题选择会在下次启动时保留。"
        ));
        add("ja", "日本語", Map.ofEntries(
                entry("language", "言語"),
                entry("subtitle", "実験室設備管理"),
                entry("username", "ユーザー名"),
                entry("password", "パスワード"),
                entry("remember", "ログイン状態を保存"),
                entry("login", "ログイン"),
                entry("signup", "アカウント作成"),
                entry("testUsers", "テストユーザー: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - 実験室設備管理システム"),
                entry("emptyCredentials", "ユーザー名とパスワードを入力してください。"),
                entry("invalidCredentials", "ユーザー名またはパスワードが正しくありません。"),
                entry("createAccount", "アカウント作成"),
                entry("fullName", "氏名"),
                entry("loadingStatus", "ワークスペースを準備中"),
                entry("cancel", "キャンセル")
        ), List.of(
                "ヒント: 設備行を右クリックすると素早く操作できます。",
                "ヒント: Export セクションから在庫を出力できます。",
                "ヒント: 故障返却は自動で故障レポートを作成します。",
                "ヒント: フィルターを組み合わせると素早く検索できます。",
                "ヒント: 管理者は Activity Log で重要な操作を確認できます。",
                "ヒント: テーマ設定はセッション間で保存されます。"
        ));
        add("ru", "Русский", Map.ofEntries(
                entry("language", "Язык"),
                entry("subtitle", "Управление лабораторным оборудованием"),
                entry("username", "Пользователь"),
                entry("password", "Пароль"),
                entry("remember", "Запомнить меня"),
                entry("login", "Войти"),
                entry("signup", "Создать аккаунт"),
                entry("testUsers", "Тестовые пользователи: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Система управления лабораторным оборудованием"),
                entry("emptyCredentials", "Введите имя пользователя и пароль."),
                entry("invalidCredentials", "Неверное имя пользователя или пароль."),
                entry("createAccount", "Создать аккаунт"),
                entry("fullName", "Полное имя"),
                entry("loadingStatus", "Подготавливаем рабочее пространство"),
                entry("cancel", "Отмена")
        ), List.of(
                "Совет: Щелкните правой кнопкой по строке оборудования для быстрых действий.",
                "Совет: Экспортируйте инвентарь из раздела Export.",
                "Совет: Возврат с дефектом автоматически создает отчет.",
                "Совет: Используйте несколько фильтров для быстрого поиска.",
                "Совет: Администраторы видят важные действия в Activity Log.",
                "Совет: Выбранная тема сохраняется между сеансами."
        ));
        add("fr", "Français", Map.ofEntries(
                entry("language", "Langue"),
                entry("subtitle", "Gestion des équipements de laboratoire"),
                entry("username", "Nom d'utilisateur"),
                entry("password", "Mot de passe"),
                entry("remember", "Se souvenir de moi"),
                entry("login", "Connexion"),
                entry("signup", "Créer un compte"),
                entry("testUsers", "Utilisateurs test : admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Système de gestion des équipements de laboratoire"),
                entry("emptyCredentials", "Saisis le nom d'utilisateur et le mot de passe."),
                entry("invalidCredentials", "Nom d'utilisateur ou mot de passe invalide."),
                entry("createAccount", "Créer un compte"),
                entry("fullName", "Nom complet"),
                entry("loadingStatus", "Préparation de ton espace de travail"),
                entry("cancel", "Annuler")
        ), List.of(
                "Astuce : clic droit sur une ligne d'équipement pour les actions rapides.",
                "Astuce : exporte l'inventaire depuis la section Export.",
                "Astuce : un retour défectueux crée automatiquement un rapport.",
                "Astuce : combine les filtres pour trouver plus vite.",
                "Astuce : les admins peuvent consulter Activity Log.",
                "Astuce : le thème choisi reste sauvegardé."
        ));
        add("de", "Deutsch", Map.ofEntries(
                entry("language", "Sprache"),
                entry("subtitle", "Verwaltung von Laborgeräten"),
                entry("username", "Benutzername"),
                entry("password", "Passwort"),
                entry("remember", "Angemeldet bleiben"),
                entry("login", "Anmelden"),
                entry("signup", "Konto erstellen"),
                entry("testUsers", "Testbenutzer: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - System zur Verwaltung von Laborgeräten"),
                entry("emptyCredentials", "Bitte Benutzername und Passwort eingeben."),
                entry("invalidCredentials", "Benutzername oder Passwort ist ungültig."),
                entry("createAccount", "Konto erstellen"),
                entry("fullName", "Vollständiger Name"),
                entry("loadingStatus", "Arbeitsbereich wird vorbereitet"),
                entry("cancel", "Abbrechen")
        ), List.of(
                "Tipp: Rechtsklick auf Gerätezeilen für Schnellaktionen.",
                "Tipp: Exportiere Inventarberichte im Bereich Export.",
                "Tipp: Defekte Rückgaben erzeugen automatisch einen Bericht.",
                "Tipp: Kombiniere Filter für eine schnelle Suche.",
                "Tipp: Admins sehen wichtige Aktionen im Activity Log.",
                "Tipp: Die Theme-Auswahl bleibt gespeichert."
        ));
        add("it", "Italiano", Map.ofEntries(
                entry("language", "Lingua"),
                entry("subtitle", "Gestione attrezzature di laboratorio"),
                entry("username", "Utente"),
                entry("password", "Password"),
                entry("remember", "Ricordami"),
                entry("login", "Accedi"),
                entry("signup", "Crea account"),
                entry("testUsers", "Utenti test: admin/admin123, professor/professor123, technician/technician123"),
                entry("footer", "LabFlow v1.0 - Sistema di gestione attrezzature di laboratorio"),
                entry("emptyCredentials", "Inserisci utente e password."),
                entry("invalidCredentials", "Utente o password non validi."),
                entry("createAccount", "Crea account"),
                entry("fullName", "Nome completo"),
                entry("loadingStatus", "Preparazione dello spazio di lavoro"),
                entry("cancel", "Annulla")
        ), List.of(
                "Suggerimento: clic destro sulle righe per azioni rapide.",
                "Suggerimento: esporta l'inventario dalla sezione Export.",
                "Suggerimento: i resi difettosi creano automaticamente un report.",
                "Suggerimento: combina i filtri per cercare più velocemente.",
                "Suggerimento: gli admin possono vedere Activity Log.",
                "Suggerimento: il tema scelto resta salvato."
        ));
    }

    private LanguageManager() {
    }

    public static String text(String key) {
        LanguagePack pack = LANGUAGES.get(currentCode());
        Map<String, String> extraPack = EXTRA_VALUES.get(currentCode());
        if (extraPack != null && extraPack.containsKey(key)) {
            return extraPack.get(key);
        }
        Map<String, String> englishExtras = EXTRA_VALUES.get("en");
        if (englishExtras != null && englishExtras.containsKey(key)) {
            return englishExtras.get(key);
        }
        return pack.values().getOrDefault(key, LANGUAGES.get("en").values().getOrDefault(key, key));
    }

    public static String text(String key, String fallback) {
        String value = text(key);
        return value == null || value.equals(key) ? fallback : value;
    }

    public static List<String> tips() {
        return LANGUAGES.get(currentCode()).tips();
    }

    public static List<LanguageOption> options() {
        return LANGUAGES.entrySet().stream()
                .map(entry -> new LanguageOption(entry.getKey(), entry.getValue().displayName()))
                .toList();
    }

    public static String currentCode() {
        String code = PREFERENCES.get(KEY, "en");
        return LANGUAGES.containsKey(code) ? code : "en";
    }

    public static void setCurrentCode(String code) {
        if (LANGUAGES.containsKey(code)) {
            PREFERENCES.put(KEY, code);
        }
    }

    public static String currentDisplayName() {
        return LANGUAGES.get(currentCode()).displayName();
    }

    private static void add(String code, String displayName, Map<String, String> values, List<String> tips) {
        LANGUAGES.put(code, new LanguagePack(displayName, values, tips));
    }

    private static void addExtra(String code, Map<String, String> values) {
        EXTRA_VALUES.merge(code, new LinkedHashMap<>(values), (existing, incoming) -> {
            existing.putAll(incoming);
            return existing;
        });
    }

    private static Map.Entry<String, String> entry(String key, String value) {
        return Map.entry(key, value);
    }

    static {
        addExtra("en", Map.ofEntries(
                entry("leaderboard.title", "Leaderboard"),
                entry("leaderboard.subtitle", "Track student points, positive behavior, and personal progress in this lab."),
                entry("leaderboard.firstPlace", "Top spot"),
                entry("leaderboard.myPoints", "My points"),
                entry("leaderboard.activeMembers", "Active members"),
                entry("leaderboard.rank", "Rank"),
                entry("leaderboard.username", "Username"),
                entry("leaderboard.points", "Points"),
                entry("leaderboard.date", "Date"),
                entry("leaderboard.reason", "Reason"),
                entry("leaderboard.noActivityTitle", "No activity yet"),
                entry("leaderboard.noActivitySubtitle", "Start borrowing equipment to earn points."),
                entry("leaderboard.top10", "Top 10 in this lab"),
                entry("leaderboard.noHistoryTitle", "No points history yet"),
                entry("leaderboard.noHistorySubtitle", "Your rewards and penalties will appear here."),
                entry("leaderboard.myHistory", "My points history"),
                entry("leaderboard.zeroLateReturns", "Zero late returns"),
                entry("leaderboard.topReturner", "Top Returner"),
                entry("leaderboard.helpfulReporter", "Helpful Reporter"),
                entry("leaderboard.top", "Top"),
                entry("home", "Home"),
                entry("show", "Show"),
                entry("hide", "Hide")
        ));
        addExtra("ro", Map.ofEntries(
                entry("leaderboard.title", "Clasament"),
                entry("leaderboard.subtitle", "Urmărește punctele elevilor, comportamentul bun și progresul personal în acest laborator."),
                entry("leaderboard.firstPlace", "Locul 1"),
                entry("leaderboard.myPoints", "Punctele mele"),
                entry("leaderboard.activeMembers", "Membri activi"),
                entry("leaderboard.rank", "Loc"),
                entry("leaderboard.username", "Utilizator"),
                entry("leaderboard.points", "Puncte"),
                entry("leaderboard.date", "Dată"),
                entry("leaderboard.reason", "Motiv"),
                entry("leaderboard.noActivityTitle", "Încă nu există activitate"),
                entry("leaderboard.noActivitySubtitle", "Începe să împrumuți echipamente ca să câștigi puncte."),
                entry("leaderboard.top10", "Top 10 în laborator"),
                entry("leaderboard.noHistoryTitle", "Încă nu există istoric de puncte"),
                entry("leaderboard.noHistorySubtitle", "Aici vor apărea recompensele și penalizările tale."),
                entry("leaderboard.myHistory", "Istoricul meu de puncte"),
                entry("leaderboard.zeroLateReturns", "Zero returnări întârziate"),
                entry("leaderboard.topReturner", "Campion la returnări"),
                entry("leaderboard.helpfulReporter", "Raportor util"),
                entry("leaderboard.top", "Top"),
                entry("home", "Acasă"),
                entry("show", "Arată"),
                entry("hide", "Ascunde")
        ));
        addExtra("en", Map.ofEntries(
                entry("logout", "Logout"),
                entry("open", "Open"),
                entry("about", "About"),
                entry("create", "Create"),
                entry("remove", "Remove"),
                entry("members", "Members"),
                entry("admins", "admins"),
                entry("technicians", "technicians"),
                entry("students", "students"),
                entry("guests", "guests"),
                entry("settings.title", "Settings"),
                entry("settings.subtitle", "Customize LabFlow, manage data safety and review admin controls."),
                entry("settings.providerPreset", "Provider preset"),
                entry("settings.baseUrl", "Base URL"),
                entry("settings.model", "Model"),
                entry("settings.aboutDescription", "Laboratory Equipment Management System\nJavaFX 21 · Java 21 · SQLite · AI-Powered"),
                entry("settings.themeSaved", "Lab color theme saved."),
                entry("settings.chooseBackupFolder", "Choose backup folder"),
                entry("settings.backupCreated", "Backup created:"),
                entry("settings.adminRestoreOnly", "Only admins can restore the database."),
                entry("settings.restoreConfirm", "Restore database from backup? A safety backup will be created first and LabFlow must be restarted."),
                entry("settings.chooseBackupFile", "Choose LabFlow database backup"),
                entry("settings.restoreSuccess", "Database restored. Restart LabFlow before continuing."),
                entry("settings.openBackupFolderError", "Could not open backup folder:"),
                entry("settings.aiSaved", "AI settings saved. AI Helper and weekly reports will use the new provider."),
                entry("settings.aiSaveError", "Could not save AI settings:"),
                entry("settings.aiConnectionOk", "AI connection looks good."),
                entry("settings.aiResetConfirm", "Clear saved AI settings and fall back to environment variables or ai.env?"),
                entry("settings.aiResetDone", "Saved AI settings cleared. LabFlow will use environment fallback again."),
                entry("settings.aiResetError", "Could not clear AI settings:"),
                entry("settings.currentProvider", "Current provider:"),
                entry("settings.notSet", "not set"),
                entry("settings.keySource", "Key source:"),
                entry("settings.getApiKey", "Get API Key"),
                entry("settings.quickstart", "Quickstart"),
                entry("ownerPanel.title", "Owner Panel"),
                entry("ownerPanel.subtitle", "Manage people, permissions and access for"),
                entry("ownerPanel.noLabTitle", "No lab selected"),
                entry("ownerPanel.noLabSubtitle", "Choose a laboratory before managing people."),
                entry("ownerPanel.ownerOnlyTitle", "Owner access only"),
                entry("ownerPanel.ownerOnlySubtitle", "Only the person who created this lab can manage members and roles."),
                entry("ownerPanel.labAccess", "Lab Access"),
                entry("ownerPanel.inviteHelp", "Share this code with someone so they can join. New joiners start as Guest until you promote them here."),
                entry("ownerPanel.exactUsername", "Exact username"),
                entry("ownerPanel.addMember", "Add Member"),
                entry("ownerPanel.addHint", "Type the exact username. LabFlow will not show the full account list here."),
                entry("ownerPanel.addExisting", "Add Existing User"),
                entry("ownerPanel.filterPrompt", "Filter by name, username or role"),
                entry("ownerPanel.noMembersTitle", "No members found"),
                entry("ownerPanel.noMembersSubtitle", "Try a different filter."),
                entry("ownerPanel.typeUsername", "Type a username first."),
                entry("ownerPanel.memberAdded", "Member added."),
                entry("ownerPanel.removeMemberTitle", "Remove Member"),
                entry("ownerPanel.removeMemberConfirm", "Remove"),
                entry("ownerPanel.removeMemberSuffix", "from the current lab?"),
                entry("ownerPanel.memberRemoved", "Member removed."),
                entry("ownerPanel.createGuestAccount", "Create Guest Account"),
                entry("ownerPanel.temporaryPassword", "Temporary password"),
                entry("ownerPanel.guestName", "Guest name"),
                entry("dashboard.searchPrompt", "Search equipment, users, records..."),
                entry("dashboard.addEquipment", "+ Add Equipment"),
                entry("dashboard.export", "Export"),
                entry("dashboard.notifications", "Notifications"),
                entry("dashboard.noNotifications", "No notifications yet."),
                entry("dashboard.notificationRead", "Notification marked as read."),
                entry("dashboard.markAllRead", "Mark all as read"),
                entry("dashboard.notificationsRead", "Notifications marked as read."),
                entry("dashboard.searchHint", "Type something to search across LabFlow."),
                entry("dashboard.globalSearch", "Global Search"),
                entry("dashboard.noResultsFor", "No results for"),
                entry("dashboard.help", "LabFlow Help"),
                entry("dashboard.quickStart", "Quick Start"),
                entry("dashboard.helpDash", "Dashboard: view analytics, notifications, and risky equipment."),
                entry("dashboard.helpInventory", "Inventory: manage equipment, consumables, QR codes, and containers."),
                entry("dashboard.helpBorrow", "Borrow Records: track active loans, returns, and overdue items."),
                entry("dashboard.helpFaults", "Fault Reports: report, assign, and resolve issues."),
                entry("dashboard.helpExport", "Export: generate Excel, PDF, backups, and QR labels."),
                entry("dashboard.tips", "Tips"),
                entry("dashboard.helpSearch", "Use the top search bar to jump to equipment, reports, reservations, tags, and logs."),
                entry("dashboard.helpAdmin", "Admins and technicians can edit stock, maintenance, tags, and containers."),
                entry("dashboard.helpIsolation", "The selected lab isolates all inventory, borrowings, analytics, and notifications."),
                entry("dashboard.recentActivity", "Recent Activity"),
                entry("dashboard.systemActor", "System"),
                entry("dashboard.newItemsAdded", "New Items Added"),
                entry("dashboard.noEquipmentAdded", "No equipment has been added yet."),
                entry("dashboard.invalidPasswordForAccount", "Invalid password for that account."),
                entry("dashboard.recentAccountUnavailable", "That recent account is no longer available."),
                entry("dashboard.switchAccount", "Switch Account"),
                entry("dashboard.reEnterPasswordFor", "Re-enter the password for"),
                entry("dashboard.toContinue", "to continue."),
                entry("dashboard.items", "items"),
                entry("dashboard.aiConnected", "AI Connected"),
                entry("dashboard.aiOffline", "AI Offline"),
                entry("dashboard.user", "User")
        ));
        addExtra("ro", Map.ofEntries(
                entry("logout", "Deconectare"),
                entry("open", "Deschide"),
                entry("about", "Despre"),
                entry("create", "Creeaza"),
                entry("remove", "Elimina"),
                entry("members", "Membri"),
                entry("admins", "admini"),
                entry("technicians", "tehnicieni"),
                entry("students", "studenti"),
                entry("guests", "oaspeti"),
                entry("settings.title", "Setari"),
                entry("settings.subtitle", "Personalizeaza LabFlow, gestioneaza datele si controleaza setarile importante."),
                entry("settings.providerPreset", "Preset provider"),
                entry("settings.baseUrl", "URL baza"),
                entry("settings.model", "Model"),
                entry("settings.aboutDescription", "Sistem de management pentru echipamente de laborator\nJavaFX 21 · Java 21 · SQLite · AI"),
                entry("settings.themeSaved", "Tema laboratorului a fost salvata."),
                entry("settings.chooseBackupFolder", "Alege folderul pentru backup"),
                entry("settings.backupCreated", "Backup creat:"),
                entry("settings.adminRestoreOnly", "Doar adminii pot restaura baza de date."),
                entry("settings.restoreConfirm", "Restaurezi baza de date din backup? Se va crea mai intai un backup de siguranta si aplicatia trebuie repornita."),
                entry("settings.chooseBackupFile", "Alege backup-ul LabFlow"),
                entry("settings.restoreSuccess", "Baza de date a fost restaurata. Reporneste LabFlow."),
                entry("settings.openBackupFolderError", "Nu am putut deschide folderul de backup:"),
                entry("settings.aiSaved", "Setarile AI au fost salvate. AI Helper si raportul saptamanal vor folosi noul provider."),
                entry("settings.aiSaveError", "Nu am putut salva setarile AI:"),
                entry("settings.aiConnectionOk", "Conexiunea AI pare in regula."),
                entry("settings.aiResetConfirm", "Sterg setarile AI salvate si revin la variabilele de mediu sau ai.env?"),
                entry("settings.aiResetDone", "Setarile AI salvate au fost sterse. LabFlow va folosi din nou fallback-ul din mediu."),
                entry("settings.aiResetError", "Nu am putut sterge setarile AI:"),
                entry("settings.currentProvider", "Provider curent:"),
                entry("settings.notSet", "nesetat"),
                entry("settings.keySource", "Sursa cheii:"),
                entry("settings.getApiKey", "Ia cheia API"),
                entry("settings.quickstart", "Pornire rapida"),
                entry("ownerPanel.title", "Panou Owner"),
                entry("ownerPanel.subtitle", "Gestioneaza persoane, permisiuni si acces pentru"),
                entry("ownerPanel.noLabTitle", "Niciun laborator selectat"),
                entry("ownerPanel.noLabSubtitle", "Alege un laborator inainte sa gestionezi membrii."),
                entry("ownerPanel.ownerOnlyTitle", "Acces doar pentru owner"),
                entry("ownerPanel.ownerOnlySubtitle", "Doar persoana care a creat laboratorul poate gestiona membrii si rolurile."),
                entry("ownerPanel.labAccess", "Acces laborator"),
                entry("ownerPanel.inviteHelp", "Trimite acest cod altcuiva ca sa intre in laborator. Noii membri incep ca Guest pana ii promovezi aici."),
                entry("ownerPanel.exactUsername", "Username exact"),
                entry("ownerPanel.addMember", "Adauga membru"),
                entry("ownerPanel.addHint", "Tasteaza username-ul exact. LabFlow nu afiseaza aici lista completa de conturi."),
                entry("ownerPanel.addExisting", "Adauga utilizator existent"),
                entry("ownerPanel.filterPrompt", "Filtreaza dupa nume, username sau rol"),
                entry("ownerPanel.noMembersTitle", "Nu am gasit membri"),
                entry("ownerPanel.noMembersSubtitle", "Incearca alt filtru."),
                entry("ownerPanel.typeUsername", "Scrie mai intai un username."),
                entry("ownerPanel.memberAdded", "Membru adaugat."),
                entry("ownerPanel.removeMemberTitle", "Elimina membru"),
                entry("ownerPanel.removeMemberConfirm", "Elimini"),
                entry("ownerPanel.removeMemberSuffix", "din laboratorul curent?"),
                entry("ownerPanel.memberRemoved", "Membru eliminat."),
                entry("ownerPanel.createGuestAccount", "Creeaza cont guest"),
                entry("ownerPanel.temporaryPassword", "Parola temporara"),
                entry("ownerPanel.guestName", "Nume guest"),
                entry("dashboard.searchPrompt", "Cauta echipamente, utilizatori, inregistrari..."),
                entry("dashboard.addEquipment", "+ Adauga echipament"),
                entry("dashboard.export", "Export"),
                entry("dashboard.notifications", "Notificari"),
                entry("dashboard.noNotifications", "Nu exista notificari."),
                entry("dashboard.notificationRead", "Notificarea a fost marcata ca citita."),
                entry("dashboard.markAllRead", "Marcheaza tot ca citit"),
                entry("dashboard.notificationsRead", "Notificarile au fost marcate ca citite."),
                entry("dashboard.searchHint", "Scrie ceva pentru a cauta in tot LabFlow."),
                entry("dashboard.globalSearch", "Cautare globala"),
                entry("dashboard.noResultsFor", "Nu exista rezultate pentru"),
                entry("dashboard.help", "Ajutor LabFlow"),
                entry("dashboard.quickStart", "Pornire rapida"),
                entry("dashboard.helpDash", "Dashboard: vezi analize, notificari si echipamente riscante."),
                entry("dashboard.helpInventory", "Inventory: gestionezi echipamente, consumabile, coduri QR si containere."),
                entry("dashboard.helpBorrow", "Borrow Records: urmaresti imprumuturile, returnarile si intarzierile."),
                entry("dashboard.helpFaults", "Fault Reports: raportezi, asignezi si rezolvi probleme."),
                entry("dashboard.helpExport", "Export: generezi Excel, PDF, backup-uri si etichete QR."),
                entry("dashboard.tips", "Sfaturi"),
                entry("dashboard.helpSearch", "Foloseste bara de cautare de sus ca sa sari rapid la echipamente, rapoarte, rezervari, taguri si loguri."),
                entry("dashboard.helpAdmin", "Adminii si tehnicienii pot edita stocul, mentenanta, tagurile si containerele."),
                entry("dashboard.helpIsolation", "Laboratorul selectat izoleaza tot inventarul, imprumuturile, analizele si notificarile."),
                entry("dashboard.recentActivity", "Activitate recenta"),
                entry("dashboard.systemActor", "Sistem"),
                entry("dashboard.newItemsAdded", "Elemente adaugate recent"),
                entry("dashboard.noEquipmentAdded", "Nu a fost adaugat niciun echipament."),
                entry("dashboard.invalidPasswordForAccount", "Parola pentru acel cont este invalida."),
                entry("dashboard.recentAccountUnavailable", "Acel cont recent nu mai este disponibil."),
                entry("dashboard.switchAccount", "Schimba contul"),
                entry("dashboard.reEnterPasswordFor", "Reintrodu parola pentru"),
                entry("dashboard.toContinue", "ca sa continui."),
                entry("dashboard.items", "iteme"),
                entry("dashboard.aiConnected", "AI conectat"),
                entry("dashboard.aiOffline", "AI offline"),
                entry("dashboard.user", "Utilizator")
        ));
    }

    private record LanguagePack(String displayName, Map<String, String> values, List<String> tips) {
    }

    public record LanguageOption(String code, String displayName) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
