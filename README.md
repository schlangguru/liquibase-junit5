# JUnit 5 Liquibase Extension

With this JUnit 5 extension, developers can easily set up a test-database
by automating the database migration before a test and dropping it afterwards.
Liquibase is used as migration tool to define the desired database state.

## Usage
Just annotate the test class with `@LiquibaseTest`. This registers the extension.
The test class needs to define two objects for Liquibase to work:

- A `DataSource` used to connect to a database (usually an in-memory database).
- A changelog file used to migrate the DB. Defined as String. Will be looked up on the classpath.

Each of these two objects can be provided by using the annotation `@ProvideForLiquibase` ether on a method of field. 

```java
import de.schlangguru.liquibase.junit5.ProvideForLiquibase;
import org.junit.jupiter.api.Test;

@LiquibaseTest
class TestClass {

    @ProvideForLiquibase
    public DataSource dataSource() {
        return ...; // Return the Datasource which Liquibase will use
    }

    @ProvideForLiquibase
    public String changelog() {
        return "my-changelog.xml"; // return the name of the changelog Liquibase will use
    }

    @Test
    public void test() {
        // Access database here
    }

    @Test
    public void anotherTest() {
        // Database will be reset between tests
    }
}
```

With this setup Liquibase will migrate the database using `my-changelog.xml` before a test runs.
It will drop everything afterwards so a new test case can work with a new clean database.