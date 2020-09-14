package com.gavin.asmdemo;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceManager {

    private Map<Class<?>, Class<?>> classMap = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Class<?>, Object> instanceMap = new HashMap<>();

    private ServiceManager() { }

    private static volatile ServiceManager INSTANCE = null;

    public static ServiceManager getInstance() {
        ServiceManager cache = INSTANCE;
        if (cache != null) return cache;
        return createAndCacheInstance();
    }

    private synchronized static ServiceManager createAndCacheInstance() {
        //double check
        ServiceManager instance = INSTANCE;
        if (instance != null) return instance;

        ServiceManager newInstance = new ServiceManager();
        INSTANCE = newInstance;
        return newInstance;
    }

    public <T> boolean containService(Class<T> clazz) {
        return getServiceOrNull(clazz) != null;
    }

    public <T> T requireService(Class<T> clazz) {
        T instance = getServiceOrNull(clazz);
        if (instance == null) throw new IllegalStateException("instance can not be null");
        return instance;
    }

    public <T> T getServiceOrNull(Class<T> clazz) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            Object cacheInstance = instanceMap.get(clazz);
            if (cacheInstance != null) return (T) cacheInstance;
        } finally {
            readLock.unlock();
        }
        return createAndCacheService(clazz);
    }

    private <T> T createAndCacheService(Class<T> clazz) {
        Class<?> targetClass = classMap.get(clazz);
        if (targetClass == null) return null;

        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            //double check
            Object cacheInstance = instanceMap.get(clazz);
            if (cacheInstance != null) return (T) cacheInstance;

            //create new instance and cache it
            T newInstance = (T) createNewInstanceOrNull(targetClass);
            instanceMap.put(clazz, newInstance);
            return newInstance;
        } finally {
            writeLock.unlock();
        }
    }

    private Object createNewInstanceOrNull(Class<?> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
