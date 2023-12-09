package dk.stonemountain.vanguard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.util.Log.LogType;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class LoggerProducer {
    @Produces
    @Log(LogType.REQUEST_LOG) 
    public Logger createRequestLog(InjectionPoint ip) {
        return LoggerFactory.getLogger("REQUEST");
    }
    
    @Produces
    @Log(LogType.APPLICATION_LOG) 
    public Logger createApplicationLog(InjectionPoint ip) {
        return LoggerFactory.getLogger(ip.getBean().getBeanClass());
    }
    
    @Produces
    public Logger createLog(InjectionPoint ip) {
        return LoggerFactory.getLogger(ip.getBean().getBeanClass());
    }    
}
