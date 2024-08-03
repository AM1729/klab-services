package org.integratedmodelling.common.authentication.scope;

import org.integratedmodelling.klab.api.collections.Parameters;
import org.integratedmodelling.klab.api.exceptions.KlabIllegalArgumentException;
import org.integratedmodelling.klab.api.exceptions.KlabResourceAccessException;
import org.integratedmodelling.klab.api.identities.Identity;
import org.integratedmodelling.klab.api.lang.kactors.KActorsBehavior;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.*;
import org.integratedmodelling.klab.api.services.runtime.Channel;
import org.integratedmodelling.klab.api.services.runtime.Message;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An abstract scope delegating all communication to an externally supplied Channel. Provides the basic API to
 * set and retrieve services according to the context of usage.
 */
public abstract class AbstractDelegatingScope implements Scope {

    Channel delegateChannel;
    Parameters<String> data = Parameters.create();
    Status status = Status.EMPTY;
    Scope parentScope;

    public AbstractDelegatingScope(Channel delegateChannel) {
        this.delegateChannel = delegateChannel;
    }

    public Channel getDelegateChannel() {
        return delegateChannel;
    }

    @Override
    public Parameters<String> getData() {
        return data;
    }

    @Override
    public KActorsBehavior.Ref getAgent() {
        return null;
    }

    @Override
    public Status getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public Identity getIdentity() {
        return delegateChannel.getIdentity();
    }

    @Override
    public void info(Object... info) {
        delegateChannel.info(info);
    }

    @Override
    public void warn(Object... o) {
        delegateChannel.warn(o);
    }

    @Override
    public void error(Object... o) {
        delegateChannel.error(o);
    }

    @Override
    public void debug(Object... o) {
        delegateChannel.debug(o);
    }

    @Override
    public Message send(Object... message) {
        return delegateChannel.send(message);
    }

    @Override
    public Message post(Consumer<Message> handler, Object... message) {
        return delegateChannel.post(handler, message);
    }

    @Override
    public void interrupt() {
        delegateChannel.interrupt();
    }

    @Override
    public boolean isInterrupted() {
        return delegateChannel.isInterrupted();
    }

    @Override
    public boolean hasErrors() {
        return delegateChannel.hasErrors();
    }

    @Override
    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

//    @Override
//    public boolean connect(KlabService service) {
//        return delegateChannel.connect(service);
//    }
//
//    @Override
//    public boolean disconnect(KlabService service) {
//        return delegateChannel.disconnect(service);
//    }

    @Override
    public void status(Status status) {
        delegateChannel.status(status);
    }

    @Override
    public void event(Message message) {
        delegateChannel.event(message);
    }

    @Override
    public void subscribe(Message.Queue... queues) {
        delegateChannel.subscribe(queues);
    }

    @Override
    public void unsubscribe(Message.Queue... queues) {
        delegateChannel.unsubscribe(queues);
    }

    @Override
    public <T extends KlabService> T getService(String serviceId, Class<T> serviceClass) {
        for (var service : getServices(serviceClass)) {
            if (serviceId.equals(service.serviceId())) {
                return service;
            }
        }
        throw new KlabResourceAccessException("cannot find service with ID=" + serviceId + " in the scope");
    }

    @Override
    public void ui(Message message) {
        delegateChannel.ui(message);
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void setParentScope(Scope parentScope) {
        this.parentScope = parentScope;
    }

//    @Override
//    public void close() {
//        delegateChannel.close();
//    }

    @Override
    public void close() throws IOException {

    }

    public void addListener(BiConsumer<Channel, Message> listener) {
        if (delegateChannel instanceof ChannelImpl channel) {
            channel.addListener(listener);
        } // TODO maybe warn otherwise
    }

    public BiConsumer<Channel, Message>[] listeners() {
        return delegateChannel instanceof ChannelImpl channel ?
               channel.listeners().toArray(BiConsumer[]::new) : null;
    }
}
