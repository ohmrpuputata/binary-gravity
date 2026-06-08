package com.example.alieninvasion.entity;

/**
 * Роли пришельцев в структуре роя.
 * Каждый юнит несёт одну роль, определяющую его место в иерархии.
 */
public enum AlienRole {
    // ── Рядовые юниты ───────────────────────────────────────
    SOLDIER  ("Солдат",      1, false),  // AlienGrunt (боевой режим)
    WORKER   ("Рабочий",     1, false),  // AlienGrunt (режим сборщика)
    SCOUT    ("Разведчик",   1, false),  // AlienChicken, SkyDrone
    TRICKSTER("Трикстер",    1, false),  // AlienTroll
    PARASITE ("Паразит",     1, false),  // ParasiteEntity

    // ── Специалисты ─────────────────────────────────────────
    HEAVY    ("Громила",     2, false),  // AlienBrute
    STALKER  ("Сталкер",     2, false),  // AlienStalker
    ARTILLERY("Артиллерия",  2, false),  // PlasmaCaster, AcidSpitter
    ENGINEER ("Инженер",     2, false),  // AlienBreacher
    LURKER   ("Засадник",    2, false),  // CaveLurker
    PSYCHIC  ("Псионик",     2, false),  // TelekineticAlien

    // ── Командиры ───────────────────────────────────────────
    SHAMAN   ("Шаман",       3, false),  // HiveShaman (поддержка + буфф)
    COMMANDER("Командир",    4, false),  // HiveTyrant (мини-босс)

    // ── Боссы ───────────────────────────────────────────────
    SUPREME  ("Верховный",   5, false),  // SwarmMother

    // ── Заражённые ──────────────────────────────────────────
    INFECTED ("Заражённый",  1, true);   // Infested Zombie/Creeper/Skeleton/Clone

    // ────────────────────────────────────────────────────────

    private final String displayName;
    /** Приоритет в иерархии: чем выше — тем важнее командир для подчинённых. */
    private final int priority;
    /** true — юнит создан из заражённой ванильной мобы, а не является чистым пришельцем. */
    private final boolean infected;

    AlienRole(String displayName, int priority, boolean infected) {
        this.displayName = displayName;
        this.priority    = priority;
        this.infected    = infected;
    }

    public String getDisplayName() { return displayName; }
    public int    getPriority()    { return priority; }
    public boolean isInfected()    { return infected; }

    /** Является ли роль командирской (за ними следуют рядовые). */
    public boolean isLeader() {
        return this == COMMANDER || this == SUPREME || this == SHAMAN;
    }
}
