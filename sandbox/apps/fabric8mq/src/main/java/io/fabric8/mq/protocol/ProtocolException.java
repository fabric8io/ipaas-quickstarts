package io.fabric8.mq.protocol;

import java.io.IOException;

public class ProtocolException extends IOException {

    private static final long serialVersionUID = -2869735532997332242L;

    private final boolean fatal;

    public ProtocolException() {
        this(null);
    }

    public ProtocolException(String s) {
        this(s, false);
    }

    public ProtocolException(String s, boolean fatal) {
        this(s, fatal, null);
    }

    public ProtocolException(String s, boolean fatal, Throwable cause) {
        super(s);
        this.fatal = fatal;
        initCause(cause);
    }

    public boolean isFatal() {
        return fatal;
    }

}
