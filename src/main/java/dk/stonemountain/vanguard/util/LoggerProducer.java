package dk.stonemountain.vanguard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.util.Log.LogType;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class LoggerProducer {
    @Produces
    @Log(LogType.REQUEST) 
    public Logger createRequestLog(InjectionPoint ip) {
        return LoggerFactory.getLogger("REQUEST");
    }

    @Produces
    @Log(LogType.BACKEND)
    public Logger createBackendLog(InjectionPoint ip) {
        return LoggerFactory.getLogger("BACKEND");
    }

    @Produces
    @Log(LogType.APPLICATION) 
    public Logger createApplicationLog(InjectionPoint ip) {
        return LoggerFactory.getLogger(ip.getBean().getBeanClass());
    }
    
    @Produces
    public Logger createLog(InjectionPoint ip) {
        return LoggerFactory.getLogger(ip.getBean().getBeanClass());
    }    
}
