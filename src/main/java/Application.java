import com.asterexcrisys.gab.ProxyManager;
import com.asterexcrisys.gab.resolvers.STDResolver;
import com.asterexcrisys.gab.utility.UDPPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Application {

    private static final int SERVER_PORT = 53;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            ProxyManager manager = new ProxyManager();
            manager.addResolver(new STDResolver("1.1.1.1"));
            manager.setCacheMaximumSize(1000);
            manager.addFilterDomains(List.of("google.com", "amazon.com", "microsoft.com", "apple.com", "meta.com"));
            BlockingQueue<UDPPacket> requests = new LinkedBlockingQueue<>();
            BlockingQueue<UDPPacket> responses = new LinkedBlockingQueue<>();
            Thread reader = new Reader(socket, requests);
            Thread handler1 = new Handler(manager, requests, responses);
            Thread handler2 = new Handler(manager, requests, responses);
            Thread handler3 = new Handler(manager, requests, responses);
            Thread writer = new Writer(socket, responses);
            reader.setDaemon(true);
            reader.start();
            handler1.setDaemon(true);
            handler1.start();
            handler2.setDaemon(true);
            handler2.start();
            handler3.setDaemon(true);
            handler3.start();
            writer.setDaemon(true);
            writer.start();
            System.out.println("DNS Ad-Blocking Proxy started on port: " + SERVER_PORT);
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}