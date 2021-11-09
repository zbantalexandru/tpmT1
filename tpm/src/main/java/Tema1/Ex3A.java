package Tema1;

import java.util.ArrayList;
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

    private boolean allDone = false;

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

    public void take () {
        boolean eaten = false;
        while ( ! eaten ) {

            this.lock.lock();

            try {

                if ( this.currentServingCount > 0 ) {
                    -- this.currentServingCount;
                    eaten = true;
                    System.out.println("Serving taken");
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

    private static int numberSequence = 1;
    private final int number = Tribesman.numberSequence ++;

    public Tribesman ( Pot pot ) {
        this.pot = pot;
    }

    @Override
    public void run() {
        this.pot.take();
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
    private void run (int tribesmenCount, int servingsRefilled) {
        var pot = new Pot();
        var cook = new Cook(pot, servingsRefilled);
        var tribesmen = IntStream.range(0, tribesmenCount).mapToObj( e->new Tribesman(pot)).toList();

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
    }

    public static final int TRIBESMEN_COUNT = 300;
    public static final int COOK_REFILL_COUNT = 14;

    public static void main(String[] args) {
        new Main().run(TRIBESMEN_COUNT, COOK_REFILL_COUNT);
    }
}