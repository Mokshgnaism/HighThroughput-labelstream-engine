package org.example;
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


public class Main {
    BlockingQueue<Integer> qu = new ArrayBlockingQueue<>(1000);
    public static  RandomStringGenerator generator = new RandomStringGenerator();
    public static  Set<String> set = new HashSet<>();
    public static  String [] arr = new String[10001];
    public static final String url = "jdbc:postgresql://localhost:5432/testdb";
    public static final String user = "postgres";
    public static final String password = "Mokshgna@123";

    public static  void insert(int start,int end){
        try(Connection conn = DriverManager.getConnection(url,user,password)){
//            System.out.println("Connected to database. thread name : " + Thread.currentThread().getName());
            conn.setAutoCommit(false);
            String sql_string = "INSERT into rvs(Value) VALUES (?) on CONFLICT DO NOTHING";
            try(PreparedStatement ps = conn.prepareStatement(sql_string)){
                for (int i = start; i <end; i++){
                    ps.setString(1,arr[i]);
                    ps.addBatch();
                }
                int [] result = ps.executeBatch();
                conn.commit();
            }catch (SQLException e){
                throw new SQLException(e);
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public static void generate(){
        for (int i = 1; i < 10001; i++) {
//            set.add(generator.randomString());
            arr[i] = generator.randomString();
        }

    }
    public static void main(String[] args) {
        long start = System.nanoTime();
        for (int i = 0; i < 1; i++) {
            generate();
        }
        try(var scope = new StructuredTaskScope.ShutdownOnFailure()){
            for(int i=1;i<=5;i++){
                final int finalI = i;
                scope.fork(
                        ()->{
                            insert((finalI-1)*1000+1,finalI*1000+1);
                            return null;
                        }
                );
            }
            scope.join();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        long end = System.nanoTime();
        long time = end - start;
        double millis = time / 1_000_000;
        System.out.println("Total time (ms): " + millis);
//        oscillates between 7-15 ms not more than once in 1000 runs . using a script so not worry about generation
    }
}