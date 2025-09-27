package com.asterexcrisys.adblocker.services;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExecutionExceptionHandler;

@SuppressWarnings("unused")
public class ExceptionHandler implements IParameterExceptionHandler, IExecutionExceptionHandler, UncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public int handleParseException(ParameterException exception, String[] arguments) {
        LOGGER.error("An error occurred during parsing: {}", exception.getMessage());
        return ExitCode.USAGE;
    }

    @Override
    public int handleExecutionException(Exception exception, CommandLine command, ParseResult result) {
        LOGGER.error("An error occurred during execution: {}", exception.getMessage());
        return exception instanceof IllegalArgumentException? ExitCode.USAGE:ExitCode.SOFTWARE;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        LOGGER.error("An error occurred within thread '{}': {}", thread.getName(), exception.getMessage());
    }

}