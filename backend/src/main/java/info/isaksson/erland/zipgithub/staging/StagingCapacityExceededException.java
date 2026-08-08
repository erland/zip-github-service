package info.isaksson.erland.zipgithub.staging;

/** Raised after durable quota serialization when staging disk/object capacity is exhausted. */
public final class StagingCapacityExceededException extends RuntimeException {
    public StagingCapacityExceededException(String message) { super(message); }
}
