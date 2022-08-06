package com.manerfan.booster.core.spring;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.manerfan.booster.core.util.idgenerator.snowflake.Ipv4IdGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 * YamlPropertyResourceFactory
 *
 * <pre>
 *      @ {@link org.springframework.context.annotation.PropertySource}
 *      @ {@link org.springframework.context.annotation.PropertySources}
 *      支持 yaml 文件
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/7
 */
@Slf4j
public class YamlPropertyResourceFactory implements PropertySourceFactory {
    private static final Set<String> YAML_SUFFIX = Sets.newHashSet(".yml", ".yaml");

    /**
     * Create a {@link PropertySource} that wraps the given resource.
     *
     * @param name            the name of the property source
     * @param encodedResource the resource (potentially encoded) to wrap
     * @return the new {@link PropertySource} (never {@code null})
     * @throws IOException if resource resolution failed
     */
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource)
        throws IOException {
        String resourceName = name;
        if (Objects.isNull(resourceName)) {
            resourceName = encodedResource.getResource().getFilename();
        }
        if (Objects.isNull(resourceName)) {
            resourceName = Ipv4IdGenerator.singleInstance().nextId().toHex();
        }
        val finalResourceName = resourceName;

        Properties properties = new Properties();
        //if (!encodedResource.getResource().exists()) {
        //    return new PropertiesPropertySource(finalResourceName, properties);
        //}
        val resourcePath = ((ClassPathResource)encodedResource.getResource()).getPath();

        try {
            if (YAML_SUFFIX.stream().anyMatch(finalResourceName::endsWith)) {
                YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                factory.setResources(new PathMatchingResourcePatternResolver().getResources(resourcePath));
                factory.afterPropertiesSet();
                properties = Optional.ofNullable(factory.getObject()).orElseGet(Properties::new);
            } else {
                PropertiesFactoryBean factory = new PropertiesFactoryBean();
                factory.setLocations(new PathMatchingResourcePatternResolver().getResources(resourcePath));
                factory.afterPropertiesSet();
                properties = Optional.ofNullable(factory.getObject()).orElseGet(Properties::new);
            }
        } catch (Exception ex) {
            log.warn("load properties from {} failed!", resourcePath, ex);
        }

        return new PropertiesPropertySource(finalResourceName, properties);
    }
}
