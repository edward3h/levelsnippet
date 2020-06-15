package org.ethelred.levelsnippet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;
import java.util.Objects;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import org.ethelred.args4jboilerplate.Args4jBoilerplate;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class App extends Args4jBoilerplate {
  @Option(
    name = "--world-name",
    aliases = { "-w", "--level-name" },
    usage = "Displayable name for world"
  )
  private String worldName;

  @Option(
    name = "--world-path",
    aliases = { "-p" },
    usage = "web path for world"
  )
  private String worldPath;

  @Option(name = "--base-path", usage = "root path of site on local disk")
  private Path basePath;

  @Argument(required = true, metaVar = "LEVEL_FILE", usage = "level.dat file")
  private Path levelFile;

  public App(String[] args) {
    parseArgs(args);
  }

  public static void main(String[] args) {
    new App(args).run();
  }

  private void run() {
    try {
      Tag<?> root = NBTUtil.readTag(
        levelFile.toFile(),
        NBTUtil
          .FileOptions.builder()
          .compressionOption(NBTUtil.CompressionOption.NONE)
          .isLittleEndian(true)
          .headerReader(
            dis -> {
              try {
                // skip past version and length headers
                dis.readInt();
                dis.readInt();
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          )
          .build()
      );

      if (!(root instanceof CompoundTag)) {
        System.err.println("Expected CompoundTag in level file");
        System.exit(2);
      }

      CompoundTag compoundTag = (CompoundTag) root;
      if (worldName == null) {
        worldName = compoundTag.getString("LevelName");
      }

      if (worldPath == null) {
        worldPath = worldName.replaceAll("[^A-Za-z0-9_]", "");
      }

      if (basePath == null) {
        basePath = Path.of(System.getProperty("user.dir"));
      }

      String papyrusPath = checkPath("map");
      String brvPath = checkPath("index");
      String defaultMap = papyrusPath == null ? brvPath : papyrusPath;
      Writer w = new OutputStreamWriter(System.out);
      tag(
          "li",
          div(
            defaultMap == null
              ? span(t(worldName))
              : a(worldPath + "/" + defaultMap + "/", t(worldName))
          ),
          div(span(t("Seed")), span(t(compoundTag.getLong("RandomSeed")))),
          div(span(t("Type")), span(t(_gameType(compoundTag)))),
          div(span(t("Difficulty")), span(t(_difficulty(compoundTag)))),
          div(span(t("Last Played")), span(t(_lastPlayed(compoundTag)))),
          ifNotNull(
            papyrusPath,
            div(a(worldPath + "/" + papyrusPath + "/", t("Papyrus Map")))
          ),
          ifNotNull(
            brvPath,
            div(a(worldPath + "/" + brvPath + "/", t("Bedrock-viz Map")))
          )
        )
        .accept(w);

      w.flush();
      w.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String checkPath(String mapDirName) {
    Path dir = basePath.resolve(worldPath).resolve(mapDirName);
    return Files.isDirectory(dir) ? mapDirName : null;
  }

  private String _lastPlayed(CompoundTag compoundTag) {
    long epoch = compoundTag.getLong("LastPlayed");
    Instant instant = Instant.ofEpochSecond(epoch);
    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(
      FormatStyle.MEDIUM
    );
    LocalDateTime localDateTime = instant
      .atZone(ZoneId.systemDefault())
      .toLocalDateTime();
    return localDateTime.format(formatter);
  }

  private String _gameType(CompoundTag compoundTag) {
    switch (compoundTag.getInt("GameType")) {
      case 1:
        return "Creative";
      case 0:
        return "Survival";
      default:
        return "Unknown";
    }
  }

  private String _difficulty(CompoundTag compoundTag) {
    switch (compoundTag.getInt("Difficulty")) {
      case 0:
        return "Peaceful";
      case 1:
        return "Easy";
      case 2:
        return "Normal";
      case 3:
        return "Hard";
      default:
        return "Unknown";
    }
  }

  HtmlWriter t(Object text) {
    return buf -> buf.append(Objects.toString(text));
  }

  HtmlWriter tag(
    String name,
    Map<String, String> attributes,
    HtmlWriter... inner
  ) {
    return buf -> {
      buf.append("<").append(name);
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        String k = entry.getKey();
        String v = entry.getValue();
        buf.append(" ").append(k).append("=\"").append(v).append("\"");
      }
      buf.append(">");
      for (HtmlWriter i : inner) {
        i.accept(buf);
      }
      buf.append("</").append(name).append(">");
    };
  }

  HtmlWriter ifNotNull(Object obj, HtmlWriter inner) {
    return obj == null
      ? buf -> {
        return;
      }
      : buf -> inner.accept(buf);
  }

  HtmlWriter a(String href, HtmlWriter... inner) {
    return tag("a", Map.of("href", href), inner);
  }

  HtmlWriter tag(String name, HtmlWriter... inner) {
    return tag(name, Map.of(), inner);
  }

  HtmlWriter div(HtmlWriter... inner) {
    return tag("div", inner);
  }

  HtmlWriter span(HtmlWriter... inner) {
    return tag("span", inner);
  }

  interface HtmlWriter {
    void accept(Writer writer) throws IOException;
  }
}
