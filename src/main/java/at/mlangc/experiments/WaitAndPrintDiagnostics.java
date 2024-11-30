package at.mlangc.experiments;

import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;

import static java.lang.System.out;

public class WaitAndPrintDiagnostics {
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final String[] EMPTY_STRINGS = new String[0];

    public static void main(String[] ignoredArgs) throws Exception {
        var mbeanServer = ManagementFactory.getPlatformMBeanServer();
        var commandName = mbeanServer.queryNames(new ObjectName("com.sun.management:type=DiagnosticCommand"), null).iterator().next();

        var showHeapInfo = (ThrowingRunnable) () -> {
            out.println();
            out.println(mbeanServer.invoke(commandName, "gcHeapInfo", EMPTY_OBJECTS, EMPTY_STRINGS));
            out.println();
        };

        var printCommandLine = (ThrowingRunnable) () -> {
            out.println();
            out.println(mbeanServer.invoke(commandName, "vmCommandLine", EMPTY_OBJECTS, EMPTY_STRINGS));
            out.println();
        };

        showHeapInfo.run();

        var showUsage = (ThrowingRunnable) () -> out.printf(
                "[pid=%s] Press `e` and enter to exit, `h` and enter to print heap info or `c` and enter to show the command line%n",
                ProcessHandle.current().pid());

        showUsage.run();
        var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            var line = in.readLine();
            if (line.contains("h")) {
                showHeapInfo.run();
            } else if (line.contains("c")) {
                printCommandLine.run();
            }else if (line.contains("e")) {
                break;
            } else {
                out.println("???");
                showUsage.run();
            }
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
