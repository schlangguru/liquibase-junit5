package de.schlangguru.liquibase.junit5;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@ExtendWith({LiquibaseJunit5Extension.class})
@Inherited
public @interface LiquibaseTest {

    /**
     * Per default the database will be dropped after
     * a test and newly setup before the next test.
     * If set to false, the database will be kept
     * after a test.
     */
    boolean resetPerTest() default true;

}
