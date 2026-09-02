package com.rental.property.property.exception;

public class PropertyNotFoundException  extends RuntimeException{
    public  PropertyNotFoundException(String msg){
        super(msg);
    }
}
