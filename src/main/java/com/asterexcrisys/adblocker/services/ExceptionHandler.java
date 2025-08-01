package com.asterexcrisys.adblocker.services;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExecutionExceptionHandler;

@SuppressWarnings("unused")
public class ExceptionHandler implements IExecutionExceptionHandler, IParameterExceptionHandler {

    @Override
    public int handleExecutionException(Exception exception, CommandLine commandLine, ParseResult parseResult) {
        System.err.printf("Error: %s\n", exception.getMessage());
        return ExitCode.SOFTWARE;
    }

    @Override
    public int handleParseException(ParameterException exception, String[] arguments) {
        System.err.printf("Error: %s\n", exception.getMessage());
        return ExitCode.SOFTWARE;
    }

}