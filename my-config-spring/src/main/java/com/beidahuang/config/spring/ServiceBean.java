package com.beidahuang.config.spring;

import com.beidahuang.config.ProtocolConfig;
import com.beidahuang.config.RegistryConfig;
import com.beidahuang.handler.rpcserver.RpcServer;
import com.beidahuang.register.impl.RegisterImpl;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceBean<T> implements InitializingBean, ApplicationContextAware,ApplicationListener<ContextRefreshedEvent> {

    private String id;
    private ApplicationContext applicationContext;
    private  List<RegistryConfig> registries;
    private List<ProtocolConfig> protocols;
    private T ref;
    private String interfaceName;

    public void afterPropertiesSet() throws Exception {

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
        Map<String, ProtocolConfig> protocolConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, ProtocolConfig.class, false, false);
        if (protocolConfigMap != null && protocolConfigMap.size() > 0) {
            List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
            for (ProtocolConfig config : protocolConfigMap.values()) {
                protocolConfigs.add(config);
            }
            if (protocolConfigs != null && !protocolConfigs.isEmpty()) {
                this.setProtocols(protocolConfigs);
            }
        }

    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;
    }

    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        export();
    }

    public  void export() {

        //获取zookeeper地址
        String registerUrl = "";
        List<RegistryConfig> registries = getRegistries();
        if(registries != null && registries.size() > 0) {
            RegistryConfig registryConfig = registries.get(0);
            String address = registryConfig.getAddress();
            if(address != null && !"".equals(address)) {
                registerUrl = address.substring(address.lastIndexOf("/") + 1);
            }
        }
        //获取当前机器的host
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //发布接口

        RegisterImpl register = null;
        try {
            register = new RegisterImpl(registerUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //得到协议的端口号，也是netty的监听端口
        String listenPort = "";
        List<ProtocolConfig> protocolConfigs = this.getProtocols();
        if(protocolConfigs != null && protocolConfigs.size() > 0) {
            ProtocolConfig protocolConfig = protocolConfigs.get(0);
            String port = protocolConfig.getPort();
            listenPort =  host + ":" + port;
        }

        RpcServer rpcServer = new RpcServer(register,listenPort);

        rpcServer.bind(ref);
        try {
            rpcServer.registetrAndListener();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        this.registries = registries;
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
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

    public List<ProtocolConfig> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<ProtocolConfig> protocols) {
        this.protocols = protocols;
    }
}
