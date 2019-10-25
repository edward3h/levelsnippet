package org.ethelred.levelsnippet;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import org.ethelred.args4jboilerplate.Args4jBoilerplate;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class App extends Args4jBoilerplate
{

    @Option(name = "--world-name", aliases = {"-w", "--level-name"}, usage = "Displayable name for world")
    private String worldName;

    @Option(name = "--world-path", aliases = {"-p"}, usage = "web path for world")
    private String worldPath;

    @Argument(required = true, metaVar = "LEVEL_FILE", usage = "level.dat file")
    private File levelFile;

    public App(String[] args)
    {
        parseArgs(args);
    }

    public static void main(String[] args) {
        new App(args).run();
    }

    private void run()
    {
        try
        {
            Tag<?> root = NBTUtil.readTag(levelFile, NBTUtil.FileOptions.builder()
                    .compressionOption(NBTUtil.CompressionOption.NONE)
                    .isLittleEndian(true)
                    .headerReader(dis -> {
                        try
                        {
                            // skip past version and length headers
                            dis.readInt();
                            dis.readInt();
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException(e);
                        }
                    })
                    .build());

            if (!(root instanceof CompoundTag))
            {
                System.err.println("Expected CompoundTag in level file");
                System.exit(2);
            }

            CompoundTag compoundTag = (CompoundTag) root;
            if (worldName == null)
            {
                worldName = compoundTag.getString("LevelName");
            }

            if (worldPath == null)
            {
                worldPath = worldName.replaceAll("[^A-Za-z0-9_]", "");
            }

            Writer w = new OutputStreamWriter(System.out);
            tag("li",
                div(
                    tag("a", Map.of("href", worldPath + "/map/"), t(worldName))
                ),
                div(span(t("Seed")), span(t(compoundTag.getLong("RandomSeed")))),
                div(span(t("Type")), span(t(compoundTag.getInt("GameType")))),
                div(span(t("Difficulty")), span(t(compoundTag.getInt("Difficulty"))))
            ).accept(w);

            w.flush();
            w.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    HtmlWriter t(Object text)
    {
        return buf -> buf.append(Objects.toString(text));
    }

    HtmlWriter tag(String name, Map<String, String> attributes, HtmlWriter... inner)
    {
        return (buf) -> {
            buf.append("<").append(name);
            for (Map.Entry<String, String> entry : attributes.entrySet())
            {
                String k = entry.getKey();
                String v = entry.getValue();
                buf.append(" ").append(k).append("=\"").append(v).append("\"");
            }
            buf.append(">");
            for(HtmlWriter i: inner)
            {
                i.accept(buf);
            }
            buf.append("</").append(name).append(">");
        };
    }


    HtmlWriter tag(String name, HtmlWriter... inner)
    {
        return tag(name, Map.of(), inner);
    }

    HtmlWriter div(HtmlWriter... inner)
    {
        return tag("div", inner);
    }

    HtmlWriter span(HtmlWriter... inner)
    {
        return tag("span", inner);
    }
    
    interface HtmlWriter {
        void accept(Writer writer) throws IOException;
    }
}
