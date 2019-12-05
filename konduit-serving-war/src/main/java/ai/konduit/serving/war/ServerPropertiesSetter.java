package ai.konduit.serving.war;

import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.io.ClassPathResource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Sets the system properties for the web.xml
 * if any exist based on a classpath resource of:
 * server.properties
 * <p>
 * This allows use of dynamic properties in the web.xml
 * If we declare this as a servlet context listener
 * in the web.xml, this will allow initialization
 * of the model server properties
 * allowing dynamic configuration.
 *
 * @author Adam Gibson
 */
@Slf4j
public class ServerPropertiesSetter implements ServletContextListener {


    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ClassPathResource serverProperties = new ClassPathResource("server.properties");
        if (!serverProperties.exists()) {
            log.debug("Unable to find server.properties file. Skipping attempted initialization. ");
        } else {
            try (InputStream is = serverProperties.getInputStream()) {
                Properties properties = new Properties();
                properties.load(is);
                for (String key : properties.stringPropertyNames()) {
                    System.setProperty(key, properties.getProperty(key));
                }

                log.debug("Loaded server.properties for use with the web.xml initialization.");
            } catch (IOException e) {
                log.error("Unable to read server.properties file!");
            }
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
