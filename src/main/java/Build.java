/*
 * Copyright (c) 2022 Brian Powell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons
 * to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

class Build extends Thread {
    private String host = "http://192.168.43.1:8080/";
    public void run() {
        boolean hostNeedsSwitch = false;
        do {
            Output.compiling.setVisible(true);
            Output.compile.setVisible(false);
            Output.console.clear();
            Output.console.print("Build started at " + new Timestamp(System.currentTimeMillis()) + "\n\n", ConsoleViewContentType.NORMAL_OUTPUT);
            long start = System.nanoTime();
            Collection<File> files = FileUtils.listFiles(
                    new File(Output.projectLoc + "/TeamCode/src/main/java/"),
                    new RegexFileFilter("^(.*\\.[jJ][aA][vV][aA]?)"),
                    DirectoryFileFilter.DIRECTORY
            );
            try {
                URL url = new URL(host + "java/file/delete");

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);
                con.setDoOutput(true);
                con.setRequestMethod("POST");

                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                out.writeBytes("delete=[\"src\"]");
                out.flush();
                out.close();
                con.disconnect();

                for (File file : files) {
                    VirtualFile file2 = VfsUtil.findFileByIoFile(file, true);

                    url = new URL(host + "java/file/save?f=/src" + file.getAbsolutePath().substring((Output.projectLoc + "/TeamCode/src/main/java").length()));

                    con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(5000);
                    con.setReadTimeout(5000);
                    con.setDoOutput(true);

                    HashMap<String, String> parameters = new HashMap<>();
                    parameters.put("data", new String(Objects.requireNonNull(file2).getInputStream().readAllBytes(), StandardCharsets.UTF_8));

                    out = new DataOutputStream(con.getOutputStream());
                    out.writeBytes(ParameterBuilder.getParamsString(parameters));
                    out.flush();
                    out.close();


                    int status = con.getResponseCode();
                    if (status > 299) {
                        Output.console.clear();
                        Output.console.print("An unexpected error occurred", ConsoleViewContentType.ERROR_OUTPUT);
                        con.disconnect();
                        return;
                    }
                    con.disconnect();
                }

                url = new URL(host + "java/build/start");

                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                int status = con.getResponseCode();

                if (status > 299) {
                    Output.console.clear();
                    Output.console.print("An unexpected error occurred", ConsoleViewContentType.ERROR_OUTPUT);
                    return;
                }

                con.disconnect();

                url = new URL(host + "java/build/status");
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                status = con.getResponseCode();
                if (status > 299) {
                    Output.console.clear();
                    Output.console.print("An unexpected error occurred", ConsoleViewContentType.ERROR_OUTPUT);
                    return;
                } else {
                    String output = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    while (output.startsWith("false", 17)) {
                        url = new URL(host + "java/build/status");
                        con = (HttpURLConnection) url.openConnection();
                        output = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    }
                }

                url = new URL(host + "java/build/log");
                con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                status = con.getResponseCode();

                if (status > 299) {
                    Output.console.clear();
                    Output.console.print("An unexpected error occurred", ConsoleViewContentType.ERROR_OUTPUT);
                } else {
                    String output = new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (output.equals("")) {
                        Output.console.print("Build SUCCESSFUL\n\nBuild finished in " + Math.round((System.nanoTime() - start) / 100000000d) / 10d, ConsoleViewContentType.NORMAL_OUTPUT);
                    } else {
                        output = ("\n" + output).replaceAll("\n(.*)\\((.*:.*)\\):", "\n" + Output.projectLoc + "/TeamCode/src/main/java/$1:$2");
                        Output.console.print(output.substring(1) + "\nBuild FAILED\n\nBuild finished in " + Math.round((System.nanoTime() - start) / 100000000d) / 10d, ConsoleViewContentType.ERROR_OUTPUT);
                    }
                }
                hostNeedsSwitch = false;
            } catch (FileNotFoundException e) {
                Output.console.clear();
                Output.console.print("Build failed", ConsoleViewContentType.ERROR_OUTPUT);
            } catch (IOException e) {
                hostNeedsSwitch = !hostNeedsSwitch;
                if(hostNeedsSwitch) {
                    if (host.equals("http://192.168.49.1:8080/")) {
                        host = "http://192.168.43.1:8080/";
                    } else {
                        host = "http://192.168.49.1:8080/";
                    }
                }
                Output.console.clear();
                Output.console.print("Not connected to robot network", ConsoleViewContentType.ERROR_OUTPUT);
            }
        } while(hostNeedsSwitch);
        Output.compile.setVisible(true);
        Output.compiling.setVisible(false);
    }

    static class ParameterBuilder {
        public static String getParamsString(Map<String, String> params) {
            StringBuilder result = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                result.append("&");
            }

            String resultString = result.toString();
            return resultString.length() > 0
                    ? resultString.substring(0, resultString.length() - 1)
                    : resultString;
        }
    }
}