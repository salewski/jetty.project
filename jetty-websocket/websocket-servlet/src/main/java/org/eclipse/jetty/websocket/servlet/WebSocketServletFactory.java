//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.servlet;

import java.util.Objects;
import java.util.function.BiFunction;

import javax.servlet.ServletContext;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.eclipse.jetty.websocket.core.FrameHandler;
import org.eclipse.jetty.websocket.core.WebSocketBehavior;
import org.eclipse.jetty.websocket.core.WebSocketException;
import org.eclipse.jetty.websocket.core.WebSocketPolicy;
import org.eclipse.jetty.websocket.core.extensions.WebSocketExtensionRegistry;
import org.eclipse.jetty.websocket.server.WebSocketCreator;

/**
 * Basic WebSocketServletFactory for working with Jetty-based WebSocketServlets
 */
public class WebSocketServletFactory
{
    /** The context that the factory was created from */
    private final ClassLoader contextClassloader;
    private WebSocketPolicy policy;
    private WebSocketCreator creator;
    private WebSocketExtensionRegistry extensionRegistry;
    private DecoratedObjectFactory objectFactory;
    private ByteBufferPool bufferPool;

    /** The API specific FrameHandler creation function */
    private BiFunction<WebSocketServletFactory, Object, FrameHandler> newFrameHandlerFunction;

    public WebSocketServletFactory(ServletContext context)
    {
        this(getContextAttribute(context, WebSocketPolicy.class),
                getContextAttribute(context, WebSocketExtensionRegistry.class),
                getContextAttribute(context, DecoratedObjectFactory.class),
                getContextAttribute(context, ByteBufferPool.class));
    }

    public WebSocketServletFactory(WebSocketPolicy policy)
    {
        this(policy, null, null, null);
    }

    public WebSocketServletFactory(WebSocketPolicy policy, WebSocketExtensionRegistry extensionRegistry, DecoratedObjectFactory objectFactory, ByteBufferPool bufferPool)
    {
        this.policy = policy;
        this.extensionRegistry = extensionRegistry;
        this.objectFactory = objectFactory;
        this.bufferPool = bufferPool;
        this.contextClassloader = Thread.currentThread().getContextClassLoader();

        if (this.policy == null)
            this.policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
        if (this.objectFactory == null)
            this.objectFactory = new DecoratedObjectFactory();
        if (this.extensionRegistry == null)
            this.extensionRegistry = new WebSocketExtensionRegistry();
        if (this.bufferPool == null)
            this.bufferPool = new MappedByteBufferPool();
    }

    private static <T> T getContextAttribute(ServletContext context, Class<T> clazz)
    {
        return (T) context.getAttribute(clazz.getName());
    }

    public void setNewFrameHandlerFunction(BiFunction<WebSocketServletFactory, Object, FrameHandler> newFrameHandlerFunction)
    {
        this.newFrameHandlerFunction = newFrameHandlerFunction;
    }

    public ClassLoader getContextClassloader()
    {
        return contextClassloader;
    }

    public WebSocketCreator getCreator()
    {
        return creator;
    }

    public WebSocketExtensionRegistry getExtensionRegistry()
    {
        return this.extensionRegistry;
    }

    public ByteBufferPool getBufferPool()
    {
        return bufferPool;
    }

    public DecoratedObjectFactory getObjectFactory()
    {
        return objectFactory;
    }

    /**
     * Get the base policy in use for WebSockets.
     *
     * @return the base policy
     */
    public WebSocketPolicy getPolicy()
    {
        return this.policy;
    }

    public FrameHandler newFrameHandler(Object websocketPojo)
    {
        Objects.requireNonNull(websocketPojo, "WebSocket Class cannot be null");

        return newFrameHandlerFunction.apply(this, websocketPojo);
    }

    /**
     * Register a websocket class pojo with the default {@link WebSocketCreator}.
     * <p>
     * Note: only required if using the default {@link WebSocketCreator} provided by this factory.
     *
     * @param websocketPojo the class to instantiate for each incoming websocket upgrade request.
     */
    public void register(Class<?> websocketPojo)
    {
        Objects.requireNonNull(websocketPojo, "WebSocket Class cannot be null");

        this.creator = (req, resp) -> {
            try
            {
                return objectFactory.createInstance(websocketPojo);
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new WebSocketException("Unable to create instance of " + websocketPojo, e);
            }
        };
    }

    public void setCreator(WebSocketCreator creator)
    {
        Objects.requireNonNull(creator, "WebSocketCreator cannot be null");

        this.creator = creator;
    }
}
