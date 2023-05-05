package de.schlangguru.liquibase.junit5;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.extension.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

public class LiquibaseJunit5Extension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private Function<Object, DataSource> datasourceProvider;
    private Function<Object, String> changelogProvider;
    private boolean resetPerTest;

    private Connection connection;
    private Liquibase liquibase;

    private boolean databaseInitialized;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        LiquibaseTest testClassAnnotation = testClass.getAnnotation(LiquibaseTest.class);
        if (testClassAnnotation == null) {
            throw new RuntimeException("Test class using %s as extension must be annotated with @%s"
                    .formatted(getClass().getSimpleName(), LiquibaseTest.class.getSimpleName()));
        }
        resetPerTest = testClassAnnotation.resetPerTest();

        AnnotationProcessor annotationProcessor = new AnnotationProcessor(testClass);
        datasourceProvider = annotationProcessor.findProvider(DataSource.class)
                .orElseThrow(() -> new RuntimeException("Test classes annotated with @%s require a public method or field annotated with @%s to provide the %s to use for database migration."
                        .formatted(LiquibaseTest.class.getSimpleName(), ProvideForLiquibase.class.getSimpleName(), DataSource.class.getCanonicalName())));
        changelogProvider = annotationProcessor.findProvider(String.class)
                .orElseThrow(() -> new RuntimeException("Test classes annotated with @%s require a public method or field annotated with @%s to provide the %s for the changelog to use."
                        .formatted(LiquibaseTest.class.getSimpleName(), ProvideForLiquibase.class.getSimpleName(), String.class.getCanonicalName())));
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Object testInstance = extensionContext.getRequiredTestInstance();
        if (connection == null) {
            DataSource datasource = datasourceProvider.apply(testInstance);
            connection = datasource.getConnection();
        }

        if (liquibase == null) {
            String changelog = changelogProvider.apply(testInstance);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        }

        if (!databaseInitialized) {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (resetPerTest) {
            liquibase.dropAll();
            databaseInitialized = false;
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (liquibase != null) {
            liquibase.close();
        }

        if (connection != null) {
            connection.close();
        }
    }
}
