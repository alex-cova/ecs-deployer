package com.alexcova.ecs;


import org.jetbrains.annotations.NotNull;


public interface CmdUtil {

    default boolean confirm(String message, @NotNull Context context) {
        System.out.println(message + " (y/n)");

        while (true) {
            String input = context.getScanner().nextLine();

            if (input.equalsIgnoreCase("abort")) {
                throw new AbortOperationException("User aborted deployment");
            }

            if (input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes")) {
                return true;
            } else if (input.equalsIgnoreCase("n") || input.equalsIgnoreCase("no")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter y or n");
            }
        }
    }

}
