package com.aipanelist.consensus;

/**
 * Exception thrown when consensus generation fails.
 * 
 * This exception is thrown when the ConsensusEngine encounters errors
 * during the synthesis of agent reports into a consensus report.
 */
public class ConsensusGenerationException extends Exception {

    public ConsensusGenerationException(String message) {
        super(message);
    }

    public ConsensusGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
