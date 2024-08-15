/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.threads;

/**
 *
 * @author hcadavid
 */
public class CountThreadsMain {

    public static void main(String a[]){

        CountThread hilo1 = new CountThread();
        CountThread hilo2 = new CountThread();
        CountThread hilo3 = new CountThread();

        hilo1.run();
        hilo2.run();
        hilo3.run();


    }
    
}
