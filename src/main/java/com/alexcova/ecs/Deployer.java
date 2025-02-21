package com.alexcova.ecs;

import com.alexcova.ecs.step.*;

public class Deployer {

    public static void main(String[] args) {
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
    }
}
