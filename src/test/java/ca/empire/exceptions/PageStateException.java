package ca.empire.exceptions;

/**
 * Used to indicate that an attempt was made to interact with a page element when said page was not
 * in the correct state. Typically this would be the result of an automation error, but it can also
 * signify other issues.
 */
public class PageStateException extends RuntimeException {
    /** Constructs an PageStateException with not detail message. */
    public PageStateException() {
        super();
    }

    /**
     * Constructs an PageStateException with the provided detail message.
     *
     * @param message - The String that contains the detail message.
     */
    public PageStateException(String message) {
        super(message);
    }

    /**
     * Constructs an PageStateException with the provided detail message and cause.
     *
     * @param message - The String that contains the detail message.
     * @param cause - The cause (can be null to indicate that the cause is nonexistent or unknown).
     */
    public PageStateException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an PageStateException with the provided cause.
     *
     * @param cause - The cause (can be null to indicate that the cause is nonexistent or unknown).
     */
    public PageStateException(Throwable cause) {
        super(cause);
    }
}
