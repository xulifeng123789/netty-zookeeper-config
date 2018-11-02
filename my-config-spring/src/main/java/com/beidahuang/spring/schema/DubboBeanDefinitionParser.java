package com.beidahuang.spring.schema;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DubboBeanDefinitionParser implements BeanDefinitionParser {

    private Class beanClass;

    public DubboBeanDefinitionParser(Class beanClass) {

        this.beanClass = beanClass;
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        String id = element.getAttribute("id");
        if (id != null && id.length() > 0) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
        }else {
            if(element.getAttribute("interface") != null && !"".equals(element.getAttribute("interface"))) {
                id = element.getAttribute("interface");
            }else {

                id = beanClass.getName();
            }
        }
        parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
        beanDefinition.getPropertyValues().addPropertyValue("id", id);
        for(Method setter : beanClass.getMethods()) {

            String name = setter.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {

                String property = camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
                String value = element.getAttribute(property);
                if(value != null && !"".equals(value)) {

                    beanDefinition.getPropertyValues().addPropertyValue(property, value);
                }

                if ("ref".equals(property) && parserContext.getRegistry().containsBeanDefinition(value)) {
                    BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                    if (!refBean.isSingleton()) {
                        throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                    }
                    Object reference = new RuntimeBeanReference(value);
                    beanDefinition.getPropertyValues().addPropertyValue(property, reference);
                }

            }
        }
        return beanDefinition;
    }

    public  String camelToSplitName(String camelName, String split) {
        if (camelName == null || camelName.length() == 0) {
            return camelName;
        }
        StringBuilder buf = null;
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                if (buf == null) {
                    buf = new StringBuilder();
                    if (i > 0) {
                        buf.append(camelName.substring(0, i));
                    }
                }
                if (i > 0) {
                    buf.append(split);
                }
                buf.append(Character.toLowerCase(ch));
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        return buf == null ? camelName : buf.toString();
    }
}
