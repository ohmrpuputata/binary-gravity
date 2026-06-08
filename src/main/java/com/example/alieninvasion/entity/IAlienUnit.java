package com.example.alieninvasion.entity;

/**
 * Маркерный интерфейс для всех юнитов роя — и чистокровных пришельцев,
 * и заражённых ванильных мобов.
 *
 * Позволяет AI-целям и системам спавна работать с любым юнитом
 * независимо от его ванильного родительского класса.
 */
public interface IAlienUnit {

    /** Роль юнита в структуре роя. */
    AlienRole getAlienRole();

    /** Является ли юнит командиром (Шаман, Командир, Верховный). */
    default boolean isAlienLeader() {
        return getAlienRole().isLeader();
    }

    /** Является ли юнит заражённой ванильной мобой, а не чистым пришельцем. */
    default boolean isInfectedUnit() {
        return getAlienRole().isInfected();
    }
}
