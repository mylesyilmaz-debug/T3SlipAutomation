package ca.empire.exceptions;

/**
 * Used to indicate that an automation error has occurred (e.g. a step definition being given an
 * invalid input).
 */
public class AutomationException extends RuntimeException {
    /** Constructs an AutomationException with not detail message. */
    public AutomationException() {
        super();
    }

    /**
     * Constructs an AutomationException with the provided detail message.
     *
     * @param message - The String that contains the detail message.
     */
    public AutomationException(String message) {
        super(message);
    }

    /**
     * Constructs an AutomationException with the provided detail message and cause.
     *
     * @param message - The String that contains the detail message.
     * @param cause - The cause (can be null to indicate that the cause is nonexistent or unknown).
     */
    public AutomationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an AutomationException with the provided cause.
     *
     * @param cause - The cause (can be null to indicate that the cause is nonexistent or unknown).
     */
    public AutomationException(Throwable cause) {
        super(cause);
    }
}
