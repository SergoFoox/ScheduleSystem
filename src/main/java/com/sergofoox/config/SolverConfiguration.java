package com.sergofoox.config;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicType;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import com.sergofoox.domain.lesson.Lesson;
import com.sergofoox.domain.solver.Schedule;
import com.sergofoox.domain.solver.ScheduleConstraintProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Configuration
public class SolverConfiguration {

    @Bean
    public SolverConfig solverConfig() {
        // Leave the random seed unset so Timefold generates a new one on each run.
        return new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.NON_REPRODUCIBLE)
                .withSolutionClass(Schedule.class)
                .withEntityClassList(Collections.singletonList(Lesson.class))
                .withConstraintProviderClass(ScheduleConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig()
                        .withSpentLimit(Duration.ofSeconds(10))
                        .withUnimprovedSpentLimit(Duration.ofSeconds(5)))
                .withPhaseList(List.of(
                        new ConstructionHeuristicPhaseConfig()
                                .withConstructionHeuristicType(ConstructionHeuristicType.FIRST_FIT),
                        new LocalSearchPhaseConfig()
                                .withLocalSearchType(LocalSearchType.TABU_SEARCH)
                ));
    }

    @Bean
    public SolverFactory<Schedule> solverFactory(SolverConfig solverConfig) {
        return SolverFactory.create(solverConfig);
    }
}
