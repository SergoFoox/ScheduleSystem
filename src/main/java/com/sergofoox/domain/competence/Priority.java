package com.sergofoox.domain.competence;

/**
 * Пріоритетність викладача для дисципліни (згідно з ТЗ 2.1)
 */
public enum Priority {
    PRIMARY,    // Основний лектор / керівник
    SECONDARY,  // Може вести, але не основний
    SUBSTITUTE  // Тільки для заміни
}
