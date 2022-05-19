# selenium-cucumber-test-runner
 This is a template testing framework that is capable of running multiple frontend tests in parallel.

## Project Structure

```
.                                               |
├── .github                                     |
│   ├── actions                                 |
│   │   └── ...                                 |< Any composite actions should go here.
│   └── workflows                               |
│       └── ...                                 |< Any GitHub workflows should go here.
├── environments                                |
│   └── ...                                     |< Any .env files should go here.
└── src.test                                    |
    ├── java.ca.empire                          |
    │   ├── pages                               |
    │   │   ├── PageObject.java                 |< The top level PageObject that all other pages should inherit from.
    │   │   └── ...                             |< Other page files should go here.
    │   ├── runner                              |
    │   │   └── CucumberRunner.java             |< The Cucumber test runner used for debugging.
    │   ├── setup                               |
    │   │   ├── DriverFactory.java              |< Factory responsible for creating the drivers for a given profile.
    │   │   ├── Hooks.java                      |< The "main" class that holds the hooks and coordinates setup.
    │   │   └── configuration                   |
    │   │       ├── Config.java                 |< Creates models based on config file and holds other config info.
    │   │       └── models                      |
    │   │           ├── Model.java              |< Base interface for the configuration models
    │   │           ├── Configuration.java      |< Serializeable model representing the config file.
    │   │           ├── Driver.java             |< Serializeable model representing a driver configuration profile.
    │   │           ├── Environment.java        |< Serializeable model representing the environment.
    │   │           ├── Mapping.java            |< Serializeable model representing a generic key/value mapping.
    │   │           └── Profile.java            |< Serializeable model representing a test profile.
    │   ├── steps                               |
    │   │   ├── GenericSteps.java               |< Generic steps that can be used regardless of test environment.
    │   │   ├── StepDefinition.java             |< The top level StepDefinition.
    │   │   └── ...                             |< Other step defs should go here.
    │   └── util                                |
    │       ├── BatchAssertion.java             |< Used to assess multiple assertions at once.
    │       └── TestEnvironment.java            |< Stores environment variables and test data.
    └── resources                               |
        ├── configs                             |
        │   └── ...                             |< Config files should go here, but you can place them elsewhere if needed.
        └── feature                             |
            └── ...                             |< Feature files must go here.
```

## Running Tests

 The following command can be used to run tests:
 ```
 gradle cucumber -Dcucumber.filter.tags="..." -Dconfig.filepath="..." -Dconfig.profile="..." -Pthreads=...
 ```
 Where:
 - `-Dcucumber.filter.tags` = The tags of tests that should be ran.
 - `-Dconfig.filepath` = The filepath pointing to the desired config file.
 - `-Dconfig.profile` = The driver profile from the provided config that should be use during testing.
 - `-Pthreads` = (OPTIONAL) The number of parallel workers that should be used during testing. Should be more than 0.
