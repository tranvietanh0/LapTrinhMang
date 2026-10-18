package racing.server.core;

import org.junit.jupiter.api.Test;
import racing.common.dto.EndReason;
import racing.common.dto.MatchRow;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchServiceTest {
    @Test
    void slowEventStorageDoesNotBlockCallerAndPreservesOrder() throws Exception {
        TestWorld world=new TestWorld();
        Session player=world.login("alice",new FakeConnection());
        CountDownLatch writing=new CountDownLatch(1), release=new CountDownLatch(1);
        List<String> saved=new ArrayList<>();
        MatchRepository repository=new MatchRepository() {
            public int createMatch(String code,int a,int b) { return 1; }
            public void saveResult(int id,int a,int b,Integer winner,EndReason reason) { }
            public List<MatchRow> findRecentByPlayer(int id,int limit) { return List.of(); }
            public void addEvent(int id,int pid,String type,String json) throws SQLException {
                writing.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS)) throw new SQLException("timed out"); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new SQLException(e); }
                saved.add(type);
            }
        };
        ExecutorService writer=Executors.newSingleThreadExecutor();
        try {
            MatchService service=new MatchService(repository,world.players,writer);
            service.event(1,player,"START",null);
            assertTrue(writing.await(2,TimeUnit.SECONDS));
            service.event(1,player,"COLLISION",null);
            service.event(1,player,"FINISH",null);
            assertTrue(saved.isEmpty(),"event calls returned while the database is still blocked");
            release.countDown(); writer.shutdown();
            assertTrue(writer.awaitTermination(5,TimeUnit.SECONDS));
            assertEquals(List.of("START","COLLISION","FINISH"),saved);
        } finally { release.countDown(); writer.shutdownNow(); }
    }
}
