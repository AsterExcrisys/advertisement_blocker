package com.asterexcrisys.adblocker.services;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExecutionExceptionHandler;

@SuppressWarnings("unused")
public class ExceptionHandler implements IParameterExceptionHandler, IExecutionExceptionHandler {

    @Override
    public int handleParseException(ParameterException exception, String[] arguments) {
        System.err.printf("Error: %s\n", exception.getMessage());
        return ExitCode.USAGE;
    }

    @Override
    public int handleExecutionException(Exception exception, CommandLine command, ParseResult result) {
        System.err.printf("Error: %s\n", exception.getMessage());
        return exception instanceof IllegalArgumentException? ExitCode.USAGE:ExitCode.SOFTWARE;
    }

}