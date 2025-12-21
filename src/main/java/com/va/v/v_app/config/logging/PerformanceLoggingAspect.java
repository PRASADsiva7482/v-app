package com.va.v.v_app.config.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Aspect for logging performance of service methods.
 */
@Aspect
public class PerformanceLoggingAspect {

    private final Environment env;

    public PerformanceLoggingAspect(Environment env) {
        this.env = env;
    }

    /**
     * Pointcut that matches all service methods.
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
        // Method is empty as this is just a Pointcut
    }

    /**
     * Pointcut that matches all repository methods.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void repositoryPointcut() {
        // Method is empty as this is just a Pointcut
    }

    /**
     * Around advice to log method execution time.
     *
     * @param joinPoint the join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("servicePointcut() || repositoryPointcut()")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());

        if (env.acceptsProfiles(Profiles.of("dev"))) {
            long start = System.currentTimeMillis();

            try {
                Object result = joinPoint.proceed();
                long executionTime = System.currentTimeMillis() - start;

                if (executionTime > 1000) {
                    log.warn("SLOW EXECUTION: {}() executed in {} ms",
                            joinPoint.getSignature().getName(), executionTime);
                } else {
                    log.debug("{}() executed in {} ms",
                            joinPoint.getSignature().getName(), executionTime);
                }

                return result;
            } catch (Throwable e) {
                long executionTime = System.currentTimeMillis() - start;
                log.error("{}() threw exception after {} ms",
                        joinPoint.getSignature().getName(), executionTime);
                throw e;
            }
        } else {
            return joinPoint.proceed();
        }
    }
}
