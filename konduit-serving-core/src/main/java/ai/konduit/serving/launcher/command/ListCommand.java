/*
 * *****************************************************************************
 * Copyright (c) 2020 Konduit K.K.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ****************************************************************************
 */

package ai.konduit.serving.launcher.command;

import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.ExecUtils;
import io.vertx.core.spi.launcher.DefaultCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name(value = "list", priority = 1)
@Summary("Lists the running konduit servers.")
@Description("List all konduit servers launched with the `serve` command")
public class ListCommand extends DefaultCommand {

    private final static Pattern PS = Pattern.compile("-serving.id=(.*)\\s*");

    private final static Pattern FAT_JAR_EXTRACTION = Pattern.compile("-jar (\\S*)");

    private final static Pattern VERTICLE_EXTRACTION = Pattern.compile("run (\\S*)");

    // Note about stack traces - the stack trace are printed on the stream passed to the command.

    /**
     * Executes the {@code list} command.
     */
    @Override
    public void run() {
        out.println("Listing konduit servers...");
        List<String> cmd = new ArrayList<>();
        if (!ExecUtils.isWindows()) {
            try {
                cmd.add("sh");
                cmd.add("-c");
                cmd.add("ps ax | grep \"serving.id=\"");

                dumpFoundVertxApplications(cmd);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace(out);
            } catch (Exception e) {
                e.printStackTrace(out);
            }

        } else {
            try {
                // Use wmic.
                cmd.add("WMIC");
                cmd.add("PROCESS");
                cmd.add("WHERE");
                cmd.add("CommandLine like '%java.exe%'");
                cmd.add("GET");
                cmd.add("CommandLine");
                cmd.add("/VALUE");

                dumpFoundVertxApplications(cmd);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace(out);
            } catch (Exception e) {
                e.printStackTrace(out);
            }
        }
    }

    private void dumpFoundVertxApplications(List<String> cmd) throws IOException, InterruptedException {
        boolean none = true;
        final Process process = new ProcessBuilder(cmd).start();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            final Matcher matcher = PS.matcher(line);
            if (matcher.find()) {
                String id = matcher.group(1);
                String details = extractApplicationDetails(line);
                out.println(id + "\t" + details);
                none = false;
            }
        }
        process.waitFor();
        reader.close();
        if (none) {
            out.println("No konduit servers found.");
        }
    }

    /**
     * Tries to extract the fat jar name of the verticle name. It's a best-effort approach looking at the name of the
     * jar or to the verticle name from the command line. If not found, no details are returned (empty string).
     *
     * @return the details, empty if it cannot be extracted.
     */
    protected static String extractApplicationDetails(String line) {
        Matcher matcher = FAT_JAR_EXTRACTION.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            matcher = VERTICLE_EXTRACTION.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        // No details.
        return "";
    }
}
