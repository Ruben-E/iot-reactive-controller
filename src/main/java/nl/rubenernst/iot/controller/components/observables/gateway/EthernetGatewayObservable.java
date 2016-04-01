package nl.rubenernst.iot.controller.components.observables.gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.rubenernst.iot.controller.domain.messages.Message;
import nl.rubenernst.iot.controller.domain.messages.builder.MessageBuilder;
import org.javatuples.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class EthernetGatewayObservable implements GatewayObservable {
    private boolean stopped = false;
    private Socket socket = new Socket();

    @Getter
    private Observable<Pair<Message, OutputStream>> observable;

    public EthernetGatewayObservable(MessageBuilder messageBuilder, ExecutorService executorService, String gatewayIp, int gatewayPort) {
        Observable<Pair<Message, OutputStream>> observable = Observable.create(subscriber -> {
            try {
                OutputStream outputStream = null;
                BufferedReader input = null;

                socket = new Socket();
                socket.setKeepAlive(true);
                socket.setSoTimeout(1000);

                while (!stopped) {
                    if (!socket.isConnected()) {
                        log.info("Disconnected from ethernet gateway on [{}:{}]", gatewayIp, gatewayPort);

                        try {
                            log.info("Connecting to ethernet gateway on [{}:{}]", gatewayIp, gatewayPort);
                            socket.connect(new InetSocketAddress(gatewayIp, gatewayPort), 5000);
                            log.info("Connected to ethernet gateway on [{}:{}]", gatewayIp, gatewayPort);

                            outputStream = socket.getOutputStream();
                            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        } catch (Exception e) {
                            log.info("Unable to connect to the gateway on [{}:{}]. Retrying in 1000ms.", gatewayIp, gatewayPort);
                            Thread.sleep(1000);
                            continue;
                        }
                    }

                    if (socket.isConnected() && outputStream != null && input != null) {
                        try {
                            String payload = input.readLine();

                            List<Message> messages = messageBuilder.fromPayload(payload);
                            for (Message message : messages) {
                                subscriber.onNext(new Pair<>(message, outputStream));
                            }
                        } catch (SocketTimeoutException e) {
                            log.trace("Didn't get data within 1000ms.");
                        } catch (Exception e) {
                            log.debug("Got exception. Continuing");
                        }
                    }
                }
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
        this.observable = observable.share()
                .doOnError(throwable -> {
                    log.error("Got exception", throwable);
                })
                .subscribeOn(Schedulers.from(executorService));
    }

    @PreDestroy
    public void preDestroy() throws IOException {
        stopped = true;
        socket.close();
    }
}
