package nl.rubenernst.iot.controller.components.observables.gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.rubenernst.iot.controller.domain.messages.Message;
import nl.rubenernst.iot.controller.domain.messages.MessageType;
import nl.rubenernst.iot.controller.domain.messages.SetReqMessageSubType;
import org.javatuples.Pair;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

@Slf4j
public class DebugGatewayObservable implements GatewayObservable {
    @Getter
    private Observable<Pair<Message, OutputStream>> observable;

    public DebugGatewayObservable(ExecutorService executorService) {
        Observable<Pair<Message, OutputStream>> observable = Observable.create(subscriber -> {
            try {
                while (true) {
                    subscriber.onNext(new Pair<Message, OutputStream>(new Message(1, 1, MessageType.SET, SetReqMessageSubType.V_TEMP, 0, "10.0"), new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {

                        }
                    }));

                    Thread.sleep(10);
                }

            } catch (Exception e) {
                executorService.submit(() -> subscriber.onError(e));
            }
        });
        this.observable = observable
                .share()
                .doOnError(throwable -> {
                    log.error("Got exception", throwable);
                })
                .subscribeOn(Schedulers.from(executorService));
    }
}
