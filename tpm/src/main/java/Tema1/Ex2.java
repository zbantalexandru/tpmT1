package Tema1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Pot {
    private int currentServingCount = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition empty = this.lock.newCondition();

    private boolean[] servingDistribution = null;
    private int portionsServed = 0;
    private int[] totalServedEach = null;

    private boolean allDone = false;

    public void prepareDistribution ( int count ) {
        this.servingDistribution = new boolean[count];
        Arrays.fill(this.servingDistribution, true);

        if ( this.totalServedEach == null ) {
            this.totalServedEach = new int[count];
            Arrays.fill(this.totalServedEach, 0);
        }

        this.portionsServed = this.servingDistribution.length;
    }

    public Pot () {

    }

    public void finish () {
        this.lock.lock();

        try {

            this.allDone = true;
            this.empty.signal();

        } finally {
            this.lock.unlock();
        }
    }

    public int[] getTotalServedEach() {
        return this.totalServedEach;
    }

    public void take (int who) {
        boolean eaten = false;
        while ( ! eaten ) {

            this.lock.lock();

            try {

                if ( this.currentServingCount > 0 ) {
                    if ( this.servingDistribution[who] ) {
                        --this.currentServingCount;

                        --this.portionsServed;
                        this.servingDistribution[who] = false;
                        ++this.totalServedEach[who];

                        eaten = true;
                        System.out.println("Serving taken by " + who);

                    } else if ( this.portionsServed == 0 ) {
                        this.prepareDistribution(this.servingDistribution.length);
                    }
                } else
                    this.empty.signal();

            } finally {
                this.lock.unlock();
            }

        }
    }

    public void fillBefore ( int servingCount ) {
        this.currentServingCount = servingCount;
    }

    public void fill ( int servingCount ) {
        while ( ! this.allDone ) {
            this.lock.lock();

            try {

                try {
                    this.empty.await();
                } catch (InterruptedException ignored) { }

                if (this.currentServingCount == 0) {
                    this.currentServingCount = servingCount;

                    System.out.println("Cook refilled servings");
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    public int getCurrentServingCount() {
        return this.currentServingCount;
    }
}

class Tribesman extends Thread {
    protected Pot pot = null;

    private static int numberSequence = 0;
    private final int number = Tribesman.numberSequence ++;
    private int requiredPortions;

    public Tribesman ( Pot pot ) {
        this.pot = pot;
    }

    public Tribesman ( Pot pot, int requiredPortions ) {
        this.pot = pot;
        this.requiredPortions = requiredPortions;
    }

    @Override
    public void run() {

        while ( this.requiredPortions > 0 ) {
            this.pot.take(this.number);
            this.requiredPortions --;
        }

    }

    @Override
    public String toString() {
        return "Tribesman{" +
                "number=" + number +
                '}';
    }
}

class Cook extends Tribesman {
    private final int servingCount;

    public Cook ( Pot pot, int servingCount ) {
        super (pot);

        this.servingCount = servingCount;
    }

    public void run() {
        this.pot.fill(this.servingCount);

        System.out.println("All have eaten");
    }

    public void fillBefore () {
        this.pot.fillBefore ( this.servingCount );
    }

    @Override
    public String toString() {
        return "Cook";
    }
}

public class Main {
    private void run (int tribesmenCount, int servingsRefilled, int portionsRequiredForEach) {
        var start = System.nanoTime();

        var pot = new Pot();

        var tribesmen = IntStream.range(0, tribesmenCount).mapToObj( e->new Tribesman(pot, portionsRequiredForEach)).toList();
        pot.prepareDistribution ( tribesmen.size() );

        var cook = new Cook(pot, servingsRefilled);

        tribesmen.forEach(Thread::start);
        cook.start();

        for (Tribesman tribesman : tribesmen) {
            try {
                tribesman.join();
            } catch ( InterruptedException ignored ) { }
        }

        pot.finish();

        try {
            cook.join();
        } catch (InterruptedException ignored) { }

        System.out.println("Total served for each : " + Arrays.toString(pot.getTotalServedEach()));

        var end = System.nanoTime();
        var inSeconds = ( end - start ) / Math.pow(10, 9);

        System.out.println("Elapsed : " + inSeconds + " seconds");
    }

    public static final int TRIBESMEN_COUNT = 300;
    public static final int COOK_REFILL_COUNT = 26;
    public static final int PORTIONS_REQUIRED_FOR_EACH = 300;

    public static void main(String[] args) {
        new Main().run(TRIBESMEN_COUNT, COOK_REFILL_COUNT, PORTIONS_REQUIRED_FOR_EACH);
    }
}

/*
daca se  impart portiile egal, 300 oameni, 26 portii la reumplere, 300 portii per om, 3.71222461 secunde
daca nu se impart si ia fiecare cum apuca : 2.569225229 secunde
 */