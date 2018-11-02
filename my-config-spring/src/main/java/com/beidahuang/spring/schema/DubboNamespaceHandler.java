package com.beidahuang.spring.schema;

import com.beidahuang.config.ProtocolConfig;
import com.beidahuang.config.RegistryConfig;
import com.beidahuang.config.spring.ReferenceBean;
import com.beidahuang.config.spring.ServiceBean;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {


        registerBeanDefinitionParser("reference",new DubboBeanDefinitionParser(ReferenceBean.class));
        registerBeanDefinitionParser("registry",new DubboBeanDefinitionParser(RegistryConfig.class));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class));
        registerBeanDefinitionParser(("protocol"),new DubboBeanDefinitionParser(ProtocolConfig.class));
    }
}
