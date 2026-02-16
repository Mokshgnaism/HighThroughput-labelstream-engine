package org.example;
import LabelClasses.*;
import HmacGenerator.*;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
public class Main3 {
    public static void main(String[] args) {
        long start = System.nanoTime();


//        ======================= FIRST SECTION -> GENERATING THE PALETS SINGLE THREADED PIEPE =========================
        List<Palet> palets =  LabelGenerator2.generatePaletIds(10000,"1234","1234","1234","1234");
//         ======================= FIRST SECTION ENDED =====================================

//        ===================== SECOND SECTION -> GENERATING THE CARTONS for ALL THE PALETS ==================
        Integer n = 10000;
        int workers = 8;
        int chunk = (n + workers - 1) / workers; // ceiling
        List<Carton> totalCartons= new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<List<Carton>>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                int startIdx = i * chunk;
                int endIdx = Math.min(startIdx + chunk, n);

                tasks.add(scope.fork(() -> {
                    List<Carton> cartons = new ArrayList<>();
                    for (int j = startIdx; j < endIdx; j++) {
                        Palet palet = palets.get(j);
                        cartons.addAll(LabelGenerator2.generateCartonForPalet(palet.PaletSSIC, 50));
                    }
                    return cartons;
                }));
            }
            scope.join();
            for (var task : tasks) {
                totalCartons.addAll(task.get());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
//        ========================================SECOND SECTION ENDED ====================================
//        =======================================DB CONNECTION AND INSERTING PALETS =======================
        final String url = "jdbc:postgresql://localhost:5432/testdb";
        final String user = "postgres";
        final String password = "Mokshgna@123";
        try(var conn = DriverManager.getConnection(url,user,password)){
            conn.setAutoCommit(false);
            final String sql_string = "INSERT INTO pallets(ssic,employee_id,factory_id,hash,hash_prefix) VALUES ((?),(?),(?),(?),(?)) on CONFLICT DO NOTHING;";
            try(PreparedStatement ps = conn.prepareStatement(sql_string)){
                for (var palet : palets) {
                    ps.setString(1,palet.PaletSSIC);
                    ps.setString(2,palet.employeeId);
                    ps.setString(3,palet.FactoryId);
                    ps.setString(4,palet.hash);
                    ps.setString(5,palet.hashPrefix);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            }
            catch (Exception e){
                conn.rollback();
            }
        }catch (Exception e){
            e.printStackTrace();

        }
//         ============================== generating 10k + 50k + inserting 10k took around 1800 ms ==========================================
//        ============================== LETS TRY TO insert the cartons ... with multiple threads running on it . ======================
        workers = 4;
        chunk = (totalCartons.size() + workers - 1) / workers;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()){
            for(int i = 0; i < workers; i++){
                final int finalI = i;
                final int chunkFinal = chunk;
//                task being done in one thread .
                scope.fork(()->{
                    try(var conn = DriverManager.getConnection(url,user,password)){
                        conn.setAutoCommit(false);
//                        final String sql_string = "INSERT INTO cartons(serial_id,parent_pallet_id,hash,hash_prefix) VALUES ((?),(?),(?),(?)) on CONFLICT DO NOTHING;";
//                        try(PreparedStatement ps = conn.prepareStatement(sql_string)){
//                            int startIdx = finalI * chunkFinal;
//                            int endIdx = Math.min(startIdx + chunkFinal, totalCartons.size());
//                            for(int j = startIdx; j < endIdx; j++){
//                                Carton c = totalCartons.get(j);
//                                ps.setString(1,c.serialId);
//                                ps.setString(2,c.parentPaletID);
//                                ps.setString(3,c.Hash);
//                                ps.setString(4,c.prefix);
//                                ps.addBatch();
//                            }
//                            ps.executeBatch();
//                            conn.commit();
//                        }
                        CopyManager copyManager = new CopyManager((BaseConnection)conn);
                        StringBuilder sb = new StringBuilder();
                        int startIdx = finalI * chunkFinal;
                            int endIdx = Math.min(startIdx + chunkFinal, totalCartons.size());
                            for(int j = startIdx; j < endIdx; j++){
                                Carton c = totalCartons.get(j);
                                sb.append(c.serialId)
                                        .append(",")
                                        .append(c.parentPaletID)
                                        .append(",")
                                        .append(c.Hash)
                                        .append(",")
                                        .append(c.prefix)
                                        .append("\n");
                            }
                            ByteArrayInputStream input = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                            copyManager.copyIn("COPY cartons(serial_id,parent_pallet_id,hash,hash_prefix) FROM STDIN WITH CSV ",input);
                            sb.setLength(0);
                            conn.commit();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return null;
                });
//                task being done in one thread ends here
            }
            scope.join();
        }catch (Exception e){
            e.printStackTrace();
        }
//        ======================================= 6 seconds until now ====================================================
        workers = 4;
        chunk = (totalCartons.size() + workers - 1) / workers;
        Unit poison = new Unit("POISON","POISON","POISON","POISON");




//        ====================consumer scope ends==========================
        BlockingQueue<Unit> queue = new ArrayBlockingQueue<>(200_000);
        try(var scope = new StructuredTaskScope.ShutdownOnFailure()){
            final int dbWorkers = 3;
            for(int i = 0; i < dbWorkers; i++){
                scope.fork(()->{
                    try(var conn = DriverManager.getConnection(url,user,password)){
                        conn.setAutoCommit(false);
                        CopyManager copyManager = new CopyManager((BaseConnection)conn);
                        int batch = 0;
                        StringBuilder buffer = new StringBuilder();
                        while(true){
                            Unit u =  queue.take();
                            if(u==poison){
                                break;
                            }
                            buffer.append(u.serialId).append(",")
                                    .append(u.parentCartonID).append(",")
                                    .append(u.Hash).append(",")
                                    .append(u.prefix).append("\n");
                            if(++batch==50000){
                                ByteArrayInputStream input = new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8));
                                copyManager.copyIn(
                                        "COPY units(serial_id,parent_carton_id,hash,hash_prefix) FROM STDIN WITH CSV",
                                        input);
                                conn.commit();
                                buffer.setLength(0);
                                batch = 0;
                            }
                        }
                        if(batch>0){
                            ByteArrayInputStream input = new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8));
                            copyManager.copyIn(
                                    "COPY units(serial_id,parent_carton_id,hash,hash_prefix) FROM STDIN WITH CSV",
                                    input);
//                            "COPY units(serial_id,parent_carton_id,hash,hash_prefix) FROM STDIN WITH CSV"
                            conn.commit();
                            buffer.setLength(0);
                            batch = 0;
                        }
                    }
                   return  null;
                });
            }
            try(var scope2 = new StructuredTaskScope.ShutdownOnFailure()){
                List<StructuredTaskScope.Subtask<Void>>Producers = new ArrayList<>();
                for(int i = 0; i < workers; i++){
                    final int finalI = i;
                    final int chunkFinal = chunk;
                    final int startIdx = finalI * chunkFinal;
                    final int endIdx = Math.min(startIdx + chunkFinal, totalCartons.size());
                    Producers.add(scope2.fork(()->{
                        for(int j = startIdx; j < endIdx; j++){
                            List<Unit> u;
                            Carton c = totalCartons.get(j);
                            u = LabelGenerator2.generateUnitsForCarton(c.serialId,10);
                            for(Unit u2 : u){
                                queue.put(u2);
                            }
                        }
                        return null;
                    }));
                }
                scope2.join();
                for(int i=0;i<dbWorkers;i++){
                    queue.put(poison);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            scope.join();
        }catch (Exception e){
            e.printStackTrace();
        }
        long end = System.nanoTime();
        double total = (end-start)/1000000.0;
        System.out.println("total time : "+total);
    }
    public static void generate(int noOfPalets,int cartonsPerPallet,int unitsPerCarton,String FactoryId,String employeeId,String companyPrefix,String ProductId,int batchSize){

        //        ======================= FIRST SECTION -> GENERATING THE PALETS SINGLE THREADED PIEPE =========================
        List<Palet> palets =  LabelGenerator2.generatePaletIds(noOfPalets,FactoryId,employeeId,companyPrefix,ProductId);
//         ======================= FIRST SECTION ENDED =====================================


//        ===================== SECOND SECTION -> GENERATING THE CARTONS for ALL THE PALETS ==================
        Integer n = noOfPalets;
        int workers = 8;
        int chunk = (n + workers - 1) / workers; // ceiling
        List<Carton> totalCartons= new ArrayList<>();

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<List<Carton>>> tasks = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                int startIdx = i * chunk;
                int endIdx = Math.min(startIdx + chunk, n);

                tasks.add(scope.fork(() -> {
                    List<Carton> cartons = new ArrayList<>();
                    for (int j = startIdx; j < endIdx; j++) {
                        Palet palet = palets.get(j);
                        cartons.addAll(LabelGenerator2.generateCartonForPalet(palet.PaletSSIC, cartonsPerPallet));
                    }
                    return cartons;
                }));
            }
            scope.join();
            for (var task : tasks) {
                totalCartons.addAll(task.get());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
//        ========================================SECOND SECTION ENDED ====================================


//        =======================================DB CONNECTION AND INSERTING PALETS =======================
        final String url = "jdbc:postgresql://localhost:5432/testdb";
        final String user = "postgres";
        final String password = "Mokshgna@123";
//        shift these to env on prod
        try(var conn = DriverManager.getConnection(url,user,password)){
            conn.setAutoCommit(false);
            final String sql_string = "INSERT INTO pallets(ssic,employee_id,factory_id,hash,hash_prefix) VALUES ((?),(?),(?),(?),(?)) on CONFLICT DO NOTHING;";
            try(PreparedStatement ps = conn.prepareStatement(sql_string)){
                for (var palet : palets) {
                    ps.setString(1,palet.PaletSSIC);
                    ps.setString(2,palet.employeeId);
                    ps.setString(3,palet.FactoryId);
                    ps.setString(4,palet.hash);
                    ps.setString(5,palet.hashPrefix);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            }
            catch (Exception e){
                conn.rollback();
            }
        }catch (Exception e){
            e.printStackTrace();

        }
//         ============================== generating 10k + 50k + inserting 10k took around 1800 ms ==========================================


//        ============================== LETS TRY TO insert the cartons ... with multiple threads running on it . ======================
        workers = 4;
        chunk = (totalCartons.size() + workers - 1) / workers;
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()){
            for(int i = 0; i < workers; i++){
                final int finalI = i;
                final int chunkFinal = chunk;
//                task being done in one thread .
                scope.fork(()->{
                    try(var conn = DriverManager.getConnection(url,user,password)){
                        conn.setAutoCommit(false);
                        CopyManager copyManager = new CopyManager((BaseConnection)conn);
                        StringBuilder sb = new StringBuilder();
                        int startIdx = finalI * chunkFinal;
                        int endIdx = Math.min(startIdx + chunkFinal, totalCartons.size());
                        for(int j = startIdx; j < endIdx; j++){
                            Carton c = totalCartons.get(j);
                            sb.append(c.serialId)
                                    .append(",")
                                    .append(c.parentPaletID)
                                    .append(",")
                                    .append(c.Hash)
                                    .append(",")
                                    .append(c.prefix)
                                    .append("\n");
                        }
                        ByteArrayInputStream input = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                        copyManager.copyIn("COPY cartons(serial_id,parent_pallet_id,hash,hash_prefix) FROM STDIN WITH CSV ",input);
                        sb.setLength(0);
                        conn.commit();
                    }catch(Exception e){
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }
            scope.join();
        }catch (Exception e){
            e.printStackTrace();
        }
//        ======================================= 6 seconds until now ====================================================

//        ===================================PRODUCER CONSUMER PIPE STARTS HERE =============================================
//        -> it happens like consumers start spinning before producers . and inside the scope of consumers only we try to spawn the producers andd wait until all producers finish and insert poison pills in the queue.
//        we did not do the prod-cons since start becuase the results were not good . it gave diminishing returns(even negative) on trying
//        this prod - cons on 5.51 M was good . normal batch generation and insertion without prod-consumer took around 1minute 40 seconds . introduction of this prod consumer dropped it to 45 seconds(at max)

        workers = 4;
        chunk = (totalCartons.size() + workers - 1) / workers;


        BlockingQueue<Unit> queue = new ArrayBlockingQueue<>(200_000);
        try(var scope = new StructuredTaskScope.ShutdownOnFailure()){
            try(var scope2 = new StructuredTaskScope.ShutdownOnFailure()){
                List<StructuredTaskScope.Subtask<Void>>Producers = new ArrayList<>();
                for(int i = 0; i < workers; i++){
                    final int finalI = i;
                    final int chunkFinal = chunk;
                    final int startIdx = finalI * chunkFinal;
                    final int endIdx = Math.min(startIdx + chunkFinal, totalCartons.size());
                    Producers.add(scope2.fork(()->{
                        PipedOutputStream output = new PipedOutputStream();
                        PipedInputStream input = new PipedInputStream(output);
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
                        scope2.fork(()->{
                           try(var conn = DriverManager.getConnection(url,user,password)){
                               conn.setAutoCommit(false);
                               CopyManager copyManager = new CopyManager((BaseConnection)conn);
                               copyManager.copyIn("COPY units(serial_id,parent_carton_id,hash,hash_prefix) FROM STDIN WITH CSV",input);
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                           return  null;
                        });
                        for(int j = startIdx; j < endIdx; j++){
                            List<Unit> u;
                            Carton c = totalCartons.get(j);
                            u = LabelGenerator2.generateUnitsForCarton(c.serialId,unitsPerCarton);
//                            String hash, String prefix, String parentCartonID, String serialId
                            for(Unit u2 : u){
                                writer.write(u2.serialId+",");
                                writer.write(u2.parentCartonID+",");
                                writer.write(u2.Hash+",");
                                writer.write(u2.prefix+"\n");
                            }
                        }
                        output.close();
                        return null;
                    }));
                }
                scope2.join();
            }catch (Exception e){
                e.printStackTrace();
            }
            scope.join();
        }catch (Exception e){
            e.printStackTrace();
        }
//        ===========================PRODUCER CONSUMER LIFE CYCLE ENDS HERE =================================================

    }
}
