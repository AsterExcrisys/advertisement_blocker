import com.asterexcrisys.gab.core.Handler;
import com.asterexcrisys.gab.ProxyManager;
import com.asterexcrisys.gab.core.Reader;
import com.asterexcrisys.gab.core.Writer;
import com.asterexcrisys.gab.resolvers.STDResolver;
import com.asterexcrisys.gab.utility.UDPPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("unused")
public class Application {

    private static final int SERVER_PORT = 53;
    private static final int REQUESTS_PER_HANDLER_THREAD = 100;
    private static final int MINIMUM_HANDLER_THREADS = 1;
    private static final int MAXIMUM_HANDLER_THREADS = 10;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            ProxyManager manager = new ProxyManager();
            manager.addResolver(new STDResolver("1.1.1.1"));
            manager.setCacheMaximumSize(1000);
            manager.addFilterDomains(List.of("google.com", "amazon.com", "microsoft.com", "apple.com", "meta.com"));
            BlockingQueue<UDPPacket> requests = new LinkedBlockingQueue<>();
            BlockingQueue<UDPPacket> responses = new LinkedBlockingQueue<>();
            Thread reader = new Reader(socket, requests);
            Thread writer = new Writer(socket, responses);
            List<Thread> handlers = new ArrayList<>(List.of(new Handler(manager, requests, responses)));
            reader.setDaemon(true);
            reader.start();
            writer.setDaemon(true);
            writer.start();
            for (Thread handler : handlers) {
                handler.setDaemon(true);
                handler.start();
            }
            System.out.println("DNS Ad-Blocking Proxy started on port: " + SERVER_PORT);
            while (!Thread.currentThread().isInterrupted()) {
                if (requests.size() < handlers.size() * (REQUESTS_PER_HANDLER_THREAD - 10)) {
                    if (handlers.size() == MINIMUM_HANDLER_THREADS) {
                        continue;
                    }
                    handlers.getLast().interrupt();
                    handlers.removeLast();
                    System.err.println("The last handler thread dispatch was reverted");
                    continue;
                }
                if (requests.size() > (handlers.size() + 1) * (REQUESTS_PER_HANDLER_THREAD + 10)) {
                    if (handlers.size() == MAXIMUM_HANDLER_THREADS) {
                        continue;
                    }
                    handlers.add(new Handler(manager, requests, responses));
                    handlers.getLast().setDaemon(true);
                    handlers.getLast().start();
                    System.err.println("A new handler thread was dispatched");
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}