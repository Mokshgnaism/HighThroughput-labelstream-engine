package org.example;
import HmacGenerator.LabelGenerator;
import HmacGenerator.LabelGenerator.*;
import java.sql.PreparedStatement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.Set;
import java.util.HashSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Main2 {
    public static void main(String[] args) {
        long start = System.nanoTime();
        LabelGenerator lg = new LabelGenerator();
        String [] palets = lg.generateLabel("102323",10000,"1234");
        String [][][] cartons = lg.generateSerialNumberForCartons(50,10000,palets);
        String [][][][] units = lg.generateSerialNumberForUnits(10,50,cartons,10000);
        long end = System.nanoTime();
        long duration = (end-start)/1000000;
        System.out.println("Time in milliseconds: "+duration);
    }
}