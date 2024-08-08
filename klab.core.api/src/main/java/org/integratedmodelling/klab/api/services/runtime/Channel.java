/*
 * This file is part of k.LAB.
 *
 * k.LAB is free software: you can redistribute it and/or modify it under the terms of the Affero
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * A copy of the GNU Affero General Public License is distributed in the root directory of the k.LAB
 * distribution (LICENSE.txt). If this cannot be found see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2007-2018 integratedmodelling.org and any authors mentioned in author tags. All
 * rights reserved.
 */
package org.integratedmodelling.klab.api.services.runtime;

import java.io.Closeable;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.sound.midi.Receiver;

import org.integratedmodelling.klab.api.identities.Identity;
import org.integratedmodelling.klab.api.scope.Scope;
import org.integratedmodelling.klab.api.services.KlabService;
import org.integratedmodelling.klab.api.services.runtime.Message.MessageClass;
import org.integratedmodelling.klab.api.services.runtime.kactors.VM;

/**
 * A channel represents the current identity and is used to report status and send messages to any subscribing
 * identity. Channels are the base class for  {@link org.integratedmodelling.klab.api.scope.Scope} and their
 * behavior depends on the kind of scope they implement.
 * <p>
 * The {@link #error(Object...)}, {@link #warn(Object...)}, {@link #info(Object...)}, {@link #ui(Message)},
 * {@link #debug(Object...)}, {@link #status(Scope.Status)} and {@link #event(Message)} methods are the point
 * of entry into the channel. Each corresponds to the handler for one of the messaging queues classified by
 * {@link org.integratedmodelling.klab.api.services.runtime.Message.Queue}. They can be called explicitly from
 * the API or be called in response to a message sent through {@link #send(Object...)}, either on the channel
 * itself or on a channel that is paired to this through the messaging system. Channels instrumented for
 * messaging (implementing MessagingChannel) may send to one or more other channels in addition to their own
 * handlers. The paired channels will receive the messages through their respective handlers, packed as
 * needed, only on the queues that they have subscribed to. Others just send the messages to their own
 * handlers according to the {@link org.integratedmodelling.klab.api.services.runtime.Message.Queue}
 * associated with the {@link Message} sent unless the channel has unsubscribed. The default queues are
 * specified by the interface.
 * <p>
 * An important function of the monitor is to obtain the current identity that owns the computation. This is
 * done through {@link #getIdentity()}. From that, any other identity (such as the network session, the engine
 * etc., up to the node and partner that owns the engine) can be obtained.
 * <p>
 *
 * @author ferdinando.villa
 * @version $Id: $Id
 */
public interface Channel {

    default Set<Message.Queue> defaultQueues() {
        return EnumSet.of(Message.Queue.Info);
    }

    /**
     * All channels (and their children, the {@link org.integratedmodelling.klab.api.scope.Scope}s) are owned
     * by an Identity, which gives access to parent identities through its methods. In remote scopes, the
     * access token associated with request must allow maintenance of the identity hierarchy representing the
     * identity and its permissions in the requesting scope.
     *
     * @return
     */
    Identity getIdentity();

    /**
     * For info to be seen by users: pass a string. Will also take an exception, but usually exceptions
     * shouldn't turn into warnings. These will be reported to the user unless the verbosity is set low. Do
     * not abuse of these - there should be only few, really necessary info messages so that things do not get
     * lost.
     * <p>
     * In addition to the main object, you can pass a string that will be interpreted as the info message
     * class. The class parameter is used by the client to categorize messages so they can be shown in special
     * ways and easily identified in a list of info messages. Other objects can also be sent along with the
     * message, according to implementation.
     *
     * @param info
     */
    void info(Object... info);

    /**
     * Pass a string. Will also take an exception, but usually exceptions shouldn't turn into warnings. These
     * will be reported to the user unless the verbosity is set lowest.
     *
     * @param o a {@link java.lang.Object} object.
     */
    void warn(Object... o);

    /**
     * Pass a string or an exception (usually the latter as a reaction to an exception in the execution).
     * These will interrupt execution from outside, so you should return after raising one of these.
     * <p>
     * In addition, you can pass a statement to communicate errors in k.IM, or other objects that can be sent
     * and used as necessary.
     *
     * @param o a {@link java.lang.Object} object.
     */
    void error(Object... o);

    /**
     * Any message that is just for you or is too verbose to be an info message should be sent as debug, which
     * is not shown by default unless you enable a higher verbosity. Don't abuse of these - it's never cheap
     * or good to show hundreds of messages even when testing.
     *
     * @param o a {@link java.lang.Object} object.
     */
    void debug(Object... o);

    void status(Scope.Status status);

    void event(Message message);

    void ui(Message message);

    void subscribe(Message.Queue... queues);

    void unsubscribe(Message.Queue... queues);

    /**
     * This is to send out serializable objects or other messages through any messaging channel registered
     * with the runtime. Information sent through this channel will only be received by receivers that have
     * subscribed. The messages are signed with the monitor's {@link #getIdentity() identity string}.
     * </p>
     * If the channel is a channel to an agent, this should automatically dispatch any objects of
     * {@link VM.AgentMessage} class to the agent reference embedded in the scope.
     *
     * @param message anything that may be sent as a message: either a preconstructed {@link Message} or the
     *                necessary info to build one, including a {@link MessageClass} and {@IMessage.Type} along
     *                with any payload (any serializable object). Sending a {@link Notification} should
     *                automatically promote it to a suitable logging message and enforce any logging level
     *                filtering configured.
     * @return the completed message that was sent, for reference, or null if sending failed
     */
    Message send(Object... message);

//    /**
//     * Like {@link #send(Object...)} but takes a handler to process a response if/when it comes back.
//     * </p>
//     * FIXME we should return a CompletableFuture<Message, Message> instead of this ugly API. Maybe.
//     *
//     * @param handler the handler for the response message
//     * @param message anything that may be sent as a message: either a preconstructed {@link Message} or the
//     *                necessary info to build one, including a {@link MessageClass} and {@IMessage.Type} along
//     *                with any payload (any serializable object). Sending a {@link Notification} should
//     *                automatically promote it to a suitable logging message
//     * @return the completed message that was sent, for reference, or null if sending failed
//     */
//    Message post(Consumer<Message> handler, Object... message);

    void interrupt();

    /**
     * Check if the monitored identity has been interrupted by a client action. Applies to any task, such as
     * an observation task, application, test case or script. In other identities it will always return
     * false.
     *
     * @return true if interrupted
     */
    boolean isInterrupted();

    /**
     * Tells us that errors have happened in the context we're monitoring.
     *
     * @return true if errors have happened in this context of monitoring.
     */
    boolean hasErrors();

//    /**
//     * Notify this client scope to the passed service (normally a service client). If the service accepts to
//     * manage the scope and returns the Websockets URL to establish the link, create the link. From that point
//     * on, this scope receives for events posted at the server side, and the server-side scope receives events
//     * posted here until disconnect is called or the scope ends.
//     * <p>
//     * THis may be called multiple times with different services.
//     *
//     * @param service
//     * @return
//     */
//    boolean connect(KlabService service);
//
//    /**
//     * Disconnect a previously connected service. If the disconnection fails (e.g. the service had not
//     * connected in the first place) return false without error.
//     *
//     * @param service
//     * @return true if the disconnection was successful.
//     */
//    boolean disconnect(KlabService service);
}
