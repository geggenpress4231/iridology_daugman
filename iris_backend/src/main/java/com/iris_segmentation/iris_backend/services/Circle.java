package com.iris_segmentation.iris_backend.services;

public class Circle {
    
    //center of circle
    int x; 
    int y;    
    int r;
    
    Circle(){
        this.x = 0;
        this.y = 0;
        this.r = 0;

    }
    
    Circle(int x, int y, int r){
        this.x = x;
        this.y = y;
        this.r = r;

    }
    
    
    @Override
    public String toString(){
        return /*"Iris coordinates: ("*/ + this.x + ", " + this.y + "), radius: " + this.r;
    }
}
