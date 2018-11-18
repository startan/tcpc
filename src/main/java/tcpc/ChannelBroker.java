package tcpc;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.NettyPipeline;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import java.time.Duration;
import java.util.function.Function;

/**
 * 客户端
 * <p>
 * Created by StAR on 2018/11/4.
 */
public class ChannelBroker {
    private String serverSideHost;
    private int serverSidePort;
    private int port;
    private byte key;

    private TcpServer server;
    private TcpClient client;
    private DisposableServer disposableServer;
    private boolean running;

    Function<byte[], byte[]> confusion = d -> {
        byte[] td = new byte[d.length];
        for (int i = 0; i < td.length; i++) {
            td[i] = (byte) (d[i] ^ key);
        }
        return td;
    };

    public ChannelBroker(String serverSideHost, int serverSidePort, int port, byte key) {
        this.serverSideHost = serverSideHost;
        this.serverSidePort = serverSidePort;
        this.port = port;
        this.key = key;
    }

    public void startup() {
        this.server = TcpServer.create().host("0.0.0.0").port(port);
        this.client = TcpClient.create().host(serverSideHost).port(serverSidePort);

        this.disposableServer = server.handle((in, out) -> {
            client.connect()
                    .handle((conn, sink) -> {
                        conn.outbound()
                                .options(NettyPipeline.SendOptions::flushOnEach) // 每次执行send都进行flush
                                .sendByteArray(in.receive().asByteArray().onErrorStop().map(confusion))
                                .then().subscribe();
                        out.options(NettyPipeline.SendOptions::flushOnEach) // 每次执行send都进行flush
                                .sendByteArray(conn.inbound().receive().asByteArray().onErrorStop().map(confusion)).then().subscribe();
                    })
                    .doOnError(throwable -> System.out.println(throwable.getMessage()))
                    .doOnTerminate(() -> System.out.println("Client Terminated..."))
                    .doOnCancel(() -> System.out.println("Client Canceled..."))
                    .subscribe();
            return Mono.never();
        }).bindNow();

        this.running = true;
    }

    public void shutdown(Duration timeout) {
        this.disposableServer.disposeNow(timeout);
    }

    public boolean isRunning() {
        return running;
    }
}
