package com.beidahuang.config.spring;

import com.beidahuang.api.RpcRequest;
import com.beidahuang.config.RegistryConfig;
import com.beidahuang.discovery.IDiscovery;
import com.beidahuang.discovery.impl.DiscoveryImpl;
import com.beidahuang.handler.RpcClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferenceBean<T> implements FactoryBean, ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    //bean Id
    private String id;
    //泛型接口的名称
    private String interfaceName;
    // registry centers
    protected List<RegistryConfig> registries;

    public Object getObject() throws Exception {
        return createProxy();
    }

    public Class<?> getObjectType() {
        return null;
    }

    public boolean isSingleton() {
        return false;
    }
    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T createProxy() {
        try{

            Class<?> clazz = Class.forName(interfaceName);

            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    //封装RpcRequest对象
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setMethodName(method.getName());
                    rpcRequest.setClassName(method.getDeclaringClass().getName());
                    rpcRequest.setMethodTypeParam(method.getParameterTypes());
                    rpcRequest.setParams(args);
                    //<dubbo:registry address="zookeeper://localhost:2181"></dubbo:registry>
                    String registerUrl = "";
                    List<RegistryConfig> registries = getRegistries();
                    if(registries != null && registries.size() > 0) {
                        RegistryConfig registryConfig = registries.get(0);
                        String address = registryConfig.getAddress();
                        if(address != null && !"".equals(address)) {
                            registerUrl = address.substring(address.lastIndexOf("/") + 1);
                        }
                    }
                    IDiscovery discovery = new DiscoveryImpl(registerUrl);
                    //服务发现
                    String discoveryUrl = discovery.discovery(interfaceName);
                    String[] split = discoveryUrl.split(":");
                    String ip = split[0];
                    String port = split[1];
                    final RpcClientHandler rpcClientHandler = new RpcClientHandler();
                    //与服务端通信
                    EventLoopGroup group = new NioEventLoopGroup();
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(group);
                    bootstrap.channel(NioSocketChannel.class);
                    bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline channelPipeline = ch.pipeline();
                            channelPipeline.addLast("encoder", new ObjectEncoder());
                            channelPipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
                            channelPipeline.addLast(rpcClientHandler);
                        }
                    });

                    ChannelFuture f = bootstrap.connect(ip, Integer.parseInt(port)).sync();
                    f.channel().writeAndFlush(rpcRequest);//写数据
                    f.channel().closeFuture().sync();
                    return rpcClientHandler.getResponse();
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }

        return  null;
    }

    public void afterPropertiesSet() throws Exception {
        if(getRegistries() == null) {

            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (registryConfigMap != null && registryConfigMap.size() > 0) {
                List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                for (RegistryConfig config : registryConfigMap.values()) {
                        registryConfigs.add(config);
                }
                if (registryConfigs != null && !registryConfigs.isEmpty()) {
                    this.setRegistries(registryConfigs);
                }
            }
        }
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
