package ca.empire.setup.configuration.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DownloadManagement implements Model {
    public final DeletionCondition deletionCondition;
    public final AttachmentCondition attachmentCondition;
    private static final Logger logger = LogManager.getLogger(DownloadManagement.class);

    public enum DeletionCondition {
        afterEach,
        afterAll,
        never
    }

    public enum AttachmentCondition {
        always,
        unsuccessful,
        failure,
        never
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DownloadManagement(
            @JsonProperty(value = "deletionCondition", required = true) String deletionCondition,
            @JsonProperty(value = "attachmentCondition", required = true) String attachmentCondition) {
        logger.traceEntry(
                () -> deletionCondition,
                () -> attachmentCondition);
        this.deletionCondition = DeletionCondition.valueOf(deletionCondition);;
        this.attachmentCondition = AttachmentCondition.valueOf(attachmentCondition);
        logger.traceExit(this);
    }
}
