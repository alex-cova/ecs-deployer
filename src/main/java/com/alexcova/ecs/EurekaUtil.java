package com.alexcova.ecs;


public class EurekaUtil {

    public static void main(String[] args) {
        Context context = new Context();
        context.doLogin();
        var result = context.getEurekaClient()
                .outOfService("product", "43fd5723857f41bb935ed1410bab4b5e", "production");

        if (result) {
            System.out.println("Instance out of service");
        } else {
            System.out.println("Instance not out of service");
        }
    }
}
