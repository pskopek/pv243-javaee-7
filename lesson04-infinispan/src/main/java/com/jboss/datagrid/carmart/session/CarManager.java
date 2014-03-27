/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.jboss.datagrid.carmart.session;

import org.infinispan.commons.api.BasicCache;
import com.jboss.datagrid.carmart.model.Car;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

/**
 * Adds, retrieves, removes new cars from the cache. Also returns a list of cars stored in the cache.
 * 
 * @author Martin Gencur
 * 
 */
@Model
public class CarManager {

    public static final String CACHE_NAME = "carcache";

    public static final String CAR_NUMBERS_KEY = "carnumbers";

    @Inject
    private CacheContainerProvider provider;

    private BasicCache<String, Object> carCache;

    private String carId;
    private Car car = new Car();

    public CarManager() {
    }

    public String addNewCar() {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        List<String> carNumbers = getNumberPlateList(carCache);
        carNumbers.add(car.getNumberPlate());
        carCache.put(CAR_NUMBERS_KEY, carNumbers);
        carCache.put(CarManager.encode(car.getNumberPlate()), car);
        return "home";
    }

    public String addNewCarWithRollback() {
        boolean throwInducedException = true;
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        List<String> carNumbers = getNumberPlateList(carCache);
        carNumbers.add(car.getNumberPlate());
        // store the new list of car numbers and then throw an exception -> roll-back
        // the car number list should not be stored in the cache
        carCache.put(CAR_NUMBERS_KEY, carNumbers);
        if (throwInducedException)
            throw new RuntimeException("Induced exception");
        carCache.put(CarManager.encode(car.getNumberPlate()), car);
        return "home";
    }

    /**
     * Operate on a clone of car number list
     */
    @SuppressWarnings("unchecked")
    private List<String> getNumberPlateList(BasicCache<String, Object> carCacheLoc) {
        List<String> result = null;
        List<String> carNumberList = (List<String>) carCacheLoc.get(CAR_NUMBERS_KEY);
        if (carNumberList == null) {
            result = new LinkedList<String>();
        } else {
            result = new LinkedList<String>(carNumberList);
        }
        return result;
    }

    public String showCarDetails(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        this.car = (Car) carCache.get(encode(numberPlate));
        return "showdetails";
    }

    public List<String> getCarList() {
        List<String> result = null;
        // retrieve a cache
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        // retrieve a list of number plates from the cache
        result = getNumberPlateList(carCache);
        return result;
    }

    public String removeCar(String numberPlate) {
        carCache = provider.getCacheContainer().getCache(CACHE_NAME);
        carCache.remove(encode(numberPlate));
        List<String> carNumbers = getNumberPlateList(carCache);
        carNumbers.remove(numberPlate);
        carCache.put(CAR_NUMBERS_KEY, carNumbers);
        return null;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public Car getCar() {
        return car;
    }

    public static String encode(String key) {
        try {
            return URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decode(String key) {
        try {
            return URLDecoder.decode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
