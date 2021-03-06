/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.app;

import org.apache.sling.launchpad.app.Main;
import org.apache.sling.launchpad.base.shared.SharedConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NakamuraMain {

  private static final String[] BUNDLE_SOURCE_LOCATIONS = new String[] { "SLING-INF/static",
                "SLING-INF/home" };
  private static final String[] FS_DEST_LOCATIONS = new String[] { "sling/static", null };
  // The name of the environment variable to consult to find out
  // about sling.home
  private static final String ENV_SLING_HOME = "SLING_HOME";
  private static String slingHome;
  private static Map<String, String> parsedArgs;

  public static void main(String[] args) throws Exception {
    if (checkLaunchDate(args)) {
      // new jar check for new content
      UnBundleStaticContent unBundleStaticContent = new UnBundleStaticContent(
          new BootStrapLogger() {

            @Override
            public void info(String message, Throwable t) {
              NakamuraMain.info(message, t);

            }

          });
      // allow the command line to add mappings using --mappings source:dest,source:dest
      String[] destLocations = FS_DEST_LOCATIONS;
      String[] sourceLocations = BUNDLE_SOURCE_LOCATIONS;
      destLocations[1] = slingHome;
      String staticContentMappings = parsedArgs.get("mappings");
      if ( staticContentMappings != null ) {
        String[]  parts = staticContentMappings.split(",");
        String[] tmpDestLocations = new String[destLocations.length+parts.length];
        String[] tmpSourceLocations = new String[sourceLocations.length+parts.length];
        System.arraycopy(destLocations, 0, tmpDestLocations, 0, destLocations.length);
        System.arraycopy(sourceLocations, 0, tmpSourceLocations, 0, sourceLocations.length);
        for ( int i = 0; i < parts.length; i++) {
          String[] m = parts[i].split(":");
          tmpSourceLocations[i+sourceLocations.length] = m[0];
          tmpDestLocations[i+destLocations.length] = m[1];
        }
        sourceLocations = tmpSourceLocations;
        destLocations = tmpDestLocations;
      }
      unBundleStaticContent.extract(unBundleStaticContent.getClass(),
          "resources/bundles/", sourceLocations, destLocations);
    }
    System.setSecurityManager(null);
    Main.main(args);
  }

  private static boolean checkLaunchDate(String[] args) throws IOException {
    // Find the last modified of this jar
    parsedArgs = parseCommandLine(args);
    // Find the last modified when the jar was loaded.
    slingHome = getSlingHome(parsedArgs);
    try {
      String resource = NakamuraMain.class.getName().replace('.', '/')
          + ".class";
      URL u = NakamuraMain.class.getClassLoader().getResource(resource);
      String jarFilePath = u.getFile();
      jarFilePath = jarFilePath.substring(0, jarFilePath.length()
          - resource.length() - 2);
      u = new URL(jarFilePath);
      File jarFile = new File(u.toURI());
      info("Loading from " + jarFile, null);
      long lastModified = jarFile.lastModified();

      File slingHomeFile = new File(slingHome);
      File loaderTimestamp = new File(slingHome, ".lauchpadLastModified");
      long launchpadLastModified = 0L;
      if (loaderTimestamp.exists()) {
        BufferedReader fr = null;
        try {
          fr = new BufferedReader(new FileReader(loaderTimestamp));
          launchpadLastModified = Long.parseLong(fr.readLine());
        } catch (NumberFormatException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (fr != null) {
            try {
              fr.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      } else {
        info("No loader timestamp ", null);
      }

      // if the jar is newer, then delete the bootstrap servialization
      // file that will
      // cause the contents of the jar to replace the contents on disk.

      if (launchpadLastModified < lastModified) {
        File bundleSer = new File(slingHomeFile,
            "felix/bundle0/bootstrapinstaller.ser");
        if (bundleSer.exists()) {
          info("Launcer Jar is newer than runtime image, removing bundle state, jar will reload ",
              null);
          bundleSer.delete();
        } else {
          info("No runtime, will use contents of launcher jar", null);
        }
        slingHomeFile.mkdirs();
        FileWriter fw = new FileWriter(loaderTimestamp);
        fw.write(String.valueOf(lastModified));
        fw.close();
        fw = null;
        return true;
      } else {
        info("Runtime image, newer than launcher, using runtime image ",
            null);
      }
    } catch (MalformedURLException e) {
      info("Not launching from a jar (malformed url)", null);
    } catch (URISyntaxException e) {
      info("Not launching from a jar (uri syntax)", null);
    }
    return false;

  }

  /**
   * Define the sling.home parameter implementing the algorithme defined on
   * the wiki page to find the setting according to this algorithm:
   * <ol>
   * <li>Command line option <code>-c</code></li>
   * <li>System property <code>sling.home</code></li>
   * <li>Environment variable <code>SLING_HOME</code></li>
   * <li>Default value <code>sling</code></li>
   * </ol>
   * 
   * @param args
   *            The command line arguments
   * @return The value to use for sling.home
   */
  private static String getSlingHome(Map<String, String> commandLine) {
    String source = null;

    String slingHome = commandLine.get("c");
    if (slingHome != null) {

      source = "command line";

    } else {

      slingHome = System.getProperty(SharedConstants.SLING_HOME);
      if (slingHome != null) {

        source = "system property sling.home";

      } else {

        slingHome = System.getenv(ENV_SLING_HOME);
        if (slingHome != null) {

          source = "environment variable SLING_HOME";

        } else {

          source = "default";
          slingHome = SharedConstants.SLING_HOME_DEFAULT;

        }
      }
    }

    info("Setting sling.home=" + slingHome + " (" + source + ")", null);
    return slingHome;
  }

  /**
   * Parses the command line arguments into a map of strings indexed by
   * strings. This method suppports single character option names only at the
   * moment. Each pair of an option name and its value is stored into the map.
   * If a single dash '-' character is encountered the rest of the command
   * line are interpreted as option names and are stored in the map unmodified
   * as entries with the same key and value.
   * <table>
   * <tr>
   * <th>Command Line</th>
   * <th>Mapping</th>
   * </tr>
   * <tr>
   * <td>x</td>
   * <td>x -> x</td>
   * </tr>
   * <tr>
   * <td>-y z</td>
   * <td>y -> z</td>
   * </tr>
   * <tr>
   * <td>-yz</td>
   * <td>y -> z</td>
   * </tr>
   * <tr>
   * <td>-y -z</td>
   * <td>y -> y, z -> z</td>
   * </tr>
   * <tr>
   * <td>-y x - -z a</td>
   * <td>y -> x, -z -> -z, a -> a</td>
   * </tr>
   * </table>
   * 
   * @param args
   *            The command line to parse
   * 
   * @return The map of command line options and their values
   */
  static Map<String, String> parseCommandLine(String[] args) {
    Map<String, String> commandLine = new HashMap<String, String>();
    boolean readUnparsed = false;
    for (int argc = 0; args != null && argc < args.length; argc++) {
      String arg = args[argc];

      if (readUnparsed) {
        commandLine.put(arg, arg);
      } else if (arg.startsWith("-")) {
        if (arg.length() == 1) {
          readUnparsed = true;
        } else {
          String key = String.valueOf(arg.charAt(1));
          if (arg.length() > 2) {
            commandLine.put(key, arg.substring(2));
          } else {
            argc++;
            if (argc < args.length
                && (args[argc].equals("-") || !args[argc]
                    .startsWith("-"))) {
              commandLine.put(key, args[argc]);
            } else {
              commandLine.put(key, key);
              argc--;
            }
          }
        }
      } else {
        commandLine.put(arg, arg);
      }
    }
    return commandLine;
  }

  // ---------- logging

  // emit an informational message to standard out
  static void info(String message, Throwable t) {
    log(System.out, "*INFO*", message, t);
  }

  // emit an error message to standard err
  static void error(String message, Throwable t) {
    log(System.err, "*ERROR*", message, t);
  }

  private static final DateFormat fmt = new SimpleDateFormat(
      "dd.MM.yyyy HH:mm:ss.SSS ");

  // helper method to format the message on the correct output channel
  // the throwable if not-null is also prefixed line by line with the prefix
  private static void log(PrintStream out, String prefix, String message,
      Throwable t) {

    final StringBuilder linePrefixBuilder = new StringBuilder();
    synchronized (fmt) {
      linePrefixBuilder.append(fmt.format(new Date()));
    }
    linePrefixBuilder.append(prefix);
    linePrefixBuilder.append(" [");
    linePrefixBuilder.append(Thread.currentThread().getName());
    linePrefixBuilder.append("] ");
    final String linePrefix = linePrefixBuilder.toString();

    out.print(linePrefix);
    out.println(message);
    if (t != null) {
      t.printStackTrace(new PrintStream(out) {
        @Override
        public void println(String x) {
          synchronized (this) {
            print(linePrefix);
            super.println(x);
            flush();
          }
        }
      });
    }
  }

}