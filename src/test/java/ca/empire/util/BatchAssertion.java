package ca.empire.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class BatchAssertion
{
    private final LinkedList<Assertion> assertions;
    private final Semaphore assertionSemaphore;
    private String message;

    public enum UnaryAssertionMethod
    {
        NOT_NULL,
        NULL,
        TRUE,
        FALSE
    }

    public enum BinaryAssertionMethod
    {
        EQUALS,
        NOT_EQUALS,
        SAME,
        NOT_SAME,
        ARRAY_EQUALS
    }


    /**
     * Creates a new batch assertion. Batch assertions are useful when you want to ensure that N > 1 assertions are
     * made regardless of the result of the other assertions in the batch. Please note that batch assertions work best
     * when each assertion in the batch is independent from one another.
     */
    public BatchAssertion()
    {
        assertions = new LinkedList<>();
        assertionSemaphore = new Semaphore(1, true);
    }


    /**
     * Creates a new batch assertion. Batch assertions are useful when you want to ensure that N > 1 assertions are
     * made regardless of the result of the other assertions in the batch. Please note that batch assertions work best
     * when each assertion in the batch is independent from one another.
     *
     * @param message String - The message to display in the event that one or more assertions do not hold.
     */
    public BatchAssertion(String message)
    {
        this();
        this.message = message;
    }


    /**
     * Assesses all of the assertions in the batch
     * @throws AssertionError - When one or more assertions in the batch do not hold.
     */
    public void assess() throws AssertionError
    {
        StringBuilder errorMessage;
        int index;
        int numAssertions;

        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new AssertionError("AUTOMATION BUG: " + e.getMessage());
        }


        // If there are not assertions to be made, then we can just return
        if (assertions.isEmpty())
        {
            System.out.println("No assertions have been made.");
            assertionSemaphore.release();
            return;
        }

        errorMessage = new StringBuilder(4096);
        index = 0;
        numAssertions = assertions.size();

        // Assess all of the assertions and capture any of their errors
        while (!assertions.isEmpty())
        {
            Assertion assertion = assertions.poll();

            if (!assertion.assess())
            {
                if(errorMessage.toString().isEmpty())
                {
                    errorMessage.append(message == null ? "The following assertions did not hold:" : message);
                }

                String newMessage = "Assertion " + index + ": " + assertion.getError().getMessage();
                errorMessage.append("\n").append(newMessage);
            }

            index++;
        }

        assertionSemaphore.release();

        // If there are no error messages then no errors have occurred, so we are all good!
        if (errorMessage.toString().isEmpty())
        {
            System.out.println("All " + numAssertions + " assertion(s) have held.");
            return;
        }

        // Otherwise, throw an AssertionError with the all the failed assertions
        throw new AssertionError(errorMessage.toString());
    }


    /**
     * Adds the equivalent of Assert.fail() to the batch.
     */
    public void addFailure()
    {
        addFailure(null);
    }


    /**
     * Adds the equivalent of Assert.fail(message) to the batch.
     * @param message The message to be included in the assertion error.
     */
    public void addFailure(@Nullable String message)
    {
        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new RuntimeException("AUTOMATION BUG: " + e.getMessage());
        }

        assertions.add(new Failure(message));
        assertionSemaphore.release();
    }


    /**
     * Add a new unary assertion that needs to be performed during the batch assertion
     * @param object - The object that the assertion will be performed on
     * @param method - The method used for the assertion
     */
    public void addAssertion(
            @Nullable Object object,
            @NotNull UnaryAssertionMethod method)
    {
        addAssertion(null, object, method);
    }


    /**
     * Add a new unary assertion that needs to be performed during the batch assertion
     * @param message - The message to display in the event that the assertion does not hold
     * @param object - The object that the assertion will be performed on
     * @param method - The method used for the assertion
     */
    public void addAssertion(
            @Nullable String message,
            @Nullable Object object,
            @NotNull UnaryAssertionMethod method)
    {
        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new RuntimeException("AUTOMATION BUG: " + e.getMessage());
        }

        assertions.add(new UnaryAssertion(message, object, method));
        assertionSemaphore.release();
    }


    /**
     * Add a new binary assertion that needs to be performed during the batch assertion
     * @param expected - The expected object
     * @param actual - The actual object
     * @param method - The method that we want to use to make out assertion
     */
    public void addAssertion(
            @Nullable Object expected,
            @Nullable Object actual,
            @NotNull BinaryAssertionMethod method)
    {
        addAssertion(null, expected, actual, method);
    }


    /**
     * Add a new binary assertion that needs to be performed during the batch assertion
     * @param expected - The expected object
     * @param actual - The actual object
     * @param method - The method that we want to use to make out assertion
     * @param allowableDelta - The allowable delta between the two doubles before they are no longer considered "equal"
     */
    public void addAssertion(
            @Nullable Object expected,
            @Nullable Object actual,
            @NotNull BinaryAssertionMethod method,
            double allowableDelta)
    {
        addAssertion(null, expected, actual, method, allowableDelta);
    }


    /**
     * Add a new binary assertion that needs to be performed during the batch assertion
     * @param message - The message to display in the event that the assertion does not hold
     * @param expected - The expected object
     * @param actual - The actual object
     * @param method - The method that we want to use to make out assertion
     */
    public void addAssertion(
            @Nullable String message,
            @Nullable Object expected,
            @Nullable Object actual,
            @NotNull BinaryAssertionMethod method)
    {
        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new RuntimeException("AUTOMATION BUG: " + e.getMessage());
        }

        assertions.add(new BinaryAssertion(message, expected, actual, method));
        assertionSemaphore.release();
    }


    /**
     * Add a new binary assertion that needs to be performed during the batch assertion
     * @param message - The message to display in the event that the assertion does not hold
     * @param expected - The expected object
     * @param actual - The actual object
     * @param method - The method that we want to use to make out assertion
     * @param allowableDelta - The allowable delta between the two doubles before they are no longer considered "equal"
     */
    public void addAssertion(
            @Nullable String message,
            @Nullable Object expected,
            @Nullable Object actual,
            @NotNull BinaryAssertionMethod method,
            double allowableDelta)
    {
        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new RuntimeException("AUTOMATION BUG: " + e.getMessage());
        }

        assertions.add(new BinaryAssertion(message, expected, actual, method, allowableDelta));
        assertionSemaphore.release();
    }


    @Override
    public String toString()
    {
        StringBuilder builtString;
        int index;

        builtString = new StringBuilder(4096);
        index = 0;

        try
        {
            assertionSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
            throw new RuntimeException("AUTOMATION BUG: " + e.getMessage());
        }

        for(Assertion assertion : assertions)
        {
            String newString = "Assertion " + index + ": " + assertion.toString()
                    + (assertion.equals(assertions.getLast()) ? "" : "\n");
            index++;

            builtString.append(newString);
        }

        assertionSemaphore.release();
        return builtString.toString();
    }


    public int size()
    {
        return assertions.size();
    }

    //==================================================================================================================
    //                                           JUnit Assert Wrappers
    //==================================================================================================================


    /**
     * The abstract class that is used to represent an Assertion. An assertion can be either Unary or Binary, and can
     * be assessed. In the case that the assessment fails, then an assertion will provide an error.
     */
    private abstract class Assertion
    {
        protected AssertionError error = null;
        protected String message = null;

        /**
         * Assesses the assertion
         * @return Whether or not the assertion held.
         */
        abstract boolean assess();


        /**
         * Retrieves the AssertionError that occurred when assessing the Assertion.
         * @return AssertionError - The AssertionError that occured. Null if no error occurred or if the assertion has
         * not been assessed yet.
         */
        public AssertionError getError()
        {
            return error;
        }
    }

    //==================================================================================================================
    //                                              Failure Wrapper
    //==================================================================================================================


    /**
     * Used to represent a forced assertion failure. This essentially acts as a wrapper for a JUnit Assert.fail().
     */
    private class Failure extends Assertion
    {
        /**
         * Creates a wrapper for Assert.fail().
         * @param message The message to be included in the assertion error.
         */
        public Failure(@Nullable String message)
        {
            this.message = message;
        }


        /**
         * Assesses the assertion. In this case, the assertion will always fail.
         * @return false since the assertion will never hold.
         */
        boolean assess()
        {
            try
            {
                Assert.fail(message);
            }
            catch (AssertionError e)
            {
                error = e;
            }
            return false;
        }
    }

    //==================================================================================================================
    //                                          Unary Assertion Wrapper
    //==================================================================================================================


    /**
     * Used to represent an unary assertion (i.e. performs an assertion against a single object).
     * This essentially acts as a wrapper for a unary JUnit assertion and will catch any assertion error so that it
     * can be retrieved later on.
     */
    private class UnaryAssertion extends Assertion
    {
        protected Object object;
        protected UnaryAssertionMethod method;


        /**
         * Create an object representing a unary assertion
         * @param object - The object that the assertion will be performed on
         * @param method - The method used for the assertion
         */
        public UnaryAssertion(
                @Nullable Object object,
                @NotNull UnaryAssertionMethod method)
        {
            this.object = object;
            this.method = method;
        }


        /**
         * Create an object representing a unary assertion
         * @param object - The object that the assertion will be performed on
         * @param method - The method used for the assertion
         */
        public UnaryAssertion(
                @Nullable String message,
                @Nullable Object object,
                @NotNull UnaryAssertionMethod method)
        {
            this(object, method);
            this.message = message;
        }


        public boolean assess()
        {
            try
            {
                switch (method)
                {
                    case NULL:
                        Assert.assertNull(message, object);
                        break;

                    case NOT_NULL:
                        Assert.assertNotNull(message, object);
                        break;

                    case FALSE:
                        if (object instanceof Boolean)
                        {
                            Assert.assertFalse(message, (Boolean) object);
                            break;
                        }
                        else if (object instanceof Number)
                        {
                            Assert.assertEquals(message, 0, object);
                            break;
                        }
                        else if(object instanceof String)
                        {
                            Assert.assertTrue(
                                    message == null ? object.toString() + " is not False" : message,
                                    ((String) object).isEmpty()
                            );
                        }

                        // At this point the object is only falsy if it is null
                        Assert.assertNull(message, object);
                        break;

                    case TRUE:
                        if (object instanceof Boolean)
                        {
                            Assert.assertTrue(message, (Boolean) object);
                            break;
                        }
                        else if (object instanceof Number)
                        {
                            Assert.assertNotEquals(message,0, object);
                            break;
                        }
                        if(object instanceof String)
                        {
                            Assert.assertFalse(
                                    message == null ? object.toString() + " is not True" : message,
                                    ((String) object).isEmpty()
                            );
                        }

                        // At this point the object is only truthy if it is not null
                        Assert.assertNotNull(message, object);
                        break;

                    default:
                        return false;
                }

            }
            catch (AssertionError e)
            {
                error = e;
                return false;
            }
            catch (Exception e)
            {
                throw new RuntimeException("AUTOMATION ERROR: An unexpected error has occurred when trying to assess " +
                        "the following assertion: " + this.toString());
            }

            return true;
        }


        @Override
        public String toString()
        {
            return "<" + object.toString() + "> is " + method.name();
        }
    }

    //==================================================================================================================
    //                                          Binary Assertion Wrapper
    //==================================================================================================================


    /**
     * Used to represent a binary assertion (i.e. performs an assertion between two objects).
     * This essentially acts as a wrapper for a binary JUnit assertion and will catch any assertion error so that it
     *      * can be retrieved later on.
     */
    private class BinaryAssertion extends Assertion
    {
        protected Object expected;
        protected Object actual;
        protected BinaryAssertionMethod method;

        protected double allowableDelta = 0;


        /**
         * Create an object representing a binary assertion.
         * @param expected - The expected object
         * @param actual - The actual object
         * @param method - The method that we want to use to make out assertion
         */
        public BinaryAssertion(
                @Nullable Object expected,
                @Nullable Object actual,
                @NotNull BinaryAssertionMethod method)
        {
            this.expected = expected;
            this.actual = actual;
            this.method = method;
        }


        /**
         * Create an object representing a binary asserion between two doubles.
         * @param expected
         * @param actual
         * @param method
         * @param allowableDelta
         */
        public BinaryAssertion(
                @NotNull Object expected,
                @NotNull Object actual,
                @NotNull BinaryAssertionMethod method,
                double allowableDelta)
        {
            this(expected, actual, method);
            this.allowableDelta = allowableDelta;
        }


        /**
         * Create an object representing a binary assertion.
         * @param expected - The expected object
         * @param actual - The actual object
         * @param method - The method that we want to use to make out assertion
         */
        public BinaryAssertion(
                @Nullable String message,
                @Nullable Object expected,
                @Nullable Object actual,
                @NotNull BinaryAssertionMethod method)
        {
            this(expected, actual, method);
            this.message = message;
        }


        /**
         *
         * @param message
         * @param expected
         * @param actual
         * @param method
         * @param allowableDelta
         */
        public BinaryAssertion(
                @Nullable String message,
                @Nullable Object expected,
                @Nullable Object actual,
                @NotNull BinaryAssertionMethod method,
                double allowableDelta)
        {
            this(message, expected, actual, method);
            this.allowableDelta = allowableDelta;
        }


        boolean assess()
        {
            try
            {
                switch (method)
                {
                    case EQUALS:
                        if (expected instanceof Double && actual instanceof Double)
                        {
                            Assert.assertEquals(message, (double) expected, (double) actual, allowableDelta);
                            break;
                        }

                        Assert.assertEquals(message, expected, actual);
                        break;

                    case NOT_EQUALS:
                        if (expected instanceof Double && actual instanceof Double)
                        {
                            Assert.assertNotEquals(message, (double) expected, (double) actual, allowableDelta);
                            break;
                        }

                        Assert.assertNotEquals(message, expected, actual);
                        break;

                    case SAME:
                        Assert.assertSame(message, expected, actual);
                        break;

                    case NOT_SAME:
                        Assert.assertNotSame(message, expected, actual);
                        break;
                    case ARRAY_EQUALS:
                        if (expected instanceof double[] && actual instanceof double[])
                        {
                            Assert.assertArrayEquals(message, (double[]) expected, (double[]) actual, allowableDelta);
                            break;
                        }

                        Assert.assertArrayEquals(message, (Object[]) expected, (Object[]) actual);
                        break;

                    default:
                        return false;
                }
            }
            catch (AssertionError e)
            {
                error = e;
                return false;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new RuntimeException("AUTOMATION ERROR: An unexpected error has occurred when trying to assess " +
                        "the following assertion: " + this.toString());
            }

            return true;
        }


        @Override
        public String toString()
        {
            return "<" + expected.toString() + "> " + method.name() + " <" + actual.toString() + ">";
        }
    }
}