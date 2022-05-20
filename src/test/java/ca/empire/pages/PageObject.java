package ca.empire.pages;

import ca.empire.setup.Hooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class PageObject {
    protected WebDriver driver;

    private static final Logger logger = LogManager.getLogger(PageObject.class);

    protected static final int DEFAULT_WAIT_TIME = 10;
    protected static final int DEFAULT_POLL_TIME = 1;

    /*
    ====================================================================================================================
                                                        PAGE SETUP
    ====================================================================================================================
     */

    public PageObject() {
        logger.traceEntry();
        driver = Hooks.getDriver();
        initFactory();
        logger.traceExit();
    }

    /** Used to setup and initialize anything related to the PageFactory. */
    private void initFactory() {
        logger.traceEntry();
        AjaxElementLocatorFactory ajaxFactory = new AjaxElementLocatorFactory(driver, 1);
        PageFactory.initElements(ajaxFactory, this);
        logger.traceExit();
    }

    /*
    ====================================================================================================================
                                                        UTIL METHODS
    ====================================================================================================================
     */

    private boolean checkJsExecutor() {
        logger.traceEntry();
        boolean isJsExecutor = JavascriptExecutor.class.isAssignableFrom(driver.getClass());
        logger.traceExit(isJsExecutor);
        return isJsExecutor;
    }

    /**
     * Generates the XPath for the provided element.
     *
     * @param element - The element that we want the XPath for.
     * @return The XPath for the provided element (e.g. "/html/body/div/div/div[2]").
     */
    public String generateXPath(WebElement element) {
        logger.traceEntry(() -> element);
        String xPath = generateXPathHelper(element, "");
        logger.traceExit(xPath);
        return xPath;
    }

    /**
     * Helper method for generateXPath(WebElement). It is strongly recommended that you use
     * generateXPath(WebElement) instead of this method.
     *
     * @param element - The current element that is being used for XPath generation.
     * @param currentPath - The current XPath that will be built upon after each recursive step.
     * @return The XPath for the provided element (e.g. "/html/body/div/div/div[2]").
     */
    private String generateXPathHelper(WebElement element, String currentPath) {
        logger.traceEntry(() -> element, () -> currentPath);

        // adapted from
        // https://stackoverflow.com/questions/18510576/find-an-element-by-text-and-get-xpath-selenium-webdriver-junit
        int matchingTagCount;
        String elementTag;
        WebElement parentElement;
        List<WebElement> siblingElements;

        elementTag = element.getTagName();

        if (elementTag.equals("html")) {
            String finalPath = "/html[1]" + currentPath;
            logger.traceExit(finalPath);
            return finalPath;
        }

        parentElement = element.findElement(By.xpath(".."));
        siblingElements = parentElement.findElements(By.xpath("*"));
        matchingTagCount = 0;

        for (WebElement siblingElement : siblingElements) {
            String siblingTag = siblingElement.getTagName();

            if (siblingTag.equals(elementTag)) {
                matchingTagCount++;
            }

            if (siblingElement.equals(element)) {
                String newPath = "/" + elementTag + "[" + matchingTagCount + "]" + currentPath;
                newPath = generateXPathHelper(parentElement, newPath);
                logger.traceExit(newPath);
                return newPath;
            }
        }

        // We really shouldn't reach this, but I am putting this here so we can at least return a
        // relative XPath...
        String relativePath = "//" + elementTag + currentPath;
        logger.traceExit(relativePath);
        return relativePath;
    }

    /**
     * Waits for an element to be visible before proceeding. This is accomplished by polling for the
     * element every {@value this#DEFAULT_POLL_TIME} second(s) for {@value this#DEFAULT_WAIT_TIME}
     * second(s).
     *
     * @param element - The element to wait for.
     */
    public void waitForVisible(WebElement element) {
        logger.traceEntry(() -> element);
        waitForVisible(element, DEFAULT_WAIT_TIME, DEFAULT_POLL_TIME);
    }

    /**
     * Waits for an element to be visible before proceeding.
     *
     * @param element - The element to wait for.
     * @param maxWait - The maximum wait time that we should wait for.
     * @param pollTime - The amount of time in seconds between each poll.
     */
    public void waitForVisible(WebElement element, int maxWait, int pollTime) {
        logger.traceEntry(() -> element, () -> maxWait, () -> pollTime);
        new WebDriverWait(driver, maxWait)
                .pollingEvery(Duration.ofSeconds(pollTime))
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.visibilityOf(element));
        logger.traceExit();
    }

    /**
     * Waits for an element to be present in the DOM before proceeding. This is accomplished by
     * polling for the element every {@value this#DEFAULT_POLL_TIME} second(s) for {@value
     * this#DEFAULT_WAIT_TIME} second(s).
     *
     * @param xPath - The xPath pointing to the element to wait for.
     */
    public void waitForPresence(String xPath) {
        logger.traceEntry(() -> xPath);
        waitForPresence(xPath, DEFAULT_WAIT_TIME, DEFAULT_POLL_TIME);
        logger.traceExit();
    }

    /**
     * Waits for an element to be present in the DOM before proceeding.
     *
     * @param xPath - The xPath pointing to the element to wait for.
     * @param maxWait - The maximum wait time that we should wait for.
     * @param pollTime - The amount of time in seconds between each poll.
     */
    public void waitForPresence(String xPath, int maxWait, int pollTime) {
        logger.traceEntry(() -> xPath, () -> maxWait, () -> pollTime);
        new WebDriverWait(driver, maxWait)
                .pollingEvery(Duration.ofSeconds(pollTime))
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath(xPath)));
        logger.traceExit();
    }

    /**
     * Waits for an element to be detached from the DOM before proceeding. This is accomplished by
     * polling for the element every {@value this#DEFAULT_POLL_TIME} second(s) for {@value
     * this#DEFAULT_WAIT_TIME} second(s).
     *
     * @param element - The element to wait for.
     */
    public void waitForStale(WebElement element) {
        logger.traceEntry(() -> element);
        waitForStale(element, DEFAULT_WAIT_TIME, DEFAULT_POLL_TIME);
        logger.traceExit();
    }

    /**
     * Waits for an element to be detached from the DOM before proceeding.
     *
     * @param element - The element to wait for.
     * @param maxWait - The maximum wait time that we should wait for.
     * @param pollTime - The amount of time in seconds between each poll.
     */
    public void waitForStale(WebElement element, int maxWait, int pollTime) {
        logger.traceEntry(() -> element, () -> maxWait, () -> pollTime);
        new WebDriverWait(driver, maxWait)
                .pollingEvery(Duration.ofSeconds(pollTime))
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.stalenessOf(element));
        logger.traceExit();
    }

    /**
     * Waits for the element to be invisible. This is accomplished by polling for the element every
     * {@value this#DEFAULT_POLL_TIME} second(s) for {@value this#DEFAULT_WAIT_TIME} second(s).
     *
     * @param element - The element to wait for.
     */
    public void waitForInvisible(WebElement element) {
        logger.traceEntry(() -> element);
        waitForInvisible(element, DEFAULT_WAIT_TIME, DEFAULT_POLL_TIME);
        logger.traceExit();
    }

    /**
     * Waits for the element to be invisible.
     *
     * @param element - The element to wait for.
     * @param maxWait - The maximum wait time that we should wait for.
     * @param pollTime - The amount of time in seconds between each poll.
     */
    public void waitForInvisible(WebElement element, int maxWait, int pollTime) {
        logger.traceEntry(() -> element, () -> maxWait, () -> pollTime);
        new WebDriverWait(driver, maxWait)
                .pollingEvery(Duration.ofSeconds(pollTime))
                .ignoring(NoSuchElementException.class)
                .until(ExpectedConditions.invisibilityOf(element));
        logger.traceExit();
    }

    /**
     * This method will wait for a given amount of time before returning. This should be used very
     * sparingly. If possible, you should be using a wait with an early exit condition instead of
     * relying on static wait times.
     *
     * @param seconds - The time in seconds that we should wait for.
     */
    public void waitFor(int seconds) {
        logger.traceEntry(() -> seconds);
        try {
            Thread.sleep(seconds);
        } catch (InterruptedException e) {
            logger.error(e);
            e.printStackTrace();
        }
        logger.traceExit();
    }

    /**
     * Wrapper for WebDriver.findElement(By). Attempts to find an element using the provided By
     * method.
     *
     * @param by - The locating mechanism.
     * @throws org.openqa.selenium.NoSuchElementException – If no matching elements are found.
     * @return The first matching element on the current page.
     */
    public WebElement findElement(By by) {
        logger.traceEntry(() -> by);
        WebElement element = driver.findElement(by);
        logger.traceExit();
        return element;
    }

    /**
     * Wrapper for WebDriver.findElements(By). Attempts to find all elements using the provided By
     * method.
     *
     * @param by - The locating mechanism.
     * @return - A list of all WebElements, or an empty list if nothing matches
     */
    public List<WebElement> findElements(By by) {
        logger.traceEntry(() -> by);
        List<WebElement> elements = driver.findElements(by);
        logger.traceExit();
        return elements;
    }

    /**
     * Used to scroll an element into view. If possible, this will be done by scrolling the element
     * into the middle of the window using JavaScript. Otherwise, this will be done using the
     * Actions library.
     *
     * @param element - The element to scroll to.
     */
    public void scrollTo(WebElement element) {
        logger.traceEntry(() -> element);
        if (checkJsExecutor()) {
            int halfHeight, windowHeight;

            windowHeight = driver.manage().window().getSize().height;
            halfHeight = windowHeight == 0 ? 0 : windowHeight / 2;

            scrollTo(element, -halfHeight);
            return;
        }

        new Actions(driver).moveToElement(element).perform();
        waitFor(500);
        logger.traceExit();
    }

    /**
     * Used to scroll an element into view with a given offset. This method relies on JavaScript to
     * perform the scrolling action. It is recommended that you use the scrollTo(WebElement) method
     * instead if there is a chance that the WebDriver you are using does not support JavaScript
     * execution.
     *
     * @param element - The element to scroll to.
     * @param yOffset - The vertical offset from the provided element that will act as the scroll to
     *     point. Using a positive value will scroll the top of the window to a point below the
     *     element. Using a negative value will scroll the top of the window to a point above the
     *     element. The top left of a web page is considered the origin (i.e. coordinate location
     *     (0, 0)).
     */
    public void scrollTo(WebElement element, int yOffset) {
        logger.traceEntry(() -> element, () -> yOffset);

        if (!checkJsExecutor()) {
            logger.warn(
                    "This method should only be used if the provided driver is capable of "
                            + "executing JavaScript...");
            scrollTo(element); // kind of a cyclical dependency here, but this is better then
            // throwing an error
            return;
        }

        int targetY = element.getLocation().y + yOffset;

        ((JavascriptExecutor) driver)
                .executeScript(
                        "window.scrollTo({top: arguments[0], behavior: arguments[1]});",
                        targetY,
                        "smooth");

        waitFor(500);
        logger.traceExit();
    }

    /**
     * Clicks on a given element. If possible, this will be done with JavaScript. Otherwise we will
     * simply use .click().
     *
     * @param element - The element to click on.
     */
    public void click(WebElement element) {
        logger.traceEntry(() -> element);
        scrollTo(element);

        if (!checkJsExecutor()) {
            element.click();
            return;
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        logger.traceExit();
    }

    /**
     * Double clicks on a given element.
     *
     * @param element - The element to double click.
     */
    public void doubleClick(WebElement element) {
        logger.traceEntry(() -> element);
        scrollTo(element);
        new Actions(driver).doubleClick(element).perform();
        logger.traceExit();
    }

    /**
     * Types the provided text into a given element.
     *
     * @param element - The element that will be typed in.
     * @param text - The text to type in.
     */
    public void typeIn(WebElement element, String text, boolean clearText) {
        logger.traceEntry(() -> element, () -> text, () -> clearText);
        click(element);

        if (clearText) {
            element.clear();
        }

        element.sendKeys(text);
        logger.traceExit();
    }

    /**
     * Retrieves the text from a given element.
     *
     * @param element - The element to pull the text from.
     * @return The text from the element.
     */
    public String getText(WebElement element) {
        logger.traceEntry(() -> element);
        String text = element.getText();
        logger.traceExit(text);
        return text;
    }

    /**
     * Retrieves the CSS value for a given element and CSS property.
     *
     * @param element - The element to pull the value from.
     * @param cssProperty - The property to pull the value from.
     * @return The current CSS value of the given property.
     */
    public String getCssValue(WebElement element, String cssProperty) {
        logger.traceEntry(() -> element, () -> cssProperty);
        String value = element.getCssValue(cssProperty);
        logger.traceExit(value);
        return value;
    }
}
