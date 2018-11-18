package tcpc;

import org.junit.Test;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import reactor.core.publisher.Mono;
import reactor.netty.NettyPipeline;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by StAR on 2018/10/14.
 */
public class CodeTest {
    @Test
    public void test2() throws IOException {
        TcpServer.create().port(80).handle((in, out) -> {
            in.receive().asString().subscribe(d -> {
                System.out.println(d);
            });
            return Mono.never();
        }).bindNow();
        System.in.read();
    }

    @Test
    public void test1() throws InterruptedException, IOException {
        int port = 9192;

        TcpServer server = TcpServer.create().port(port);
        TcpClient client = TcpClient.create().host("projectreactor.io").port(80);

        server.handle((in, out) -> {
            client.connect()
                    .handle((conn, sink) -> {
                        conn.outbound()
                                .options(NettyPipeline.SendOptions::flushOnEach) // 每次执行send都进行flush
                                .sendByteArray(in.receive().asByteArray()).then().subscribe();
                        out.options(NettyPipeline.SendOptions::flushOnEach) // 每次执行send都进行flush
                                .sendByteArray(conn.inbound().receive().asByteArray()).then().subscribe();
                    })
                    .doOnTerminate(() -> System.out.println("Client Terminated..."))
                    .subscribe();
            return Mono.never();
        }).bindNow();
        System.in.read();
    }

    @Test
    public void test3() {
        MainClass.main(new String[]{"-m","c","-s","127.0.0.1:8124","-p","8123","-k","password"});
    }
}
