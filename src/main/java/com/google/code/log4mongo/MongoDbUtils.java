package com.google.code.log4mongo;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * This class has all utility methods to transform a LogginEvent object into a Bson object
 * to be stored into MongoDb or other Json/Bson storage
 *  
 * @author geisbruch
 *
 */
public class MongoDbUtils {

	  /**
     * BSONifies a single Log4J LoggingEvent object.
     * 
     * @param loggingEvent The LoggingEvent object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the LoggingEvent object <i>(may be null)</i>.
     */
    public static DBObject bsonifyLoggingEvent(final LoggingEvent loggingEvent)
    {
        DBObject result = null;

        if (loggingEvent != null)
        {
            result = new BasicDBObject();
            
            result.put("timestamp", new Date(loggingEvent.getTimeStamp()));
            nullSafePut(result, "level",   loggingEvent.getLevel().toString());
            nullSafePut(result, "thread",  loggingEvent.getThreadName());
            nullSafePut(result, "message", loggingEvent.getMessage());
            nullSafePut(result, "loggerName", bsonifyClassName(loggingEvent.getLoggerName()));
            
            addLocationInformation(result, loggingEvent.getLocationInformation());
            addThrowableInformation(result, loggingEvent.getThrowableInformation());
        }
        
        return(result);
    }
    
    /**
     * Adds the ThrowableInformation object to an existing BSON object.
     * 
     * @param bson          The BSON object to add the throwable info to <i>(must not be null)</i>.
     * @param throwableInfo The ThrowableInformation object to add to the BSON object <i>(may be null)</i>.
     */
    public static void addThrowableInformation(DBObject bson, final ThrowableInformation throwableInfo)
    {
        if (throwableInfo != null)
        {
            Throwable currentThrowable = throwableInfo.getThrowable();
            List      throwables       = new BasicDBList();
            
            while (currentThrowable != null)
            {
                DBObject throwableBson = bsonifyThrowable(currentThrowable);
                
                if (throwableBson != null)
                {
                    throwables.add(throwableBson);
                }
                
                currentThrowable = currentThrowable.getCause();
            }
            
            if (throwables.size() > 0)
            {
                bson.put("throwables", throwables);
            }
        }
    }
    

    /**
     * BSONifies the given Throwable.
     * 
     * @param throwable The throwable object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the Throwable object <i>(may be null)</i>.
     */
    public static DBObject bsonifyThrowable(final Throwable throwable)
    {
        DBObject result = null;
       
        if (throwable != null)
        {
            result = new BasicDBObject();
            
            nullSafePut(result, "message",    throwable.getMessage());
            nullSafePut(result, "stackTrace", bsonifyStackTrace(throwable.getStackTrace()));
        }
        
        return(result);
    }
    
    
    /**
     * BSONifies the given stack trace.
     * 
     * @param stackTrace The stack trace object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace object <i>(may be null)</i>.
     */
    public static DBObject bsonifyStackTrace(final StackTraceElement[] stackTrace)
    {
        BasicDBList result = null;
        
        if (stackTrace != null && stackTrace.length > 0)
        {
            result = new BasicDBList();
            
            for (StackTraceElement element : stackTrace)
            {
                DBObject bson = bsonifyStackTraceElement(element);
                
                if (bson != null)
                {
                    result.add(bson);
                }
            }
        }
        
        return(result);
    }
    
    
    
    /**
     * BSONifies the given stack trace element.
     * 
     * @param element The stack trace element object to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the stack trace element object <i>(may be null)</i>.
     */
    public static DBObject bsonifyStackTraceElement(final StackTraceElement element)
    {
        DBObject result = null;
        
        if (element != null)
        {
            result = new BasicDBObject();
            
            nullSafePut(result, "fileName",   element.getFileName());
            nullSafePut(result, "method",     element.getMethodName());
            nullSafePut(result, "lineNumber", element.getLineNumber());
            nullSafePut(result, "class",      bsonifyClassName(element.getClassName()));
        }
        
        return(result);
    }
    
    
    /**
     * BSONifies the given class name.
     * 
     * @param className The class name to BSONify <i>(may be null)</i>.
     * @return The BSONified equivalent of the class name <i>(may be null)</i>.
     */
    public static DBObject bsonifyClassName(final String className)
    {
        DBObject result = null;
        
        if (className != null && className.trim().length() > 0)
        {
            result = new BasicDBObject();
            
            result.put("fullyQualifiedClassName", className);
            
            List     packageComponents   = new BasicDBList();
            String[] packageAndClassName = className.split("\\.");
            
            packageComponents.addAll(Arrays.asList(Arrays.copyOf(packageAndClassName, packageAndClassName.length - 1)));
            
            if (packageComponents.size() > 0)
            {
                result.put("package",   packageComponents);
            }
            
            result.put("className", packageAndClassName[packageAndClassName.length - 1]);
        }
        
        return(result);
    }


    /**
     * Adds the given value to the given key, except if it's null (in which case this method does nothing).
     * 
     * @param bson  The BSON object to add the key/value to <i>(must not be null)</i>.
     * @param key   The key of the object <i>(must not be null)</i>.
     * @param value The value of the object <i>(may be null)</i>.
     */
    public static void nullSafePut(DBObject bson, final String key, final Object value)
    {
        if (value != null)
        {
            if (value instanceof String)
            {
                String stringValue = (String)value;
                
                if (stringValue.trim().length() > 0)
                {
                    bson.put(key, stringValue);
                }
            }
            else
            {
                bson.put(key, value);
            }
        }
    }
    
    /**
     * Adds the LocationInfo object to an existing BSON object. 
     * 
     * @param bson         The BSON object to add the location info to <i>(must not be null)</i>.
     * @param locationInfo The LocationInfo object to add to the BSON object <i>(may be null)</i>.
     */
    public static void addLocationInformation(DBObject bson, final LocationInfo locationInfo)
    {
        if (locationInfo != null)
        {
            nullSafePut(bson, "fileName",   locationInfo.getFileName());
            nullSafePut(bson, "method",     locationInfo.getMethodName());
            nullSafePut(bson, "lineNumber", locationInfo.getLineNumber());
            nullSafePut(bson, "class",      bsonifyClassName(locationInfo.getClassName()));
        }
    }
}
