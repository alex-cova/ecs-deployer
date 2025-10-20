package com.alexcova.ecs.step;

import com.alexcova.ecs.Context;
import com.alexcova.ecs.Step;
import com.alexcova.ecs.TimestampedResponse;
import org.jetbrains.annotations.NotNull;

public class StopHealthCheckerStep extends Step {
    @Override
    public void execute(@NotNull Context context) {
        var healthChecker = context.stopHealthChecker();

        System.out.println("----------------------------");
        System.out.println("    HEALTH CHECK RESULTS");
        System.out.println("----------------------------");

        for (TimestampedResponse responseCode : healthChecker.getResponseCodes()) {
            if (responseCode.getStatus() != 200) {
                System.err.println("Response code: " + responseCode);
            }
        }

        System.out.println("TOTAL HEALTH CHECKS: " + healthChecker.getResponseCodes().size());
        System.out.println("FAILED HEALTH CHECKS: " + healthChecker.getResponseCodes().stream().filter(rc -> rc.getStatus() != 200L).count());
        System.out.println("TOTAL TIME FAILED: " + healthChecker.getResponseCodes().stream().filter(rc -> rc.getStatus() != 200L).count() * 0.5 + " seconds");

        System.out.println("-------------------------------------------------------------------------------------------------------------------------------");

        System.exit(0);
    }
}
