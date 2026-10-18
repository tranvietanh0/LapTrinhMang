import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.*;

class TimerProbe {
    public static void main(String[] args)throws Exception {
        for(int delay:new int[]{16,15,8}) {
            CountDownLatch done=new CountDownLatch(1); List<Double> samples=new ArrayList<>(); long[] stamp={0};
            SwingUtilities.invokeAndWait(()->{
                Timer timer=new Timer(delay,null);
                timer.addActionListener(e->{
                    long now=System.nanoTime(); if(stamp[0]!=0)samples.add((now-stamp[0])/1e6);stamp[0]=now;
                    if(samples.size()==180){timer.stop();done.countDown();}
                });timer.start();
            });
            if(!done.await(15,TimeUnit.SECONDS))throw new IllegalStateException("Timer stalled");
            Collections.sort(samples);
            System.out.printf("Swing Timer %d ms: median=%.2f ms p95=%.2f ms%n",delay,samples.get(90),samples.get(171));
        }
        CountDownLatch done=new CountDownLatch(1);List<Double> samples=new ArrayList<>();long[] stamp={0};
        ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor();
        clock.scheduleAtFixedRate(()->SwingUtilities.invokeLater(()->{
            long now=System.nanoTime();if(stamp[0]!=0)samples.add((now-stamp[0])/1e6);stamp[0]=now;
            if(samples.size()==180){clock.shutdown();done.countDown();}
        }),0,16,TimeUnit.MILLISECONDS);
        if(!done.await(15,TimeUnit.SECONDS)){clock.shutdownNow();throw new IllegalStateException("Frame clock stalled");}
        Collections.sort(samples);
        System.out.printf("Fixed deadline 16 ms: median=%.2f ms p95=%.2f ms mean=%.2f ms%n",samples.get(90),samples.get(171),samples.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        System.exit(0);
    }
}
