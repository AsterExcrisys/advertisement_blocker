import com.asterexcrisys.adblocker.ProxyCommand;
import com.asterexcrisys.adblocker.services.ExceptionHandler;
import picocli.CommandLine;

@SuppressWarnings("unused")
public class Application {

    public static void main(String[] arguments) {
        CommandLine command = new CommandLine(new ProxyCommand());
        command.setParameterExceptionHandler(new ExceptionHandler());
        command.setExecutionExceptionHandler(new ExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        System.exit(command.execute(arguments));
    }

}