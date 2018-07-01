package my;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {

    public static void main(String[] args) throws Exception {
//        WebSocketImpl.DEBUG = true;
        run("wss://api.fcoin.com/v2/ws");
    }

    private static MyClient myClient = null;

    private static void run(String url) throws Exception {
        myClient = new MyClient(new URI(url), true);
        myClient.run();
    }

}

class MyClient {

    private ScheduledExecutorService sss = Executors.newSingleThreadScheduledExecutor();

    MyClient(URI uri, boolean autoReconnect) {
        this.uri = uri;
        this.autoReconnect = autoReconnect;
    }

    private final URI uri;
    private boolean autoReconnect;
    String message;


    void run() {
        try {
            connect();
        } catch (Exception e) {
            System.out.println(e.toString());

            // NOTICE: should not call `reconnect` here, since this exception (from `connectBlocking`) will trigger `onClose` method
            // reconnect();
        }
    }

    private void reconnect() {
        if (!autoReconnect) {
            return;
        }

        sss.schedule(() -> {
            System.out.println();
            System.out.println("### reconnect");
            System.out.println();
            MyClient.this.run();
        }, 3, TimeUnit.SECONDS);
    }

    private void connect() throws Exception {
        WebSocketClient client = createClient();
        client.connectBlocking();
        client.send("{" +
                "  \"cmd\": \"sub\", " +
                "  \"args\": [\"ticker.ftusdt\"]" +
                "}");
    }

    private WebSocketClient createClient() {
        WebSocketClient client = new WebSocketClient(uri, new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("onOpen");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("onMessage: " + message);
                MyClient.this.message = message;
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("onError");
                ex.printStackTrace();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println(String.format("onClose(code: %s, reason: %s, remote: %s)", code, reason, remote));
                reconnect();
            }
        };
        // If not receive any message from server more than 10s, close the connection
        client.setConnectionLostTimeout(10);
        return client;
    }
}