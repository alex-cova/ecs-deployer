package com.alexcova.ecs;

import com.alexcova.ecs.step.*;

import java.util.Scanner;

public class Deployer {

    public static void main(String[] args) {
        System.out.println("""
                                      _            _                      \s
                   ___  ___ ___    __| | ___ _ __ | | ___  _   _  ___ _ __\s
                  / _ \\/ __/ __|  / _` |/ _ \\ '_ \\| |/ _ \\| | | |/ _ \\ '__|
                 |  __/ (__\\__ \\ | (_| |  __/ |_) | | (_) | |_| |  __/ |  \s
                  \\___|\\___|___/  \\__,_|\\___| .__/|_|\\___/ \\__, |\\___|_|  \s
                    by Alex                 |_|            |___/          \s
                """);

        System.out.println("----------- MENU -----------");
        System.out.println("0. Exit");
        System.out.println("1. Deploy");
        System.out.println("2. Start stable");
        System.out.println("3. Stop stable");
        System.out.println("4. Tag Image");

        var scanner = new Scanner(System.in);

        System.out.println("Enter option: ");
        var option = scanner.nextLine();

        if (!option.matches("[0-9]+")) {
            System.out.println("Invalid option");
            return;
        }

        var choice = Integer.parseInt(option);

        if (choice <= 0 || choice > 4) {
            System.out.println("Goodbye!");
            return;
        }

        if (choice == 1) {
            new EcsDeployEngine()
                    .and(new ConfigureStep())
                    .and(new CheckECSStep())
                    .and(new CheckECRStep())
                    .and(new ConfigureImageStep())
                    .and(new AskForConfirmationStep())
                    .and(new StartHealthCheckerStep())
                    .and(new RunStableInstanceStep())
                    .and(new DeployStep())
                    .and(new StopBackupStep())
                    .and(new StopHealthCheckerStep())
                    .execute();
        } else if (choice == 2) {
            new EcsDeployEngine()
                    .and(new ConfigureStep())
                    .and(new CheckECSStep())
                    .and(new CheckECRStep())
                    .and(new RunStableInstanceStep())
                    .and(new RollbackStableStep())
                    .execute();
        } else if (choice == 3) {
            new EcsDeployEngine()
                    .and(new ConfigureStep())
                    .and(new StopBackupStep())
                    .execute();
        } else {
            ImageTagger.main(args);
        }
    }
}
