package ca.empire.steps;

import io.cucumber.java.en.Given;

public class SanitySteps {

    public SanitySteps() {
        // empty constructor REQUIRED
    }

    @Given("framework is ready")
    public void framework_is_ready() {

        System.out.println("Framework sanity check passed");

    }
}
