package org.integratedmodelling.klab.services.application.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.integratedmodelling.common.data.jackson.JacksonConfiguration;
import org.integratedmodelling.common.logging.Logging;
import org.integratedmodelling.common.utils.Utils;
import org.integratedmodelling.klab.api.Klab;
import org.integratedmodelling.klab.api.ServicesAPI;
import org.integratedmodelling.klab.api.exceptions.KlabInternalErrorException;
import org.integratedmodelling.klab.api.identities.Identity;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.KlabService;
import org.integratedmodelling.klab.api.services.runtime.Message;
import org.integratedmodelling.klab.api.services.runtime.MessageBus;
import org.integratedmodelling.klab.services.application.ServiceNetworkedInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Controller
public class WebsocketsServerMessageBus /*implements MessageBus*/ {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ServiceNetworkedInstance<?> service;

//    private final String url;

//    private Map<String, Set<Scope>> receivers = Collections.synchronizedMap(new HashMap<>());
//    private static final String UNKNOWN_IDENTITY = "UNKNOWN_IDENTITY";
//
//    class ReceiverDescription {
//
//        private Map<Class<?>, Set<MethodDescriptor>> handlers = new HashMap<>();
//
//        public ReceiverDescription(Class<?> cls) {
//            for (Method method : cls.getDeclaredMethods()) {
//                for (Annotation annotation : method.getDeclaredAnnotations()) {
//                    //                    if (annotation instanceof MessageHandler) {
//                    //                        MethodDescriptor mdesc = new MethodDescriptor(method,
//                    //                        (MessageHandler) annotation);
//                    //                        if (!this.handlers.containsKey(mdesc.payloadType)) {
//                    //                            this.handlers.put(mdesc.payloadType, new HashSet<>());
//                    //                        }
//                    //                        this.handlers.get(mdesc.payloadType).add(mdesc);
//                    //                    }
//                }
//            }
//            receiverTypes.put(cls, this);
//        }
//
//        class MethodDescriptor {
//
//            Method method;
//            Class<?> payloadType;
//            Message.MessageClass mclass = null;
//            Message.MessageType mtype = null;
//
//            //            public MethodDescriptor(Method method, MessageHandler handler) {
//            //
//            //                this.method = method;
//            //                this.method.setAccessible(true);
//            //                for (Class<?> cls : method.getParameterTypes()) {
//            //                    if (!IMessage.Type.class.isAssignableFrom(cls) && !IMessage.MessageClass
//            //                    .class.isAssignableFrom(cls)
//            //                            && !IMessage.class.isAssignableFrom(cls)) {
//            //                        this.payloadType = cls;
//            //                        break;
//            //                    }
//            //                }
//            //                if (this.payloadType == null) {
//            //                    throw new IllegalStateException(
//            //                            "wrong usage of @MessageHandler: the annotated method must have a
//            //                            parameter for the payload"
//            //                                    + IConfigurationService.REST_RESOURCES_PACKAGE_ID + " as
//            //                                    parameter");
//            //                }
//            //                if (handler.type() != IMessage.Type.Void) {
//            //                    this.mtype = handler.type();
//            //                }
//            //                if (handler.messageClass() != IMessage.MessageClass.Void) {
//            //                    this.mclass = handler.messageClass();
//            //                }
//            //            }
//
//            void handle(Object identity, Object payload, Message message) {
//
//                List<Object> params = new ArrayList<>();
//                for (Class<?> cls : method.getParameterTypes()) {
//                    if (cls.isAssignableFrom(this.payloadType)) {
//                        params.add(payload);
//                    } else if (cls.isAssignableFrom(Message.class)) {
//                        params.add(message);
//                    } else if (cls.isAssignableFrom(Date.class)) {
//                        params.add(new Date(message.getTimestamp()));
//                    } else if (cls.isAssignableFrom(Message.MessageClass.class)) {
//                        params.add(message.getMessageClass());
//                    } else if (cls.isAssignableFrom(Message.MessageType.class)) {
//                        params.add(message.getMessageType());
//                    } else if (cls.isAssignableFrom(String.class)) {
//                        params.add(payload.toString());
//                    } else {
//                        params.add(null);
//                    }
//                }
//
//                try {
//                    //                    if (identity instanceof Session) {
//                    //                        ((Session)identity).touch();
//                    //                    }
//                    this.method.invoke(identity, params.toArray());
//                } catch (Throwable e) {
//                    if (e instanceof InvocationTargetException) {
//                        e = ((InvocationTargetException) e).getTargetException();
//                    }
//                    //                    if (identity instanceof IRuntimeIdentity) {
//                    //                        ((IRuntimeIdentity) identity).getMonitor().error(e);
//                    //                    } else {
//                    //                        Logging.INSTANCE.error("error while dispatching message to
//                    //                        handler: " + e.getMessage());
//                    //                    }
//                }
//            }
//
//            public boolean appliesTo(Message message) {
//                return (mclass == null || mclass == message.getMessageClass()) && (mtype == null || mtype == message.getMessageType());
//            }
//        }
//
//    }

//    private Map<Class<?>, ReceiverDescription> receiverTypes = Collections.synchronizedMap(new HashMap<>());
//    private Map<String, Consumer<Message>> responders = Collections.synchronizedMap(new HashMap<>());
//    private Map<String, Message> responses = Collections.synchronizedMap(new HashMap<>());
//    private Set<String> requests = Collections.synchronizedSet(new HashSet<>());
//
//    @Autowired
//    ObjectMapper objectMapper;

//    @Autowired
//    private SimpMessagingTemplate webSocket;

    public WebsocketsServerMessageBus(/*KlabService.Type serviceType*/) {
//        objectMapper = JacksonConfiguration.newObjectMapper();
//        JacksonConfiguration.configureObjectMapperForKlabTypes(objectMapper);
//        this.url =
//                "ws://localhost:" + serviceType.defaultPort + "/" + serviceType.defaultServicePath + ServicesAPI.MESSAGE;
    }

    @PostConstruct
    public void publishMessageBus() {
        Logging.INSTANCE.info("Setting up message bus");
        //        Klab.INSTANCE.setMessageBus(this);
    }

    @SubscribeMapping("/subscribe")
    public String sendOneTimeMessage() {
        return "server one-time message via the application";
    }
    /**
     * This gets messages sent to /message from the remote side.
     *
     * @param message
     */
    @MessageMapping(ServicesAPI.MESSAGE)
    public void handleTask(Message message) {

        // TODO for now: print out all messages except network status, which clutters
        // the output. This is really important for development but obviously should be removed.
        //        if (Configuration.INSTANCE.isEchoEnabled() && message.getType() != IMessage.Type
        //        .NetworkStatus) {
        System.out.println(Utils.Json.printAsJson(message));
        //        }

        //        if (message.getInResponseTo() != null) {
        //
        //            if (requests.contains(message.getInResponseTo())) {
        //
        //                requests.remove(message.getInResponseTo());
        //                responses.put(message.getInResponseTo(), message);
        //                return;
        //
        //            } else {
        //
        //                Consumer<IMessage> responder = message.getRepeatability() == Repeatability.Once
        //                                               ? responders.remove(message.getInResponseTo())
        //                                               : responders.get(message.getInResponseTo());
        //
        //                if (responder != null) {
        //                    responder.accept(message);
        //                    return;
        //                }
        //            }
    }

    /*
     * If the identity is known at our end, check if it has a handler for our
     * specific payload type. If so, turn the payload into that and dispatch it.
     */
    //        Identity auth = Authentication.INSTANCE.getIdentity(message.getIdentity(), IIdentity.class);
    //        if (auth != null) {
    //            dispatchMessage(message, auth);
    //        } else {
    //            post(Message.create(new Notification(UNKNOWN_IDENTITY, Level.SEVERE.getName()), message
    //            .getIdentity()));
    //        }
    //        /*
    //         * Any other subscribed object
    //         */
    //        for (Object identity : getReceivers(message.getIdentity())) {
    //            dispatchMessage(message, identity);
    //        }

    private void dispatchMessage(Message message, Object identity) {

        System.out.println("DISPATCHING " + message);

        //        try {
        /*
         * 1. Determine payload type
         */
        //            Class<?> cls = message.getPayloadClass().equals("String") ? String.class
        //                                                                      : Class.forName
        //                                                                      (IConfigurationService
        //                                                                      .REST_RESOURCES_PACKAGE_ID + "
        //                                                                      ." + message
        //                                                                      .getPayloadClass());
        //
        //            /*
        //             * 2. Determine if the object has a method to react to it, caching the result
        //             * and the parameter sequence.
        //             */
        //            ReceiverDescription rdesc = receiverTypes.get(identity.getClass());
        //            if (rdesc == null) {
        //                rdesc = new ReceiverDescription(identity.getClass());
        //            }
        //
        //            /*
        //             * 3. If there is a method, invoke it.
        //             */
        //            if (rdesc.handlers.containsKey(cls)) {
        //                for (MethodDescriptor mdesc : rdesc.handlers.get(cls)) {
        //                    if (mdesc.appliesTo(message)) {
        //                        Object payload = cls == String.class ? message.getPayload().toString()
        //                                                             : objectMapper.convertValue(message
        //                                                             .getPayload(), cls);
        //                        mdesc.handle(identity, payload, message);
        //                        if (identity instanceof IMessageBus.Relay) {
        //                            for (String relayId : ((IMessageBus.Relay) identity)
        //                            .getRelayIdentities()) {
        //                                post(message.copyWithIdentity(relayId));
        //                            }
        //                        }
        //                    }
        //                }
        //            }

        //        } catch (Throwable e) {
        //            Logging.INSTANCE.error("internal error: converting payload of message " + message);
        //        }

    }

//    @Override
//    public synchronized void post(Message message) {
//
//        //	    System.out.println("POSTING " + message);
//
//        try {
//            webSocket.convertAndSend(ServicesAPI.MESSAGE + "/" + message.getIdentity(), message);
//        } catch (Throwable e) {
//            Logging.INSTANCE.error("internal error: posting message " + message);
//        }
//    }
//
//    @Override
//    public synchronized Future<Message> ask(Message message) {
//
//        //    requests.add(message.getId());
//        webSocket.convertAndSend(ServicesAPI.MESSAGE + "/" + message.getIdentity(), message);
//        return new Future<Message>() {
//
//            long origin = System.currentTimeMillis();
//            Message m;
//            boolean cancelled;
//
//            @Override
//            public boolean cancel(boolean mayInterruptIfRunning) {
//                requests.remove(message.getId());
//                cancelled = true;
//                return true;
//            }
//
//            @Override
//            public boolean isCancelled() {
//                return cancelled;
//            }
//
//            @Override
//            public boolean isDone() {
//                if (responses.containsKey(message.getId())) {
//                    m = responses.get(message.getId());
//                    requests.remove(message.getId());
//                    responses.remove(message.getId());
//                }
//                return m != null;
//            }
//
//            @Override
//            public Message get() throws InterruptedException, ExecutionException {
//                while (true) {
//                    if (m != null) {
//                        break;
//                    } else if (responses.containsKey(message.getId())) {
//                        m = responses.get(message.getId());
//                        requests.remove(message.getId());
//                        responses.remove(message.getId());
//                        break;
//                    }
//                    Thread.sleep(250);
//                }
//                return m;
//            }
//
//            @Override
//            public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException
//                    , TimeoutException {
//                while (true) {
//                    if (System.currentTimeMillis() - origin >= unit.toMillis(timeout)) {
//                        return null;
//                    } else if (m != null) {
//                        break;
//                    } else if (responses.containsKey(message.getId())) {
//                        m = responses.get(message.getId());
//                        requests.remove(message.getId());
//                        responses.remove(message.getId());
//                        break;
//                    }
//                    Thread.sleep(250);
//                }
//                return m;
//            }
//
//        };
//    }
//
//    @Override
//    public synchronized void post(Message message, Consumer<Message> responder) {
//        //        responders.put(((Message) message).getId(), responder);
//        post(message);
//    }
//
//    @Override
//    public Collection<Scope> getReceivers(String identity) {
//        var ret = receivers.get(identity);
//        if (ret == null) {
//            ret = new HashSet<>();
//            receivers.put(identity, ret);
//        }
//        return ret;
//    }
//
//    @Override
//    public void subscribe(Scope scope) {
//        //            Set<Object> ret = receivers.get(identity);
//        //            if (ret == null) {
//        //                ret = new HashSet<>();
//        //                receivers.put(identity, ret);
//        //            }
//        //            ret.add(receiver);
//    }
//
//    @Override
//    public void unsubscribe(Scope identity) {
//        //        receivers.remove(identity);
//    }

    //@Override
    //public void unsubscribe(String identity, Object receiver) {
    //    Set<Object> ret = receivers.get(identity);
    //    if (ret == null) {
    //        ret = new HashSet<>();
    //        receivers.put(identity, ret);
    //    }
    //    ret.remove(receiver);
    //}
}
