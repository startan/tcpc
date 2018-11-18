package tcpc;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 启动类
 *
 * Created by StAR on 2018/11/4.
 */
public class MainClass {
    public static void main(String[] args) {
        CommandLine cl;
        try {
            cl = parseCommandLine(args);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        String mode = cl.getOptionValue("m", "c");
        if ("c".equals(mode)) {
            String[] hap = cl.getOptionValue("s").split(":");
            String brokerPort = cl.getOptionValue("p", "8123");
            String keyBase = cl.getOptionValue("k");

            String serverSideHost = hap[0];
            int serverSidePort = Integer.parseInt(hap[1]);
            int port = Integer.parseInt(brokerPort);
            byte key = DigestUtils.sha1(keyBase)[0];
            ChannelBroker channelBroker = new ChannelBroker(serverSideHost, serverSidePort, port, key);
            channelBroker.startup();
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String brokerPort = cl.getOptionValue("p", "8123");
            String keyBase = cl.getOptionValue("k");

            String httpProxyHost = "127.0.0.1";
            int httpProxyPort = 18124;
            int port = Integer.parseInt(brokerPort);
            byte key = DigestUtils.sha1(keyBase)[0];
            // broker server
            ChannelBroker channelBroker = new ChannelBroker(httpProxyHost, httpProxyPort, port, key);
            channelBroker.startup();

            // http proxy server
            DefaultHttpProxyServer.bootstrap()
                    .withAddress(new InetSocketAddress(httpProxyHost, httpProxyPort))
                    .start();
        }
    }

    public static CommandLine parseCommandLine(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("s", true, "Server side host and port. (eg. www.serversied.com:8123)");
        opts.addOption("p", true, "Registration port of broker. Default: 8123");
        opts.addOption("k", true, "Password.");
        opts.addOption("m", true, "Broker mode. `c` for client side; `s` for server side.");
        DefaultParser parser = new DefaultParser();
        return parser.parse(opts, args);
    }
}
