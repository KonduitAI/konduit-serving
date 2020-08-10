package ai.konduit.serving.vertx.api;

import ai.konduit.serving.vertx.config.ServerProtocol;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.apache.commons.lang3.StringUtils;

import static ai.konduit.serving.vertx.api.DeployKonduitServing.PROTOCOL_SERVICE_MAP;
import static ai.konduit.serving.vertx.api.DeployKonduitServing.SERVICE_PREFIX;

public class ServiceVerticleFactory implements VerticleFactory {
    private Vertx vertx;

    public ServiceVerticleFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public String prefix() {
        return SERVICE_PREFIX;
    }

    @Override
    public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
        return createInferenceVerticleFromProtocolName(verticleName.substring(verticleName.lastIndexOf(':') + 1));
    }

    private Verticle createInferenceVerticleFromProtocolName(String protocolName) throws Exception {
        ServerProtocol serverProtocol = ServerProtocol.valueOf(protocolName.toUpperCase());
        if(PROTOCOL_SERVICE_MAP.containsKey(serverProtocol)) {
            try {
                return (Verticle) Thread.currentThread().getContextClassLoader()
                        .loadClass(PROTOCOL_SERVICE_MAP.get(serverProtocol))
                        .getConstructor().newInstance();
            } catch (ClassNotFoundException classNotFoundException) {
                vertx.close();
                throw new IllegalStateException(
                        String.format("Missing classes for protocol service %s. Make sure the binaries contain the '%s' module.",
                                protocolName,
                                "konduit-serving-" + serverProtocol.name().toLowerCase())
                );
            }
        } else {
            vertx.close();
            throw new IllegalStateException(
                    String.format("No inference service found for type: %s. Available service types are: [%s]",
                            protocolName,
                            StringUtils.join(PROTOCOL_SERVICE_MAP.keySet(), ", ")
                    )
            );
        }
    }
}

