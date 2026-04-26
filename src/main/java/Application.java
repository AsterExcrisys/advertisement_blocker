import com.asterexcrisys.adblocker.ProxyCommand;
import com.asterexcrisys.adblocker.services.ExceptionHandler;
import picocli.CommandLine;

@SuppressWarnings("unused")
public class Application {

    // TODO: implement TLS, HTTP, and HTTPS modes as well as make them dynamically dispatchable

    public static void main(String[] arguments) {
        CommandLine command = new CommandLine(new ProxyCommand());
        ExceptionHandler handler = new ExceptionHandler();
        command.setParameterExceptionHandler(handler);
        command.setExecutionExceptionHandler(handler);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        System.exit(command.execute(arguments));
    }

}