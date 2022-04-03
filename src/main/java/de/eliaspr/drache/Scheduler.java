package de.eliaspr.drache;

import java.util.ArrayList;

public class Scheduler implements Runnable {

    private final ArrayList<Task> newTasks = new ArrayList<>();
    private final Task[] activeTasks = new Task[100];

    public Scheduler() {
        Thread th = new Thread(this, "Drache-Scheduler");
        th.setDaemon(true);
        th.start();
    }

    @Override
    public void run() {
        long before, time;
        while (true) {
            before = System.currentTimeMillis();

            synchronized (newTasks) {
                int i = 0;
                while (!newTasks.isEmpty()) {
                    Task next = newTasks.get(newTasks.size() - 1);
                    for (; i < activeTasks.length; i++) {
                        if (activeTasks[i] == null) {
                            activeTasks[i++] = next;
                            break;
                        }
                    }
                    newTasks.remove(newTasks.size() - 1);
                }
            }

            long now = System.currentTimeMillis();
            for (int i = 0; i < activeTasks.length; i++) {
                Task next = activeTasks[i];
                if(next != null) {
                    if (now > next.nextTime) {
                        boolean shouldStop = next.runnable.run();
                        if (shouldStop || next.repeatTime <= 0) {
                            next.isStopped = true;
                        } else {
                            next.nextTime += next.repeatTime * 1000L;
                        }
                    }
                    if(next.isStopped) {
                        if(next.afterTask != null)
                            next.afterTask.run();
                        activeTasks[i] = null;
                        System.out.println("Task " + next + " stopped");
                    }
                }
            }

            time = System.currentTimeMillis() - before;
            if(time > 0L) {
                try {
                    Thread.sleep(1000L - time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public TaskBuilder newTask(TaskAction runnable) {
        return new TaskBuilder(runnable);
    }

    private void start(Task task) {
        synchronized (newTasks) {
            newTasks.add(task);
        }
    }

    private static class Task {

        final long startTime, repeatTime;
        final String name;
        long nextTime;
        TaskAction runnable;
        Runnable afterTask;
        boolean isStopped;

        private Task(String name, long startTime, long repeatTime, TaskAction runnable, Runnable afterTask) {
            this.name = name == null ? ("Task@" + hashCode()) : name;
            this.startTime = startTime == -1 ? System.currentTimeMillis() : startTime;
            this.repeatTime = repeatTime;
            this.runnable = runnable;
            this.nextTime = this.startTime;
            this.afterTask = afterTask;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public class TaskBuilder {
        long starTime = -1, repeatTime = -1;
        String name;
        TaskAction runnable;
        Runnable afterTask;

        private TaskBuilder(TaskAction runnable) {
            this.runnable = runnable;
        }

        public TaskBuilder setStartTime(long starTime) {
            this.starTime = starTime;
            return this;
        }

        public TaskBuilder setRepeatTime(long repeatTime) {
            this.repeatTime = repeatTime;
            return this;
        }

        public TaskBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public TaskBuilder after(Runnable afterTask) {
            this.afterTask = afterTask;
            return this;
        }

        public void start() {
            Task task = new Task(name, starTime, repeatTime, runnable, afterTask);
            Scheduler.this.start(task);
            System.out.println("Task " + task + " started");
        }

    }

    public interface TaskAction {
        boolean run();
    }

}
