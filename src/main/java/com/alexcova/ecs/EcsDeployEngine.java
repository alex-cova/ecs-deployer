package com.alexcova.ecs;

import org.jetbrains.annotations.NotNull;

public class EcsDeployEngine {

    private Step head;
    private final Context context;

    public EcsDeployEngine() {
        this.context = new Context();
    }

    public void execute() {
        Step current = head;
        while (current != null) {
            try {
                current.execute(context);
                current = current.getNext();
            } catch (AbortOperationException e) {
                System.err.println(e.getMessage());
                System.exit(1);
                break;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    EcsDeployEngine and(@NotNull Step step) {
        System.out.println("- " + step.getClass().getSimpleName().substring(0, step.getClass().getSimpleName().length() - 4));
        if (head == null) {
            head = step;
        } else {
            Step current = head;
            while (current.getNext() != null) {
                current = current.getNext();
            }
            current.setNext(step);
        }
        return this;
    }

}
