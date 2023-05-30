package net.minecraft.server;

public final class RunningOnDifferentThreadException extends RuntimeException {
    public static final RunningOnDifferentThreadException RUNNING_ON_DIFFERENT_THREAD = new RunningOnDifferentThreadException();

    private RunningOnDifferentThreadException() {
        this.setStackTrace(org.plazmamc.plazma.util.Constants.STACK_TRACE_ELEMENT);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        this.setStackTrace(org.plazmamc.plazma.util.Constants.STACK_TRACE_ELEMENT);
        return this;
    }
}
