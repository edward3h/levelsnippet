package org.ethelred.levelsnippet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AppTest {
  Path workingDirectory;
  String stdOut;
  String stdErr;
  ExecutorService exec = Executors.newCachedThreadPool();

  @BeforeMethod
  public void setup() throws IOException {
    workingDirectory = Files.createTempDirectory("lvlsnp");
  }

  @AfterMethod
  public void teardown() throws IOException {
    _deleteDir(workingDirectory);
    stdOut = null;
    stdErr = null;
  }

  private void _deleteDir(Path workingDirectory2) throws IOException {
    if (workingDirectory2 == null) {
      return;
    }

    Files
      .walk(workingDirectory2)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(File::delete);
  }

  @Test
  public void testAppWithNoArgs()
    throws IOException, InterruptedException, ExecutionException {
    int exitCode = runAppWithArgs();
    Assert.assertEquals(exitCode, 1);
    MatcherAssert.assertThat(
      stdErr,
      CoreMatchers.containsString("Argument \"LEVEL_FILE\" is required")
    );
  }

  @Test
  public void testAppWithLevelArg()
    throws IOException, InterruptedException, ExecutionException {
    String filename = "test_level.dat";
    Files.copy(
      getClass().getResourceAsStream(filename),
      workingDirectory.resolve(filename)
    );
    int exitCode = runAppWithArgs(filename);
    Assert.assertEquals(exitCode, 0);
    MatcherAssert.assertThat(
      stdOut,
      CoreMatchers.containsString("<div><span>acheivements world</span></div>")
    );
  }

  @Test
  public void testAppWithLevelArgAndPapyrusMap()
    throws IOException, InterruptedException, ExecutionException {
    String filename = "test_level.dat";
    String worldPath = "acheivementsworld";
    Files.copy(
      getClass().getResourceAsStream(filename),
      workingDirectory.resolve(filename)
    );
    Files.createDirectories(workingDirectory.resolve(worldPath).resolve("map"));
    int exitCode = runAppWithArgs(filename);
    Assert.assertEquals(exitCode, 0);
    MatcherAssert.assertThat(
      stdOut,
      CoreMatchers.containsString(
        "<div><a href=\"acheivementsworld/map/\">acheivements world</a></div>"
      )
    );
  }

  @Test
  public void testAppWithLevelArgAndBrvMap()
    throws IOException, InterruptedException, ExecutionException {
    String filename = "test_level.dat";
    String worldPath = "acheivementsworld";
    Files.copy(
      getClass().getResourceAsStream(filename),
      workingDirectory.resolve(filename)
    );
    Files.createDirectories(
      workingDirectory.resolve(worldPath).resolve("index")
    );
    int exitCode = runAppWithArgs(filename);
    Assert.assertEquals(exitCode, 0);
    MatcherAssert.assertThat(
      stdOut,
      CoreMatchers.containsString(
        "<div><a href=\"acheivementsworld/index/\">acheivements world</a></div>"
      )
    );
  }

  @Test
  public void testAppWithLevelArgAndBothMaps()
    throws IOException, InterruptedException, ExecutionException {
    String filename = "test_level.dat";
    String worldPath = "acheivementsworld";
    Files.copy(
      getClass().getResourceAsStream(filename),
      workingDirectory.resolve(filename)
    );
    Files.createDirectories(workingDirectory.resolve(worldPath).resolve("map"));
    Files.createDirectories(
      workingDirectory.resolve(worldPath).resolve("index")
    );
    int exitCode = runAppWithArgs(filename);
    Assert.assertEquals(exitCode, 0);
    MatcherAssert.assertThat(
      stdOut,
      CoreMatchers.containsString(
        "<div><a href=\"acheivementsworld/map/\">acheivements world</a></div>"
      )
    );
  }

  private int runAppWithArgs(String... args)
    throws IOException, InterruptedException, ExecutionException {
    ProcessBuilder pb = createProcess(App.class.getName(), args);
    pb.directory(workingDirectory.toFile());
    ByteArrayOutputStream stdOutBuf = new ByteArrayOutputStream();
    ByteArrayOutputStream stdErrBuf = new ByteArrayOutputStream();
    Process p = pb.start();

    Future<Long> outTransferFuture = exec.submit(
      () -> p.getInputStream().transferTo(stdOutBuf)
    );
    Future<Long> errorTransferFuture = exec.submit(
      () -> p.getErrorStream().transferTo(stdErrBuf)
    );
    int exitCode = p.waitFor();
    long outputLength = outTransferFuture.get();
    long errorLength = errorTransferFuture.get();
    stdOutBuf.flush();
    stdErrBuf.flush();
    stdOut = new String(stdOutBuf.toByteArray());
    stdErr = new String(stdErrBuf.toByteArray());

    return exitCode;
  }

  private ProcessBuilder createProcess(
    final String mainClass,
    final String... arguments
  ) {
    String jvm =
      System.getProperty("java.home") +
      File.separator +
      "bin" +
      File.separator +
      "java";
    String classpath = System.getProperty("java.class.path");
    System.out.println(classpath);
    //log.debug("classpath: " + classpath);
    // String workingDirectory = System.getProperty("user.dir");

    List<String> command = new ArrayList<String>();
    command.add(jvm);
    command.add("-cp");
    command.add(classpath);
    command.add(mainClass);
    command.addAll(Arrays.asList(arguments));
    System.out.println(command.stream().collect(Collectors.joining(" ")));
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    // Map< String, String > environment = processBuilder.environment();
    // environment.put("CLASSPATH", classpath);
    return processBuilder;
  }
}
