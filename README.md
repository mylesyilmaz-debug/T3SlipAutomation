# selenium-cucumber-test-runner

This is a template testing framework that is capable of running multiple frontend tests in parallel.

## Table of Contents

- [Best Practices](#best-practices)
  - [Logging](#logging)
  - [Thread Safe Code](#thread-safe-code)
  - [Creating Page Files](#creating-page-files)
  - [Creating Step Definitions](#creating-step-definitions)
  - [Documentation](#documentation)
- [Project Structure](#project-structure)
- [Handling Configuration](#handling-configuration)
  - [VM Args](#vm-args)
  - [File Discovery](#file-discovery)
- [Config Structure](#config-structure)
- [Running Tests](#running-tests)

## Best Practices:

### Logging:

Although this test framework will likely never work with prod data, we need to respect 
[The Personal Information Protection and Electronic Documents Act](https://www.priv.gc.ca/en/privacy-topics/privacy-laws-in-canada/the-personal-information-protection-and-electronic-documents-act-pipeda/) 
(PIPEDA) rules and not log any Personally Identifiable Information (PII) directly into logs.

This framework uses the [Log4j2](https://logging.apache.org/log4j/2.x/) library to handle logging. If possible, using
`System.out.print...` should be avoided and the Log4j2 API should be used instead. When logging information, try to
use the correct log level (i.e. don't use `logger.warn(...)` for something that should be `logger.debug(...)`) so that
we can properly control the granularity of our logging.

### Thread Safe Code:

If you want to make use of parallel execution, you need to make sure that your code is thread safe and handles shared
resources properly. It wouldn't be possible to cover thread safety in its entirety here, but the main idea is that
access to shared resources should be controlled. Avoid the usage of `static` variables, pass data around using 
method calls or `ca.empire.utils.TestEnvironment`, and make use of encapsulation to control read/write operations on 
objects. For more information, check the links below:

- https://en.wikipedia.org/wiki/Thread_safety
- https://www.baeldung.com/java-thread-safety

### Creating Page Files:

All page files should derive from `ca.empire.pages.PageObject` so that they have access to important variables 
(e.g. `driver`) and the generic methods that might be required when creating a page file (i.e. `click(...)`). This will
also reduce the number of contact points with core of the framework which will help with maintainability in the event
that the core of the framework needs to change for whatever reason. In the same vein, the page files should _try_ to
avoid relying on external classes too much, though, some reliance on other classes and utilities is unavoidable.

### Creating Step Definitions:

Similar to our page files, make sure that all step definitions derive from `ca.empire.steps.StepDefinition`. This will
give them access to the WebDriver, TestEnvironment and the download directory used by the WebDriver. Excluding
additional utility classes and page files, this should give the step definitions access to all the core components that
they need to execute tests.

### Documentation:

Try to include a [properly formatted](https://google.github.io/styleguide/javaguide.html#s7-javadoc) Javadoc with every
public/protected member if appropriate. If the method expects certain values, make sure that those values are 
sufficiently communicated in said Javadoc. Outside that, just add comments where its appropriate.

## Project Structure:

```
.                                                   |
├── .github                                         |
│   ├── actions                                     |
│   │   └── ...                                     |< Any composite actions should go here.
│   └── workflows                                   |
│       └── ...                                     |< Any GitHub workflows should go here.
├── environments                                    |
│   └── ...                                         |< Any .env files should go here.
└── src.test                                        |
    ├── java.ca.empire                              |
    │   ├── exceptions                              |
    │   │   ├── AutomationException.java            |< Indicates that there was an automation error.
    │   │   ├── PageStateException.java             |< Indicates that there was an issue with the state of a PageObject.
    │   │   ├── ...                                 |< Any other exceptions should go here
    │   ├── pages                                   |
    │   │   ├── PageObject.java                     |< The top level PageObject that all other pages should inherit from.
    │   │   └── ...                                 |< Other page files should go here.
    │   ├── runner                                  |
    │   │   └── CucumberRunner.java                 |< The Cucumber test runner used for debugging.
    │   ├── setup                                   |
    │   │   ├── DriverFactory.java                  |< Factory responsible for creating the drivers for a given profile.
    │   │   ├── DriverDecorator.java                |< A decorator for the WebDrivers that contains additional information.
    │   │   ├── Hooks.java                          |< The "main" class that holds the hooks and coordinates setup.
    │   │   └── configuration                       |
    │   │       ├── Config.java                     |< Creates models based on config file and holds other config info.
    │   │       └── models                          |
    │   │           ├── Model.java                  |< Base interface for the configuration models
    │   │           ├── Configuration.java          |< Serializeable model representing the config file.
    │   │           ├── DownloadManagement.java     |< Serializeable model representing the download management configurations.
    │   │           ├── DriverOptions.java          |< Serializeable model representing a driver configuration profile.
    │   │           ├── Environment.java            |< Serializeable model representing the environment.
    │   │           └── Profile.java                |< Serializeable model representing a test profile.
    │   ├── steps                                   |
    │   │   ├── GenericSteps.java                   |< Generic steps that can be used regardless of test environment.
    │   │   ├── StepDefinition.java                 |< The top level StepDefinition.
    │   │   └── ...                                 |< Other step defs should go here.
    │   └── util                                    |
    │       ├── BatchAssertion.java                 |< Used to assess multiple assertions at once.
    │       └── FileOperations.java                 |< Some generic operations that can be performed in the file system.
    │       └── TestEnvironment.java                |< Stores environment variables and test data.
    │       └── ...                                 |< Any other utils should go here.
    └── resources                                   |
        ├── log4j2.yaml                             |< The configuration file for the log4j implementation.
        ├── configs                                 |
        │   └── example-config.yaml                 |< A basic config template that can be used for bare metal test exectutions.
        │   └── ...                                 |< Config files should go here, but you can place them elsewhere if needed.
        └── feature                                 |
            └── ...                                 |< Feature files must go here.
```

## Handling Configuration:

The framework provides two different ways of handling configuration files. The first utilizes VM arguments to
define the exact location of the configuration file that should be used, while the second relies on very basic file
discovery.

### VM Args:

If you wish to use multiple config files, then this would be the best choice. You can use the VM argument
`-Dconfig.filepath=<path>` where `<path>` is the path to the config file (either relative or absolute).

### File Discovery:

If you only plan on having a single config file, then this would be the best choice. All you need to do is create the
file `frameworkConfig.yaml` in either the repo root, `./src/test/resources` or `src/test/resources/configs` and the
framework will take care of the rest.

## Config Structure:

For a working example, refer to `src/test/resources/configs/frameworkConfig.yaml`

```yaml
environment:                    #< (OPTIONAL) The object containing information about the test environment.
  filepath: <string>            #< The filepath that points to the .env file that will be used.
  name: <string>                #< (OPTIONAL) The name of the environment that is being used.
                                #
downloadManagement:             # (OPTIONAL) Used to define how we want to handle the downloaded files.
  deletionCondition: <string>   #< Dictates when to delete downloaded files [afterAll, afterEach or never]
  attachmentCondition: <string> #< Dictates when to attach downloaded files to a scenario [always, failure, never]
                                #
profiles:                       #< The list of driver profiles that are available to us.
  - name: <string>              #< The name of a driver profile.
    description: <string>       #< (OPTIONAL) A description for the device profile. Mainly used for documentation.
    systemProperties:           #< (OPTIONAL) The list of system properties that should be set for this driver profile.
      key: value                #< The SystemProperty mapping that we want to set.
      ...                       #
    driverOptions:              #< The object that outlines the specifics of the driver profile.
      name: <string>            #< What driver should be used [chrome, edge, firefox, browserstack, appium]
      driverVersion: <string>   #< The version of the driver that should be used (only applies to local drivers)
      browserVersion: <string>  #< The version of the browser that should be used (only applies to local drivers)
      capabilities:             #< The list of capabilities that will be used when creating the driver.
        key: value              #< The capability mapping.
        ...                     #
      preferences:              #< (OPTIONAL) The preferences that should be applied to the driver. Mainly used by Firefox.
        key: value              #< The preference mapping.
        ...                     #
      arguments:                #< (OPTIONAL) The list of launch arguments that should be used when starting the driver.
        - <string>              #< A launch argument.
        - ...                   #
      experimentalOptions:      #< (OPTIONAL) The map containing the experimental options that we want to use for Chromium based drivers.
        key: value              # The experimental option mapping.
        ...                     #
  - ...                         #
                                #
defaultProfile: <string>        #< (OPTIONAL) The default profile that should be used in the event that one is not provided in the VM args.
                                #
systemProperties:               #< (OPTIONAL) The list of system properties that should be set before running any tests. Will be overwritten by driver specific system properties.
  key: value                    #< The SystemProperty mapping that we want to set.
  ...                           #
```

## Running Tests:

The following command can be used to run tests:

 ```shell
 gradle cucumber -Dcucumber.filter.tags="..."
 ```

All the possible arguments that can be used are listed below:

- `-Dcucumber.filter.tags` = The tags of tests that should be run.
- `-Dconfig.filepath` = (OPTIONAL) The filepath pointing to the desired config file.
- `-Prunner.profiles` = (OPTIONAL) The comma separated list of profiles that will be used to run tests. All profiles 
    in this list will be run in the order they are provided. This will overwrite any profile set by `-Dconfig.profile`.
- `-Prunner.threads` = (OPTIONAL) The number of parallel workers that should be used during testing. 
    Should be more than 0.

If you want to run a specific test set using multiple different profiles, you can do so by leveraging the 
`-Prunner.profiles` property. For example, if we use the following property: 
`-Prunner.profiles="chrome-desktop, firefox-desktop, edge-desktop"`. Then the test runner will execute the provided test 
set on the profiles `chrome-desktop`, `firefox-desktop`, and `edge-desktop` sequentially and in that order. Each test 
set will track test results separately, although, the build will only succeed if all tests from all test sets have passed.