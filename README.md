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
    │   ├── exceptions                          |
    │   │   ├── AutomationException.java        |< Indicates that there was an automation error.
    │   │   ├── PageStateException.java         |< Indicates that there was an issue with the state of a PageObject.
    │   │   ├── ...                             |< Any other exceptions should go here
    │   ├── pages                               |
    │   │   ├── PageObject.java                 |< The top level PageObject that all other pages should inherit from.
    │   │   └── ...                             |< Other page files should go here.
    │   ├── runner                              |
    │   │   └── CucumberRunner.java             |< The Cucumber test runner used for debugging.
    │   ├── setup                               |
    │   │   ├── DriverFactory.java              |< Factory responsible for creating the drivers for a given profile.
    │   │   ├── DriverDecorator.java            |< A decorator for the WebDrivers that contains additional information.
    │   │   ├── Hooks.java                      |< The "main" class that holds the hooks and coordinates setup.
    │   │   └── configuration                   |
    │   │       ├── Config.java                 |< Creates models based on config file and holds other config info.
    │   │       └── models                      |
    │   │           ├── Model.java              |< Base interface for the configuration models
    │   │           ├── Configuration.java      |< Serializeable model representing the config file.
    │   │           ├── Driver.java             |< Serializeable model representing a driver configuration profile.
    │   │           ├── Environment.java        |< Serializeable model representing the environment.
    │   │           └── Profile.java            |< Serializeable model representing a test profile.
    │   ├── steps                               |
    │   │   ├── GenericSteps.java               |< Generic steps that can be used regardless of test environment.
    │   │   ├── StepDefinition.java             |< The top level StepDefinition.
    │   │   └── ...                             |< Other step defs should go here.
    │   └── util                                |
    │       ├── BatchAssertion.java             |< Used to assess multiple assertions at once.
    │       └── TestEnvironment.java            |< Stores environment variables and test data.
    │       └── ...                             |< Any other utils should go here.
    └── resources                               |
        ├── log4j2.yaml                         |< The configuration file for the log4j implementation.
        ├── configs                             |
        │   └── example-config.yaml             |< A basic config template that can be used for bare metal test exectutions.
        │   └── ...                             |< Config files should go here, but you can place them elsewhere if needed.
        └── feature                             |
            └── ...                             |< Feature files must go here.
```

## Config Structure:

 For a working example, refer to `src/test/resources/configs/example-config.yaml`
```yaml
environment:                  #< (OPTIONAL) The object containing information about the test environment.
  filepath: <string>          #< The filepath that points to the .env file that will be used.
  name: <string>              #< (OPTIONAL) The name of the environment that is being used.
                              #
profiles:                     #< The list of driver profiles that are available to us.
  - name: <string>            #< The name of a driver profile.
    description: <string>     #< (OPTIONAL) A description for the device profile. Mainly used for documentation.
    systemProperties:         #< (OPTIONAL) The list of system properties that should be set for this driver profile.
      key: value              #< The SystemProperty mapping that we want to set.
      ...                     #
    driver:                   #< The object that outlines the specifics of the driver profile.
      name: <string>          #< What framework should be used to create the driver [selenium, browserstack, appium]
      capabilities:           #< The list of capabilities that will be used when creating the driver.
        key: value            #< The capability mapping.
        ...                   #
      preferences:            #< (OPTIONAL) The preferences that should be applied to the driver. Mainly used by Firefox.
        key: value            #< The preference mapping.
        ...                   #
      arguments:              #< (OPTIONAL) The list of launch arguments that should be used when starting the driver.
        - <string>            #< A launch argument.
        - ...                 #
      experimentalOptions:    #< (OPTIONAL) The map containing the experimental options that we want to use for Chromium based drivers.
        key: value            # The experimental option mapping.
        ...                   #
  - ...                       #
                              #
defaultProfile: <string>      #< (OPTIONAL) The default profile that should be used in the event that none are provided.
                              #
systemProperties:             #< (OPTIONAL) The list of system properties that should be set before running any tests. Will be overwritten by driver specific system properties.
  key: value                  #< The SystemProperty mapping that we want to set.
  ...                         #
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
